package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.GarantieDao;
import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

// Règles et lectures concernant le catalogue des types d'assurance
// Les servlets vérifient le format des saisies (Validateur), ce service applique les règles métier
public class TypeAssuranceService {

    private final TypeAssuranceDao typeAssuranceDao = new TypeAssuranceDao();
    private final GarantieDao garantieDao = new GarantieDao();

    // Types d'assurance triés par nom
    public List<TypeAssurance> listerTous() {
        return typeAssuranceDao.listerParNom();
    }

    // Types non archivés triés par nom : ceux que l'on peut proposer pour un nouveau contrat
    public List<TypeAssurance> listerActifs() {
        return typeAssuranceDao.listerActifs();
    }

    // Types triés par nom, avec leurs garanties disponibles déjà chargées (lisibles dans une JSP)
    public List<TypeAssurance> listerAvecGaranties() {
        return typeAssuranceDao.listerAvecGaranties();
    }

    // Type avec ses garanties disponibles, ou null
    public TypeAssurance trouverAvecGaranties(Long id) {
        return typeAssuranceDao.trouverAvecGaranties(id);
    }

    // Toutes les garanties existantes triées par nom, pour choisir celles qu'un type propose
    public List<Garantie> listerGaranties() {
        return garantieDao.listerParNom();
    }

    // Crée un type avec les garanties qu'il propose, en une seule opération
    // Lève TypeAssuranceInvalideException si le nom est déjà pris, le prix incorrect,
    // aucune garantie choisie (un contrat en exige au moins une) ou une garantie inexistante
    public TypeAssurance creer(String nom, String description, BigDecimal prixBase, Set<Long> idsGaranties) {
        verifierNomDisponible(nom, null);
        verifierPrixBase(prixBase);
        if (idsGaranties == null || idsGaranties.isEmpty()) {
            throw new TypeAssuranceInvalideException("Choisissez au moins une garantie.");
        }

        TypeAssurance type = new TypeAssurance();
        type.setNom(nom);
        type.setDescription(descriptionOuNull(description));
        type.setPrixBase(prixBase);
        for (Long idGarantie : idsGaranties) {
            Garantie garantie = garantieDao.trouverParId(idGarantie);
            if (garantie == null) {
                throw new TypeAssuranceInvalideException("Une des garanties choisies n'existe pas.");
            }
            type.getGaranties().add(garantie);
        }
        // Le type et ses lignes dans type_assurance_garantie sont enregistrés dans la même transaction
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

    // Propose une garantie existante avec ce type (sans effet si elle l'était déjà)
    // Lève TypeAssuranceInvalideException si le type ou la garantie n'existe pas
    public void ajouterGarantie(Long idType, Long idGarantie) {
        if (!typeAssuranceDao.ajouterGarantie(idType, idGarantie)) {
            throw new TypeAssuranceInvalideException("Ce type d'assurance ou cette garantie n'existe pas.");
        }
    }

    // Ne propose plus cette garantie pour les nouveaux contrats ; les contrats existants la conservent
    // Lève TypeAssuranceInvalideException si le type n'existe pas, si la garantie n'est pas proposée
    // ou si c'est la dernière (un contrat exige au moins une garantie : le type ne serait plus souscriptible)
    public void retirerGarantie(Long idType, Long idGarantie) {
        TypeAssurance type = typeAssuranceDao.trouverAvecGaranties(idType);
        if (type == null) {
            throw typeIntrouvable();
        }
        boolean proposee = type.getGaranties().stream().anyMatch(g -> g.getId().equals(idGarantie));
        if (!proposee) {
            throw new TypeAssuranceInvalideException("Cette garantie n'est pas proposée pour ce type d'assurance.");
        }
        if (type.getGaranties().size() == 1) {
            throw new TypeAssuranceInvalideException(
                    "Un type d'assurance doit proposer au moins une garantie : celle-ci ne peut pas être retirée.");
        }
        typeAssuranceDao.retirerGarantie(idType, idGarantie);
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
