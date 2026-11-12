package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.service.ContratService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.MessageFlash;
import fr.cytech.assurance.util.Validateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

// Fiche d'un contrat pour l'agent (et l'admin) : client, type, garanties, paiement (§2.2, « consulter un contrat »)
@WebServlet("/agent/contrats/detail")
public class ContratDetailServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/agent/contrat-detail.jsp";

    private final ContratService contratService = new ContratService();
    private final PaiementService paiementService = new PaiementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long id = Validateur.idOuNull(request.getParameter("id"));
        Contrat contrat = id == null ? null : contratService.trouverAvecDetails(id);
        if (contrat == null) {
            MessageFlash.erreur(request, "Ce contrat n'existe pas.");
            response.sendRedirect(request.getContextPath() + "/agent/contrats");
            return;
        }
        request.setAttribute("contrat", contrat);
        request.setAttribute("paiement", paiementService.trouverParContrat(id));
        // Renouvellement, suspension et remboursement : affichés sur la fiche (voir ContratService)
        request.setAttribute("renouvellement", contratService.trouverRenouvellement(id));
        request.setAttribute("renouvelable", contratService.raisonNonRenouvelable(contrat) == null);
        request.setAttribute("remboursement", contratService.trouverRemboursement(id));
        request.setAttribute("remboursementSiResiliation", contratService.remboursement(contrat, LocalDate.now()));
        request.getRequestDispatcher(VUE).forward(request, response);
    }
}
