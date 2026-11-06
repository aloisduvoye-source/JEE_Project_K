package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.GarantieInvalideException;
import fr.cytech.assurance.service.GarantieService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Suppression d'une garantie du référentiel, depuis le bouton de la liste des garanties.
 * En POST uniquement : un simple lien (GET) ne doit pas pouvoir modifier des données.
 * URL protégée automatiquement par RoleFiltre : le préfixe /agent est ouvert aux AGENT et ADMIN.
 */
@WebServlet("/agent/garanties/supprimer")
public class SuppressionGarantieServlet extends HttpServlet {

    private final GarantieService garantieService = new GarantieService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String liste = request.getContextPath() + "/agent/garanties";

        // Même lecture de l'identifiant que pour les types d'assurance
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "La garantie");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(liste);
            return;
        }

        try {
            garantieService.supprimer(id);
        } catch (GarantieInvalideException e) {
            // Garantie introuvable, ou souscrite par au moins un contrat : le message s'affichera sur la liste
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(liste);
            return;
        }

        // Post-Redirect-Get : retour à la liste, avec un paramètre pour le message de confirmation
        response.sendRedirect(liste + "?supprime=1");
    }
}
