package fr.cytech.assurance.controller;

import fr.cytech.assurance.dao.UtilisateurDao;
import fr.cytech.assurance.model.Utilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

// Modification du nom, du prénom et de l'email de l'utilisateur connecté
@WebServlet("/compte/profil")
public class ProfilServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/compte/profil.jsp";
    private static final Pattern FORMAT_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    // Affiche le formulaire prérempli
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur connecte = (Utilisateur) request.getSession().getAttribute("utilisateur");
        Utilisateur utilisateur = utilisateurDao.trouverParId(connecte.getId());
        request.setAttribute("nom", utilisateur.getNom());
        request.setAttribute("prenom", utilisateur.getPrenom());
        request.setAttribute("email", utilisateur.getEmail());
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie les saisies, puis enregistre
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // L'id vient de la session, jamais du formulaire : on ne peut modifier que son propre compte
        Utilisateur connecte = (Utilisateur) request.getSession().getAttribute("utilisateur");
        String nom = nettoyer(request.getParameter("nom"));
        String prenom = nettoyer(request.getParameter("prenom"));
        String email = nettoyer(request.getParameter("email"));

        Map<String, String> erreurs = new HashMap<>();
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
        } else if (utilisateurDao.emailExistePourUnAutre(email, connecte.getId())) {
            erreurs.put("email", "Cet email est déjà utilisé par un autre compte.");
        }

        if (!erreurs.isEmpty()) {
            // Réaffiche le formulaire avec les valeurs saisies et les messages
            request.setAttribute("erreurs", erreurs);
            request.setAttribute("nom", nom);
            request.setAttribute("prenom", prenom);
            request.setAttribute("email", email);
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        utilisateurDao.modifierProfil(connecte.getId(), nom, prenom, email);
        // Met la session à jour pour que les pages affichent le nouveau nom
        request.getSession().setAttribute("utilisateur", utilisateurDao.trouverParId(connecte.getId()));

        response.sendRedirect(request.getContextPath() + "/compte?succes=profil");
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }
}
