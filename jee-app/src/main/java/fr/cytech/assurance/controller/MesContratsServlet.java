package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

// Contrats du client connecté, avec leurs garanties, filtrables et triables : il ne voit que les siens
@WebServlet("/client/contrats")
public class MesContratsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/contrats.jsp";

    private final ClientService clientService = new ClientService();
    private final ContratService contratService = new ContratService();
    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Le client vient de la session, jamais de l'adresse : impossible de voir les contrats d'un autre
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());

        StatutContrat statut = statut(request.getParameter("statut"));
        Long idType = Validateur.idOuNull(request.getParameter("type"));
        String tri = Validateur.nettoyer(request.getParameter("tri"));

        request.setAttribute("contrats", client == null
                ? List.of()
                : contratService.listerContratsDuClient(client.getId(), statut, idType, tri));
        request.setAttribute("statutChoisi", statut);
        request.setAttribute("typeChoisi", idType);
        request.setAttribute("triChoisi", tri.isEmpty() ? "debut_desc" : tri);
        request.setAttribute("statuts", StatutContrat.values());
        request.setAttribute("types", typeAssuranceService.listerAvecGaranties());
        request.setAttribute("recherche", statut != null || idType != null || !tri.isEmpty());
        request.getRequestDispatcher(VUE).forward(request, response);
    }

    private StatutContrat statut(String valeur) {
        try {
            return valeur == null || valeur.isBlank() ? null : StatutContrat.valueOf(valeur.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}