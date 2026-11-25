package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.AuthentificationException;
import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

/**
 * Connexion, inscription d'un client et déconnexion (remplace ConnexionServlet, InscriptionServlet
 * et DeconnexionServlet).
 */
@Controller
public class AuthController {

    private static final String VUE_CONNEXION = "auth/connexion";
    private static final String VUE_INSCRIPTION = "auth/inscription";
    private static final String[] CHAMPS_REAFFICHES =
            {"identifiant", "nom", "prenom", "email", "telephone", "adresse", "dateNaissance"};

    private final UtilisateurService utilisateurService;

    /** Dépendances injectées par Spring. */
    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    /** Affiche le formulaire de connexion. */
    @GetMapping("/connexion")
    public String connexion() {
        return VUE_CONNEXION;
    }

    /** Vérifie l'identifiant et le mot de passe envoyés par le formulaire. */
    @PostMapping("/connexion")
    public String seConnecter(HttpServletRequest request) {
        String identifiant = request.getParameter("identifiant");
        String motDePasse = request.getParameter("motDePasse");
        Utilisateur utilisateur;
        try {
            if (identifiant == null || identifiant.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
                throw new AuthentificationException("Veuillez remplir tous les champs.");
            }
            utilisateur = utilisateurService.authentifier(identifiant.trim(), motDePasse);
        } catch (AuthentificationException e) {
            request.setAttribute("erreur", e.getMessage());
            return VUE_CONNEXION;
        }
        SessionUtilisateur.connecter(request, utilisateur);
        // Chacun arrive directement dans l'espace de son rôle
        return "redirect:" + utilisateur.getRole().getAccueil();
    }

    /** Affiche le formulaire de création d'un compte client (page publique). */
    @GetMapping("/inscription")
    public String inscription() {
        return VUE_INSCRIPTION;
    }

    /** Vérifie les saisies, puis crée le compte. */
    @PostMapping("/inscription")
    public String inscrire(HttpServletRequest request) {
        String identifiant = Validateur.nettoyer(request.getParameter("identifiant"));
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String prenom = Validateur.nettoyer(request.getParameter("prenom"));
        String email = Validateur.nettoyer(request.getParameter("email"));
        String telephone = Validateur.nettoyer(request.getParameter("telephone"));
        String adresse = Validateur.nettoyer(request.getParameter("adresse"));
        String dateNaissanceSaisie = Validateur.nettoyer(request.getParameter("dateNaissance"));
        String motDePasse = request.getParameter("motDePasse");
        String confirmation = request.getParameter("confirmation");

        Validateur v = new Validateur();
        v.obligatoire("identifiant", identifiant, "L'identifiant", 100);
        v.obligatoire("nom", nom, "Le nom", 100);
        v.obligatoire("prenom", prenom, "Le prénom", 100);
        v.email("email", email);
        // Téléphone, adresse et date de naissance sont facultatifs
        v.telephone("telephone", telephone);
        v.longueur("adresse", adresse, "L'adresse", 255);
        LocalDate dateNaissance = v.datePassee("dateNaissance", dateNaissanceSaisie, "La date de naissance");
        v.nouveauMotDePasse("motDePasse", motDePasse, "Le mot de passe", "confirmation", confirmation);

        // Disponibilité vérifiée seulement si le format est bon, pour signaler toutes les erreurs d'un coup
        if (!v.aErreur("identifiant") && !utilisateurService.identifiantDisponible(identifiant)) {
            v.erreur("identifiant", "Cet identifiant est déjà pris.");
        }
        if (!v.aErreur("email") && !utilisateurService.emailDisponible(email)) {
            v.erreur("email", "Cet email est déjà utilisé.");
        }

        if (v.estValide()) {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setIdentifiant(identifiant);
            utilisateur.setNom(nom);
            utilisateur.setPrenom(prenom);
            utilisateur.setEmail(email);

            Client client = new Client();
            client.setTelephone(telephone.isEmpty() ? null : telephone);
            client.setAdresse(adresse.isEmpty() ? null : adresse);
            client.setDateNaissance(dateNaissance);

            try {
                utilisateurService.inscrireClient(utilisateur, client, motDePasse);
            } catch (UtilisateurDejaExistantException e) {
                // Pris entre la vérification ci-dessus et l'enregistrement (inscription simultanée)
                if ("email".equals(e.getChamp())) {
                    v.erreur("email", "Cet email est déjà utilisé.");
                } else if ("identifiant".equals(e.getChamp())) {
                    v.erreur("identifiant", "Cet identifiant est déjà pris.");
                } else {
                    v.erreur("identifiant", "Cet identifiant ou cet email est déjà utilisé.");
                }
            }
        }

        if (!v.estValide()) {
            // Réaffiche le formulaire avec les valeurs saisies (jamais les mots de passe) et les messages
            request.setAttribute("erreurs", v.getErreurs());
            for (String champ : CHAMPS_REAFFICHES) {
                request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
            }
            return VUE_INSCRIPTION;
        }
        // Pas de connexion automatique : le client se connecte ensuite avec ses identifiants
        return "redirect:/";
    }

    /** Déconnexion en POST (bouton) : un simple lien ou une image ne peut pas déconnecter quelqu'un à son insu. */
    @PostMapping("/deconnexion")
    public String deconnecter(HttpServletRequest request) {
        SessionUtilisateur.deconnecter(request);
        return "redirect:/";
    }

    /** Accès direct à l'URL : retour à l'accueil, sans déconnecter. */
    @GetMapping("/deconnexion")
    public String deconnexionDirecte() {
        return "redirect:/";
    }
}
