package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

// Création d'un compte client (page publique)
@WebServlet("/inscription")
public class InscriptionServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/auth/inscription.jsp";
    private static final String[] CHAMPS_REAFFICHES =
            {"identifiant", "nom", "prenom", "email", "telephone", "adresse", "dateNaissance"};

    private final UtilisateurService utilisateurService = new UtilisateurService();

    // Affiche le formulaire
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie les saisies, puis crée le compte
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        // Pas de connexion automatique : le client se connecte ensuite avec ses identifiants
        response.sendRedirect(request.getContextPath() + "/");
    }
}
