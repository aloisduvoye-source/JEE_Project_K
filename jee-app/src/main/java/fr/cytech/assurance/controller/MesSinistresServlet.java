package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.SinistreService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

// Sinistres du client connecté, du plus récent au plus ancien
@WebServlet("/client/sinistres")
public class MesSinistresServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/sinistres.jsp";

    private final ClientService clientService = new ClientService();
    private final SinistreService sinistreService = new SinistreService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());
        request.setAttribute("sinistres", client == null ? List.of() : sinistreService.listerDuClient(client.getId()));
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
