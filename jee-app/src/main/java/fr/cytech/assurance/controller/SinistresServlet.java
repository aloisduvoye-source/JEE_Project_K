package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.enums.StatutSinistre;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Liste des sinistres pour les agents (et les admins, en consultation).
 * Filtres facultatifs dans l'adresse : ?statut=DECLARE (file de traitement), ?mien=1 (dossiers de l'agent connecté).
 */
@WebServlet("/agent/sinistres")
public class SinistresServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/sinistres.jsp";

    private final SinistreService sinistreService = new SinistreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StatutSinistre statut = statut(request.getParameter("statut"));
        boolean mien = "1".equals(request.getParameter("mien"));
        Long idAgent = mien ? SessionUtilisateur.connecte(request).getId() : null;

        request.setAttribute("sinistres", sinistreService.lister(statut, idAgent));
        request.setAttribute("statuts", StatutSinistre.values());
        request.setAttribute("statutChoisi", statut);
        request.setAttribute("mien", mien);
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Statut demandé, ou null (tous) s'il est absent ou inconnu
    private StatutSinistre statut(String valeur) {
        try {
            return valeur == null || valeur.isBlank() ? null : StatutSinistre.valueOf(valeur.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
