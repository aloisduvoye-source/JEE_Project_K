package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Types d'assurance et garanties qu'ils proposent, pour l'administrateur (remplace TypeAssuranceServlet,
 * NouveauTypeAssuranceServlet, ModifierTypeAssuranceServlet et ActionTypeAssuranceServlet).
 */
@Controller
public class TypeAssuranceController {

    private static final String LISTE = "redirect:/admin/types-assurance";
    private static final String VUE_NOUVEAU = "admin/type-assurance-nouveau";
    private static final String VUE_MODIFIER = "admin/type-assurance-modifier";

    private final TypeAssuranceService typeAssuranceService;

    /** Dépendances injectées par Spring. */
    public TypeAssuranceController(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    /** Liste des types avec leurs garanties disponibles. */
    @GetMapping("/admin/types-assurance")
    public String lister(HttpServletRequest request) {
        // Garanties chargées dans la requête : la JSP lit type.garanties après la fin de la transaction
        request.setAttribute("types", typeAssuranceService.listerAvecGaranties());
        // Types utilisés par un contrat : la JSP ne leur affiche pas le bouton Supprimer
        request.setAttribute("typesUtilises", typeAssuranceService.idsUtilisesParContrat());
        return "admin/types-assurance";
    }

    /** Formulaire de création : nom, description, prix de base, garanties proposées. */
    @GetMapping("/admin/types-assurance/nouveau")
    public String nouveau(HttpServletRequest request) {
        return afficherNouveau(request, Set.of());
    }

    /** Vérifie le format des saisies, puis le service applique les règles et crée le type. */
    @PostMapping("/admin/types-assurance/nouveau")
    public String creer(HttpServletRequest request) {
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal prixBase = v.montantPositif("prixBase", Validateur.nettoyer(request.getParameter("prixBase")), "Le prix de base");
        Set<Long> idsGaranties = v.identifiants("garanties", request.getParameterValues("garanties"), "Une garantie");

        if (v.estValide()) {
            try {
                TypeAssurance type = typeAssuranceService.creer(nom, description, prixBase, idsGaranties);
                return LISTE + "?cree=" + type.getId();
            } catch (TypeAssuranceInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }
        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"nom", "description", "prixBase"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        return afficherNouveau(request, idsGaranties);
    }

    /** Formulaire de modification (?id=...), prérempli avec les valeurs enregistrées. */
    @GetMapping("/admin/types-assurance/modifier")
    public String modification(HttpServletRequest request) {
        TypeAssurance type = typeDemande(request);
        if (type == null) {
            return retourListe(request);
        }
        preremplir(request, type);
        return afficherModification(request, type);
    }

    /**
     * La page de modification contient plusieurs formulaires, distingués par le paramètre "action" :
     * "infos" (nom, description et prix de base), "ajouter" ou "retirer" (une garantie proposée).
     */
    @PostMapping("/admin/types-assurance/modifier")
    public String modifier(HttpServletRequest request) {
        TypeAssurance type = typeDemande(request);
        if (type == null) {
            return retourListe(request);
        }
        String action = Validateur.nettoyer(request.getParameter("action"));
        if (action.equals("ajouter") || action.equals("retirer")) {
            return modifierGaranties(request, type, action);
        }
        return modifierInfos(request, type);
    }

    /**
     * Boutons de la liste, en POST uniquement : /supprimer (type jamais utilisé par un contrat),
     * /archiver (plus proposé pour les nouveaux contrats) ou /reactiver (de nouveau proposé).
     */
    @PostMapping("/admin/types-assurance/{action:supprimer|archiver|reactiver}")
    public String agir(@PathVariable String action, HttpServletRequest request) {
        Validateur v = new Validateur();
        Long id = v.identifiant("id", Validateur.nettoyer(request.getParameter("id")), "Le type d'assurance");
        if (!v.estValide()) {
            MessageFlash.erreur(request, v.getErreurs().get("id"));
            return LISTE;
        }
        try {
            // Paramètre ajouté à l'adresse de la liste pour afficher le message de confirmation
            String confirmation = switch (action) {
                case "supprimer" -> {
                    typeAssuranceService.supprimer(id);
                    yield "supprime";
                }
                case "archiver" -> {
                    typeAssuranceService.archiver(id);
                    yield "archive";
                }
                default -> {
                    typeAssuranceService.reactiver(id);
                    yield "reactive";
                }
            };
            return LISTE + "?" + confirmation + "=1";
        } catch (TypeAssuranceInvalideException e) {
            // Type introuvable, ou utilisé par un contrat (suppression)
            MessageFlash.erreur(request, e.getMessage());
            return LISTE;
        }
    }

    /** Vérifie le format des saisies, puis le service applique les règles (nom unique, prix > 0). */
    private String modifierInfos(HttpServletRequest request, TypeAssurance type) {
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal prixBase = v.montantPositif("prixBase", Validateur.nettoyer(request.getParameter("prixBase")), "Le prix de base");

        if (v.estValide()) {
            try {
                typeAssuranceService.modifier(type.getId(), nom, description, prixBase);
                return LISTE + "?modifie=1";
            } catch (TypeAssuranceInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }
        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"nom", "description", "prixBase"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        return afficherModification(request, type);
    }

    /** Ajoute ou retire une garantie, puis revient sur la page de modification pour en voir le résultat. */
    private String modifierGaranties(HttpServletRequest request, TypeAssurance type, String action) {
        Validateur v = new Validateur();
        Long idGarantie = v.identifiant("garantie", Validateur.nettoyer(request.getParameter("garantie")), "La garantie");
        if (v.estValide()) {
            try {
                if (action.equals("ajouter")) {
                    typeAssuranceService.ajouterGarantie(type.getId(), idGarantie);
                } else {
                    typeAssuranceService.retirerGarantie(type.getId(), idGarantie);
                }
                return "redirect:/admin/types-assurance/modifier?id=" + type.getId() + "&garanties=1";
            } catch (TypeAssuranceInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }
        // Réaffiche la page avec le message ; le formulaire des informations garde les valeurs enregistrées
        request.setAttribute("erreurs", v.getErreurs());
        preremplir(request, type);
        return afficherModification(request, type);
    }

    /** Type désigné par le paramètre id (avec ses garanties), ou null s'il est absent, invalide ou inconnu. */
    private TypeAssurance typeDemande(HttpServletRequest request) {
        Long id = Validateur.idOuNull(request.getParameter("id"));
        return id == null ? null : typeAssuranceService.trouverAvecGaranties(id);
    }

    /** Type introuvable : retour à la liste, qui affiche le message une fois. */
    private String retourListe(HttpServletRequest request) {
        MessageFlash.erreur(request, "Ce type d'assurance n'existe pas.");
        return LISTE;
    }

    /** Valeurs enregistrées en base, affichées dans le formulaire des informations. */
    private void preremplir(HttpServletRequest request, TypeAssurance type) {
        request.setAttribute("nom", type.getNom());
        request.setAttribute("description", type.getDescription());
        request.setAttribute("prixBase", type.getPrixBase().toPlainString());
    }

    /** Formulaire de création, avec toutes les garanties à cocher. */
    private String afficherNouveau(HttpServletRequest request, Set<Long> garantiesCochees) {
        request.setAttribute("garanties", typeAssuranceService.listerGaranties());
        request.setAttribute("garantiesCochees", garantiesCochees);
        return VUE_NOUVEAU;
    }

    /** Garanties proposées par ce type, et les autres garanties existantes que l'on peut ajouter. */
    private String afficherModification(HttpServletRequest request, TypeAssurance type) {
        List<Garantie> proposees = type.getGaranties().stream()
                .sorted(Comparator.comparing(Garantie::getNom)).toList();
        Set<Long> idsProposees = proposees.stream().map(Garantie::getId).collect(Collectors.toSet());
        List<Garantie> autres = typeAssuranceService.listerGaranties().stream()
                .filter(g -> !idsProposees.contains(g.getId())).toList();

        request.setAttribute("type", type);
        request.setAttribute("garantiesProposees", proposees);
        request.setAttribute("garantiesAutres", autres);
        return VUE_MODIFIER;
    }
}
