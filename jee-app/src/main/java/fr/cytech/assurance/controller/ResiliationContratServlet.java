package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
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
 * Résiliation d'un contrat depuis sa fiche. L'adresse appelée dit qui résilie
 * (même principe que ActionTypeAssuranceServlet) :
 *   /agent/contrats/resilier  : un agent, n'importe quel contrat (pas l'admin, qui consulte seulement)
 *   /client/contrats/resilier : le client connecté, seulement l'un de ses contrats
 * RoleFiltre a déjà vérifié le rôle d'après le préfixe. En POST uniquement.
 */
@WebServlet({"/agent/contrats/resilier", "/client/contrats/resilier"})
public class ResiliationContratServlet extends HttpServlet {

    private static final int LONGUEUR_MAX_MOTIF = 255;   // colonne motif_resiliation

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

        String motif = Validateur.nettoyer(request.getParameter("motif"));
        v.longueur("motif", motif, "Le motif", LONGUEUR_MAX_MOTIF);
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("motif"));
            response.sendRedirect(fiche);
            return;
        }

        try {
            if (parClient) {
                // Le client vient de la session : le service vérifie que le contrat est bien le sien
                Client client = clientService.trouverParUtilisateur(connecte.getId());
                contratService.resilierParClient(id, client == null ? null : client.getId(), motif);
            } else {
                contratService.resilierParAgent(id, motif, connecte.getId());
            }
            response.sendRedirect(fiche + "&resilie=1");
        } catch (ContratInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(fiche);
        }
    }
}
