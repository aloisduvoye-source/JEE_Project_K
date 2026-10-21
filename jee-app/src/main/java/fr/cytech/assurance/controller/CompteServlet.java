package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.UtilisateurService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Page « Mon compte » : informations de l'utilisateur connecté et liens vers les modifications
@WebServlet("/compte")
public class CompteServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/compte/compte.jsp";

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Rechargé depuis la base : la copie en session date de la connexion
        Utilisateur connecte = (Utilisateur) request.getSession().getAttribute("utilisateur");
        Utilisateur utilisateur = utilisateurService.trouverParId(connecte.getId());
        request.getSession().setAttribute("utilisateur", utilisateur);

        request.setAttribute("compte", utilisateur);
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
