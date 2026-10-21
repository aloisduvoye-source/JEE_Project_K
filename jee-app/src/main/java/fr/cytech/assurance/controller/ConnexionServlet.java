package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.AuthentificationException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.UtilisateurService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/connexion")
public class ConnexionServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/auth/connexion.jsp";

    private final UtilisateurService utilisateurService = new UtilisateurService();

    // Affiche le formulaire
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie l'identifiant et le mot de passe envoyés par le formulaire
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identifiant = request.getParameter("identifiant");
        String motDePasse = request.getParameter("motDePasse");

        Utilisateur utilisateur;
        try {
            if (identifiant == null || identifiant.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
                throw new AuthentificationException("Veuillez remplir tous les champs.");
            }
            utilisateur = utilisateurService.authentifier(identifiant.trim(), motDePasse);
        } catch (AuthentificationException e) {
            request.setAttribute("erreur", e.getMessage());
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        // Nouvelle session à chaque connexion (protège contre la fixation de session)
        HttpSession ancienne = request.getSession(false);
        if (ancienne != null) {
            ancienne.invalidate();
        }
        request.getSession(true).setAttribute("utilisateur", utilisateur);

        // Chacun arrive directement dans l'espace de son rôle
        response.sendRedirect(request.getContextPath() + utilisateur.getRole().getAccueil());
    }
}
