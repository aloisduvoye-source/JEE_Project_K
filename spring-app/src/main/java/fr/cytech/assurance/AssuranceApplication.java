package fr.cytech.assurance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de la version Spring Boot : lancée par java -jar, ou déployée dans un Tomcat externe.
 */
@SpringBootApplication
@EnableScheduling
public class AssuranceApplication extends SpringBootServletInitializer {

    /** Démarre l'application avec le Tomcat embarqué. */
    public static void main(String[] args) {
        SpringApplication.run(AssuranceApplication.class, args);
    }

    /** Configuration utilisée quand le .war est déployé dans un Tomcat externe. */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AssuranceApplication.class);
    }
}
