package fr.cytech.assurance.controller;

import fr.cytech.assurance.dao.UtilisateurDao;
import fr.cytech.assurance.model.Utilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Changement du mot de passe de l'utilisateur connecté
@WebServlet("/compte/mot-de-passe")
public class MotDePasseServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/compte/mot-de-passe.jsp";
    private static final int LONGUEUR_MIN = 8;

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    // Affiche le formulaire
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie l'ancien mot de passe et le nouveau, puis enregistre le nouveau haché
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur connecte = (Utilisateur) request.getSession().getAttribute("utilisateur");
        String actuel = request.getParameter("actuel");
        String nouveau = request.getParameter("nouveau");
        String confirmation = request.getParameter("confirmation");

        // Hash relu en base : celui de la session peut dater d'avant un changement
        Utilisateur utilisateur = utilisateurDao.trouverParId(connecte.getId());

        Map<String, String> erreurs = new HashMap<>();
        // Le mot de passe actuel est exigé : une session laissée ouverte ne suffit pas pour le changer
        if (actuel == null || !BCrypt.checkpw(actuel, utilisateur.getMotDePasseHash())) {
            erreurs.put("actuel", "Mot de passe actuel incorrect.");
        }
        if (nouveau == null || nouveau.length() < LONGUEUR_MIN) {
            erreurs.put("nouveau", "Le nouveau mot de passe doit contenir au moins " + LONGUEUR_MIN + " caractères.");
        } else if (!nouveau.equals(confirmation)) {
            erreurs.put("confirmation", "La confirmation ne correspond pas.");
        }

        if (!erreurs.isEmpty()) {
            // Les mots de passe ne sont jamais renvoyés dans le formulaire
            request.setAttribute("erreurs", erreurs);
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        utilisateurDao.changerMotDePasse(connecte.getId(), BCrypt.hashpw(nouveau, BCrypt.gensalt()));
        response.sendRedirect(request.getContextPath() + "/compte?succes=motdepasse");
    }
}
