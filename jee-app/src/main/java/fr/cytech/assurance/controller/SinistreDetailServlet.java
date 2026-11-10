package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Fiche d'un sinistre pour les agents (traitement) et les admins (consultation)
@WebServlet("/agent/sinistres/detail")
public class SinistreDetailServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/sinistre-detail.jsp";

    private final SinistreService sinistreService = new SinistreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Sinistre sinistre = id == null ? null : sinistreService.trouverAvecDetails(id);
        if (sinistre == null) {
            MessageFlash.erreur(request, "Ce sinistre n'existe pas.");
            response.sendRedirect(request.getContextPath() + "/agent/sinistres");
            return;
        }
        request.setAttribute("sinistre", sinistre);
        request.setAttribute("indemnisation", sinistreService.indemnisation(id));
        request.setAttribute("propositions", sinistreService.propositions(sinistre));
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
