package fr.cytech.assurance.controller;

import com.google.gson.JsonObject;
import fr.cytech.assurance.exception.AssistantIndisponibleException;
import fr.cytech.assurance.exception.TropDeQuestionsException;
import fr.cytech.assurance.model.MessageAssistant;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.AssistantService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Conversation du client connecté avec l'assistant IA.
 * GET : affiche la conversation. POST : pose une question, ou efface la conversation (action=effacer).
 * <p>
 * Une question peut être posée de deux façons :
 * <ul>
 *     <li>par le JavaScript de la page (en-tête Accept: application/json) : la réponse est renvoyée en JSON
 *     et ajoutée à la conversation sans recharger la page ;</li>
 *     <li>par un envoi classique du formulaire (JavaScript désactivé) : redirection vers la page,
 *     pour qu'un rechargement ne renvoie pas la question.</li>
 * </ul>
 */
@WebServlet("/client/assistant")
public class AssistantServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/assistant.jsp";

    // Attribut de session : question à remettre dans le champ si l'assistant n'a pas pu répondre
    private static final String QUESTION_NON_ENVOYEE = "questionNonEnvoyee";

    // Même affichage que dans la JSP : "2026-11-13 à 15:48"
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd 'à' HH:mm");

    private final AssistantService assistantService = new AssistantService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur utilisateur = SessionUtilisateur.connecte(request);
        request.setAttribute("messages", assistantService.historique(utilisateur.getId()));
        request.setAttribute("longueurMax", AssistantService.LONGUEUR_MAX_QUESTION);
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur utilisateur = SessionUtilisateur.connecte(request);

        if ("effacer".equals(request.getParameter("action"))) {
            assistantService.effacerHistorique(utilisateur.getId());
            response.sendRedirect(request.getContextPath() + "/client/assistant");
            return;
        }

        String question = Validateur.nettoyer(request.getParameter("question"));
        Resultat resultat = poserQuestion(utilisateur, question);

        if (demandeJson(request)) {
            repondreEnJson(response, resultat);
            return;
        }
        if (resultat.erreur() != null) {
            MessageFlash.erreur(request, resultat.erreur());
            // Le client n'a pas à retaper sa question
            request.getSession().setAttribute(QUESTION_NON_ENVOYEE, question);
        }
        // Ancre : la page s'ouvre en bas de la conversation, sur la dernière réponse
        response.sendRedirect(request.getContextPath() + "/client/assistant#bas");
    }

    // Réponse de l'assistant, ou message d'erreur à afficher avec le code HTTP correspondant
    private record Resultat(MessageAssistant reponse, String erreur, int statut) {
        static Resultat succes(MessageAssistant reponse) {
            return new Resultat(reponse, null, HttpServletResponse.SC_OK);
        }

        static Resultat echec(int statut, String erreur) {
            return new Resultat(null, erreur, statut);
        }
    }

    private Resultat poserQuestion(Utilisateur utilisateur, String question) {
        Validateur v = new Validateur();
        v.obligatoire("question", question, "La question", AssistantService.LONGUEUR_MAX_QUESTION);
        if (!v.estValide()) {
            return Resultat.echec(HttpServletResponse.SC_BAD_REQUEST, v.getErreurs().get("question"));
        }
        try {
            return Resultat.succes(assistantService.poserQuestion(utilisateur, question));
        } catch (TropDeQuestionsException e) {
            return Resultat.echec(429, e.getMessage());
        } catch (AssistantIndisponibleException e) {
            log("Assistant indisponible pour l'utilisateur " + utilisateur.getId(), e);
            return Resultat.echec(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    // Le JavaScript de la page demande du JSON ; un navigateur qui envoie le formulaire demande du HTML
    private static boolean demandeJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /*
     * Succès : {"auteur": "ASSISTANT", "contenu": "...", "date": "2026-11-13 à 15:48"}
     * Échec  : {"erreur": "..."}, avec le code HTTP 400, 429 ou 503
     */
    private static void repondreEnJson(HttpServletResponse response, Resultat resultat) throws IOException {
        JsonObject json = new JsonObject();
        if (resultat.erreur() != null) {
            json.addProperty("erreur", resultat.erreur());
        } else {
            MessageAssistant reponse = resultat.reponse();
            json.addProperty("auteur", reponse.getAuteur().name());
            json.addProperty("contenu", reponse.getContenu());
            json.addProperty("date", reponse.getDateMessage().format(FORMAT_DATE));
        }
        response.setStatus(resultat.statut());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.toString());
    }
}
