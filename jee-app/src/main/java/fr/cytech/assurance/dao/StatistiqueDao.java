package fr.cytech.assurance.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.model.enums.TypeVersement;


public class StatistiqueDao extends GenericDao<Object> {

    public StatistiqueDao() {
        super(Object.class);
    }

    public int compterTotalClients() {
        return enLecture(em -> em.createQuery("SELECT COUNT(c) FROM Client c", Long.class)
                .getSingleResult()
                .intValue());
    }

    public int compterNouveauxClients(LocalDateTime depuis) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Client c WHERE c.utilisateur.dateCreation >= :depuis", Long.class)
                .setParameter("depuis", depuis)
                .getSingleResult()
                .intValue());
    }

    public List<Object[]> repartitionClientsParType() {
        return enLecture(em -> em.createQuery(
                        "SELECT c.typeAssurance.nom, COUNT( DISTINCT c.client.id) FROM Contrat c GROUP BY c.typeAssurance.nom",
                        Object[].class)
                .getResultList());
    }

    public int compterContratsParStatut(StatutContrat statut) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Contrat c WHERE c.statut = :statut", Long.class)
                .setParameter("statut", statut)
                .getSingleResult()
                .intValue());
    }

    public List<Object[]> evolutionContratsParMois() {
        return enLecture(em -> em.createQuery(
                        "SELECT YEAR(c.dateDebut), MONTH(c.dateDebut), COUNT(c) FROM Contrat c"
                                + " GROUP BY YEAR(c.dateDebut), MONTH(c.dateDebut)"
                                + " ORDER BY YEAR(c.dateDebut), MONTH(c.dateDebut)",
                        Object[].class)
                .getResultList());
    } 

    public BigDecimal calculerChiffreAffaires(){
        return enLecture(em -> em.createQuery(
                        "SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.statut = :statut",
                        BigDecimal.class)
                .setParameter("statut", StatutPaiement.EFFECTUE)
                .getSingleResult());
    }

    public BigDecimal calculerTotalPrimes(){
        return enLecture(em -> em.createQuery(
                        "SELECT COALESCE(SUM(c.prime), 0) FROM Contrat c WHERE c.statut = :statut",
                        BigDecimal.class)
                .setParameter("statut", StatutContrat.ACTIF)
                .getSingleResult());
    }

    public BigDecimal calculerTotalIndemnisations(){
        return enLecture(em -> em.createQuery(
                        "SELECT COALESCE(SUM(v.montant), 0) FROM Versement v WHERE v.type = :type",
                        BigDecimal.class)
                .setParameter("type", TypeVersement.INDEMNISATION)
                .getSingleResult());
    }

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

    
}