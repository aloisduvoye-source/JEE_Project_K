package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/agent/paiements")
public class PaiementsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/paiements.jsp";

    private final PaiementService paiementService = new PaiementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StatutPaiement statut = Validateur.enumOuNull(StatutPaiement.class, request.getParameter("statut"));
        ModePaiement mode = Validateur.enumOuNull(ModePaiement.class, request.getParameter("mode"));
        String tri = request.getParameter("tri");

        request.setAttribute("paiements", paiementService.listerTous(statut, mode, tri));
        request.setAttribute("statuts", StatutPaiement.values());
        request.setAttribute("modes", ModePaiement.values());
        request.setAttribute("statutSelectionne", statut);
        request.setAttribute("modeSelectionne", mode);
        request.setAttribute("triSelectionne", tri == null ? "echeance_desc" : tri);

        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
