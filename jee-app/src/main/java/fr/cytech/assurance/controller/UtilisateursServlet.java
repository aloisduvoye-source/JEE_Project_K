package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Liste de tous les comptes (clients, agents, administrateurs) pour l'administrateur : §2.1, « gérer les utilisateurs »
@WebServlet("/admin/utilisateurs")
public class UtilisateursServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/admin/utilisateurs.jsp";

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Filtre facultatif par rôle (?role=AGENT) ; une valeur inconnue affiche tout le monde
        Role role = roleOuNull(request.getParameter("role"));
        request.setAttribute("role", role);
        request.setAttribute("roles", Role.values());
        request.setAttribute("utilisateurs", utilisateurService.lister(role));
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Aussi utilisé par ActionUtilisateurServlet pour revenir à la liste avec le même filtre
    static Role roleOuNull(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(valeur.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
