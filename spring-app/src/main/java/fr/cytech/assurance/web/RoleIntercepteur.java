package fr.cytech.assurance.web;

import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

/**
 * Protège les pages /compte, /client, /agent et /admin (remplace RoleFiltre de jee-app),
 * enregistré pour ces préfixes dans config.WebMvcConfig.
 * Non connecté ou compte désactivé : retour à la connexion. Mauvais rôle : erreur 403.
 */
@Component
public class RoleIntercepteur implements HandlerInterceptor {

    private final UtilisateurService utilisateurService;

    /** Dépendances injectées par Spring. */
    public RoleIntercepteur(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** Laisse passer la requête si l'utilisateur est connecté, actif et a un rôle autorisé. */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Utilisateur utilisateur = SessionUtilisateur.connecte(request);

        if (utilisateur == null) {
            response.sendRedirect(request.getContextPath() + "/connexion");
            return false;
        }
        Utilisateur enBase = utilisateurService.trouverParId(utilisateur.getId());
        if (enBase == null || !enBase.isActif()) {
            SessionUtilisateur.deconnecter(request);
            response.sendRedirect(request.getContextPath() + "/connexion?desactive=1");
            return false;
        }
        if (!rolesAutorises(request.getServletPath()).contains(utilisateur.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    /**
     * @return les rôles autorisés pour ce chemin (seul endroit où ils sont définis)
     */
    static Set<Role> rolesAutorises(String chemin) {
        if (chemin.startsWith("/compte")) {return Set.of(Role.values());}
        if (chemin.startsWith("/client")) {return Set.of(Role.CLIENT);}
        if (chemin.startsWith("/agent")) {return Set.of(Role.AGENT, Role.ADMIN);}
        return Set.of(Role.ADMIN);
    }
}
