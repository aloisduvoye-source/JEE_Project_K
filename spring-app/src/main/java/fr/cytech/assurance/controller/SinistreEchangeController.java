package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.PieceJustificative;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PieceJustificativeService;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Échanges sur un sinistre, communs au client et à l'agent : discussion, envoi et affichage des pièces
 * (remplace CommentaireSinistreServlet, AjoutPieceServlet et TelechargementPieceServlet).
 * L'adresse dit qui agit : /client/... (seulement ses sinistres) ou /agent/... (l'admin est refusé par le service).
 */
@Controller
public class SinistreEchangeController {

    private final SinistreService sinistreService;
    private final PieceJustificativeService pieceService;
    private final ClientService clientService;

    /** Dépendances injectées par Spring. */
    public SinistreEchangeController(SinistreService sinistreService, PieceJustificativeService pieceService,
                                     ClientService clientService) {
        this.sinistreService = sinistreService;
        this.pieceService = pieceService;
        this.clientService = clientService;
    }

    /** Message dans la discussion d'un sinistre, depuis sa fiche ; retour en bas de la discussion. */
    @PostMapping("/{espace:client|agent}/sinistres/commenter")
    public String commenter(@PathVariable String espace, HttpServletRequest request) {
        Utilisateur auteur = SessionUtilisateur.connecte(request);
        String liste = "redirect:/" + espace + "/sinistres";

        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le sinistre");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return liste;
        }
        String fiche = liste + "/detail?id=" + id + "#discussion";

        String contenu = Validateur.nettoyer(request.getParameter("contenu"));
        v.obligatoire("contenu", contenu, "Le message", SinistreService.LONGUEUR_MAX_COMMENTAIRE);
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("contenu"));
            return fiche;
        }
        try {
            // Le client vient de la session : le service vérifie que le sinistre est bien le sien
            Client client = espace.equals("client") ? clientService.trouverParUtilisateur(auteur.getId()) : null;
            sinistreService.commenter(id, auteur, client == null ? null : client.getId(), contenu);
        } catch (SinistreInvalideException e) {
            MessageFlash.erreur(request, e.getMessage());
        }
        return fiche;
    }

    /**
     * Ajout de pièces depuis la fiche d'un sinistre (formulaire multipart, champ "pieces", plusieurs fichiers).
     * L'id est dans l'adresse (?id=...) et non dans le formulaire : si un fichier est trop gros, aucun champ
     * du formulaire n'est lu, mais l'adresse reste lisible pour revenir à la bonne fiche avec le message.
     */
    @PostMapping("/{espace:client|agent}/sinistres/pieces")
    public String ajouterPieces(@PathVariable String espace, HttpServletRequest request) {
        String liste = "redirect:/" + espace + "/sinistres";
        // Lu dans l'adresse seulement : avec Tomcat 11, getParameter lit aussi le corps du formulaire
        // et lève une exception si un fichier est trop gros
        String idDansAdresse = UriComponentsBuilder.newInstance().query(request.getQueryString()).build()
                .getQueryParams().getFirst("id");
        Long id = Validateur.idOuNull(idDansAdresse);
        if (id == null) {
            MessageFlash.erreur(request, "Sinistre inconnu.");
            return liste;
        }
        List<String> erreurs = enregistrerPieces(request, id, SessionUtilisateur.connecte(request),
                pieceService, clientService);
        if (!erreurs.isEmpty()) {
            MessageFlash.erreur(request, String.join(" ", erreurs));
        }
        return liste + "/detail?id=" + id + "#pieces";
    }

    /**
     * Affichage d'une pièce (?id=...) : le client seulement pour ses sinistres, les agents et l'admin pour toutes.
     * Les fichiers ne sont jamais servis directement depuis le disque : tout passe par ce contrôle d'accès.
     */
    @GetMapping("/{espace:client|agent}/sinistres/piece")
    public void afficherPiece(HttpServletRequest request, HttpServletResponse response) throws IOException {
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

    /**
     * Enregistre les fichiers du champ "pieces" d'un formulaire multipart (aussi utilisé à la déclaration).
     * Chaque fichier est traité séparément : un fichier refusé n'empêche pas l'enregistrement des autres.
     *
     * @return les messages d'erreur à afficher, liste vide si tout s'est bien passé ou si aucun fichier n'a été choisi
     */
    static List<String> enregistrerPieces(HttpServletRequest request, Long idSinistre, Utilisateur utilisateur,
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
        } catch (IllegalStateException | MultipartException e) {
            // Limite de spring.servlet.multipart dépassée : aucun fichier n'est lu
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

    /** true si la requête est un formulaire multipart (le seul qui puisse contenir des fichiers). */
    static boolean estMultipart(HttpServletRequest request) {
        String type = request.getContentType();
        return type != null && type.toLowerCase().startsWith("multipart/");
    }
}
