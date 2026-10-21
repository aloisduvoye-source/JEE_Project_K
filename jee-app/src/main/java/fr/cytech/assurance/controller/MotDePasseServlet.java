package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.MotDePasseIncorrectException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Changement du mot de passe de l'utilisateur connecté
@WebServlet("/compte/mot-de-passe")
public class MotDePasseServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/compte/mot-de-passe.jsp";

    private final UtilisateurService utilisateurService = new UtilisateurService();

    // Affiche le formulaire
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie le nouveau mot de passe, puis le service vérifie l'ancien et enregistre
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        String actuel = request.getParameter("actuel");
        String nouveau = request.getParameter("nouveau");
        String confirmation = request.getParameter("confirmation");

        Validateur v = new Validateur();
        if (actuel == null || actuel.isEmpty()) {
            v.erreur("actuel", "Mot de passe actuel incorrect.");
        }
        v.nouveauMotDePasse("nouveau", nouveau, "Le nouveau mot de passe", "confirmation", confirmation);

        if (v.estValide()) {
            try {
                utilisateurService.changerMotDePasse(connecte.getId(), actuel, nouveau);
            } catch (MotDePasseIncorrectException e) {
                v.erreur("actuel", e.getMessage());
            }
        }

        if (!v.estValide()) {
            // Les mots de passe ne sont jamais renvoyés dans le formulaire
            request.setAttribute("erreurs", v.getErreurs());
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/compte?succes=motdepasse");
    }
}
