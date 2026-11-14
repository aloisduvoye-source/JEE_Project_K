package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.model.enums.TypeVersement;
import fr.cytech.assurance.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Requêtes d'agrégation (COUNT, SUM, GROUP BY) de la page Statistiques.
 * <p>
 * N'hérite pas de GenericDao : elle ne gère aucune entité, seulement des calculs en lecture
 * sur les clients, contrats, paiements et versements.
 */
public class StatistiqueDao {

    /**
     * Nombre total de clients.
     *
     * @return le nombre de fiches client
     */
    public int compterTotalClients() {
        return enLecture(em -> em.createQuery("SELECT COUNT(c) FROM Client c", Long.class)
                .getSingleResult()
                .intValue());
    }

    /**
     * Nombre de clients dont le compte a été créé depuis une date.
     *
     * @param depuis date et heure de début (incluse)
     * @return le nombre de nouveaux clients
     */
    public int compterNouveauxClients(LocalDateTime depuis) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Client c WHERE c.utilisateur.dateCreation >= :depuis", Long.class)
                .setParameter("depuis", depuis)
                .getSingleResult()
                .intValue());
    }

    /**
     * Nombre de clients ayant au moins un contrat actif, pour chaque type d'assurance.
     * Un client qui a deux contrats actifs du même type n'est compté qu'une fois.
     *
     * @return lignes {nom du type (String), nombre de clients (Long)}, triées par nom
     */
    public List<Object[]> repartitionClientsParType() {
        return enLecture(em -> em.createQuery(
                        "SELECT c.typeAssurance.nom, COUNT(DISTINCT c.client.id) FROM Contrat c"
                                + " WHERE c.statut = :statut"
                                + " GROUP BY c.typeAssurance.nom"
                                + " ORDER BY c.typeAssurance.nom",
                        Object[].class)
                .setParameter("statut", StatutContrat.ACTIF)
                .getResultList());
    }

    /**
     * Nombre de contrats ayant un statut.
     *
     * @param statut statut des contrats à compter
     * @return le nombre de contrats
     */
    public int compterContratsParStatut(StatutContrat statut) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Contrat c WHERE c.statut = :statut", Long.class)
                .setParameter("statut", statut)
                .getSingleResult()
                .intValue());
    }

    /**
     * Périodes de tous les contrats, pour savoir combien étaient actifs à une date donnée (calcul dans StatistiqueService).
     *
     * @return lignes {date de début, date de fin, date de résiliation, date de suspension, date du premier paiement
     *         effectué} (LocalDate, les trois dernières peuvent être null), triées par date de début
     */
    public List<Object[]> periodesContrats() {
        return enLecture(em -> em.createQuery(
                        "SELECT c.dateDebut, c.dateFin, c.dateResiliation, c.dateSuspension,"
                                + " (SELECT MIN(p.datePaiement) FROM Paiement p"
                                + "  WHERE p.contrat = c AND p.statut = :effectue)"
                                + " FROM Contrat c ORDER BY c.dateDebut",
                        Object[].class)
                .setParameter("effectue", StatutPaiement.EFFECTUE)
                .getResultList());
    }

    /**
     * Chiffre d'affaires encaissé : somme des paiements effectués.
     * Les remboursements versés à la résiliation ne sont pas déduits.
     *
     * @return le montant total, 0 s'il n'y a aucun paiement effectué
     */
    public BigDecimal calculerChiffreAffaires() {
        return enLecture(em -> em.createQuery(
                        "SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.statut = :statut",
                        BigDecimal.class)
                .setParameter("statut", StatutPaiement.EFFECTUE)
                .getSingleResult());
    }

    /**
     * Somme des primes annuelles des contrats actifs.
     *
     * @return le montant total, 0 s'il n'y a aucun contrat actif
     */
    public BigDecimal calculerTotalPrimes() {
        return enLecture(em -> em.createQuery(
                        "SELECT COALESCE(SUM(c.prime), 0) FROM Contrat c WHERE c.statut = :statut",
                        BigDecimal.class)
                .setParameter("statut", StatutContrat.ACTIF)
                .getSingleResult());
    }

    /**
     * Somme des indemnisations de sinistres versées (hors remboursements).
     *
     * @return le montant total, 0 s'il n'y a aucune indemnisation
     */
    public BigDecimal calculerTotalIndemnisations() {
        return calculerTotalVersements(TypeVersement.INDEMNISATION);
    }

    /**
     * Somme des remboursements de prime versés à la résiliation des contrats.
     *
     * @return le montant total, 0 s'il n'y a aucun remboursement
     */
    public BigDecimal calculerTotalRemboursements() {
        return calculerTotalVersements(TypeVersement.REMBOURSEMENT);
    }

    private BigDecimal calculerTotalVersements(TypeVersement type) {
        return enLecture(em -> em.createQuery(
                        "SELECT COALESCE(SUM(v.montant), 0) FROM Versement v WHERE v.type = :type",
                        BigDecimal.class)
                .setParameter("type", type)
                .getSingleResult());
    }

    /**
     * Montant des paiements effectués, par mois de paiement.
     * Les mois sans paiement sont absents (complétés par StatistiqueService).
     *
     * @return lignes {année (Integer), mois (Integer), montant (BigDecimal)}, de la plus ancienne à la plus récente
     */
    public List<Object[]> evolutionRevenusParMois() {
        return enLecture(em -> em.createQuery(
                        "SELECT YEAR(p.datePaiement), MONTH(p.datePaiement), COALESCE(SUM(p.montant), 0)"
                                + " FROM Paiement p WHERE p.statut = :statut"
                                + " GROUP BY YEAR(p.datePaiement), MONTH(p.datePaiement)"
                                + " ORDER BY YEAR(p.datePaiement), MONTH(p.datePaiement)",
                        Object[].class)
                .setParameter("statut", StatutPaiement.EFFECTUE)
                .getResultList());
    }

    // Lecture seule avec un EntityManager fermé à la fin, comme GenericDao.enLecture
    private <R> R enLecture(Function<EntityManager, R> operation) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return operation.apply(em);
        }
    }
}
