package fr.cytech.assurance.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Page d'accueil : "" correspond à la racine du site (http://localhost:8080/)
@WebServlet("")
public class AccueilServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // La servlet (contrôleur) délègue l'affichage à la JSP (vue)
        request.getRequestDispatcher("/WEB-INF/views/common/accueil.jsp").forward(request, response);
    }
}
