package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Liste et recherche des contrats, pour les agents (et les admins) (§2.2, « rechercher un contrat »).
 * Critères facultatifs dans l'adresse (formulaire en GET) :
 *   ?q=      numéro de contrat, ou nom, prénom ou email du client
 *   ?statut= statut du contrat
 *   ?type=   id du type d'assurance
 */
@WebServlet("/agent/contrats")
public class ContratsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/contrats.jsp";
    private static final int LONGUEUR_MAX_RECHERCHE = 100;

    private final ContratService contratService = new ContratService();
    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String texte = Validateur.nettoyer(request.getParameter("q"));
        if (texte.length() > LONGUEUR_MAX_RECHERCHE) {
            texte = texte.substring(0, LONGUEUR_MAX_RECHERCHE);
        }
        StatutContrat statut = statut(request.getParameter("statut"));
        Long idType = Validateur.idOuNull(request.getParameter("type"));

        request.setAttribute("contrats", contratService.rechercher(texte, statut, idType));
        request.setAttribute("q", texte);
        request.setAttribute("statutChoisi", statut);
        request.setAttribute("typeChoisi", idType);
        request.setAttribute("statuts", StatutContrat.values());
        // Tous les types, archivés compris : ils restent liés à des contrats existants
        request.setAttribute("types", typeAssuranceService.listerAvecGaranties());
        request.setAttribute("recherche", !texte.isEmpty() || statut != null || idType != null);
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    // Statut demandé, ou null (tous) s'il est absent ou inconnu
    private StatutContrat statut(String valeur) {
        try {
            return valeur == null || valeur.isBlank() ? null : StatutContrat.valueOf(valeur.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
