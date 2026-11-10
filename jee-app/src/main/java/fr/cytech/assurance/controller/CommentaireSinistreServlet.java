package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Message dans la discussion d'un sinistre, depuis sa fiche. L'adresse appelée dit qui écrit
 * (même principe que ResiliationContratServlet) :
 *   /client/sinistres/commenter : le client connecté, seulement sur l'un de ses sinistres
 *   /agent/sinistres/commenter  : un agent, sur n'importe quel sinistre (l'admin est refusé par le service)
 * En POST uniquement. Après l'envoi, retour à la fiche, en bas de la discussion.
 */
@WebServlet({"/client/sinistres/commenter", "/agent/sinistres/commenter"})
public class CommentaireSinistreServlet extends HttpServlet {

    private final SinistreService sinistreService = new SinistreService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utilisateur auteur = SessionUtilisateur.connecte(request);
        boolean parClient = request.getServletPath().startsWith("/client");
        String espace = request.getContextPath() + (parClient ? "/client" : "/agent") + "/sinistres";

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le sinistre");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            response.sendRedirect(espace);
            return;
        }
        String fiche = espace + "/detail?id=" + id + "#discussion";

        String contenu = Validateur.nettoyer(request.getParameter("contenu"));
        v.obligatoire("contenu", contenu, "Le message", SinistreService.LONGUEUR_MAX_COMMENTAIRE);
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("contenu"));
            response.sendRedirect(fiche);
            return;
        }

        try {
            // Le client vient de la session : le service vérifie que le sinistre est bien le sien
            Client client = parClient ? clientService.trouverParUtilisateur(auteur.getId()) : null;
            sinistreService.commenter(id, auteur, client == null ? null : client.getId(), contenu);
        } catch (SinistreInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
        }
        response.sendRedirect(fiche);
    }
}
