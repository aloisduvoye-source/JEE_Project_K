package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.StatistiqueDao;
import fr.cytech.assurance.model.enums.StatutContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StatistiqueService {
    
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
        return statistiqueDao.evolutionContratsParMois();
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
        return statistiqueDao.evolutionRevenusParMois();
    }

}
