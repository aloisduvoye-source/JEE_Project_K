package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Clients et contrats côté agent (et admin, en consultation) : §2.1 et §2.2 du sujet
 * (remplace ClientsServlet, ContratsServlet, ContratDetailServlet, NouveauContratServlet et ModifierContratServlet).
 */
@Controller
public class ContratAgentController {

    private static final int LONGUEUR_MAX_RECHERCHE = 100;
    private static final String VUE_NOUVEAU = "agent/contrat-nouveau";
    private static final String VUE_MODIFIER = "agent/contrat-modifier";

    private final ContratService contratService;
    private final ClientService clientService;
    private final PaiementService paiementService;
    private final TypeAssuranceService typeAssuranceService;

    /** Dépendances injectées par Spring. */
    public ContratAgentController(ContratService contratService, ClientService clientService,
                                  PaiementService paiementService, TypeAssuranceService typeAssuranceService) {
        this.contratService = contratService;
        this.clientService = clientService;
        this.paiementService = paiementService;
        this.typeAssuranceService = typeAssuranceService;
    }

    /** Liste de tous les clients, avec une recherche facultative (?q=...) sur le nom, le prénom ou l'email. */
    @GetMapping("/agent/clients")
    public String clients(HttpServletRequest request) {
        String motCle = Validateur.nettoyer(request.getParameter("q"));
        Validateur v = new Validateur();
        v.longueur("q", motCle, "La recherche", 100);
        if (!v.estValide()) {
            request.setAttribute("erreurs", v.getErreurs());
            motCle = "";
        }
        request.setAttribute("q", motCle);
        request.setAttribute("clients", clientService.rechercher(motCle));
        return "agent/clients";
    }

    /**
     * Liste et recherche des contrats. Critères facultatifs dans l'adresse (formulaire en GET) :
     * ?q= (numéro de contrat, ou nom, prénom ou email du client), ?statut=, ?type= (id du type), ?tri=.
     */
    @GetMapping("/agent/contrats")
    public String contrats(HttpServletRequest request) {
        String texte = Validateur.nettoyer(request.getParameter("q"));
        if (texte.length() > LONGUEUR_MAX_RECHERCHE) {
            texte = texte.substring(0, LONGUEUR_MAX_RECHERCHE);
        }
        StatutContrat statut = Validateur.enumOuNull(StatutContrat.class, request.getParameter("statut"));
        Long idType = Validateur.idOuNull(request.getParameter("type"));
        String tri = Validateur.nettoyer(request.getParameter("tri"));

        request.setAttribute("contrats", contratService.rechercher(texte, statut, idType, tri));
        request.setAttribute("q", texte);
        request.setAttribute("statutChoisi", statut);
        request.setAttribute("typeChoisi", idType);
        request.setAttribute("triChoisi", tri.isEmpty() ? "debut_desc" : tri);
        request.setAttribute("statuts", StatutContrat.values());
        // Tous les types, archivés compris : ils restent liés à des contrats existants
        request.setAttribute("types", typeAssuranceService.listerAvecGaranties());
        request.setAttribute("recherche", !texte.isEmpty() || statut != null || idType != null);
        return "agent/contrats";
    }

    /** Fiche d'un contrat : client, type, garanties, paiement (§2.2, « consulter un contrat »). */
    @GetMapping("/agent/contrats/detail")
    public String detail(HttpServletRequest request) {
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = id == null ? null : contratService.trouverAvecDetails(id);
        if (contrat == null) {
            MessageFlash.erreur(request, "Ce contrat n'existe pas.");
            return "redirect:/agent/contrats";
        }
        request.setAttribute("contrat", contrat);
        request.setAttribute("paiement", paiementService.trouverParContrat(id));
        // Renouvellement, suspension et remboursement : affichés sur la fiche (voir ContratService)
        request.setAttribute("renouvellement", contratService.trouverRenouvellement(id));
        request.setAttribute("renouvelable", contratService.raisonNonRenouvelable(contrat) == null);
        request.setAttribute("remboursement", contratService.trouverRemboursement(id));
        request.setAttribute("remboursementSiResiliation", contratService.remboursement(contrat, LocalDate.now()));
        return "agent/contrat-detail";
    }

    /**
     * Création d'un contrat, en deux étapes sans JavaScript : choix du type (la page se recharge avec ?type=...),
     * puis formulaire complet qui ne propose que les garanties de ce type, avec la prime préremplie.
     */
    @GetMapping("/agent/contrats/nouveau")
    public String nouveau(HttpServletRequest request) {
        TypeAssurance type = typeDemande(request.getParameter("type"));
        if (type != null) {
            request.setAttribute("prime", type.getPrixBase().toPlainString());
        }
        return afficherNouveau(request, type, Set.of());
    }

