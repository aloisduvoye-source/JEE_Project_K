package fr.cytech.assurance.controller;

import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.service.TypeAssuranceService;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

// Création d'un type d'assurance par l'administrateur : nom, description, prix de base, garanties proposées
@WebServlet("/admin/types-assurance/nouveau")
public class NouveauTypeAssuranceServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/admin/type-assurance-nouveau.jsp";

    private final TypeAssuranceService typeAssuranceService = new TypeAssuranceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        afficher(request, response, Set.of());
    }

    // Vérifie le format des saisies, puis le service applique les règles et crée le type
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String nom = Validateur.nettoyer(request.getParameter("nom"));
        String description = Validateur.nettoyer(request.getParameter("description"));

        Validateur v = new Validateur();
        v.obligatoire("nom", nom, "Le nom", 100);
        v.longueur("description", description, "La description", 1000);
        BigDecimal prixBase = v.montantPositif("prixBase", Validateur.nettoyer(request.getParameter("prixBase")), "Le prix de base");

        Set<Long> idsGaranties = new HashSet<>();
        String[] garantiesCochees = request.getParameterValues("garanties");
        if (garantiesCochees != null) {
            for (String id : garantiesCochees) {
                Long idGarantie = v.identifiant("garanties", Validateur.nettoyer(id), "Une garantie");
                if (idGarantie != null) {
                    idsGaranties.add(idGarantie);
                }
            }
        }

        if (v.estValide()) {
            try {
                TypeAssurance type = typeAssuranceService.creer(nom, description, prixBase, idsGaranties);
                response.sendRedirect(request.getContextPath() + "/admin/types-assurance?cree=" + type.getId());
                return;
            } catch (TypeAssuranceInvalideException e) {
                v.erreur("global", e.getMessage());
            }
        }

        // Réaffiche le formulaire avec les valeurs saisies et les messages
        request.setAttribute("erreurs", v.getErreurs());
        for (String champ : new String[]{"nom", "description", "prixBase"}) {
            request.setAttribute(champ, Validateur.nettoyer(request.getParameter(champ)));
        }
        afficher(request, response, idsGaranties);
    }

    private void afficher(HttpServletRequest request, HttpServletResponse response, Set<Long> garantiesCochees)
            throws ServletException, IOException {
        request.setAttribute("garanties", typeAssuranceService.listerGaranties());
        request.setAttribute("garantiesCochees", garantiesCochees);
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
