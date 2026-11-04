package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.TypeAssuranceService;
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

/**
 * Création d'un contrat par un agent (ou un admin), en deux étapes sans JavaScript :
 *   1. choix du type d'assurance : la page se recharge avec ?type=...
 *   2. formulaire complet, qui ne propose que les garanties de ce type
 */
@WebServlet("/agent/contrats/nouveau")
public class NouveauContratServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/contrat-nouveau.jsp";

    // Le contrôleur prépare la page à partir de plusieurs domaines : clients, catalogue, contrats
    private final ContratService contratService = new ContratService();
    private final ClientService clientService = new ClientService();
    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    // Affiche l'étape 1, ou l'étape 2 si un type est choisi (prime préremplie avec son prix de base)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TypeAssurance type = typeDemande(Validateur.nettoyer(request.getParameter("type")));
        if (type != null) {
            request.setAttribute("prime", type.getPrixBase().toPlainString());
        }
        afficher(request, response, type, Set.of());
    }

    // Vérifie le format des saisies, puis le service applique les règles et crée le contrat
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Validateur v = new Validateur();
        Long idType = v.identifiant("type", Validateur.nettoyer(request.getParameter("type")), "Le type d'assurance");
        Long idClient = v.identifiant("client", Validateur.nettoyer(request.getParameter("client")), "Le client");
        LocalDate dateDebut = v.date("dateDebut", Validateur.nettoyer(request.getParameter("dateDebut")), "La date de début");
        LocalDate dateFin = v.date("dateFin", Validateur.nettoyer(request.getParameter("dateFin")), "La date de fin");
        BigDecimal prime = v.montant("prime", Validateur.nettoyer(request.getParameter("prime")), "La prime");

        Set<Long> idsGaranties = v.identifiants("garanties", request.getParameterValues("garanties"), "Une garantie");

        if (v.estValide()) {
            try {
                Contrat contrat = contratService.creerContrat(idClient, idType, idsGaranties, dateDebut, dateFin, prime);
                response.sendRedirect(request.getContextPath() + "/agent/contrats?cree=" + contrat.getId());
                return;
            } catch (ContratInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }

        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"client", "dateDebut", "dateFin", "prime"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        afficher(request, response, typeDemande(Validateur.nettoyer(request.getParameter("type"))), idsGaranties);
    }

    // Type demandé (?type=...), ou null s'il est absent, inconnu ou archivé (retour à l'étape 1)
    private TypeAssurance typeDemande(String valeur) {
        try {
            TypeAssurance type = valeur.isEmpty() ? null : typeAssuranceService.trouverAvecGaranties(Long.valueOf(valeur));
            return type != null && type.isActif() ? type : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void afficher(HttpServletRequest request, HttpServletResponse response,
                          TypeAssurance type, Set<Long> garantiesCochees) throws ServletException, IOException {
        // Seuls les types non archivés sont proposés pour un nouveau contrat
        request.setAttribute("types", typeAssuranceService.listerActifs());
        request.setAttribute("typeChoisi", type);
        if (type != null) {
            request.setAttribute("clients", clientService.listerActifs());
            request.setAttribute("garantiesDisponibles", type.getGaranties().stream()
                    .sorted(Comparator.comparing(Garantie::getNom)).toList());
            request.setAttribute("garantiesCochees", garantiesCochees);
        }
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
