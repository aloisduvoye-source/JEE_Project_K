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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sinistres du client connecté : liste, fiche et déclaration
 * (remplace MesSinistresServlet, MonSinistreServlet et DeclarationSinistreServlet).
 * Le client vient toujours de la session : il ne voit que ses propres sinistres.
 */
@Controller
public class SinistreClientController {

    private static final String VUE_DECLARATION = "client/sinistre-nouveau";

    private final ClientService clientService;
    private final SinistreService sinistreService;
    private final PieceJustificativeService pieceService;

    /** Dépendances injectées par Spring. */
    public SinistreClientController(ClientService clientService, SinistreService sinistreService,
                                    PieceJustificativeService pieceService) {
        this.clientService = clientService;
        this.sinistreService = sinistreService;
        this.pieceService = pieceService;
    }

    /** Sinistres du client, du plus récent au plus ancien. */
    @GetMapping("/client/sinistres")
    public String lister(HttpServletRequest request) {
        Client client = client(request);
        request.setAttribute("sinistres", client == null ? List.of() : sinistreService.listerDuClient(client.getId()));
        return "client/sinistres";
    }

    /** Fiche d'un sinistre du client : un id de sinistre d'un autre client ne donne rien. */
    @GetMapping("/client/sinistres/detail")
    public String detail(HttpServletRequest request) {
        Client client = client(request);
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Sinistre sinistre = client == null || id == null ? null : sinistreService.trouverDuClient(id, client.getId());
        if (sinistre == null) {
            MessageFlash.erreur(request, "Ce sinistre n'existe pas.");
            return "redirect:/client/sinistres";
        }
        request.setAttribute("sinistre", sinistre);
        request.setAttribute("indemnisation", sinistreService.indemnisation(id));
        request.setAttribute("pieces", pieceService.lister(id));
        request.setAttribute("commentaires", sinistreService.discussion(id));
        request.setAttribute("clos", SinistreService.estClos(sinistre));
        return "client/sinistre-detail";
    }

    /** Formulaire de déclaration ; ?contrat=... présélectionne le contrat, depuis sa fiche. */
    @GetMapping("/client/sinistres/nouveau")
    public String declaration(HttpServletRequest request) {
        request.setAttribute("contrat", Validateur.idOuNull(request.getParameter("contrat")));
        return afficherDeclaration(request, client(request));
    }

    /**
     * Vérifie le format des saisies, puis le service applique les règles et enregistre la déclaration,
     * puis les pièces justificatives jointes (formulaire multipart, champ "pieces", facultatif).
     */
    @PostMapping("/client/sinistres/nouveau")
    public String declarer(HttpServletRequest request) throws IOException, ServletException {
        Client client = client(request);
        Validateur v = new Validateur();

        // Fichiers trop gros : Tomcat refuse de lire le formulaire, aucun champ n'est disponible
        try {
            if (SinistreEchangeController.estMultipart(request)) {
                request.getParts();
            }
        } catch (IllegalStateException | MultipartException e) {
            v.erreur("global", "Un fichier dépasse 5 Mo, ou l'ensemble dépasse 25 Mo : rien n'a été enregistré.");
            request.setAttribute("erreurs", v.getErreurs());
            return afficherDeclaration(request, client);
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
                List<String> erreursPieces = SinistreEchangeController.enregistrerPieces(request, sinistre.getId(),
                        SessionUtilisateur.connecte(request), pieceService, clientService);
                if (!erreursPieces.isEmpty()) {
                    MessageFlash.erreur(request, String.join(" ", erreursPieces)
                            + " Vous pouvez ajouter vos pièces depuis cette page.");
                }
                return "redirect:/client/sinistres/detail?id=" + sinistre.getId() + "&declare=1";
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
        return afficherDeclaration(request, client);
    }

    /** Type choisi dans la liste : obligatoire et parmi les valeurs connues. */
    private static TypeSinistre type(Validateur v, String valeur) {
        String nettoye = Validateur.nettoyer(valeur);
        if (nettoye.isEmpty()) {
            v.erreur("type", "Le type de sinistre est obligatoire.");
            return null;
        }
        TypeSinistre type = Validateur.enumOuNull(TypeSinistre.class, nettoye);
        if (type == null) {
            v.erreur("type", "Le type de sinistre n'est pas valide.");
        }
        return type;
    }

    /** Fiche client de l'utilisateur connecté (jamais lue dans le formulaire). */
    private Client client(HttpServletRequest request) {
        return clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
    }

    /** Formulaire de déclaration, avec les contrats sur lesquels le client peut déclarer un sinistre. */
    private String afficherDeclaration(HttpServletRequest request, Client client) {
        request.setAttribute("contrats", client == null ? List.of() : sinistreService.contratsDeclarables(client.getId()));
        request.setAttribute("types", TypeSinistre.values());
        request.setAttribute("aujourdhui", LocalDate.now());   // date maximale du sinistre
        request.setAttribute("longueurMax", SinistreService.LONGUEUR_MAX_DESCRIPTION);
        return VUE_DECLARATION;
    }
}
