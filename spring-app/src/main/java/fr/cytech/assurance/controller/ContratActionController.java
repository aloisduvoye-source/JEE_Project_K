package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Actions sur un contrat depuis sa fiche, en POST uniquement (§2.2) : résilier, suspendre, lever la suspension,
 * renouveler (remplace ResiliationContratServlet et ActionContratServlet). L'adresse dit qui agit :
 * /agent/... pour un agent (l'admin consulte seulement : erreur 403), /client/... pour le client connecté.
 */
@Controller
public class ContratActionController {

    private static final int LONGUEUR_MAX_MOTIF = 255;   // colonnes motif_resiliation et motif_suspension

    private final ContratService contratService;
    private final ClientService clientService;

    /** Dépendances injectées par Spring. */
    public ContratActionController(ContratService contratService, ClientService clientService) {
        this.contratService = contratService;
        this.clientService = clientService;
    }

    /** Résiliation : un agent résilie n'importe quel contrat, le client seulement l'un des siens. */
    @PostMapping("/{espace:agent|client}/contrats/resilier")
    public String resilier(@PathVariable String espace, HttpServletRequest request) {
        Utilisateur connecte = utilisateurAutorise(espace, request);
        Long id = contratDemande(request);
        if (id == null) {
            return "redirect:/" + espace + "/contrats";
        }
        String fiche = fiche(espace, id);

        Validateur v = new Validateur();
        String motif = Validateur.nettoyer(request.getParameter("motif"));
        v.longueur("motif", motif, "Le motif", LONGUEUR_MAX_MOTIF);
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("motif"));
            return fiche;
        }
        try {
            if (espace.equals("client")) {
                // Le client vient de la session : le service vérifie que le contrat est bien le sien
                contratService.resilierParClient(id, idClient(connecte), motif);
            } else {
                contratService.resilierParAgent(id, motif, connecte.getId());
            }
            return fiche + "&resilie=1";
        } catch (ContratInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            return fiche;
        }
    }

    /** Un agent suspend un contrat actif, avec un motif obligatoire. */
    @PostMapping("/agent/contrats/suspendre")
    public String suspendre(HttpServletRequest request) {
        utilisateurAutorise("agent", request);
        Long id = contratDemande(request);
        if (id == null) {
            return "redirect:/agent/contrats";
        }
        String fiche = fiche("agent", id);

        Validateur v = new Validateur();
        String motif = Validateur.nettoyer(request.getParameter("motif"));
        v.obligatoire("motif", motif, "Le motif de la suspension", LONGUEUR_MAX_MOTIF);
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("motif"));
            return fiche;
        }
        try {
            contratService.suspendre(id, motif);
            return fiche + "&suspendu=1";
        } catch (ContratInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            return fiche;
        }
    }

    /** Un agent lève la suspension qu'un agent avait décidée. */
    @PostMapping("/agent/contrats/lever-suspension")
    public String leverSuspension(HttpServletRequest request) {
        utilisateurAutorise("agent", request);
        Long id = contratDemande(request);
        if (id == null) {
            return "redirect:/agent/contrats";
        }
        try {
            contratService.leverSuspension(id);
            return fiche("agent", id) + "&reactive=1";
        } catch (ContratInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            return fiche("agent", id);
        }
    }

    /** Renouvellement : la fiche du nouveau contrat s'ouvre, avec son paiement à régler. */
    @PostMapping("/{espace:agent|client}/contrats/renouveler")
    public String renouveler(@PathVariable String espace, HttpServletRequest request) {
        Utilisateur connecte = utilisateurAutorise(espace, request);
        Long id = contratDemande(request);
        if (id == null) {
            return "redirect:/" + espace + "/contrats";
        }
        try {
            Contrat nouveau = espace.equals("client")
                    // Le client vient de la session : le service vérifie que le contrat est bien le sien
                    ? contratService.renouvelerParClient(id, idClient(connecte))
                    : contratService.renouvelerParAgent(id);
            return "redirect:/" + espace + "/contrats/detail?id=" + nouveau.getId() + "&renouvele=1";
        } catch (ContratInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            return fiche(espace, id);
        }
    }

    /** Utilisateur connecté ; dans l'espace agent, seul un AGENT agit (l'admin reçoit une erreur 403). */
    private static Utilisateur utilisateurAutorise(String espace, HttpServletRequest request) {
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        if (espace.equals("agent") && connecte.getRole() != Role.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return connecte;
    }

    /** Id du contrat envoyé par le formulaire, ou null avec un message s'il est absent ou invalide. */
    private static Long contratDemande(HttpServletRequest request) {
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le contrat");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return null;
        }
        return id;
    }

    /** Redirection vers la fiche du contrat dans l'espace donné. */
    private static String fiche(String espace, Long id) {
        return "redirect:/" + espace + "/contrats/detail?id=" + id;
    }

    /** Id de la fiche client de cet utilisateur, ou null s'il n'en a pas. */
    private Long idClient(Utilisateur utilisateur) {
        Client client = clientService.trouverParUtilisateur(utilisateur.getId());
        return client == null ? null : client.getId();
    }
}
