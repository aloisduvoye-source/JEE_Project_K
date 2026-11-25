package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Listes des paiements, filtrables par statut et mode, triables (remplace PaiementsServlet et MesPaiementsServlet).
 */
@Controller
public class PaiementController {

    private final PaiementService paiementService;
    private final ClientService clientService;

    /** Dépendances injectées par Spring. */
    public PaiementController(PaiementService paiementService, ClientService clientService) {
        this.paiementService = paiementService;
        this.clientService = clientService;
    }

    /** Tous les paiements, pour les agents et l'admin. */
    @GetMapping("/agent/paiements")
    public String tous(HttpServletRequest request) {
        StatutPaiement statut = Validateur.enumOuNull(StatutPaiement.class, request.getParameter("statut"));
        ModePaiement mode = Validateur.enumOuNull(ModePaiement.class, request.getParameter("mode"));
        String tri = request.getParameter("tri");
        request.setAttribute("paiements", paiementService.listerTous(statut, mode, tri));
        preparerFiltres(request, statut, mode, tri);
        return "agent/paiements";
    }

    /** Paiements et échéances du client connecté. */
    @GetMapping("/client/paiements")
    public String duClient(HttpServletRequest request) {
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
        StatutPaiement statut = Validateur.enumOuNull(StatutPaiement.class, request.getParameter("statut"));
        ModePaiement mode = Validateur.enumOuNull(ModePaiement.class, request.getParameter("mode"));
        String tri = request.getParameter("tri");
        request.setAttribute("paiements", client == null
                ? List.of()
                : paiementService.listerPourClient(client.getId(), statut, mode, tri));
        preparerFiltres(request, statut, mode, tri);
        return "client/paiements";
    }

    /** Choix proposés dans les filtres et valeurs sélectionnées. */
    private static void preparerFiltres(HttpServletRequest request, StatutPaiement statut, ModePaiement mode, String tri) {
        request.setAttribute("statuts", StatutPaiement.values());
        request.setAttribute("modes", ModePaiement.values());
        request.setAttribute("statutSelectionne", statut);
        request.setAttribute("modeSelectionne", mode);
        request.setAttribute("triSelectionne", tri == null ? "echeance_desc" : tri);
    }
}
