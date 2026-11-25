package fr.cytech.assurance.config;

import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * Remplace la page d'erreur du web.xml de jee-app, que Spring Boot ne lit pas.
 */
@Configuration
public class WebServeurConfig {

    /** Page affichée quand un utilisateur ouvre une page réservée à un autre rôle. */
    @Bean
    public ErrorPageRegistrar pageAccesRefuse() {
        return registre -> registre.addErrorPages(
                new ErrorPage(HttpStatus.FORBIDDEN, "/WEB-INF/views/common/acces-refuse.jsp"));
    }
}
