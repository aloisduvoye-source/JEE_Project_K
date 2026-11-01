package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.model.enums.StatutPaiement;

import java.time.LocalDate;
import java.util.List;

/**
 * Accès en base aux paiements (échéances) des contrats.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class PaiementDao extends GenericDao<Paiement> {

    public PaiementDao() {
        super(Paiement.class);
    }

    /**
     * Liste les échéances d'un contrat, de la plus ancienne à la plus récente.
     *
     * @param idContrat identifiant du contrat
     * @return les paiements du contrat, liste vide s'il n'en a aucun
     */
    public List<Paiement> listerParContrat(Long idContrat) {
        return enLecture(em -> em.createQuery(
                        "SELECT p FROM Paiement p WHERE p.contrat.id = :idContrat ORDER BY p.dateEcheance",
                        Paiement.class)
                .setParameter("idContrat", idContrat)
                .getResultList());
    }

    /**
     * Liste les échéances de tous les contrats d'un client, de la plus récente à la plus ancienne.
     * <p>
     * Le contrat et son type sont déjà chargés (lisibles dans une JSP).
     *
     * @param idClient identifiant du client
     * @return les paiements du client, liste vide s'il n'en a aucun
     */
    public List<Paiement> listerParClient(Long idClient) {
        return enLecture(em -> em.createQuery(
                        "SELECT p FROM Paiement p JOIN FETCH p.contrat c JOIN FETCH c.typeAssurance"
                                + " WHERE c.client.id = :idClient ORDER BY p.dateEcheance DESC", Paiement.class)
                .setParameter("idClient", idClient)
                .getResultList());
    }

    /**
     * Liste les paiements en retard, du plus ancien au plus récent.
     * <p>
     * Le contrat et son client sont déjà chargés, pour permettre à un agent de relancer le client.
     *
     * @return les paiements au statut EN_RETARD, liste vide s'il n'y en a aucun
     */
    public List<Paiement> listerEnRetard() {
        return enLecture(em -> em.createQuery(
                        "SELECT p FROM Paiement p"
                                + " JOIN FETCH p.contrat c JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
                                + " WHERE p.statut = :statut ORDER BY p.dateEcheance", Paiement.class)
                .setParameter("statut", StatutPaiement.EN_RETARD)
                .getResultList());
    }

    /**
     * Passe au statut EN_RETARD les paiements en attente dont l'échéance est dépassée.
     * <p>
     * À appeler régulièrement (au démarrage de l'application, ou avant d'afficher les retards).
     *
     * @param aujourdhui date du jour : les échéances strictement antérieures sont en retard
     * @return le nombre de paiements passés en retard
     */
    public int marquerEnRetard(LocalDate aujourdhui) {
        return enTransaction(em -> em.createQuery(
                        "UPDATE Paiement p SET p.statut = :enRetard"
                                + " WHERE p.statut = :enAttente AND p.dateEcheance < :aujourdhui")
                .setParameter("enRetard", StatutPaiement.EN_RETARD)
                .setParameter("enAttente", StatutPaiement.EN_ATTENTE)
                .setParameter("aujourdhui", aujourdhui)
                .executeUpdate());
    }

    /**
     * Enregistre le règlement d'une échéance et la passe au statut EFFECTUE.
     *
     * @param id           identifiant du paiement
     * @param datePaiement date du règlement
     * @param mode         moyen de paiement utilisé
     * @return {@code false} si aucun paiement n'a cet id
     */
    public boolean enregistrerPaiement(Long id, LocalDate datePaiement, ModePaiement mode) {
        return mettreAJour(id, p -> {
            p.setDatePaiement(datePaiement);
            p.setModePaiement(mode);
            p.setStatut(StatutPaiement.EFFECTUE);
        });
    }

    /**
     * Annule une échéance (par exemple après la résiliation du contrat).
     *
     * @param id identifiant du paiement
     * @return {@code false} si aucun paiement n'a cet id
     */
    public boolean annuler(Long id) {
        return mettreAJour(id, p -> p.setStatut(StatutPaiement.ANNULE));
    }
}
