package fr.cytech.assurance.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Ajoute les variables du .env (dossier courant ou dossier parent) à la configuration, après celles de l'environnement.
 */
public class ChargementDotenv implements EnvironmentPostProcessor {

    /** Lit le premier .env trouvé, guillemets retirés comme le font jee-app et Docker Compose. */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String dossier : new String[]{".", ".."}) {
            if (Files.isRegularFile(Path.of(dossier, ".env"))) {
                Dotenv dotenv = Dotenv.configure().directory(dossier).load();
                Map<String, Object> variables = new HashMap<>();
                for (DotenvEntry entree : dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)) {
                    variables.put(entree.getKey(), entree.getValue());
                }
                environment.getPropertySources().addLast(new MapPropertySource("dotenv", variables));
                return;
            }
        }
    }
}
