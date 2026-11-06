package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.GarantieInvalideException;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.service.GarantieService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Modification d'une garantie du référentiel, pour l'administrateur.
 *   GET  /admin/garanties/modifier?id=... : affiche le formulaire pré-rempli
 *   POST : vérifie les saisies, enregistre puis redirige vers la liste (PRG)
 * URL protégée automatiquement par RoleFiltre : le préfixe /admin est réservé aux ADMIN.
 */
@WebServlet("/admin/garanties/modifier")
public class ModifierGarantieServlet extends HttpServlet {

    private final GarantieService garantieService = new GarantieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Identifiant facultatif dans l'adresse : null s'il est absent ou non numérique
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Garantie garantie = id == null ? null : garantieService.trouver(id);
        if (garantie == null) {
            MessageFlash.erreur(request, "Cette garantie n'existe pas.");
            response.sendRedirect(request.getContextPath() + "/admin/garanties");
            return;
        }

        // Le formulaire est pré-rempli avec les valeurs actuelles de la garantie
        reafficherFormulaire(request, response, garantie,
                garantie.getNom(), garantie.getDescription(),
                montantEnTexte(garantie.getPlafond()), montantEnTexte(garantie.getFranchise()),
                null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String liste = request.getContextPath() + "/admin/garanties";

        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));
        String plafondSaisi = Validateur.nettoyer(request.getParameter("plafond"));
        String franchiseSaisie = Validateur.nettoyer(request.getParameter("franchise"));

        Validateur v = new Validateur();
        Long id = Validateur.idOuNull(request.getParameter("id"));
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal plafond = v.montantFacultatif("plafond", plafondSaisi, "Le plafond");
        BigDecimal franchise = v.montantFacultatif("franchise", franchiseSaisie, "La franchise");

        Garantie garantie = id == null ? null : garantieService.trouver(id);
        if (garantie == null) {
            // Adresse modifiée à la main, ou garantie supprimée entre-temps
            MessageFlash.erreur(request, "Cette garantie n'existe pas.");
            response.sendRedirect(liste);
            return;
        }

        if (!v.estValide()) {
            reafficherFormulaire(request, response, garantie,
                    nom, description, plafondSaisi, franchiseSaisie, v.getErreurs());
            return;
        }

        try {
            garantieService.modifier(id, nom, description, plafond, franchise);
        } catch (GarantieInvalideException e) {
            // Nom déjà pris, montant négatif... : le message de la règle métier s'affiche en haut du formulaire
            v.erreur("global", e.getMessage());
            reafficherFormulaire(request, response, garantie,
                    nom, description, plafondSaisi, franchiseSaisie, v.getErreurs());
            return;
        }

        // Post-Redirect-Get : un rechargement de la page ne renvoie pas le formulaire
        response.sendRedirect(liste + "?modifie=1");
    }

    // Ré-affiche le formulaire, pré-rempli, avec les messages d'erreur (ou sans si null)
    private void reafficherFormulaire(HttpServletRequest request, HttpServletResponse response,
                                      Garantie garantie, String nom, String description,
                                      String plafond, String franchise, Map<String, String> erreurs)
            throws ServletException, IOException {
        request.setAttribute("garantie", garantie);
        request.setAttribute("nom", nom);
        request.setAttribute("description", description);
        request.setAttribute("plafond", plafond);
        request.setAttribute("franchise", franchise);
        if (erreurs != null) {
            request.setAttribute("erreurs", erreurs);
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/garantie-modifier.jsp")
                .forward(request, response);
    }

    // Affiche un montant dans un champ de formulaire (chaîne vide s'il est absent)
    private String montantEnTexte(BigDecimal montant) {
        return montant == null ? "" : montant.toPlainString();
    }
}
