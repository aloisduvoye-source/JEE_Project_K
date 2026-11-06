package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.GarantieService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Liste des garanties du référentiel (responsabilité civile, vol, incendie...), pour l'agent (et l'administrateur) : §2.1 du sujet.
 * URL protégée automatiquement par RoleFiltre : le préfixe /agent est ouvert aux AGENT et ADMIN.
 */
@WebServlet("/agent/garanties")
public class GarantiesServlet extends HttpServlet {

    private final GarantieService garantieService = new GarantieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // La liste triée par nom est mise dans la requête pour la vue
        request.setAttribute("garanties", garantieService.lister());
        request.getRequestDispatcher("/WEB-INF/views/agent/garanties.jsp")
                .forward(request, response);
    }
}
