package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.PaiementInvalideException;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

// Enregistrement du paiement d'un contrat par l'agent, depuis la fiche du contrat (§2.1, « enregistrer les paiements »)
// En POST uniquement : un simple lien (GET) ne doit pas pouvoir modifier des données
@WebServlet("/agent/contrats/paiement")
public class PaiementContratServlet extends HttpServlet {

    private final PaiementService paiementService = new PaiementService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le contrat");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(request.getContextPath() + "/agent/contrats");
            return;
        }
        String fiche = request.getContextPath() + "/agent/contrats/detail?id=" + id;

        ModePaiement mode = modeOuNull(request.getParameter("mode"));
        if (mode == null) {
            v.erreur("mode", "Choisissez un mode de paiement.");
        }
        String dateSaisie = Validateur.nettoyer(request.getParameter("datePaiement"));
        if (dateSaisie.isEmpty()) {
            v.erreur("datePaiement", "La date de paiement est obligatoire.");
        }
        LocalDate datePaiement = v.datePassee("datePaiement", dateSaisie, "La date de paiement");

        if (!v.estValide()) {
            // Une seule erreur affichée, au-dessus du formulaire de la fiche
            MessageFlash.erreur(request, v.getErreurs().values().iterator().next());
            response.sendRedirect(fiche);
            return;
        }

        try {
            paiementService.enregistrer(id, mode, datePaiement);
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
