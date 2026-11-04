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

// Suppression d'un type d'assurance par l'administrateur, depuis la liste des types
// En POST uniquement : un simple lien (GET) ne doit pas pouvoir supprimer des données
@WebServlet("/admin/types-assurance/supprimer")
public class SupprimerTypeAssuranceServlet extends HttpServlet {

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
            typeAssuranceService.supprimer(id);
            response.sendRedirect(liste + "?supprime=1");
        } catch (TypeAssuranceInvalideException e) {
            // Type introuvable ou utilisé par un contrat
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(liste);
        }
    }
}
