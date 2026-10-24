package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.ContratService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Liste de tous les contrats, pour les agents (et les admins)
@WebServlet("/agent/contrats")
public class ContratsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/contrats.jsp";

    private final ContratService contratService = new ContratService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("contrats", contratService.listerContrats());
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
