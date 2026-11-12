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
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Actions sur un contrat depuis sa fiche (§2.2). L'adresse appelée dit l'action et qui la fait
 * (même principe que ResiliationContratServlet) :
 *   /agent/contrats/suspendre          : un agent suspend un contrat actif, avec un motif obligatoire
 *   /agent/contrats/lever-suspension   : un agent lève la suspension
 *   /agent/contrats/renouveler         : un agent renouvelle n'importe quel contrat
 *   /client/contrats/renouveler        : le client connecté renouvelle l'un de ses contrats
 * L'admin consulte seulement : RoleFiltre le laisse entrer dans /agent, cette servlet le refuse.
 * En POST uniquement.
 */
@WebServlet({"/agent/contrats/suspendre", "/agent/contrats/lever-suspension",
        "/agent/contrats/renouveler", "/client/contrats/renouveler"})
public class ActionContratServlet extends HttpServlet {

    private static final int LONGUEUR_MAX_MOTIF = 255;   // colonne motif_suspension

    private final ContratService contratService = new ContratService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        boolean parClient = request.getServletPath().startsWith("/client");
        if (!parClient && connecte.getRole() != Role.AGENT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String espace = request.getContextPath() + (parClient ? "/client" : "/agent") + "/contrats";

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le contrat");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(espace);
            return;
        }
        String fiche = espace + "/detail?id=" + id;

        try {
            switch (request.getServletPath()) {
                case "/agent/contrats/suspendre" -> {
                    String motif = Validateur.nettoyer(request.getParameter("motif"));
                    v.obligatoire("motif", motif, "Le motif de la suspension", LONGUEUR_MAX_MOTIF);
                    if (!v.estValide()) {
                        MessageFlash.erreur(request, v.getErreurs().get("motif"));
                        response.sendRedirect(fiche);
                        return;
                    }
                    contratService.suspendre(id, motif);
                    response.sendRedirect(fiche + "&suspendu=1");
                }
                case "/agent/contrats/lever-suspension" -> {
                    contratService.leverSuspension(id);
                    response.sendRedirect(fiche + "&reactive=1");
                }
                default -> {
                    // Renouvellement : la fiche du nouveau contrat s'ouvre, avec son paiement à régler
                    Contrat nouveau;
                    if (parClient) {
                        // Le client vient de la session : le service vérifie que le contrat est bien le sien
                        Client client = clientService.trouverParUtilisateur(connecte.getId());
                        nouveau = contratService.renouvelerParClient(id, client == null ? null : client.getId());
                    } else {
                        nouveau = contratService.renouvelerParAgent(id);
                    }
                    response.sendRedirect(espace + "/detail?id=" + nouveau.getId() + "&renouvele=1");
                }
            }
        } catch (ContratInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(fiche);
        }
    }
}
