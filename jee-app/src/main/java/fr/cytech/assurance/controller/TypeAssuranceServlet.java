package fr.cytech.assurance.controller;

import fr.cytech.assurance.service.TypeAssuranceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Liste des types d'assurance avec leurs garanties disponibles, pour l'administrateur
@WebServlet("/admin/types-assurance")
public class TypeAssuranceServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/admin/types-assurance.jsp";

    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Garanties chargées dans la requête : la JSP lit type.garanties après la fermeture de l'EntityManager
        request.setAttribute("types", typeAssuranceService.listerAvecGaranties());
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
