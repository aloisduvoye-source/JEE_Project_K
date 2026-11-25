package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.model.enums.StatutSinistre;
import fr.cytech.assurance.service.PieceJustificativeService;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Sinistres côté agent (traitement) et admin (consultation)
 * (remplace SinistresServlet, SinistreDetailServlet et ActionSinistreServlet).
 */
@Controller
public class SinistreAgentController {

    private static final int LONGUEUR_MAX_MOTIF = 1000;

    private final SinistreService sinistreService;
    private final PieceJustificativeService pieceService;

    /** Dépendances injectées par Spring. */
    public SinistreAgentController(SinistreService sinistreService, PieceJustificativeService pieceService) {
        this.sinistreService = sinistreService;
        this.pieceService = pieceService;
    }

    /**
     * Liste des sinistres. Filtres facultatifs dans l'adresse :
     * ?statut=DECLARE (file de traitement), ?mien=1 (dossiers de l'agent connecté).
     */
    @GetMapping("/agent/sinistres")
    public String lister(HttpServletRequest request) {
        StatutSinistre statut = Validateur.enumOuNull(StatutSinistre.class, request.getParameter("statut"));
        boolean mien = "1".equals(request.getParameter("mien"));
        Long idAgent = mien ? SessionUtilisateur.connecte(request).getId() : null;

        request.setAttribute("sinistres", sinistreService.lister(statut, idAgent));
        request.setAttribute("statuts", StatutSinistre.values());
        request.setAttribute("statutChoisi", statut);
        request.setAttribute("mien", mien);
        return "agent/sinistres";
    }

    /** Fiche d'un sinistre, avec ses pièces, sa discussion et les indemnisations possibles. */
    @GetMapping("/agent/sinistres/detail")
    public String detail(HttpServletRequest request) {
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Sinistre sinistre = id == null ? null : sinistreService.trouverAvecDetails(id);
        if (sinistre == null) {
            MessageFlash.erreur(request, "Ce sinistre n'existe pas.");
            return "redirect:/agent/sinistres";
        }
        request.setAttribute("sinistre", sinistre);
        request.setAttribute("indemnisation", sinistreService.indemnisation(id));
        request.setAttribute("pieces", pieceService.lister(id));
        request.setAttribute("commentaires", sinistreService.discussion(id));
        request.setAttribute("clos", SinistreService.estClos(sinistre));
        request.setAttribute("propositions", sinistreService.propositions(sinistre));
        return "agent/sinistre-detail";
    }

    /**
     * Traitement par un agent, en POST uniquement, paramètre "action" :
     * prendre (DECLARE → EN_ANALYSE), accepter (EN_ANALYSE → ACCEPTE), refuser (EN_ANALYSE → REFUSE, motif obligatoire)
     * ou indemniser (ACCEPTE → INDEMNISE, avec la garantie appliquée et le dommage retenu).
     * L'admin consulte seulement : erreur 403.
     */
    @PostMapping("/agent/sinistres/action")
    public String agir(HttpServletRequest request) {
        Utilisateur agent = SessionUtilisateur.connecte(request);
        if (agent.getRole() != Role.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le sinistre");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return "redirect:/agent/sinistres";
        }

        try {
            switch (Validateur.nettoyer(request.getParameter("action"))) {
                case "prendre" -> sinistreService.prendreEnCharge(id, agent);
                case "accepter" -> sinistreService.accepter(id, agent);
                case "refuser" -> {
                    String motif = Validateur.nettoyer(request.getParameter("motif"));
                    v.obligatoire("motif", motif, "Le motif du refus", LONGUEUR_MAX_MOTIF);
                    if (v.estValide()) {
                        sinistreService.refuser(id, agent, motif);
                    }
                }
                case "indemniser" -> {
                    Long idGarantie = v.identifiant("garantie",
                            Validateur.nettoyer(request.getParameter("garantie")), "La garantie appliquée");
                    BigDecimal dommage = v.montantPositif("dommage",
                            Validateur.nettoyer(request.getParameter("dommage")), "Le dommage retenu");
                    if (v.estValide()) {
                        sinistreService.indemniser(id, agent, idGarantie, dommage);
                    }
                }
                default -> v.erreur("action", "Action inconnue.");
            }
        } catch (SinistreInvalideException e) {
            v.erreur("global", e.getMessage());
        }
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().values().iterator().next());
        }
        return "redirect:/agent/sinistres/detail?id=" + id;
    }
}
