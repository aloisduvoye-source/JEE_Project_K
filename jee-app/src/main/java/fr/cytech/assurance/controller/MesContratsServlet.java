package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

// Contrats du client connecté, avec leurs garanties : il ne voit que les siens
@WebServlet("/client/contrats")
public class MesContratsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/contrats.jsp";

    private final ClientService clientService = new ClientService();
    private final ContratService contratService = new ContratService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Le client vient de la session, jamais de l'adresse : impossible de voir les contrats d'un autre
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
        request.setAttribute("contrats", client == null ? List.of() : contratService.listerContratsDuClient(client.getId()));
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
