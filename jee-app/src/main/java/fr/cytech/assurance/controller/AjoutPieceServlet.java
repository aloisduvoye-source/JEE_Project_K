package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PieceJustificativeService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Ajout de pièces justificatives depuis la fiche d'un sinistre (formulaire multipart, champ "pieces",
 * plusieurs fichiers possibles). L'id du sinistre est dans l'adresse (?id=...) et non dans le formulaire :
 * si un fichier est trop gros, Tomcat ne lit aucun champ du formulaire, mais l'adresse reste lisible,
 * ce qui permet de revenir à la bonne fiche avec le message. L'adresse appelée dit qui envoie :
 *   /client/sinistres/pieces : le client connecté, seulement sur l'un de ses sinistres
 *   /agent/sinistres/pieces  : un agent, sur n'importe quel sinistre (l'admin est refusé par le service)
 * En POST uniquement. Après l'envoi, retour à la fiche, sur la liste des pièces.
 */
@WebServlet({"/client/sinistres/pieces", "/agent/sinistres/pieces"})
@MultipartConfig(maxFileSize = PieceJustificativeService.TAILLE_MAX,
        maxRequestSize = PieceJustificativeService.TAILLE_MAX * 5)
public class AjoutPieceServlet extends HttpServlet {

    private final PieceJustificativeService pieceService = new PieceJustificativeService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utilisateur utilisateur = SessionUtilisateur.connecte(request);
        boolean parClient = request.getServletPath().startsWith("/client");
        String espace = request.getContextPath() + (parClient ? "/client" : "/agent") + "/sinistres";

        Long id = Validateur.idOuNull(request.getParameter("id"));
        if (id == null) {
            MessageFlash.erreur(request, "Sinistre inconnu.");
            response.sendRedirect(espace);
            return;
        }
        List<String> erreurs = enregistrer(request, id, utilisateur, pieceService, clientService);
        if (!erreurs.isEmpty()) {
            MessageFlash.erreur(request, String.join(" ", erreurs));
        }
        response.sendRedirect(espace + "/detail?id=" + id + "#pieces");
    }

    /**
     * Enregistre les fichiers du champ "pieces" d'un formulaire multipart (aussi utilisé à la déclaration).
     * Chaque fichier est traité séparément : un fichier refusé n'empêche pas l'enregistrement des autres.
     *
     * @return les messages d'erreur à afficher, liste vide si tout s'est bien passé ou si aucun fichier n'a été choisi
     */
    static List<String> enregistrer(HttpServletRequest request, Long idSinistre, Utilisateur utilisateur,
                                    PieceJustificativeService pieceService, ClientService clientService) {
        List<String> erreurs = new ArrayList<>();
        if (!estMultipart(request)) {
            // Formulaire envoyé sans fichier possible : getParts() lèverait une exception
            return erreurs;
        }
        List<Part> fichiers;
        try {
            fichiers = request.getParts().stream()
                    .filter(p -> "pieces".equals(p.getName()))
                    // Champ laissé vide : une partie sans nom de fichier
                    .filter(p -> p.getSubmittedFileName() != null && !p.getSubmittedFileName().isBlank())
                    .toList();
        } catch (IllegalStateException e) {
            // Limite de @MultipartConfig dépassée : aucun fichier n'est lu
            erreurs.add("Un fichier dépasse 5 Mo, ou l'ensemble dépasse 25 Mo : aucune pièce n'a été enregistrée.");
            return erreurs;
        } catch (IOException | ServletException e) {
            erreurs.add("Les fichiers n'ont pas pu être reçus.");
            return erreurs;
        }

        // Le client vient de la session : le service vérifie que le sinistre est bien le sien
        Client client = utilisateur.getRole() == Role.CLIENT ? clientService.trouverParUtilisateur(utilisateur.getId()) : null;
        for (Part fichier : fichiers) {
            try (InputStream contenu = fichier.getInputStream()) {
                pieceService.ajouter(idSinistre, utilisateur, client == null ? null : client.getId(),
                        fichier.getSubmittedFileName(), fichier.getSize(), contenu);
            } catch (SinistreInvalideException e) {
                erreurs.add(e.getMessage());
            } catch (IOException e) {
                erreurs.add("Le fichier « " + fichier.getSubmittedFileName() + " » n'a pas pu être enregistré.");
            }
        }
        return erreurs;
    }

    // true si la requête est un formulaire multipart (le seul qui puisse contenir des fichiers)
    static boolean estMultipart(HttpServletRequest request) {
        String type = request.getContentType();
        return type != null && type.toLowerCase().startsWith("multipart/");
    }
}
