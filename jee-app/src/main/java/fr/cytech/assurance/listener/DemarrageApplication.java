package fr.cytech.assurance.listener;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import fr.cytech.assurance.util.JpaUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Appelé par Tomcat au démarrage et à l'arrêt de l'application.
 * Démarre Hibernate dès le lancement : sinon, la première requête qui accède à la base
 * (souvent une connexion) attend plusieurs secondes le temps qu'il s'initialise.
 * Une base inaccessible ou un schéma incorrect est ainsi signalé dès le démarrage, dans les logs.
 */
@WebListener
public class DemarrageApplication implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // Le premier appel à JpaUtil crée l'EntityManagerFactory (connexion, vérification du schéma)
        JpaUtil.getEntityManager().close();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
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
