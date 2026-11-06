package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.ClientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Liste de tous les clients, pour les agents (et les admins) : §2.1 du sujet, « consulter les clients »
@WebServlet("/agent/clients")
public class ClientsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/clients.jsp";

    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("clients", clientService.lister());
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
