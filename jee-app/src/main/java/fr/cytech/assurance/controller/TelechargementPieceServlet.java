package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.PieceJustificative;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PieceJustificativeService;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Affichage d'une pièce justificative (?id=...) :
 *   /client/sinistres/piece : le client connecté, seulement les pièces de ses sinistres
 *   /agent/sinistres/piece  : les agents et l'admin, toutes les pièces
 * Les fichiers ne sont jamais servis directement depuis le disque : tout passe par ce contrôle d'accès.
 */
@WebServlet({"/client/sinistres/piece", "/agent/sinistres/piece"})
public class TelechargementPieceServlet extends HttpServlet {

    private final PieceJustificativeService pieceService = new PieceJustificativeService();
    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Utilisateur utilisateur = SessionUtilisateur.connecte(request);
        // Le client vient de la session : une pièce d'un autre client donne la même erreur qu'une pièce inexistante
        Client client = utilisateur.getRole() == Role.CLIENT ? clientService.trouverParUtilisateur(utilisateur.getId()) : null;
        PieceJustificative piece = pieceService.trouverPourTelechargement(
                Validateur.idOuNull(request.getParameter("id")), utilisateur, client == null ? null : client.getId());
        if (piece == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path fichier = pieceService.fichier(piece);
        response.setContentType(piece.getTypeMime());
        response.setContentLengthLong(Files.size(fichier));
        // Le navigateur ne doit pas deviner un autre type que celui reconnu à l'envoi (PDF, JPG ou PNG)
        response.setHeader("X-Content-Type-Options", "nosniff");
        // Ouvert dans le navigateur, avec le nom d'origine si l'utilisateur l'enregistre (accents encodés)
        String nom = URLEncoder.encode(piece.getNomFichier(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + nom);
        Files.copy(fichier, response.getOutputStream());
    }
}
