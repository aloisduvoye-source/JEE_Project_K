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

            // Variables du .env ou de l'environnement (docker-compose.yml) : obligatoires
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

    // Message clair si une variable manque, au lieu d'une erreur de connexion difficile à comprendre
    private static String variableObligatoire(Dotenv dotenv, String nom) {
        String valeur = dotenv.get(nom);
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalStateException("Variable " + nom + " absente : vérifier le fichier .env ou docker-compose.yml");
        }
        return valeur;
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