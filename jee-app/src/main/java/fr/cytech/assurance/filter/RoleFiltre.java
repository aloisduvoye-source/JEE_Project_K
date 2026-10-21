package fr.cytech.assurance.filter;

import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Protège les pages réservées aux utilisateurs connectés :
 *   /compte/*  : tous les rôles (profil, mot de passe)
 *   /client/*  : CLIENT
 *   /agent/*   : AGENT et ADMIN (l'administrateur consulte aussi contrats et sinistres)
 *   /admin/*   : ADMIN
 * Non connecté : redirection vers la connexion. Mauvais rôle : erreur 403.
 * Une nouvelle page est donc protégée automatiquement si son URL commence par l'un de ces préfixes.
 */
@WebFilter(urlPatterns = {"/compte/*", "/client/*", "/agent/*", "/admin/*"})
public class RoleFiltre extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        Utilisateur utilisateur = session == null ? null : (Utilisateur) session.getAttribute("utilisateur");

        if (utilisateur == null) {
            response.sendRedirect(request.getContextPath() + "/connexion");
            return;
        }
        if (!rolesAutorises(request.getServletPath()).contains(utilisateur.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    // Seul endroit où sont définis les rôles autorisés pour chaque espace
    static Set<Role> rolesAutorises(String chemin) {
        if (chemin.startsWith("/compte")) {return Set.of(Role.values());}
        if (chemin.startsWith("/client")) {return Set.of(Role.CLIENT);}
        if (chemin.startsWith("/agent")) {return Set.of(Role.AGENT, Role.ADMIN);}
        return Set.of(Role.ADMIN);
    }
}
