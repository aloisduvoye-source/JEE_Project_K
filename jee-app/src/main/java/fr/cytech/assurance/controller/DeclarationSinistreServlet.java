package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.enums.TypeSinistre;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PieceJustificativeService;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Déclaration d'un sinistre par le client connecté.
 * GET : formulaire (?contrat=... présélectionne le contrat, depuis sa fiche). POST : enregistre la déclaration,
 * puis les pièces justificatives jointes (formulaire multipart, champ "pieces", facultatif).
 */
@WebServlet("/client/sinistres/nouveau")
@MultipartConfig(maxFileSize = PieceJustificativeService.TAILLE_MAX,
        maxRequestSize = PieceJustificativeService.TAILLE_MAX * 5)
public class DeclarationSinistreServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/sinistre-nouveau.jsp";

    private final ClientService clientService = new ClientService();
    private final SinistreService sinistreService = new SinistreService();
    private final PieceJustificativeService pieceService = new PieceJustificativeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("contrat", Validateur.idOuNull(request.getParameter("contrat")));
        afficher(request, response, client(request));
    }

    // Vérifie le format des saisies, puis le service applique les règles et enregistre la déclaration
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Client client = client(request);
        Validateur v = new Validateur();
        // Fichiers trop gros : Tomcat refuse de lire le formulaire, aucun champ n'est disponible
        try {
            if (AjoutPieceServlet.estMultipart(request)) {
                request.getParts();
            }
        } catch (IllegalStateException e) {
            v.erreur("global", "Un fichier dépasse 5 Mo, ou l'ensemble dépasse 25 Mo : rien n'a été enregistré.");
            request.setAttribute("erreurs", v.getErreurs());
            afficher(request, response, client);
            return;
        }
        Long idContrat = v.identifiant("contrat", Validateur.nettoyer(request.getParameter("contrat")), "Le contrat");
        LocalDate date = v.date("dateSinistre", Validateur.nettoyer(request.getParameter("dateSinistre")),
                "La date du sinistre");
        TypeSinistre type = type(v, request.getParameter("type"));
        String description = Validateur.nettoyer(request.getParameter("description"));
        v.obligatoire("description", description, "La description", SinistreService.LONGUEUR_MAX_DESCRIPTION);
        BigDecimal montant = v.montantFacultatif("montantEstime",
                Validateur.nettoyer(request.getParameter("montantEstime")), "Le montant estimé");

        if (v.estValide()) {
            try {
                Sinistre sinistre = sinistreService.declarer(client == null ? null : client.getId(),
                        idContrat, date, type, description, montant);
                // Le sinistre est déclaré même si une pièce est refusée : le client pourra l'ajouter depuis la fiche
                List<String> erreursPieces = AjoutPieceServlet.enregistrer(request, sinistre.getId(),
                        SessionUtilisateur.connecte(request), pieceService, clientService);
                if (!erreursPieces.isEmpty()) {
                    MessageFlash.erreur(request, String.join(" ", erreursPieces)
                            + " Vous pouvez ajouter vos pièces depuis cette page.");
                }
                response.sendRedirect(request.getContextPath() + "/client/sinistres/detail?id=" + sinistre.getId() + "&declare=1");
                return;
            } catch (SinistreInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }

        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        request.setAttribute("contrat", idContrat);
        for (String champ : new String[]{"dateSinistre", "type", "description", "montantEstime"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        afficher(request, response, client);
    }

    // Type choisi dans la liste : obligatoire et parmi les valeurs connues
    private TypeSinistre type(Validateur v, String valeur) {
        String nettoye = Validateur.nettoyer(valeur);
        if (nettoye.isEmpty()) {
            v.erreur("type", "Le type de sinistre est obligatoire.");
            return null;
        }
        try {
            return TypeSinistre.valueOf(nettoye);
        } catch (IllegalArgumentException e) {
            v.erreur("type", "Le type de sinistre n'est pas valide.");
            return null;
        }
    }

    // Le client vient de la session, jamais du formulaire
    private Client client(HttpServletRequest request) {
        return clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
    }

    private void afficher(HttpServletRequest request, HttpServletResponse response, Client client)
            throws ServletException, IOException {
        request.setAttribute("contrats", client == null ? List.of() : sinistreService.contratsDeclarables(client.getId()));
        request.setAttribute("types", TypeSinistre.values());
        request.setAttribute("aujourdhui", LocalDate.now());   // date maximale du sinistre
        request.setAttribute("longueurMax", SinistreService.LONGUEUR_MAX_DESCRIPTION);
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
