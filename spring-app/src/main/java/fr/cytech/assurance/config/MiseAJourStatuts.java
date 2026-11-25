package fr.cytech.assurance.config;

import fr.cytech.assurance.service.ContratService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Met à jour les statuts des contrats et des paiements au démarrage, puis toutes les heures
 * (remplace DemarrageApplication de jee-app ; la planification est activée par @EnableScheduling).
 */
@Component
public class MiseAJourStatuts {

    private static final Logger LOG = LoggerFactory.getLogger(MiseAJourStatuts.class);

    private final ContratService contratService;

    /** Dépendances injectées par Spring. */
    public MiseAJourStatuts(ContratService contratService) {
        this.contratService = contratService;
    }

    /** Lancée dès le démarrage, puis toutes les heures ; une erreur n'arrête pas les exécutions suivantes. */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void mettreAJour() {
        try {
            int changes = contratService.mettreAJourStatuts(LocalDate.now());
            if (changes > 0) {
                LOG.info("Statuts mis à jour : {} contrat(s)", changes);
            }
        } catch (RuntimeException e) {
            LOG.error("Échec de la mise à jour des statuts des contrats", e);
        }
    }
}
