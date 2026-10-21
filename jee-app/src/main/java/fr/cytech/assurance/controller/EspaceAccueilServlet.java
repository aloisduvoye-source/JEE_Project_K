package fr.cytech.assurance.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Page d'accueil de chaque espace (l'accès par rôle est vérifié par RoleFiltre)
// "/client/accueil" affiche "/WEB-INF/views/client/accueil.jsp", et de même pour agent et admin
@WebServlet({"/client/accueil", "/agent/accueil", "/admin/accueil"})
public class EspaceAccueilServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views" + request.getServletPath() + ".jsp").forward(request, response);
    }
}
