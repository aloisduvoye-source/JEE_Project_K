package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.exception.UtilisateurInvalideException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Gestion des comptes par l'administrateur : §2.1, « gérer les utilisateurs »
 * (remplace UtilisateursServlet, ActionUtilisateurServlet et NouvelAgentServlet).
 */
@Controller
public class UtilisateurController {

    private static final String VUE_NOUVEL_AGENT = "admin/agent-nouveau";
    private static final String[] CHAMPS_REAFFICHES = {"identifiant", "nom", "prenom", "email"};

    private final UtilisateurService utilisateurService;

    /** Dépendances injectées par Spring. */
    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** Liste de tous les comptes ; filtre facultatif par rôle (?role=AGENT), une valeur inconnue affiche tout le monde. */
    @GetMapping("/admin/utilisateurs")
    public String lister(HttpServletRequest request) {
        Role role = Validateur.enumOuNull(Role.class, request.getParameter("role"));
        request.setAttribute("role", role);
        request.setAttribute("roles", Role.values());
        request.setAttribute("utilisateurs", utilisateurService.lister(role));
        return "admin/utilisateurs";
    }

    /**
     * Boutons de la liste, en POST uniquement : /desactiver (le compte ne peut plus se connecter,
     * son historique est conservé) ou /reactiver.
     */
    @PostMapping("/admin/utilisateurs/{action:desactiver|reactiver}")
    public String changerEtat(@PathVariable String action, HttpServletRequest request) {
        // Retour à la liste avec le même filtre par rôle qu'avant le clic
        Role role = Validateur.enumOuNull(Role.class, request.getParameter("role"));
        String liste = "redirect:/admin/utilisateurs?" + (role == null ? "" : "role=" + role + "&");

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le compte");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return liste;
        }
        boolean desactiver = action.equals("desactiver");
        try {
            utilisateurService.changerEtat(id, !desactiver);
            // Paramètre ajouté à l'adresse de la liste pour afficher le message de confirmation
            return liste + (desactiver ? "desactive" : "reactive") + "=1";
        } catch (UtilisateurInvalideException e) {
            // Compte introuvable, ou compte administrateur
            MessageFlash.erreur(request, e.getMessage());
            return liste;
        }
    }

    /** Formulaire de création d'un agent : seul moyen d'obtenir un compte AGENT. */
    @GetMapping("/admin/utilisateurs/nouvel-agent")
    public String nouvelAgent() {
        return VUE_NOUVEL_AGENT;
    }

    /** Vérifie les saisies, puis crée le compte agent. */
    @PostMapping("/admin/utilisateurs/nouvel-agent")
    public String creerAgent(HttpServletRequest request) {
        String identifiant = Validateur.nettoyer(request.getParameter("identifiant"));
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String prenom = Validateur.nettoyer(request.getParameter("prenom"));
        String email = Validateur.nettoyer(request.getParameter("email"));
        String motDePasse = request.getParameter("motDePasse");
        String confirmation = request.getParameter("confirmation");

        Validateur v = new Validateur();
        v.obligatoire("identifiant", identifiant, "L'identifiant", 100);
        v.obligatoire("nom", nom, "Le nom", 100);
        v.obligatoire("prenom", prenom, "Le prénom", 100);
        v.email("email", email);
        // Mot de passe provisoire choisi par l'administrateur ; l'agent le change ensuite dans « Mon compte »
        v.nouveauMotDePasse("motDePasse", motDePasse, "Le mot de passe", "confirmation", confirmation);

        // Disponibilité vérifiée seulement si le format est bon, pour signaler toutes les erreurs d'un coup
        if (!v.aErreur("identifiant") && !utilisateurService.identifiantDisponible(identifiant)) {
            v.erreur("identifiant", "Cet identifiant est déjà pris.");
        }
        if (!v.aErreur("email") && !utilisateurService.emailDisponible(email)) {
            v.erreur("email", "Cet email est déjà utilisé.");
        }

        if (v.estValide()) {
            Utilisateur agent = new Utilisateur();
            agent.setIdentifiant(identifiant);
            agent.setNom(nom);
            agent.setPrenom(prenom);
            agent.setEmail(email);
            try {
                utilisateurService.creerAgent(agent, motDePasse);
                return "redirect:/admin/utilisateurs?role=AGENT&agentCree=1";
            } catch (UtilisateurDejaExistantException e) {
                // Pris entre la vérification ci-dessus et l'enregistrement (création simultanée)
                if ("email".equals(e.getChamp())) {
                    v.erreur("email", "Cet email est déjà utilisé.");
                } else if ("identifiant".equals(e.getChamp())) {
                    v.erreur("identifiant", "Cet identifiant est déjà pris.");
                } else {
                    v.erreur("identifiant", "Cet identifiant ou cet email est déjà utilisé.");
                }
            }
        }

        // Réaffiche le formulaire avec les valeurs saisies (jamais les mots de passe) et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : CHAMPS_REAFFICHES) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        return VUE_NOUVEL_AGENT;
    }
}
