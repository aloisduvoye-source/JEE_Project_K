package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.AssistantIndisponibleException;
import fr.cytech.assurance.exception.TropDeQuestionsException;
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

/**
 * Conversation du client connecté avec l'assistant IA.
 * GET : affiche la conversation. POST : pose une question, ou efface la conversation (action=effacer).
 * Après un POST, redirection vers la page (un rechargement ne renvoie pas la question).
 */
@WebServlet("/client/assistant")
public class AssistantServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/assistant.jsp";

    // Attribut de session : question à remettre dans le champ si l'assistant n'a pas pu répondre
    private static final String QUESTION_NON_ENVOYEE = "questionNonEnvoyee";

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
        } else {
            poserQuestion(request, utilisateur);
        }
        // Ancre : la page s'ouvre en bas de la conversation, sur la dernière réponse
        response.sendRedirect(request.getContextPath() + "/client/assistant#bas");
    }

    private void poserQuestion(HttpServletRequest request, Utilisateur utilisateur) {
        String question = Validateur.nettoyer(request.getParameter("question"));
        Validateur v = new Validateur();
        v.obligatoire("question", question, "La question", AssistantService.LONGUEUR_MAX_QUESTION);
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("question"));
            return;
        }
        try {
            assistantService.poserQuestion(utilisateur, question);
        } catch (TropDeQuestionsException e) {
            MessageFlash.erreur(request, e.getMessage());
            request.getSession().setAttribute(QUESTION_NON_ENVOYEE, question);
        } catch (AssistantIndisponibleException e) {
            log("Assistant indisponible pour l'utilisateur " + utilisateur.getId(), e);
            MessageFlash.erreur(request, e.getMessage());
            // Le client n'a pas à retaper sa question
            request.getSession().setAttribute(QUESTION_NON_ENVOYEE, question);
        }
    }
}
