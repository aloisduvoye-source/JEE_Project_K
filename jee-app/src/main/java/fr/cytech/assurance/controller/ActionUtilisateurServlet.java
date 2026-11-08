package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.UtilisateurInvalideException;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Boutons de la liste des utilisateurs, pour l'administrateur.
 * L'action dépend de l'adresse appelée (même principe que ActionTypeAssuranceServlet) :
 *   /desactiver : le compte ne peut plus se connecter, son historique est conservé
 *   /reactiver  : le compte peut de nouveau se connecter
 * En POST uniquement : un simple lien (GET) ne doit pas pouvoir modifier des données.
 */
@WebServlet({"/admin/utilisateurs/desactiver", "/admin/utilisateurs/reactiver"})
public class ActionUtilisateurServlet extends HttpServlet {

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Retour à la liste avec le même filtre par rôle qu'avant le clic
        Role role = UtilisateursServlet.roleOuNull(request.getParameter("role"));
        String liste = request.getContextPath() + "/admin/utilisateurs?" + (role == null ? "" : "role=" + role + "&");

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le compte");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(liste);
            return;
        }

        boolean desactiver = "/admin/utilisateurs/desactiver".equals(request.getServletPath());
        try {
            utilisateurService.changerEtat(id, !desactiver);
            // Paramètre ajouté à l'adresse de la liste pour afficher le message de confirmation
            response.sendRedirect(liste + (desactiver ? "desactive" : "reactive") + "=1");
        } catch (UtilisateurInvalideException e) {
            // Compte introuvable, ou compte administrateur
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(liste);
        }
    }
}
