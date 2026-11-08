package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

// Modification d'un contrat pas encore payé par un agent (pas l'admin, qui consulte seulement) :
// garanties, date de début, prime. Le client et le type ne changent pas.
@WebServlet("/agent/contrats/modifier")
public class ModifierContratServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/contrat-modifier.jsp";

    private final ContratService contratService = new ContratService();
    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    // Formulaire prérempli avec les valeurs actuelles du contrat
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Contrat contrat = contratModifiable(request, response);
        if (contrat == null) {
            return;
        }
        request.setAttribute("dateDebut", contrat.getDateDebut());
        request.setAttribute("prime", contrat.getPrime().toPlainString());
        afficher(request, response, contrat,
                contrat.getGaranties().stream().map(Garantie::getId).collect(Collectors.toSet()));
    }

    // Vérifie le format des saisies, puis le service applique les règles et enregistre
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Contrat contrat = contratModifiable(request, response);
        if (contrat == null) {
            return;
        }
        Validateur v = new Validateur();
        LocalDate dateDebut = v.date("dateDebut", Validateur.nettoyer(request.getParameter("dateDebut")), "La date de début");
        BigDecimal prime = v.montant("prime", Validateur.nettoyer(request.getParameter("prime")), "La prime");
        Set<Long> idsGaranties = v.identifiants("garanties", request.getParameterValues("garanties"), "Une garantie");

        if (v.estValide()) {
            try {
                contratService.modifierContrat(contrat.getId(), idsGaranties, dateDebut, prime);
                response.sendRedirect(request.getContextPath() + "/agent/contrats/detail?id=" + contrat.getId() + "&modifie=1");
                return;
            } catch (ContratInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }

        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"dateDebut", "prime"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        afficher(request, response, contrat, idsGaranties);
    }

    // Contrat demandé (?id=...) s'il peut être modifié par l'utilisateur connecté ; sinon répond (403 ou retour
    // à la fiche avec un message) et renvoie null
    private Contrat contratModifiable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SessionUtilisateur.connecte(request).getRole() != Role.AGENT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = id == null ? null : contratService.trouverAvecDetails(id);
        if (contrat == null) {
            MessageFlash.erreur(request, "Ce contrat n'existe pas.");
            response.sendRedirect(request.getContextPath() + "/agent/contrats");
            return null;
        }
        if (!ContratService.estModifiable(contrat) || contratService.estPaye(id)) {
            MessageFlash.erreur(request, "Seul un contrat en attente ou suspendu, pas encore payé, peut être modifié.");
            response.sendRedirect(request.getContextPath() + "/agent/contrats/detail?id=" + id);
            return null;
        }
        return contrat;
    }

    private void afficher(HttpServletRequest request, HttpServletResponse response, Contrat contrat,
                          Set<Long> garantiesCochees) throws ServletException, IOException {
        TypeAssurance type = typeAssuranceService.trouverAvecGaranties(contrat.getTypeAssurance().getId());
        request.setAttribute("contrat", contrat);
        request.setAttribute("garantiesDisponibles", type.getGaranties().stream()
                .sorted(Comparator.comparing(Garantie::getNom)).toList());
        request.setAttribute("garantiesCochees", garantiesCochees);
        request.setAttribute("dureeEnMois", ContratService.DUREE_EN_MOIS);
        request.setAttribute("aujourdhui", LocalDate.now());   // date de début minimale
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
