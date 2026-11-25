package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.MotDePasseIncorrectException;
import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.exception.UtilisateurInvalideException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Page « Mon compte » de l'utilisateur connecté : profil, mot de passe et suppression du compte
 * (remplace CompteServlet, ProfilServlet, MotDePasseServlet et SuppressionCompteServlet).
 */
@Controller
public class CompteController {

    private static final String VUE_PROFIL = "compte/profil";
    private static final String VUE_MOT_DE_PASSE = "compte/mot-de-passe";
    private static final String VUE_SUPPRIMER = "compte/supprimer";

    private final UtilisateurService utilisateurService;

    /** Dépendances injectées par Spring. */
    public CompteController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** Informations de l'utilisateur connecté et liens vers les modifications. */
    @GetMapping("/compte")
    public String compte(HttpServletRequest request) {
        // Rechargé depuis la base : la copie en session date de la connexion
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        Utilisateur utilisateur = utilisateurService.trouverParId(connecte.getId());
        SessionUtilisateur.mettreAJour(request, utilisateur);
        request.setAttribute("compte", utilisateur);
        return "compte/compte";
    }

    /** Formulaire du profil, prérempli. */
    @GetMapping("/compte/profil")
    public String profil(HttpServletRequest request) {
        Utilisateur utilisateur = utilisateurService.trouverParId(SessionUtilisateur.connecte(request).getId());
        request.setAttribute("nom", utilisateur.getNom());
        request.setAttribute("prenom", utilisateur.getPrenom());
        request.setAttribute("email", utilisateur.getEmail());
        return VUE_PROFIL;
    }

    /** Vérifie les saisies, puis enregistre le nom, le prénom et l'email. */
    @PostMapping("/compte/profil")
    public String modifierProfil(HttpServletRequest request) {
        // L'id vient de la session, jamais du formulaire : on ne peut modifier que son propre compte
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String prenom = Validateur.nettoyer(request.getParameter("prenom"));
        String email = Validateur.nettoyer(request.getParameter("email"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.obligatoire("prenom", prenom, "Le prénom", 100);
        v.email("email", email);
        if (v.estValide()) {
            try {
                utilisateurService.modifierProfil(connecte.getId(), nom, prenom, email);
            } catch (UtilisateurDejaExistantException e) {
                v.erreur("email", "Cet email est déjà utilisé par un autre compte.");
            }
        }

        if (!v.estValide()) {
            // Réaffiche le formulaire avec les valeurs saisies et les messages
            request.setAttribute("erreurs", v.getErreurs());
            request.setAttribute("nom", nom);
            request.setAttribute("prenom", prenom);
            request.setAttribute("email", email);
            return VUE_PROFIL;
        }
        // Met la session à jour pour que les pages affichent le nouveau nom
        SessionUtilisateur.mettreAJour(request, utilisateurService.trouverParId(connecte.getId()));
        return "redirect:/compte?succes=profil";
    }

    /** Formulaire de changement du mot de passe. */
    @GetMapping("/compte/mot-de-passe")
    public String motDePasse() {
        return VUE_MOT_DE_PASSE;
    }

    /** Vérifie le nouveau mot de passe, puis le service vérifie l'ancien et enregistre. */
    @PostMapping("/compte/mot-de-passe")
    public String changerMotDePasse(HttpServletRequest request) {
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        String actuel = request.getParameter("actuel");
        String nouveau = request.getParameter("nouveau");
        String confirmation = request.getParameter("confirmation");

        Validateur v = new Validateur();
        if (actuel == null || actuel.isEmpty()) {
            v.erreur("actuel", "Mot de passe actuel incorrect.");
        }
        v.nouveauMotDePasse("nouveau", nouveau, "Le nouveau mot de passe", "confirmation", confirmation);
        if (v.estValide()) {
            try {
                utilisateurService.changerMotDePasse(connecte.getId(), actuel, nouveau);
            } catch (MotDePasseIncorrectException e) {
                v.erreur("actuel", e.getMessage());
            }
        }

        if (!v.estValide()) {
            // Les mots de passe ne sont jamais renvoyés dans le formulaire
            request.setAttribute("erreurs", v.getErreurs());
            return VUE_MOT_DE_PASSE;
        }
        return "redirect:/compte?succes=motdepasse";
    }

    /** Page de confirmation de la suppression, avec le mot de passe à saisir (interdite à l'administrateur). */
    @GetMapping("/compte/supprimer")
    public String suppression(HttpServletRequest request) {
        if (SessionUtilisateur.connecte(request).getRole() == Role.ADMIN) {
            return "redirect:/compte";
        }
        return VUE_SUPPRIMER;
    }

    /** Le service vérifie le mot de passe, puis efface ou désactive le compte ; l'utilisateur est ensuite déconnecté. */
    @PostMapping("/compte/supprimer")
    public String supprimer(HttpServletRequest request) {
        Utilisateur connecte = SessionUtilisateur.connecte(request);
        String motDePasse = request.getParameter("motDePasse");

        String erreur;
        if (motDePasse == null || motDePasse.isEmpty()) {
            erreur = "Le mot de passe est obligatoire.";
        } else {
            try {
                boolean efface = utilisateurService.supprimerCompte(connecte.getId(), motDePasse);
                SessionUtilisateur.deconnecter(request);
                // Message affiché sur la page d'accueil
                return "redirect:/?compte=" + (efface ? "supprime" : "desactive");
            } catch (MotDePasseIncorrectException e) {
                erreur = "Mot de passe incorrect.";
            } catch (UtilisateurInvalideException e) {
                erreur = e.getMessage();
            }
        }
        // Le mot de passe n'est jamais renvoyé dans le formulaire
        request.setAttribute("erreur", erreur);
        return VUE_SUPPRIMER;
    }
}
