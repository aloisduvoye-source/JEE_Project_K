package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.util.Validateur;
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
        // Recherche facultative (?q=...) sur le nom, le prénom ou l'email
        String motCle = Validateur.nettoyer(request.getParameter("q"));
        Validateur v = new Validateur();
        v.longueur("q", motCle, "La recherche", 100);
        if (!v.estValide()) {
            request.setAttribute("erreurs", v.getErreurs());
            motCle = "";
        }
        request.setAttribute("q", motCle);
        request.setAttribute("clients", clientService.rechercher(motCle));
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
