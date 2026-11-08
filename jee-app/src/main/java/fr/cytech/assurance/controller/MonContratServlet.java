package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Fiche d'un contrat du client connecté, avec son paiement : le client ne voit que ses propres contrats
@WebServlet("/client/contrats/detail")
public class MonContratServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/contrat-detail.jsp";

    private final ClientService clientService = new ClientService();
    private final ContratService contratService = new ContratService();
    private final PaiementService paiementService = new PaiementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Le client vient de la session, jamais de l'adresse : un id de contrat d'un autre client ne donne rien
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = client == null || id == null ? null : contratService.trouverDuClient(id, client.getId());
        if (contrat == null) {
            MessageFlash.erreur(request, "Ce contrat n'existe pas.");
            response.sendRedirect(request.getContextPath() + "/client/contrats");
            return;
        }
        request.setAttribute("contrat", contrat);
        request.setAttribute("paiement", paiementService.trouverParContrat(id));
        request.setAttribute("modes", ModePaiement.values());
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
