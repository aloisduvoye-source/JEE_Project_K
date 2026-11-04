package fr.cytech.assurance.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Message d'erreur à afficher une seule fois, sur la page vers laquelle une servlet redirige.
 * <p>
 * Après un POST, la servlet redirige (pour qu'un rechargement ne renvoie pas le formulaire) :
 * le message ne peut pas passer par la requête, il est donc gardé en session le temps de la redirection.
 * La JSP de destination l'affiche puis l'efface :
 *
 * <pre>{@code
 *   <c:if test="${not empty sessionScope.messageErreur}">
 *     <p><c:out value="${sessionScope.messageErreur}"/></p>
 *     <c:remove var="messageErreur" scope="session"/>
 *   </c:if>
 * }</pre>
 */
public final class MessageFlash {

    // Nom de l'attribut de session (le même que dans les JSP : ${sessionScope.messageErreur})
    public static final String ERREUR = "messageErreur";

    private MessageFlash() {
    }

    // Enregistre le message pour la prochaine page affichée
    public static void erreur(HttpServletRequest request, String message) {
        request.getSession().setAttribute(ERREUR, message);
    }
}
