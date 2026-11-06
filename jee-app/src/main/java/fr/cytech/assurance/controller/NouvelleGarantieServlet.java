package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.GarantieInvalideException;
import fr.cytech.assurance.service.GarantieService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Création d'une garantie du référentiel, pour l'agent (et l'administrateur) : §2.1 du sujet.
 *   GET  : affiche le formulaire vide
 *   POST : vérifie les saisies, enregistre puis redirige vers la liste (PRG)
 * URL protégée automatiquement par RoleFiltre : le préfixe /agent est ouvert aux AGENT et ADMIN.
 */
@WebServlet("/agent/garanties/nouvelle")
public class NouvelleGarantieServlet extends HttpServlet {

    private final GarantieService garantieService = new GarantieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/agent/garantie-nouvelle.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));
        String plafondSaisi = Validateur.nettoyer(request.getParameter("plafond"));
        String franchiseSaisie = Validateur.nettoyer(request.getParameter("franchise"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal plafond = v.montantFacultatif("plafond", plafondSaisi, "Le plafond");
        BigDecimal franchise = v.montantFacultatif("franchise", franchiseSaisie, "La franchise");

        if (!v.estValide()) {
            reafficherFormulaire(request, response, nom, description, plafondSaisi, franchiseSaisie, v);
            return;
        }

        try {
            garantieService.creer(nom, description, plafond, franchise);
        } catch (GarantieInvalideException e) {
            // Nom déjà pris, montant négatif... : le message de la règle métier s'affiche en haut du formulaire
            v.erreur("global", e.getMessage());
            reafficherFormulaire(request, response, nom, description, plafondSaisi, franchiseSaisie, v);
            return;
        }

        // Post-Redirect-Get : un rechargement de la page ne renvoie pas le formulaire
        response.sendRedirect(request.getContextPath() + "/agent/garanties?cree=1");
    }

    // Ré-affiche le formulaire avec les saisies de l'utilisateur et les messages d'erreur
    private void reafficherFormulaire(HttpServletRequest request, HttpServletResponse response,
                                      String nom, String description, String plafond, String franchise,
                                      Validateur v) throws ServletException, IOException {
        request.setAttribute("nom", nom);
        request.setAttribute("description", description);
        request.setAttribute("plafond", plafond);
        request.setAttribute("franchise", franchise);
        request.setAttribute("erreurs", v.getErreurs());
        request.getRequestDispatcher("/WEB-INF/views/agent/garantie-nouvelle.jsp")
                .forward(request, response);
    }
}
