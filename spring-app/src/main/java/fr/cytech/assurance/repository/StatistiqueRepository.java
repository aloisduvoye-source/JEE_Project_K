package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.model.enums.TypeVersement;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Requêtes de statistiques sur plusieurs tables (remplace StatistiqueDao).
 * Pas de JpaRepository : ces requêtes ne portent pas sur une seule entité, l'EntityManager est injecté par Spring.
 */
@Repository
public class StatistiqueRepository {

    @PersistenceContext
    private EntityManager em;

    /** @return le nombre total de clients */
    public int compterTotalClients() {
        return em.createQuery("SELECT COUNT(c) FROM Client c", Long.class).getSingleResult().intValue();
    }

    /** @return le nombre de clients dont le compte a été créé depuis cette date */
    public int compterNouveauxClients(LocalDateTime depuis) {
        return em.createQuery("SELECT COUNT(c) FROM Client c WHERE c.utilisateur.dateCreation >= :depuis", Long.class)
                .setParameter("depuis", depuis)
                .getSingleResult()
                .intValue();
    }

    /** @return lignes [nom du type, nombre de clients] pour les contrats actifs, triées par nom de type */
    public List<Object[]> repartitionClientsParType() {
        return em.createQuery(
                        "SELECT c.typeAssurance.nom, COUNT(DISTINCT c.client.id) FROM Contrat c"
                                + " WHERE c.statut = :statut"
                                + " GROUP BY c.typeAssurance.nom"
                                + " ORDER BY c.typeAssurance.nom", Object[].class)
                .setParameter("statut", StatutContrat.ACTIF)
                .getResultList();
    }

    /** @return le nombre de contrats de ce statut */
    public int compterContratsParStatut(StatutContrat statut) {
        return em.createQuery("SELECT COUNT(c) FROM Contrat c WHERE c.statut = :statut", Long.class)
                .setParameter("statut", statut)
                .getSingleResult()
                .intValue();
    }

    /** @return lignes [début, fin, résiliation, suspension, premier paiement effectué] de chaque contrat, par date de début */
    public List<Object[]> periodesContrats() {
        return em.createQuery(
                        "SELECT c.dateDebut, c.dateFin, c.dateResiliation, c.dateSuspension,"
                                + " (SELECT MIN(p.datePaiement) FROM Paiement p"
                                + "  WHERE p.contrat = c AND p.statut = :effectue)"
                                + " FROM Contrat c ORDER BY c.dateDebut", Object[].class)
                .setParameter("effectue", StatutPaiement.EFFECTUE)
                .getResultList();
    }

    /** @return la somme des paiements effectués */
    public BigDecimal calculerChiffreAffaires() {
        return em.createQuery("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.statut = :statut",
                        BigDecimal.class)
                .setParameter("statut", StatutPaiement.EFFECTUE)
                .getSingleResult();
    }

    /** @return la somme des primes des contrats actifs */
    public BigDecimal calculerTotalPrimes() {
        return em.createQuery("SELECT COALESCE(SUM(c.prime), 0) FROM Contrat c WHERE c.statut = :statut",
                        BigDecimal.class)
                .setParameter("statut", StatutContrat.ACTIF)
                .getSingleResult();
    }

    /** @return la somme des indemnisations versées */
    public BigDecimal calculerTotalIndemnisations() {
        return calculerTotalVersements(TypeVersement.INDEMNISATION);
    }

    /** @return la somme des remboursements de prime versés */
    public BigDecimal calculerTotalRemboursements() {
        return calculerTotalVersements(TypeVersement.REMBOURSEMENT);
    }

    private BigDecimal calculerTotalVersements(TypeVersement type) {
        return em.createQuery("SELECT COALESCE(SUM(v.montant), 0) FROM Versement v WHERE v.type = :type",
                        BigDecimal.class)
                .setParameter("type", type)
                .getSingleResult();
    }

    /** @return lignes [année, mois, total des paiements effectués], par mois */
    public List<Object[]> evolutionRevenusParMois() {
        return em.createQuery(
                        "SELECT YEAR(p.datePaiement), MONTH(p.datePaiement), COALESCE(SUM(p.montant), 0)"
                                + " FROM Paiement p WHERE p.statut = :statut"
                                + " GROUP BY YEAR(p.datePaiement), MONTH(p.datePaiement)"
                                + " ORDER BY YEAR(p.datePaiement), MONTH(p.datePaiement)", Object[].class)
                .setParameter("statut", StatutPaiement.EFFECTUE)
                .getResultList();
    }

    /** @return lignes [année, mois, total des versements de ce type], par mois */
    public List<Object[]> evolutionVersementsParMois(TypeVersement type) {
        return em.createQuery(
                        "SELECT YEAR(v.dateVersement), MONTH(v.dateVersement), COALESCE(SUM(v.montant), 0)"
                                + " FROM Versement v WHERE v.type = :type"
                                + " GROUP BY YEAR(v.dateVersement), MONTH(v.dateVersement)"
                                + " ORDER BY YEAR(v.dateVersement), MONTH(v.dateVersement)", Object[].class)
                .setParameter("type", type)
                .getResultList();
    }
}
