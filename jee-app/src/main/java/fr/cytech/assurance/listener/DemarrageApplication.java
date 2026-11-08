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
 * Appelé par Tomcat au démarrage et à l'arrêt de l'application.
 * Démarre Hibernate dès le lancement : sinon, la première requête qui accède à la base
 * (souvent une connexion) attend plusieurs secondes le temps qu'il s'initialise.
 * Une base inaccessible ou un schéma incorrect est ainsi signalé dès le démarrage, dans les logs.
 * <p>
 * Lance aussi la mise à jour automatique des statuts des contrats et des paiements
 * (date de début atteinte, paiement en retard, date de fin dépassée) : au démarrage, puis toutes les heures.
 */
@WebListener
public class DemarrageApplication implements ServletContextListener {

    private ScheduledExecutorService planificateur;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // Le premier appel à JpaUtil crée l'EntityManagerFactory (connexion, vérification du schéma)
        JpaUtil.getEntityManager().close();

        ContratService contratService = new ContratService();
        planificateur = Executors.newSingleThreadScheduledExecutor(tache -> {
            Thread thread = new Thread(tache, "mise-a-jour-statuts");
            thread.setDaemon(true);   // n'empêche pas Tomcat de s'arrêter
            return thread;
        });
        planificateur.scheduleAtFixedRate(() -> {
            // Sans ce try, une seule erreur (base momentanément inaccessible) arrêterait définitivement la tâche
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

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Arrête la tâche avant de fermer la connexion à la base qu'elle utilise
        if (planificateur != null) {
            planificateur.shutdownNow();
        }

        // Ferme proprement les connexions à MySQL
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
