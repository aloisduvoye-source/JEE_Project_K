package fr.cytech.assurance.util;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class JpaUtil {
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    private static EntityManagerFactory buildEntityManagerFactory() {
        try {
            // Charger le fichier .env à la racine du projet
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            // Récupérer les variables du .env et les injecter dans la configuration JPA
            Map<String, String> properties = new HashMap<>();
            if (dotenv.get("DB_URL") != null) {
                properties.put("jakarta.persistence.jdbc.url", dotenv.get("DB_URL"));
            }
            if (dotenv.get("DB_USER") != null) {
                properties.put("jakarta.persistence.jdbc.user", dotenv.get("DB_USER"));
            }
            if (dotenv.get("DB_PASSWORD") != null) {
                properties.put("jakarta.persistence.jdbc.password", dotenv.get("DB_PASSWORD"));
            }

            // "AssurancePU" doit correspondre exactement au nom dans persistence.xml
            return Persistence.createEntityManagerFactory("AssurancePU", properties);
        } catch (Throwable ex) {
            System.err.println("Échec de la création de l'EntityManagerFactory : " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}