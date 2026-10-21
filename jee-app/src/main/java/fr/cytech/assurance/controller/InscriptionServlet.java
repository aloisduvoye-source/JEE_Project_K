package fr.cytech.assurance.controller;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.UtilisateurDao;
import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

// Création d'un compte client (page publique)
@WebServlet("/inscription")
public class InscriptionServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/auth/inscription.jsp";
    private static final Pattern FORMAT_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern FORMAT_TELEPHONE = Pattern.compile("^[0-9 +.()-]{6,20}$");
    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final ClientDao clientDao = new ClientDao();

    // Affiche le formulaire
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie les saisies, crée le compte, puis connecte directement le nouveau client
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identifiant = nettoyer(request.getParameter("identifiant"));
        String nom = nettoyer(request.getParameter("nom"));
        String prenom = nettoyer(request.getParameter("prenom"));
        String email = nettoyer(request.getParameter("email"));
        String telephone = nettoyer(request.getParameter("telephone"));
        String adresse = nettoyer(request.getParameter("adresse"));
        String dateNaissanceSaisie = nettoyer(request.getParameter("dateNaissance"));
        String motDePasse = request.getParameter("motDePasse");
        String confirmation = request.getParameter("confirmation");

        Map<String, String> erreurs = new HashMap<>();

        if (identifiant.isEmpty()) {
            erreurs.put("identifiant", "L'identifiant est obligatoire.");
        } else if (identifiant.length() > 100) {
            erreurs.put("identifiant", "L'identifiant ne doit pas dépasser 100 caractères.");
        } else if (utilisateurDao.identifiantExiste(identifiant)) {
            erreurs.put("identifiant", "Cet identifiant est déjà pris.");
        }

        if (nom.isEmpty()) {
            erreurs.put("nom", "Le nom est obligatoire.");
        } else if (nom.length() > 100) {
            erreurs.put("nom", "Le nom ne doit pas dépasser 100 caractères.");
        }

        if (prenom.isEmpty()) {
            erreurs.put("prenom", "Le prénom est obligatoire.");
        } else if (prenom.length() > 100) {
            erreurs.put("prenom", "Le prénom ne doit pas dépasser 100 caractères.");
        }

        if (email.isEmpty()) {
            erreurs.put("email", "L'email est obligatoire.");
        } else if (email.length() > 180 || !FORMAT_EMAIL.matcher(email).matches()) {
            erreurs.put("email", "L'email n'est pas valide.");
        } else if (utilisateurDao.emailExiste(email)) {
            erreurs.put("email", "Cet email est déjà utilisé.");
        }

        // Téléphone, adresse et date de naissance sont facultatifs
        if (!telephone.isEmpty() && !FORMAT_TELEPHONE.matcher(telephone).matches()) {
            erreurs.put("telephone", "Le numéro de téléphone n'est pas valide.");
        }
        if (adresse.length() > 255) {
            erreurs.put("adresse", "L'adresse ne doit pas dépasser 255 caractères.");
        }
        LocalDate dateNaissance = null;
        if (!dateNaissanceSaisie.isEmpty()) {
            try {
                dateNaissance = LocalDate.parse(dateNaissanceSaisie);
                if (dateNaissance.isAfter(LocalDate.now())) {
                    erreurs.put("dateNaissance", "La date de naissance ne peut pas être dans le futur.");
                }
            } catch (DateTimeParseException e) {
                erreurs.put("dateNaissance", "La date de naissance n'est pas valide.");
            }
        }

        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            erreurs.put("motDePasse", "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        } else if (!motDePasse.equals(confirmation)) {
            erreurs.put("confirmation", "La confirmation ne correspond pas.");
        }

        if (!erreurs.isEmpty()) {
            reafficher(request, response, erreurs);
            return;
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setIdentifiant(identifiant);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setEmail(email);
        utilisateur.setMotDePasseHash(BCrypt.hashpw(motDePasse, BCrypt.gensalt()));
        utilisateur.setRole(Role.CLIENT);   // toujours CLIENT : le rôle n'est jamais lu depuis le formulaire

        Client client = new Client();
        client.setTelephone(telephone.isEmpty() ? null : telephone);
        client.setAdresse(adresse.isEmpty() ? null : adresse);
        client.setDateNaissance(dateNaissance);

        try {
            clientDao.creerAvecUtilisateur(client, utilisateur);
        } catch (UtilisateurDejaExistantException e) {
            // Pris entre la vérification ci-dessus et l'enregistrement (inscription simultanée)
            if ("email".equals(e.getChamp())) {
                erreurs.put("email", "Cet email est déjà utilisé.");
            } else if ("identifiant".equals(e.getChamp())) {
                erreurs.put("identifiant", "Cet identifiant est déjà pris.");
            } else {
                erreurs.put("identifiant", "Cet identifiant ou cet email est déjà utilisé.");
            }
            reafficher(request, response, erreurs);
            return;
        }

        // Pas de connexion automatique : le client se connecte ensuite avec ses identifiants
        response.sendRedirect(request.getContextPath() + "/");
    }

    // Réaffiche le formulaire avec les valeurs saisies (jamais les mots de passe) et les messages
    private static void reafficher(HttpServletRequest request, HttpServletResponse response, Map<String, String> erreurs)
            throws ServletException, IOException {
        request.setAttribute("erreurs", erreurs);
        for (String champ : new String[]{"identifiant", "nom", "prenom", "email", "telephone", "adresse", "dateNaissance"}) {
            request.setAttribute(champ, nettoyer(request.getParameter(champ)));
        }
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }
}
