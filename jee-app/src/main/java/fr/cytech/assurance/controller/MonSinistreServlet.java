package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.SessionUtilisateur;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Fiche d'un sinistre du client connecté : le client ne voit que ses propres sinistres
@WebServlet("/client/sinistres/detail")
public class MonSinistreServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/sinistre-detail.jsp";

    private final ClientService clientService = new ClientService();
    private final SinistreService sinistreService = new SinistreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Le client vient de la session, jamais de l'adresse : un id de sinistre d'un autre client ne donne rien
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Sinistre sinistre = client == null || id == null ? null : sinistreService.trouverDuClient(id, client.getId());
        if (sinistre == null) {
            MessageFlash.erreur(request, "Ce sinistre n'existe pas.");
            response.sendRedirect(request.getContextPath() + "/client/sinistres");
            return;
        }
        request.setAttribute("sinistre", sinistre);
        request.setAttribute("indemnisation", sinistreService.indemnisation(id));
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
