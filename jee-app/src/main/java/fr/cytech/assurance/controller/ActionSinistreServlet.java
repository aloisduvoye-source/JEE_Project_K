package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Traitement d'un sinistre par un agent, depuis sa fiche. En POST uniquement, paramètre "action" :
 *   prendre    : DECLARE → EN_ANALYSE, l'agent devient responsable du dossier
 *   accepter   : EN_ANALYSE → ACCEPTE
 *   refuser    : EN_ANALYSE → REFUSE, avec un motif obligatoire (ajouté à la discussion)
 *   indemniser : ACCEPTE → INDEMNISE, avec la garantie appliquée et le dommage retenu (le montant est calculé)
 * L'admin consulte seulement : RoleFiltre le laisse entrer dans /agent, cette servlet le refuse.
 */
@WebServlet("/agent/sinistres/action")
public class ActionSinistreServlet extends HttpServlet {

    private static final int LONGUEUR_MAX_MOTIF = 1000;

    private final SinistreService sinistreService = new SinistreService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utilisateur agent = SessionUtilisateur.connecte(request);
        if (agent.getRole() != Role.AGENT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le sinistre");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(request.getContextPath() + "/agent/sinistres");
            return;
        }
        String fiche = request.getContextPath() + "/agent/sinistres/detail?id=" + id;

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
        response.sendRedirect(fiche);
    }
}
