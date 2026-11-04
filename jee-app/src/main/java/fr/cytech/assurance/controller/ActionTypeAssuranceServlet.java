package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Boutons de la liste des types d'assurance, pour l'administrateur.
 * L'action dépend de l'adresse appelée (même principe que EspaceAccueilServlet) :
 *   /supprimer : seulement un type jamais utilisé par un contrat
 *   /archiver  : le type n'est plus proposé pour les nouveaux contrats
 *   /reactiver : le type est de nouveau proposé
 * En POST uniquement : un simple lien (GET) ne doit pas pouvoir modifier des données.
 */
@WebServlet({"/admin/types-assurance/supprimer", "/admin/types-assurance/archiver", "/admin/types-assurance/reactiver"})
public class ActionTypeAssuranceServlet extends HttpServlet {

    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String liste = request.getContextPath() + "/admin/types-assurance";

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le type d'assurance");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(liste);
            return;
        }

        try {
            // Paramètre ajouté à l'adresse de la liste pour afficher le message de confirmation
            String confirmation = switch (request.getServletPath()) {
                case "/admin/types-assurance/supprimer" -> {
                    typeAssuranceService.supprimer(id);
                    yield "supprime";
                }
                case "/admin/types-assurance/archiver" -> {
                    typeAssuranceService.archiver(id);
                    yield "archive";
                }
                case "/admin/types-assurance/reactiver" -> {
                    typeAssuranceService.reactiver(id);
                    yield "reactive";
                }
                default -> throw new IllegalStateException("Adresse non prévue : " + request.getServletPath());
            };
            response.sendRedirect(liste + "?" + confirmation + "=1");
        } catch (TypeAssuranceInvalideException e) {
            // Type introuvable, ou utilisé par un contrat (suppression)
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(liste);
        }
    }
}
