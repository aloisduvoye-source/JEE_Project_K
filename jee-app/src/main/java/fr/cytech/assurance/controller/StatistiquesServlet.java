package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.AnalyseStatistiquesService;
import fr.cytech.assurance.service.StatistiqueService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/agent/statistiques")
public class StatistiquesServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/statistiques.jsp";

    private final StatistiqueService statistiqueService = new StatistiqueService();
    private final AnalyseStatistiquesService analyseStatistiquesService = new AnalyseStatistiquesService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int totalClients = statistiqueService.compterTotalClients();
        int nouveauxClients = statistiqueService.compterNouveauxClients();
        int contratsActifs = statistiqueService.compterContratsActifs();
        int contratsExpires = statistiqueService.compterContratsExpires();
        int contratsResilies = statistiqueService.compterContratsResilies();
        BigDecimal chiffreAffaires = statistiqueService.calculerChiffreAffaires();
        BigDecimal totalPrimes = statistiqueService.calculerTotalPrimes();
        BigDecimal totalIndemnisations = statistiqueService.calculerTotalIndemnisations();
        
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
        request.setAttribute("evolutionRevenus", statistiqueService.evolutionRevenusParMois());

        if ("true".equals(request.getParameter("analyser"))) {
            String analyseIa = analyseStatistiquesService.analyser(
                    totalClients, nouveauxClients,
                    contratsActifs, contratsExpires, contratsResilies,
                    chiffreAffaires, totalPrimes, totalIndemnisations);
            request.setAttribute("analyseIa", analyseIa);
        }

        request.getRequestDispatcher(VUE).forward(request, response);
    }
}