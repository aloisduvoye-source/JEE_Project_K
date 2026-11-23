package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.StatistiqueDao;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.TypeVersement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcule les statistiques globales affichées sur la page Statistiques (pour les agents et l'administrateur) :
 * suivi des clients, des contrats et des indicateurs financiers.
 */
public class StatistiqueService {

    // Libellé d'un mois dans les évolutions : "09/2026"
    private static final DateTimeFormatter FORMAT_MOIS = DateTimeFormatter.ofPattern("MM/yyyy");

    private final StatistiqueDao statistiqueDao = new StatistiqueDao();

    /**
     * Compte le nombre total de clients enregistrés sur la plateforme.
     *
     * @return le nombre total de clients
     */
    public int compterTotalClients() {
        return statistiqueDao.compterTotalClients();
    }

    /**
     * Compte le nombre de nouveaux clients inscrits depuis le 1er jour du mois en cours.
     *
     * @return le nombre de nouveaux clients du mois
     */
    public int compterNouveauxClients() {
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return statistiqueDao.compterNouveauxClients(debutMois);
    }

    /**
     * Calcule la répartition des clients ayant au moins un contrat actif selon le type d'assurance.
     *
     * @return une liste de lignes contenant {nom du type d'assurance, nombre de clients actifs}
     */
    public List<Object[]> repartitionClientsParType() {
        return statistiqueDao.repartitionClientsParType();
    }

    /**
     * Compte le nombre de contrats actuellement actifs.
     *
     * @return le nombre de contrats actifs
     */
    public int compterContratsActifs() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.ACTIF);
    }

    /**
     * Compte le nombre de contrats expirés.
     *
     * @return le nombre de contrats expirés
     */
    public int compterContratsExpires() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.EXPIRE);
    }

    /**
     * Compte le nombre de contrats résiliés.
     *
     * @return le nombre de contrats résiliés
     */
    public int compterContratsResilies() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.RESILIE);
    }

    /**
     * Calcule l'évolution du nombre de contrats actifs mois par mois (au dernier jour de chaque mois,
     * ou à la date du jour pour le mois en cours), du premier contrat jusqu'à aujourd'hui.
     *
     * @return une liste de lignes {"MM/AAAA", nombre de contrats actifs (Long)}, de la plus ancienne à la plus récente
     */
    public List<Object[]> evolutionContratsParMois() {
        List<Object[]> periodes = statistiqueDao.periodesContrats();
        List<Object[]> resultat = new ArrayList<>();
        if (periodes.isEmpty()) {
            return resultat;
        }
        LocalDate aujourdhui = LocalDate.now();
        YearMonth dernier = YearMonth.from(aujourdhui);
        for (YearMonth mois = YearMonth.from((LocalDate) periodes.get(0)[0]); !mois.isAfter(dernier);
             mois = mois.plusMonths(1)) {
            LocalDate date = mois.equals(dernier) ? aujourdhui : mois.atEndOfMonth();
            long actifs = periodes.stream().filter(p -> actifLe(p, date)).count();
            resultat.add(new Object[]{mois.format(FORMAT_MOIS), actifs});
        }
        return resultat;
    }

    /**
     * Vérifie si un contrat était actif à une date donnée (commencé, non terminé, payé,
     * et ni résilié ni suspendu avant cette date).
     *
     * @param periode tableau contenant les dates du contrat {debut, fin, resiliation, suspension, paiement}
     * @param date    date à vérifier
     * @return true si le contrat était actif à cette date, false sinon
     */
    private static boolean actifLe(Object[] periode, LocalDate date) {
        LocalDate debut = (LocalDate) periode[0];
        LocalDate fin = (LocalDate) periode[1];
        LocalDate resiliation = (LocalDate) periode[2];
        LocalDate suspension = (LocalDate) periode[3];
        LocalDate paiement = (LocalDate) periode[4];
        return !date.isBefore(debut) && date.isBefore(fin)
                && paiement != null && !date.isBefore(paiement)
                && (resiliation == null || date.isBefore(resiliation))
                && (suspension == null || date.isBefore(suspension));
    }

    /**
     * Calcule le chiffre d'affaires brut (somme des paiements effectués, avant remboursements).
     *
     * @return le montant total des paiements encaissés
     */
    public BigDecimal calculerChiffreAffaires() {
        return statistiqueDao.calculerChiffreAffaires();
    }

    /**
     * Calcule la somme des primes annuelles de tous les contrats actuellement actifs.
     *
     * @return le montant total des primes actives
     */
    public BigDecimal calculerTotalPrimes() {
        return statistiqueDao.calculerTotalPrimes();
    }

    /**
     * Calcule le montant total des indemnisations versées aux clients suite à des sinistres.
     *
     * @return la somme des indemnisations versées
     */
    public BigDecimal calculerTotalIndemnisations() {
        return statistiqueDao.calculerTotalIndemnisations();
    }

    /**
     * Calcule le montant total des remboursements de prime versés lors des résiliations.
     *
     * @return la somme des remboursements versés
     */
    public BigDecimal calculerTotalRemboursements() {
        return statistiqueDao.calculerTotalRemboursements();
    }

    /**
     * Calcule le chiffre d'affaires net (paiements encaissés moins les remboursements versés).
     *
     * @return le chiffre d'affaires net
     */
    public BigDecimal calculerChiffreAffairesNet() {
        return calculerChiffreAffaires().subtract(calculerTotalRemboursements());
    }

    /**
     * Calcule le pourcentage des primes encaissées (chiffre d'affaires net) reversé en indemnisations.
     * Au-dessus de 100 %, l'assurance a versé plus qu'elle n'a encaissé.
     *
     * @return le ratio en pourcentage arrondi à 0,1 %, ou null s'il n'y a aucune prime encaissée
     */
    public BigDecimal calculerRatioIndemnisations() {
        BigDecimal net = calculerChiffreAffairesNet();
        if (net.signum() <= 0) {
            return null;
        }
        return calculerTotalIndemnisations().multiply(BigDecimal.valueOf(100))
                .divide(net, 1, RoundingMode.HALF_UP);
    }

    /**
     * Calcule l'évolution mensuelle des revenus encaissés, des indemnisations et des remboursements,
     * en comblant les mois sans activité par 0 entre le premier et le dernier mois.
     *
     * @return une liste de lignes {"MM/AAAA", encaissé, indemnisations, remboursements} en BigDecimal,
     *         de la plus ancienne à la plus récente
     */
    public List<Object[]> evolutionFinancesParMois() {
        // Par mois : {encaissé, indemnisations, remboursements}
        Map<YearMonth, BigDecimal[]> montants = new HashMap<>();
        ajouterParMois(montants, statistiqueDao.evolutionRevenusParMois(), 0);
        ajouterParMois(montants, statistiqueDao.evolutionVersementsParMois(TypeVersement.INDEMNISATION), 1);
        ajouterParMois(montants, statistiqueDao.evolutionVersementsParMois(TypeVersement.REMBOURSEMENT), 2);

        List<Object[]> resultat = new ArrayList<>();
        if (montants.isEmpty()) {
            return resultat;
        }
        YearMonth dernier = Collections.max(montants.keySet());
        for (YearMonth mois = Collections.min(montants.keySet()); !mois.isAfter(dernier); mois = mois.plusMonths(1)) {
            BigDecimal[] valeurs = montants.getOrDefault(mois, nouveauxMontants());
            resultat.add(new Object[]{mois.format(FORMAT_MOIS), valeurs[0], valeurs[1], valeurs[2]});
        }
        return resultat;
    }

    /**
     * Range les résultats mensuels issus du DAO dans la colonne correspondante de la carte par mois.
     *
     * @param montants carte associant chaque mois à un tableau de 3 montants
     * @param lignes   lignes brutes {année, mois, montant} renvoyées par le DAO
     * @param colonne  index de la colonne à remplir (0 = encaissé, 1 = indemnisations, 2 = remboursements)
     */
    private static void ajouterParMois(Map<YearMonth, BigDecimal[]> montants, List<Object[]> lignes, int colonne) {
        for (Object[] ligne : lignes) {
            if (ligne[0] != null && ligne[1] != null) {
                YearMonth mois = YearMonth.of(((Number) ligne[0]).intValue(), ((Number) ligne[1]).intValue());
                montants.computeIfAbsent(mois, m -> nouveauxMontants())[colonne] = (BigDecimal) ligne[2];
            }
        }
    }

    /**
     * Crée un tableau initialisé avec trois montants à zéro (0.00) pour un mois donné.
     *
     * @return un tableau de trois BigDecimal égaux à 0.00
     */
    private static BigDecimal[] nouveauxMontants() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        return new BigDecimal[]{zero, zero, zero};
    }
}