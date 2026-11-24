package fr.cytech.assurance.config;

import fr.cytech.assurance.web.RoleIntercepteur;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Branche le contrôle d'accès par rôle sur les pages protégées (remplace le @WebFilter de jee-app).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RoleIntercepteur roleIntercepteur;

    /** Dépendances injectées par Spring. */
    public WebMvcConfig(RoleIntercepteur roleIntercepteur) {
        this.roleIntercepteur = roleIntercepteur;
    }

    /** Mêmes préfixes que le filtre de jee-app : /compte, /client, /agent et /admin. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleIntercepteur)
                .addPathPatterns("/compte", "/compte/**", "/client/**", "/agent/**", "/admin/**");
    }
}
