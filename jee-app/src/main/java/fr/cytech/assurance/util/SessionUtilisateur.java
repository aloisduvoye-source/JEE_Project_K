package fr.cytech.assurance.util;

import fr.cytech.assurance.model.Utilisateur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Seul endroit qui manipule l'utilisateur connecté dans la session.
 * Les servlets et les filtres passent par ces méthodes plutôt que par getAttribute("utilisateur").
 * Dans les JSP, l'utilisateur connecté reste accessible avec ${sessionScope.utilisateur}.
 */
public final class SessionUtilisateur {

    // Nom de l'attribut de session (le même que dans les JSP : ${sessionScope.utilisateur})
    public static final String ATTRIBUT = "utilisateur";

    private SessionUtilisateur() {
    }

    // Utilisateur connecté, ou null si personne ne l'est (ne crée pas de session)
    public static Utilisateur connecte(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Utilisateur) session.getAttribute(ATTRIBUT);
    }

    // Connexion : nouvelle session à chaque fois (protège contre la fixation de session)
    public static void connecter(HttpServletRequest request, Utilisateur utilisateur) {
        deconnecter(request);
        request.getSession(true).setAttribute(ATTRIBUT, utilisateur);
    }

    // Remplace la copie en session (après une modification du profil, par exemple)
    public static void mettreAJour(HttpServletRequest request, Utilisateur utilisateur) {
        request.getSession().setAttribute(ATTRIBUT, utilisateur);
    }

    public static void deconnecter(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
