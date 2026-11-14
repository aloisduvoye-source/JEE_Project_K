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

public class StatistiqueService {

    private static final DateTimeFormatter FORMAT_MOIS = DateTimeFormatter.ofPattern("MM/yyyy");

private final StatistiqueDao statistiqueDao = new StatistiqueDao();

    public int compterTotalClients() {
        return statistiqueDao.compterTotalClients();
    }

    public int compterNouveauxClients() {
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return statistiqueDao.compterNouveauxClients(debutMois);
    }

    public List<Object[]> repartitionClientsParType() {
        return statistiqueDao.repartitionClientsParType();
    }

    public int compterContratsActifs() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.ACTIF);
    }

    public int compterContratsExpires() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.EXPIRE);
    }

    public int compterContratsResilies() {
        return statistiqueDao.compterContratsParStatut(StatutContrat.RESILIE);
    }

    public List<Object[]> evolutionContratsParMois() {
        return completerMois(statistiqueDao.evolutionContratsParMois(), 0L);
    }

    public BigDecimal calculerChiffreAffaires() {
        return statistiqueDao.calculerChiffreAffaires();
    }

    public BigDecimal calculerTotalPrimes(){
        return statistiqueDao.calculerTotalPrimes();
    }

    public BigDecimal calculerTotalIndemnisations(){
        return statistiqueDao.calculerTotalIndemnisations();
    }

    public List<Object[]> evolutionRevenusParMois(){
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