    /** Vérifie le format des saisies, puis le service applique les règles et crée le contrat. */
    @PostMapping("/agent/contrats/nouveau")
    public String creer(HttpServletRequest request) {
        Validateur v = new Validateur();
        Long idType = v.identifiant("type", Validateur.nettoyer(request.getParameter("type")), "Le type d'assurance");
        Long idClient = v.identifiant("client", Validateur.nettoyer(request.getParameter("client")), "Le client");
        LocalDate dateDebut = v.date("dateDebut", Validateur.nettoyer(request.getParameter("dateDebut")), "La date de début");
        BigDecimal prime = v.montant("prime", Validateur.nettoyer(request.getParameter("prime")), "La prime");
        Set<Long> idsGaranties = v.identifiants("garanties", request.getParameterValues("garanties"), "Une garantie");

        if (v.estValide()) {
            try {
                Contrat contrat = contratService.creerContrat(idClient, idType, idsGaranties, dateDebut, prime);
                return "redirect:/agent/contrats?cree=" + contrat.getId();
            } catch (ContratInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }
        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"client", "dateDebut", "prime"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        return afficherNouveau(request, typeDemande(request.getParameter("type")), idsGaranties);
    }

    /** Formulaire de modification d'un contrat pas encore payé, prérempli avec ses valeurs actuelles. */
    @GetMapping("/agent/contrats/modifier")
    public String modification(HttpServletRequest request) {
        verifierAgent(request);
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = id == null ? null : contratService.trouverAvecDetails(id);
        String refus = refusModification(request, id, contrat);
        if (refus != null) {
            return refus;
        }
        request.setAttribute("dateDebut", contrat.getDateDebut());
        request.setAttribute("prime", contrat.getPrime().toPlainString());
        return afficherModification(request, contrat,
                contrat.getGaranties().stream().map(Garantie::getId).collect(Collectors.toSet()));
    }

    /**
     * Modification par un agent (pas l'admin, qui consulte seulement) : garanties, date de début, prime.
     * Le client et le type ne changent pas.
     */
    @PostMapping("/agent/contrats/modifier")
    public String modifier(HttpServletRequest request) {
        verifierAgent(request);
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = id == null ? null : contratService.trouverAvecDetails(id);
        String refus = refusModification(request, id, contrat);
        if (refus != null) {
            return refus;
        }

        Validateur v = new Validateur();
        LocalDate dateDebut = v.date("dateDebut", Validateur.nettoyer(request.getParameter("dateDebut")), "La date de début");
        BigDecimal prime = v.montant("prime", Validateur.nettoyer(request.getParameter("prime")), "La prime");
        Set<Long> idsGaranties = v.identifiants("garanties", request.getParameterValues("garanties"), "Une garantie");

        if (v.estValide()) {
            try {
                contratService.modifierContrat(contrat.getId(), idsGaranties, dateDebut, prime);
                return "redirect:/agent/contrats/detail?id=" + contrat.getId() + "&modifie=1";
            } catch (ContratInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }
        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"dateDebut", "prime"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        return afficherModification(request, contrat, idsGaranties);
    }

    /** L'admin entre dans /agent mais consulte seulement : erreur 403 pour lui. */
    private static void verifierAgent(HttpServletRequest request) {
        if (SessionUtilisateur.connecte(request).getRole() != Role.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /** Redirection avec un message si le contrat n'existe pas ou ne peut plus être modifié, sinon null. */
    private String refusModification(HttpServletRequest request, Long id, Contrat contrat) {
        if (contrat == null) {
            MessageFlash.erreur(request, "Ce contrat n'existe pas.");
            return "redirect:/agent/contrats";
        }
        if (!ContratService.estModifiable(contrat) || contratService.estPaye(id)) {
            MessageFlash.erreur(request, "Seul un contrat en attente ou suspendu, pas encore payé, peut être modifié.");
            return "redirect:/agent/contrats/detail?id=" + id;
        }
        return null;
    }

    /** Type demandé (?type=...), ou null s'il est absent, inconnu ou archivé (retour à l'étape 1). */
    private TypeAssurance typeDemande(String valeur) {
        Long id = Validateur.idOuNull(valeur);
        TypeAssurance type = id == null ? null : typeAssuranceService.trouverAvecGaranties(id);
        return type != null && type.isActif() ? type : null;
    }

    /** Étape 1 (choix du type) ou étape 2 (formulaire complet) de la création. */
    private String afficherNouveau(HttpServletRequest request, TypeAssurance type, Set<Long> garantiesCochees) {
        // Seuls les types non archivés sont proposés pour un nouveau contrat
        request.setAttribute("types", typeAssuranceService.listerActifs());
        request.setAttribute("typeChoisi", type);
        request.setAttribute("dureeEnMois", ContratService.DUREE_EN_MOIS);
        request.setAttribute("aujourdhui", LocalDate.now());   // date de début minimale
        if (type != null) {
            request.setAttribute("clients", clientService.listerActifs());
            request.setAttribute("garantiesDisponibles", type.getGaranties().stream()
                    .sorted(Comparator.comparing(Garantie::getNom)).toList());
            request.setAttribute("garantiesCochees", garantiesCochees);
        }
        return VUE_NOUVEAU;
    }

    /** Formulaire de modification, avec les garanties proposées par le type du contrat. */
    private String afficherModification(HttpServletRequest request, Contrat contrat, Set<Long> garantiesCochees) {
        TypeAssurance type = typeAssuranceService.trouverAvecGaranties(contrat.getTypeAssurance().getId());
        request.setAttribute("contrat", contrat);
        request.setAttribute("garantiesDisponibles", type.getGaranties().stream()
                .sorted(Comparator.comparing(Garantie::getNom)).toList());
        request.setAttribute("garantiesCochees", garantiesCochees);
        request.setAttribute("dureeEnMois", ContratService.DUREE_EN_MOIS);
        request.setAttribute("aujourdhui", LocalDate.now());   // date de début minimale
        return VUE_MODIFIER;
    }
}
