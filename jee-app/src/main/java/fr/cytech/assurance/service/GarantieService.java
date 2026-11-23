package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.GarantieDao;
import fr.cytech.assurance.exception.GarantieInvalideException;
import fr.cytech.assurance.model.Garantie;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Règles et lectures du catalogue des garanties.
 * Les servlets vérifient le format des saisies (Validateur), ce service applique les règles métier.
 */
public class GarantieService {

    private final GarantieDao garantieDao = new GarantieDao();

    /**
     * @return les garanties triées par nom
     */
    public List<Garantie> lister() {
        return garantieDao.listerParNom();
    }

    /**
     * @return la garantie de cet id, ou null
     */
    public Garantie trouver(Long id) {
        return garantieDao.trouverParId(id);
    }

    /**
     * Crée une garantie. Plafond et franchise sont facultatifs (null : non précisé).
     *
     * @throws GarantieInvalideException si le nom est déjà pris ou si un montant est négatif
     */
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

    /**
     * Modifie le nom, la description, le plafond et la franchise.
     *
     * @throws GarantieInvalideException si la garantie n'existe pas, si le nom est déjà pris ou si un montant est négatif
     */
    public void modifier(Long id, String nom, String description, BigDecimal plafond, BigDecimal franchise) {
        verifierNomDisponible(nom, id);
        verifierMontants(plafond, franchise);

        // modifierInfos et non modifier (merge) : seuls les champs voulus sont enregistrés
        if (!garantieDao.modifierInfos(id, nom, descriptionOuNull(description), plafond, franchise)) {
            throw garantieIntrouvable();
        }
    }

    /**
     * Supprime une garantie et la retire des types d'assurance qui la proposent.
     *
     * @throws GarantieInvalideException si la garantie n'existe pas ou est souscrite par un contrat
     */
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

    /**
     * @param idExclu garantie en cours de modification (elle peut garder son nom), null à la création
     * @throws GarantieInvalideException si une autre garantie porte déjà ce nom
     */
    private void verifierNomDisponible(String nom, Long idExclu) {
        if (garantieDao.existeAvecNom(nom, idExclu)) {
            throw new GarantieInvalideException("Une garantie nommée « " + nom + " » existe déjà.");
        }
    }

    /**
     * Montants absents acceptés, jamais négatifs.
     *
     * @throws GarantieInvalideException si le plafond ou la franchise est négatif
     */
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

    /**
     * @return la description, ou null si elle est vide (NULL en base)
     */
    private String descriptionOuNull(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
