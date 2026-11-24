package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.GarantieInvalideException;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.service.GarantieService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Référentiel des garanties pour l'agent (et l'administrateur) : §2.1 du sujet
 * (remplace GarantiesServlet, NouvelleGarantieServlet, ModifierGarantieServlet et SuppressionGarantieServlet).
 * Le préfixe /agent est ouvert aux AGENT et ADMIN par RoleIntercepteur.
 */
@Controller
public class GarantieController {

    private static final String LISTE = "redirect:/agent/garanties";
    private static final String VUE_NOUVELLE = "agent/garantie-nouvelle";
    private static final String VUE_MODIFIER = "agent/garantie-modifier";

    private final GarantieService garantieService;

    /** Dépendances injectées par Spring. */
    public GarantieController(GarantieService garantieService) {
        this.garantieService = garantieService;
    }

    /** Liste des garanties triées par nom. */
    @GetMapping("/agent/garanties")
    public String lister(HttpServletRequest request) {
        request.setAttribute("garanties", garantieService.lister());
        return "agent/garanties";
    }

    /** Formulaire vide de création. */
    @GetMapping("/agent/garanties/nouvelle")
    public String nouvelle() {
        return VUE_NOUVELLE;
    }

    /** Vérifie les saisies, enregistre puis redirige vers la liste (Post-Redirect-Get). */
    @PostMapping("/agent/garanties/nouvelle")
    public String creer(HttpServletRequest request) {
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));
        String plafondSaisi = Validateur.nettoyer(request.getParameter("plafond"));
        String franchiseSaisie = Validateur.nettoyer(request.getParameter("franchise"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal plafond = v.montantFacultatif("plafond", plafondSaisi, "Le plafond");
        BigDecimal franchise = v.montantFacultatif("franchise", franchiseSaisie, "La franchise");
        if (v.estValide()) {
            try {
                garantieService.creer(nom, description, plafond, franchise);
                // Post-Redirect-Get : un rechargement de la page ne renvoie pas le formulaire
                return LISTE + "?cree=1";
            } catch (GarantieInvalideException e) {
                // Nom déjà pris, montant négatif... : le message de la règle métier s'affiche en haut du formulaire
                v.erreur("global", e.getMessage());
            }
        }
        preremplir(request, null, nom, description, plafondSaisi, franchiseSaisie, v.getErreurs());
        return VUE_NOUVELLE;
    }

    /** Formulaire de modification, prérempli avec les valeurs actuelles (?id=...). */
    @GetMapping("/agent/garanties/modifier")
    public String modification(HttpServletRequest request) {
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Garantie garantie = id == null ? null : garantieService.trouver(id);
        if (garantie == null) {
            MessageFlash.erreur(request, "Cette garantie n'existe pas.");
            return LISTE;
        }
        preremplir(request, garantie, garantie.getNom(), garantie.getDescription(),
                montantEnTexte(garantie.getPlafond()), montantEnTexte(garantie.getFranchise()), null);
        return VUE_MODIFIER;
    }

    /** Vérifie les saisies, enregistre puis redirige vers la liste (Post-Redirect-Get). */
    @PostMapping("/agent/garanties/modifier")
    public String modifier(HttpServletRequest request) {
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
            return LISTE;
        }
        if (v.estValide()) {
            try {
                garantieService.modifier(id, nom, description, plafond, franchise);
                return LISTE + "?modifie=1";
            } catch (GarantieInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }
        preremplir(request, garantie, nom, description, plafondSaisi, franchiseSaisie, v.getErreurs());
        return VUE_MODIFIER;
    }

    /** Suppression depuis le bouton de la liste, en POST uniquement : un simple lien ne doit pas modifier de données. */
    @PostMapping("/agent/garanties/supprimer")
    public String supprimer(HttpServletRequest request) {
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "La garantie");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return LISTE;
        }
        try {
            garantieService.supprimer(id);
        } catch (GarantieInvalideException e) {
            // Garantie introuvable, ou souscrite par au moins un contrat : le message s'affichera sur la liste
            MessageFlash.erreur(request, e.getMessage());
            return LISTE;
        }
        return LISTE + "?supprime=1";
    }

    /** Valeurs du formulaire et messages d'erreur (aucun si null) ; garantie null à la création. */
    private void preremplir(HttpServletRequest request, Garantie garantie, String nom, String description,
                            String plafond, String franchise, Map<String, String> erreurs) {
        request.setAttribute("garantie", garantie);
        request.setAttribute("nom", nom);
        request.setAttribute("description", description);
        request.setAttribute("plafond", plafond);
        request.setAttribute("franchise", franchise);
        if (erreurs != null) {
            request.setAttribute("erreurs", erreurs);
        }
    }

    /** Montant affiché dans un champ de formulaire (chaîne vide s'il est absent). */
    private static String montantEnTexte(BigDecimal montant) {
        return montant == null ? "" : montant.toPlainString();
    }
}
