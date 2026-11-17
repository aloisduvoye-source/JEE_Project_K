package fr.cytech.assurance.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Message d'erreur affiché une seule fois, sur la page vers laquelle une servlet redirige après un POST.
 * Gardé en session le temps de la redirection : la JSP de destination l'affiche puis l'efface.
 */
public final class MessageFlash {

    /** Nom de l'attribut de session, le même que dans les JSP (${sessionScope.messageErreur}). */
    public static final String ERREUR = "messageErreur";

    private MessageFlash() {
    }

    /** Enregistre le message pour la prochaine page affichée. */
    public static void erreur(HttpServletRequest request, String message) {
        request.getSession().setAttribute(ERREUR, message);
    }
}
