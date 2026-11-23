package fr.cytech.assurance.config;

import org.apache.tomcat.util.descriptor.web.JspConfigDescriptorImpl;
import org.apache.tomcat.util.descriptor.web.JspPropertyGroup;
import org.apache.tomcat.util.descriptor.web.JspPropertyGroupDescriptorImpl;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

/**
 * Remplace le web.xml de jee-app, que le Tomcat embarqué ne lit pas.
 */
@Configuration
public class WebServeurConfig {

    /** JSP et fragments inclus (.jspf, comme l'en-tête) lus en UTF-8. */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> encodageJsp() {
        return factory -> factory.addContextCustomizers(context -> {
            JspPropertyGroup groupe = new JspPropertyGroup();
            groupe.addUrlPattern("*.jsp");
            groupe.addUrlPattern("*.jspf");
            groupe.setPageEncoding("UTF-8");
            context.setJspConfigDescriptor(new JspConfigDescriptorImpl(
                    List.of(new JspPropertyGroupDescriptorImpl(groupe)), Set.of()));
        });
    }

    /** Page affichée quand un utilisateur ouvre une page réservée à un autre rôle. */
    @Bean
    public ErrorPageRegistrar pageAccesRefuse() {
        return registre -> registre.addErrorPages(
                new ErrorPage(HttpStatus.FORBIDDEN, "/WEB-INF/views/common/acces-refuse.jsp"));
    }
}
