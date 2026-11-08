package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.MotDePasseIncorrectException;
import fr.cytech.assurance.exception.UtilisateurInvalideException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Suppression de son propre compte par un client ou un agent (interdit à l'administrateur)
@WebServlet("/compte/supprimer")
public class SuppressionCompteServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/compte/supprimer.jsp";

    private final UtilisateurService utilisateurService = new UtilisateurService();

    // Page de confirmation, avec le mot de passe à saisir
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SessionUtilisateur.connecte(request).getRole() == Role.ADMIN) {
            response.sendRedirect(request.getContextPath() + "/compte");
            return;
        }
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Le service vérifie le mot de passe, puis efface ou désactive le compte ; l'utilisateur est ensuite déconnecté
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        String motDePasse = request.getParameter("motDePasse");

        String erreur;
        if (motDePasse == null || motDePasse.isEmpty()) {
            erreur = "Le mot de passe est obligatoire.";
        } else {
            try {
                boolean efface = utilisateurService.supprimerCompte(connecte.getId(), motDePasse);
                SessionUtilisateur.deconnecter(request);
                // Message affiché sur la page d'accueil
                response.sendRedirect(request.getContextPath() + "/?compte=" + (efface ? "supprime" : "desactive"));
                return;
            } catch (MotDePasseIncorrectException e) {
                erreur = "Mot de passe incorrect.";
            } catch (UtilisateurInvalideException e) {
                erreur = e.getMessage();
            }
        }

        // Le mot de passe n'est jamais renvoyé dans le formulaire
        request.setAttribute("erreur", erreur);
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
