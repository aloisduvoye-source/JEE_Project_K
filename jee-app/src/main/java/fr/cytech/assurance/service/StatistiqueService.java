package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.StatistiqueDao;
import fr.cytech.assurance.model.enums.StatutContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques globales de la page Statistiques (agents et administrateur) : clients, contrats et finances.
 */
public class StatistiqueService {

    // Libellé d'un mois dans les évolutions : "09/2026"
    private static final DateTimeFormatter FORMAT_MOIS = DateTimeFormatter.ofPattern("MM/yyyy");

    private final StatistiqueDao statistiqueDao = new StatistiqueDao();

    /**
     * @return le nombre total de clients
     */
    public int compterTotalClients() {
        return statistiqueDao.compterTotalClients();
    }

    /**
     * @return le nombre de clients inscrits depuis le 1er du mois en cours
     */
    public int compterNouveauxClients() {
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return statistiqueDao.compterNouveauxClients(debutMois);
    }

    /**
     * @return lignes {nom du type, nombre de clients ayant un contrat actif de ce type}
     */
    public List<Object[]> repartitionClientsParType() {
        return statistiqueDao.repartitionClientsParType();
    }

    /**
     * @return le nombre de contrats actifs
     */
    public int compterContratsActifs() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.ACTIF);
    }

    /**
     * @return le nombre de contrats expirés
     */
    public int compterContratsExpires() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.EXPIRE);
    }

    /**
     * @return le nombre de contrats résiliés
     */
    public int compterContratsResilies() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.RESILIE);
    }

    /**
     * Nombre de contrats actifs en même temps, mois par mois : ceux actifs le dernier jour du mois
     * (aujourd'hui pour le mois en cours), du mois du premier contrat jusqu'au mois en cours.
     *
     * @return lignes {"MM/AAAA", nombre de contrats actifs (Long)}, de la plus ancienne à la plus récente
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

    // Actif à cette date : commencé, pas encore fini, payé, ni résilié ni suspendu par un agent avant
    // (même règle que ContratService.statutAttendu ; une suspension déjà levée n'est plus connue)
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
     * @return la somme des paiements effectués, avant remboursements
     */
    public BigDecimal calculerChiffreAffaires() {
        return statistiqueDao.calculerChiffreAffaires();
    }

    /**
     * @return la somme des primes annuelles des contrats actifs
     */
    public BigDecimal calculerTotalPrimes() {
        return statistiqueDao.calculerTotalPrimes();
    }

    /**
     * @return la somme des indemnisations de sinistres versées
     */
    public BigDecimal calculerTotalIndemnisations() {
        return statistiqueDao.calculerTotalIndemnisations();
    }

    /**
     * Montant des paiements effectués par mois, sans trou entre le premier et le dernier mois.
     *
     * @return lignes {"MM/AAAA", montant}, de la plus ancienne à la plus récente
     */
    public List<Object[]> evolutionRevenusParMois() {
        return completerMois(statistiqueDao.evolutionRevenusParMois(), BigDecimal.ZERO.setScale(2));
    }

    // Lignes {année, mois, valeur} triées -> lignes {"MM/AAAA", valeur}, du premier au dernier mois,
    // avec "zero" pour les mois absents (sinon la courbe relierait deux mois éloignés comme s'ils se suivaient)
    private List<Object[]> completerMois(List<Object[]> lignes, Number zero) {
        Map<YearMonth, Number> valeurs = new LinkedHashMap<>();
        for (Object[] ligne : lignes) {
            if (ligne[0] != null && ligne[1] != null) {
                valeurs.put(YearMonth.of(((Number) ligne[0]).intValue(), ((Number) ligne[1]).intValue()),
                        (Number) ligne[2]);
            }
        }
        List<Object[]> resultat = new ArrayList<>();
        if (valeurs.isEmpty()) {
            return resultat;
        }
        YearMonth dernier = Collections.max(valeurs.keySet());
        for (YearMonth mois = Collections.min(valeurs.keySet()); !mois.isAfter(dernier); mois = mois.plusMonths(1)) {
            resultat.add(new Object[]{mois.format(FORMAT_MOIS), valeurs.getOrDefault(mois, zero)});
        }
        return resultat;
    }
}
