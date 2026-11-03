package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.TypeAssurance;

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

    // Une description vide est enregistrée comme absente (NULL en base)
    private String descriptionOuNull(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
