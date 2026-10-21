package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.UtilisateurService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Modification du nom, du prénom et de l'email de l'utilisateur connecté
@WebServlet("/compte/profil")
public class ProfilServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/compte/profil.jsp";

    private final UtilisateurService utilisateurService = new UtilisateurService();

    // Affiche le formulaire prérempli
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Utilisateur connecte = (Utilisateur) request.getSession().getAttribute("utilisateur");
        Utilisateur utilisateur = utilisateurService.trouverParId(connecte.getId());
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
            request.getRequestDispatcher(VUE).forward(request, response);
            return;
        }

        // Met la session à jour pour que les pages affichent le nouveau nom
        request.getSession().setAttribute("utilisateur", utilisateurService.trouverParId(connecte.getId()));

        response.sendRedirect(request.getContextPath() + "/compte?succes=profil");
    }
}
