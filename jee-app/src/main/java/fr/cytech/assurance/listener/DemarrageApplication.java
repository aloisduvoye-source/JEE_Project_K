package fr.cytech.assurance.listener;

import fr.cytech.assurance.util.JpaUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

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
    }
}
