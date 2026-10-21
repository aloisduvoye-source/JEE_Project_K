package fr.cytech.assurance.controller;

import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Déconnexion en POST (bouton) : un simple lien ou une image ne peut pas déconnecter quelqu'un à son insu
@WebServlet("/deconnexion")
public class DeconnexionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUtilisateur.deconnecter(request);
        response.sendRedirect(request.getContextPath() + "/");
    }

    // Accès direct à l'URL : retour à l'accueil, sans déconnecter
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/");
    }
}
