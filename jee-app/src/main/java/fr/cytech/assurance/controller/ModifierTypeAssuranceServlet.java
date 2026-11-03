package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modification d'un type d'assurance par l'administrateur (/admin/types-assurance/modifier?id=...).
 * La page contient plusieurs formulaires, distingués par le paramètre "action" :
 *   - "infos"   : nom, description et prix de base
 *   - "ajouter" : propose une garantie de plus avec ce type
 *   - "retirer" : ne propose plus une garantie (les contrats existants la gardent)
 */
@WebServlet("/admin/types-assurance/modifier")
public class ModifierTypeAssuranceServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/admin/type-assurance-modifier.jsp";

    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    // Affiche le formulaire prérempli avec les valeurs enregistrées
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TypeAssurance type = typeDemande(request);
        if (type == null) {
            retourListe(request, response);
            return;
        }
        preremplir(request, type);
        afficher(request, response, type);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TypeAssurance type = typeDemande(request);
        if (type == null) {
            retourListe(request, response);
            return;
        }
        String action = Validateur.nettoyer(request.getParameter("action"));
        if (action.equals("ajouter") || action.equals("retirer")) {
            modifierGaranties(request, response, type, action);
        } else {
            modifierInfos(request, response, type);
        }
    }

    // Vérifie le format des saisies, puis le service applique les règles (nom unique, prix > 0)
    private void modifierInfos(HttpServletRequest request, HttpServletResponse response, TypeAssurance type)
            throws ServletException, IOException {
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal prixBase = v.montantPositif("prixBase", Validateur.nettoyer(request.getParameter("prixBase")), "Le prix de base");

        if (v.estValide()) {
            try {
                typeAssuranceService.modifier(type.getId(), nom, description, prixBase);
                response.sendRedirect(request.getContextPath() + "/admin/types-assurance?modifie=1");
                return;
            } catch (TypeAssuranceInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }

        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"nom", "description", "prixBase"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        afficher(request, response, type);
    }

    // Ajoute ou retire une garantie, puis revient sur cette page pour en voir le résultat
    private void modifierGaranties(HttpServletRequest request, HttpServletResponse response,
                                   TypeAssurance type, String action) throws ServletException, IOException {
        Validateur v = new Validateur();
        Long idGarantie = v.identifiant("garantie", Validateur.nettoyer(request.getParameter("garantie")), "La garantie");

        if (v.estValide()) {
            try {
                if (action.equals("ajouter")) {
                    typeAssuranceService.ajouterGarantie(type.getId(), idGarantie);
                } else {
                    typeAssuranceService.retirerGarantie(type.getId(), idGarantie);
                }
                response.sendRedirect(request.getContextPath()
                        + "/admin/types-assurance/modifier?id=" + type.getId() + "&garanties=1");
                return;
            } catch (TypeAssuranceInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }

        // Réaffiche la page avec le message ; le formulaire des informations garde les valeurs enregistrées
        request.setAttribute("erreurs", v.getErreurs());
        preremplir(request, type);
        afficher(request, response, type);
    }

    // Type désigné par le paramètre id (avec ses garanties), ou null s'il est absent, invalide ou inconnu
    private TypeAssurance typeDemande(HttpServletRequest request) {
        String valeur = Validateur.nettoyer(request.getParameter("id"));
        try {
            return valeur.isEmpty() ? null : typeAssuranceService.trouverAvecGaranties(Long.valueOf(valeur));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Type introuvable : retour à la liste, qui affiche le message une fois
    private void retourListe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.getSession().setAttribute(SupprimerTypeAssuranceServlet.ATTRIBUT_ERREUR, "Ce type d'assurance n'existe pas.");
        response.sendRedirect(request.getContextPath() + "/admin/types-assurance");
    }

    // Valeurs enregistrées en base, affichées dans le formulaire des informations
    private void preremplir(HttpServletRequest request, TypeAssurance type) {
        request.setAttribute("nom", type.getNom());
        request.setAttribute("description", type.getDescription());
        request.setAttribute("prixBase", type.getPrixBase().toPlainString());
    }

    // Garanties proposées par ce type, et les autres garanties existantes que l'on peut ajouter
    private void afficher(HttpServletRequest request, HttpServletResponse response, TypeAssurance type)
            throws ServletException, IOException {
        List<Garantie> proposees = type.getGaranties().stream()
                .sorted(Comparator.comparing(Garantie::getNom)).toList();
        Set<Long> idsProposees = proposees.stream().map(Garantie::getId).collect(Collectors.toSet());
        List<Garantie> autres = typeAssuranceService.listerGaranties().stream()
                .filter(g -> !idsProposees.contains(g.getId())).toList();

        request.setAttribute("type", type);
        request.setAttribute("garantiesProposees", proposees);
        request.setAttribute("garantiesAutres", autres);
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
