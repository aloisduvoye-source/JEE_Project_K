package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.GarantieDao;
import fr.cytech.assurance.exception.GarantieInvalideException;
import fr.cytech.assurance.model.Garantie;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.util.List;

// Règles et lectures concernant le catalogue des garanties
// Les servlets vérifient le format des saisies (Validateur), ce service applique les règles métier
public class GarantieService {

    private final GarantieDao garantieDao = new GarantieDao();

    // Garanties triées par nom
    public List<Garantie> lister() {
        return garantieDao.listerParNom();
    }

    // Garantie d'après son id, ou null
    public Garantie trouver(Long id) {
        return garantieDao.trouverParId(id);
    }

    // Crée une garantie dans le référentiel. Le plafond et la franchise sont facultatifs :
    // null signifie « non précisé ». Lève GarantieInvalideException si le nom est déjà pris
    // ou si un montant renseigné est négatif
    public Garantie creer(String nom, String description, BigDecimal plafond, BigDecimal franchise) {
        verifierNomDisponible(nom, null);
        verifierMontants(plafond, franchise);

        Garantie garantie = new Garantie();
        garantie.setNom(nom);
        garantie.setDescription(descriptionOuNull(description));
        garantie.setPlafond(plafond);
        garantie.setFranchise(franchise);
        return garantieDao.creer(garantie);
    }

    // Modifie le nom, la description, le plafond et la franchise.
    // Lève GarantieInvalideException si la garantie n'existe pas, si le nom est déjà pris
    // ou si un montant renseigné est négatif
    public void modifier(Long id, String nom, String description, BigDecimal plafond, BigDecimal franchise) {
        verifierNomDisponible(nom, id);
        verifierMontants(plafond, franchise);

        // modifierInfos et non modifier (merge) : seuls les champs voulus sont enregistrés
        if (!garantieDao.modifierInfos(id, nom, descriptionOuNull(description), plafond, franchise)) {
            throw garantieIntrouvable();
        }
    }

    // Supprime une garantie du référentiel. Les liens vers les types d'assurance sont déliés,
    // mais une garantie souscrite par au moins un contrat est conservée : les contrats la référencent.
    // Lève GarantieInvalideException si la garantie n'existe pas ou est souscrite par un contrat
    public void supprimer(Long id) {
        if (garantieDao.trouverParId(id) == null) {
            throw garantieIntrouvable();
        }
        if (garantieDao.estUtiliseeParContrat(id)) {
            throw garantieSouscrite();
        }
        try {
            garantieDao.supprimer(id);
        } catch (PersistenceException e) {
            // Un contrat a souscrit cette garantie entre la vérification et la suppression :
            // la clé étrangère de la table contrat_garantie refuse la suppression
            throw garantieSouscrite();
        }
    }

    // idExclu : id de la garantie en cours de modification (elle peut garder son nom), null à la création
    private void verifierNomDisponible(String nom, Long idExclu) {
        if (garantieDao.existeAvecNom(nom, idExclu)) {
            throw new GarantieInvalideException("Une garantie nommée « " + nom + " » existe déjà.");
        }
    }

    // Le schéma accepte des montants absents (NULL) mais jamais négatifs
    private void verifierMontants(BigDecimal plafond, BigDecimal franchise) {
        if (plafond != null && plafond.signum() < 0) {
            throw new GarantieInvalideException("Le plafond ne peut pas être négatif.");
        }
        if (franchise != null && franchise.signum() < 0) {
            throw new GarantieInvalideException("La franchise ne peut pas être négative.");
        }
    }

    private GarantieInvalideException garantieIntrouvable() {
        return new GarantieInvalideException("Cette garantie n'existe pas.");
    }

    private GarantieInvalideException garantieSouscrite() {
        return new GarantieInvalideException(
                "Cette garantie est souscrite par au moins un contrat : elle ne peut pas être supprimée.");
    }

    // Une description vide est enregistrée comme absente (NULL en base)
    private String descriptionOuNull(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
