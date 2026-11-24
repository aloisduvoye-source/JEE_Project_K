package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.PaiementInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrats et garanties du client connecté : il ne voit que les siens
 * (remplace MesContratsServlet, MonContratServlet et PaiementContratServlet).
 */
@Controller
public class ContratClientController {

    private final ClientService clientService;
    private final ContratService contratService;
    private final PaiementService paiementService;
    private final TypeAssuranceService typeAssuranceService;

    /** Dépendances injectées par Spring. */
    public ContratClientController(ClientService clientService, ContratService contratService,
                                   PaiementService paiementService, TypeAssuranceService typeAssuranceService) {
        this.clientService = clientService;
        this.contratService = contratService;
        this.paiementService = paiementService;
        this.typeAssuranceService = typeAssuranceService;
    }

    /** Contrats du client avec leurs garanties, filtrables et triables. */
    @GetMapping("/client/contrats")
    public String contrats(HttpServletRequest request) {
        preparerContrats(request);
        return "client/contrats";
    }

    /** Garanties de chaque contrat du client : mêmes données que « Mes contrats », présentées en tableau. */
    @GetMapping("/client/garanties")
    public String garanties(HttpServletRequest request) {
        preparerContrats(request);
        return "client/garanties";
    }

    /** Fiche d'un contrat du client, avec son paiement. */
    @GetMapping("/client/contrats/detail")
    public String detail(HttpServletRequest request) {
        // Le client vient de la session, jamais de l'adresse : un id de contrat d'un autre client ne donne rien
        Client client = client(request);
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = client == null || id == null ? null : contratService.trouverDuClient(id, client.getId());
        if (contrat == null) {
            MessageFlash.erreur(request, "Ce contrat n'existe pas.");
            return "redirect:/client/contrats";
        }
        request.setAttribute("contrat", contrat);
        request.setAttribute("paiement", paiementService.trouverParContrat(id));
        // Renouvellement, suspension et remboursement : affichés sur la fiche (voir ContratService)
        request.setAttribute("renouvellement", contratService.trouverRenouvellement(id));
        request.setAttribute("renouvelable", contratService.raisonNonRenouvelable(contrat) == null);
        request.setAttribute("remboursement", contratService.trouverRemboursement(id));
        request.setAttribute("remboursementSiResiliation", contratService.remboursement(contrat, LocalDate.now()));
        request.setAttribute("modes", ModePaiement.values());
        return "client/contrat-detail";
    }

    /** Paiement d'un contrat depuis sa fiche, en une fois, à la date du jour. En POST uniquement. */
    @PostMapping("/client/contrats/paiement")
    public String payer(HttpServletRequest request) {
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le contrat");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return "redirect:/client/contrats";
        }
        String fiche = "redirect:/client/contrats/detail?id=" + id;
        ModePaiement mode = Validateur.enumOuNull(ModePaiement.class, request.getParameter("mode"));
        if (mode == null) {
            MessageFlash.erreur(request, "Choisissez un mode de paiement.");
            return fiche;
        }
        // Le client vient de la session : le service vérifie que le contrat est bien le sien
        Client client = client(request);
        try {
            paiementService.payer(id, client == null ? null : client.getId(), mode);
            return fiche + "&paye=1";
        } catch (PaiementInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            return fiche;
        }
    }

    /** Contrats du client connecté, filtrés (?statut=, ?type=) et triés (?tri=), pour les deux pages de liste. */
    private void preparerContrats(HttpServletRequest request) {
        // Le client vient de la session, jamais de l'adresse : impossible de voir les contrats d'un autre
        Client client = client(request);
        StatutContrat statut = Validateur.enumOuNull(StatutContrat.class, request.getParameter("statut"));
        Long idType = Validateur.idOuNull(request.getParameter("type"));
        String tri = Validateur.nettoyer(request.getParameter("tri"));

        request.setAttribute("contrats", client == null
                ? List.of()
                : contratService.listerContratsDuClient(client.getId(), statut, idType, tri));
        request.setAttribute("statutChoisi", statut);
        request.setAttribute("typeChoisi", idType);
        request.setAttribute("triChoisi", tri.isEmpty() ? "debut_desc" : tri);
        request.setAttribute("statuts", StatutContrat.values());
        request.setAttribute("types", typeAssuranceService.listerAvecGaranties());
        request.setAttribute("recherche", statut != null || idType != null);
    }

    /** Fiche client de l'utilisateur connecté. */
    private Client client(HttpServletRequest request) {
        return clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
    }
}
