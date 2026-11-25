package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.AnalyseStatistiquesService;
import fr.cytech.assurance.service.StatistiqueService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Page Statistiques : clients, contrats et finances, avec des graphiques (remplace StatistiquesServlet).
 * Le préfixe /agent est ouvert aux AGENT et ADMIN par RoleIntercepteur.
 */
@Controller
public class StatistiqueController {

    /** Synthèse IA gardée en session le temps de la redirection (comme MessageFlash), effacée par la JSP. */
    private static final String ATTRIBUT_ANALYSE = "analyseIa";

    private final StatistiqueService statistiqueService;
    private final AnalyseStatistiquesService analyseStatistiquesService;

    /** Dépendances injectées par Spring. */
    public StatistiqueController(StatistiqueService statistiqueService,
                                 AnalyseStatistiquesService analyseStatistiquesService) {
        this.statistiqueService = statistiqueService;
        this.analyseStatistiquesService = analyseStatistiquesService;
    }

    /** Tous les indicateurs de la page. */
    @GetMapping("/agent/statistiques")
    public String afficher(HttpServletRequest request) {
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
        return "agent/statistiques";
    }

    /**
     * Demande la synthèse IA, puis revient sur la page (en GET) qui l'affiche une fois.
     * En POST, et suivi d'une redirection : recharger la page ne relance pas un appel à l'API.
     */
    @PostMapping("/agent/statistiques")
    public String analyser(HttpServletRequest request) {
        request.getSession().setAttribute(ATTRIBUT_ANALYSE, analyseStatistiquesService.analyser());
        return "redirect:/agent/statistiques#analyse";
    }
}
