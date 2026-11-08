package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.PaiementInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Paiement d'un contrat par le client connecté, depuis la fiche du contrat (en une fois, à la date du jour)
// En POST uniquement : un simple lien (GET) ne doit pas pouvoir modifier des données
@WebServlet("/client/contrats/paiement")
public class PaiementContratServlet extends HttpServlet {

    private final ClientService clientService = new ClientService();
    private final PaiementService paiementService = new PaiementService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le contrat");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(request.getContextPath() + "/client/contrats");
            return;
        }
        String fiche = request.getContextPath() + "/client/contrats/detail?id=" + id;

        ModePaiement mode = modeOuNull(request.getParameter("mode"));
        if (mode == null) {
            MessageFlash.erreur(request, "Choisissez un mode de paiement.");
            response.sendRedirect(fiche);
            return;
        }

        // Le client vient de la session : le service vérifie que le contrat est bien le sien
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
        try {
            paiementService.payer(id, client == null ? null : client.getId(), mode);
            response.sendRedirect(fiche + "&paye=1");
        } catch (PaiementInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(fiche);
        }
    }

    private static ModePaiement modeOuNull(String valeur) {
        try {
            return ModePaiement.valueOf(Validateur.nettoyer(valeur));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
