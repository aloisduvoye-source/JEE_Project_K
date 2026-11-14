package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.AnalyseStatistiquesService;
import fr.cytech.assurance.service.StatistiqueService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Page Statistiques : clients, contrats et finances, avec des graphiques.
 * URL protégée automatiquement par RoleFiltre : le préfixe /agent est ouvert aux AGENT et ADMIN.
 */
@WebServlet("/agent/statistiques")
public class StatistiquesServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/statistiques.jsp";

    private final StatistiqueService statistiqueService = new StatistiqueService();
    private final AnalyseStatistiquesService analyseStatistiquesService = new AnalyseStatistiquesService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("totalClients", statistiqueService.compterTotalClients());
        request.setAttribute("nouveauxClients", statistiqueService.compterNouveauxClients());
        request.setAttribute("repartitionClients", statistiqueService.repartitionClientsParType());
        request.setAttribute("contratsActifs", statistiqueService.compterContratsActifs());
        request.setAttribute("contratsExpires", statistiqueService.compterContratsExpires());
        request.setAttribute("contratsResilies", statistiqueService.compterContratsResilies());
        request.setAttribute("evolutionContrats", statistiqueService.evolutionContratsParMois());
        request.setAttribute("chiffreAffaires", statistiqueService.calculerChiffreAffaires());
        request.setAttribute("totalPrimes", statistiqueService.calculerTotalPrimes());
        request.setAttribute("totalIndemnisations", statistiqueService.calculerTotalIndemnisations());
        request.setAttribute("totalRemboursements", statistiqueService.calculerTotalRemboursements());
        request.setAttribute("chiffreAffairesNet", statistiqueService.calculerChiffreAffairesNet());
        request.setAttribute("ratioIndemnisations", statistiqueService.calculerRatioIndemnisations());
        request.setAttribute("evolutionFinances", statistiqueService.evolutionFinancesParMois());

        if ("true".equals(request.getParameter("analyser"))) {
            request.setAttribute("analyseIa", analyseStatistiquesService.analyser());
        }

        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
