package fr.cytech.assurance.filter;

import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Protège les pages /compte, /client, /agent et /admin : toute page dont l'URL commence par l'un de ces préfixes.
 * Non connecté ou compte désactivé : retour à la connexion. Mauvais rôle : erreur 403.
 */
@WebFilter(urlPatterns = {"/compte/*", "/client/*", "/agent/*", "/admin/*"})
public class RoleFiltre extends HttpFilter {

    private final UtilisateurService utilisateurService = new UtilisateurService();

    /** Laisse passer la requête si l'utilisateur est connecté, actif et a un rôle autorisé. */
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Utilisateur utilisateur = SessionUtilisateur.connecte(request);

        if (utilisateur == null) {
            response.sendRedirect(request.getContextPath() + "/connexion");
            return;
        }
        // La copie en session date de la connexion : l'état du compte est relu en base à chaque page protégée,
        // pour qu'un compte désactivé par l'administrateur perde l'accès tout de suite, sans attendre la fin de la session
        Utilisateur enBase = utilisateurService.trouverParId(utilisateur.getId());
        if (enBase == null || !enBase.isActif()) {
            SessionUtilisateur.deconnecter(request);
            response.sendRedirect(request.getContextPath() + "/connexion?desactive=1");
            return;
        }
        if (!rolesAutorises(request.getServletPath()).contains(utilisateur.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
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
