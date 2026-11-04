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

// Archivage ou réactivation d'un type d'assurance par l'administrateur, depuis la liste des types
// Paramètre "action" : "archiver" (plus proposé pour les nouveaux contrats) ou "reactiver"
// En POST uniquement : un simple lien (GET) ne doit pas pouvoir modifier des données
@WebServlet("/admin/types-assurance/archiver")
public class ArchiverTypeAssuranceServlet extends HttpServlet {

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

        boolean reactiver = Validateur.nettoyer(request.getParameter("action")).equals("reactiver");
        try {
            if (reactiver) {
                typeAssuranceService.reactiver(id);
                response.sendRedirect(liste + "?reactive=1");
            } else {
                typeAssuranceService.archiver(id);
                response.sendRedirect(liste + "?archive=1");
            }
        } catch (TypeAssuranceInvalideException e) {
            // Type introuvable
            MessageFlash.erreur(request, e.getMessage());
            response.sendRedirect(liste);
        }
    }
}
