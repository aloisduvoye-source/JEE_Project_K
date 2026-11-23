package fr.cytech.assurance.util;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabrique unique des EntityManager, configurée par DB_URL, DB_USER et DB_PASSWORD (.env ou docker-compose.yml).
 */
public class JpaUtil {
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    private static EntityManagerFactory buildEntityManagerFactory() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            Map<String, String> properties = new HashMap<>();
            properties.put("jakarta.persistence.jdbc.url", variableObligatoire(dotenv, "DB_URL"));
            properties.put("jakarta.persistence.jdbc.user", variableObligatoire(dotenv, "DB_USER"));
            properties.put("jakarta.persistence.jdbc.password", variableObligatoire(dotenv, "DB_PASSWORD"));

            // "AssurancePU" doit correspondre exactement au nom dans persistence.xml
            return Persistence.createEntityManagerFactory("AssurancePU", properties);
        } catch (Throwable ex) {
            System.err.println("Échec de la création de l'EntityManagerFactory : " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /** Valeur de la variable, avec un message clair si elle manque plutôt qu'une erreur de connexion obscure. */
    private static String variableObligatoire(Dotenv dotenv, String nom) {
        String valeur = dotenv.get(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("Variable " + nom + " absente : vérifier le fichier .env ou docker-compose.yml");
        }
        return valeur;
    }

    /** Nouvel EntityManager, à fermer après usage. */
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /** Ferme la fabrique et ses connexions, à l'arrêt de l'application. */
    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
