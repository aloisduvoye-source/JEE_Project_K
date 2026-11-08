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

// Création d'un compte agent par l'administrateur : seul moyen d'obtenir un compte AGENT
// (l'inscription publique ne crée que des clients)
@WebServlet("/admin/utilisateurs/nouvel-agent")
public class NouvelAgentServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/admin/agent-nouveau.jsp";
    private static final String[] CHAMPS_REAFFICHES = {"identifiant", "nom", "prenom", "email"};

    private final UtilisateurService utilisateurService = new UtilisateurService();

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
                response.sendRedirect(request.getContextPath() + "/admin/utilisateurs?role=AGENT&agentCree=1");
                return;
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
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
