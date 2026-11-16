package fr.cytech.assurance.listener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.util.JpaUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Appelé par Tomcat au démarrage et à l'arrêt de l'application : démarre Hibernate
 * et met à jour les statuts des contrats et des paiements toutes les heures.
 */
@WebListener
public class DemarrageApplication implements ServletContextListener {

    /** Tâche de mise à jour des statuts. */
    private ScheduledExecutorService planificateur;

    /** Démarre Hibernate, puis lance la mise à jour des statuts : tout de suite, puis toutes les heures. */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        JpaUtil.getEntityManager().close();

        ContratService contratService = new ContratService();
        planificateur = Executors.newSingleThreadScheduledExecutor(tache -> {
            Thread thread = new Thread(tache, "mise-a-jour-statuts");
            thread.setDaemon(true);
            return thread;
        });
        planificateur.scheduleAtFixedRate(() -> {
            try {
                int changes = contratService.mettreAJourStatuts(LocalDate.now());
                if (changes > 0) {
                    event.getServletContext().log("Statuts mis à jour : " + changes + " contrat(s)");
                }
            } catch (RuntimeException e) {
                event.getServletContext().log("Échec de la mise à jour des statuts des contrats", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    /** Arrête la tâche, ferme Hibernate et libère le pilote MySQL. */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (planificateur != null) {
            planificateur.shutdownNow();
        }

        JpaUtil.shutdown();

        // Le pilote MySQL est chargé par l'application : sans ce nettoyage, Tomcat signale
        // une fuite de mémoire à chaque arrêt ou redéploiement
        ClassLoader chargeurApplication = Thread.currentThread().getContextClassLoader();
        for (Driver pilote : Collections.list(DriverManager.getDrivers())) {
            if (pilote.getClass().getClassLoader() == chargeurApplication) {
                try {
                    DriverManager.deregisterDriver(pilote);
                } catch (SQLException e) {
                    event.getServletContext().log("Impossible de désenregistrer le pilote " + pilote, e);
                }
            }
        }
        AbandonedConnectionCleanupThread.checkedShutdown();
    }
}
