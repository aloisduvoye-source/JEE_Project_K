package fr.cytech.assurance.controller;

import fr.cytech.assurance.dao.UtilisateurDao;
import fr.cytech.assurance.model.Utilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;

@WebServlet("/connexion")
public class ConnexionServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/auth/connexion.jsp";

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    // Affiche le formulaire
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Vérifie l'identifiant et le mot de passe envoyés par le formulaire
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String identifiant = request.getParameter("identifiant");
        String motDePasse = request.getParameter("motDePasse");

        String erreur = null;
        Utilisateur utilisateur = null;
        if (identifiant == null || identifiant.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
            erreur = "Veuillez remplir tous les champs.";
        } else {
            utilisateur = utilisateurDao.trouverParIdentifiant(identifiant.trim());
            // Même message que l'identifiant ou le mot de passe soit faux : on ne révèle pas lequel
            if (utilisateur == null || !BCrypt.checkpw(motDePasse, utilisateur.getMotDePasseHash())) {
                erreur = "Identifiant ou mot de passe incorrect.";
            } else if (!utilisateur.isActif()) {
                erreur = "Ce compte est désactivé.";
            }
        }

        if (erreur != null) {
            request.setAttribute("erreur", erreur);
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        // Nouvelle session à chaque connexion (protège contre la fixation de session)
        HttpSession ancienne = request.getSession(false);
        if (ancienne != null) {
            ancienne.invalidate();
        }
        request.getSession(true).setAttribute("utilisateur", utilisateur);

        // Chacun arrive directement dans l'espace de son rôle
        response.sendRedirect(request.getContextPath() + utilisateur.getRole().getAccueil());
    }
}
