package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.TypeAssurance;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.util.List;

// Règles et lectures concernant le catalogue des types d'assurance
// Les servlets vérifient le format des saisies (Validateur), ce service applique les règles métier
public class TypeAssuranceService {

    private final TypeAssuranceDao typeAssuranceDao = new TypeAssuranceDao();

    // Types d'assurance triés par nom
    public List<TypeAssurance> listerTous() {
        return typeAssuranceDao.listerParNom();
    }

    // Type avec ses garanties disponibles, ou null
    public TypeAssurance trouverAvecGaranties(Long id) {
        return typeAssuranceDao.trouverAvecGaranties(id);
    }

    // Crée un type sans garantie : elles sont ajoutées ensuite avec ajouterGarantie
    // Lève TypeAssuranceInvalideException si le nom est déjà pris ou le prix incorrect
    public TypeAssurance creer(String nom, String description, BigDecimal prixBase) {
        verifierNomDisponible(nom, null);
        verifierPrixBase(prixBase);

        TypeAssurance type = new TypeAssurance();
        type.setNom(nom);
        type.setDescription(descriptionOuNull(description));
        type.setPrixBase(prixBase);
        return typeAssuranceDao.creer(type);
    }

    // Modifie le nom, la description et le prix de base ; les garanties proposées ne changent pas
    // Les contrats existants gardent leur prime : le prix de base ne compte que pour les nouveaux contrats
    // Lève TypeAssuranceInvalideException si le type n'existe pas, le nom est pris ou le prix incorrect
    public void modifier(Long id, String nom, String description, BigDecimal prixBase) {
        verifierNomDisponible(nom, id);
        verifierPrixBase(prixBase);

        // modifierInfos et non modifier (merge) : merge effacerait les garanties non chargées
        if (!typeAssuranceDao.modifierInfos(id, nom, descriptionOuNull(description), prixBase)) {
            throw typeIntrouvable();
        }
    }

    // Supprime un type et ses liens vers les garanties (les garanties elles-mêmes sont conservées)
    // Lève TypeAssuranceInvalideException si le type n'existe pas ou si un contrat l'utilise
    public void supprimer(Long id) {
        if (typeAssuranceDao.trouverParId(id) == null) {
            throw typeIntrouvable();
        }
        if (typeAssuranceDao.estUtiliseParContrat(id)) {
            throw typeUtiliseParContrat();
        }
        try {
            typeAssuranceDao.supprimer(id);
        } catch (PersistenceException e) {
            // Un contrat a été créé avec ce type entre la vérification et la suppression :
            // la clé étrangère de la table contrat refuse la suppression
            throw typeUtiliseParContrat();
        }
    }

    // idExclu : id du type en cours de modification (il peut garder son nom), null à la création
    private void verifierNomDisponible(String nom, Long idExclu) {
        if (typeAssuranceDao.existeAvecNom(nom, idExclu)) {
            throw new TypeAssuranceInvalideException("Un type d'assurance nommé « " + nom + " » existe déjà.");
        }
    }

    // La prime d'un contrat doit être > 0 et au moins égale au prix de base : un prix nul n'a pas de sens
    private void verifierPrixBase(BigDecimal prixBase) {
        if (prixBase == null || prixBase.signum() <= 0) {
            throw new TypeAssuranceInvalideException("Le prix de base doit être supérieur à 0.");
        }
    }

    private TypeAssuranceInvalideException typeIntrouvable() {
        return new TypeAssuranceInvalideException("Ce type d'assurance n'existe pas.");
    }

    private TypeAssuranceInvalideException typeUtiliseParContrat() {
        return new TypeAssuranceInvalideException(
                "Ce type d'assurance est utilisé par au moins un contrat : il ne peut pas être supprimé.");
    }

    // Une description vide est enregistrée comme absente (NULL en base)
    private String descriptionOuNull(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
