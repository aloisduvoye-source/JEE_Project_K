package fr.cytech.assurance.controller;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.service.ClientService;
import fr.cytech.assurance.service.PaiementService;
import fr.cytech.assurance.util.SessionUtilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/client/paiements")
public class MesPaiementsServlet extends HttpServlet {

    private static final String VUE = "/WEB-INF/views/client/paiements.jsp";

    private final ClientService clientService = new ClientService();
    private final PaiementService paiementService = new PaiementService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Client client = clientService.trouverParUtilisateur(SessionUtilisateur.connecte(request).getId());

        StatutPaiement statut = parseEnum(StatutPaiement.class, request.getParameter("statut"));
        ModePaiement mode = parseEnum(ModePaiement.class, request.getParameter("mode"));
        String tri = request.getParameter("tri");

        request.setAttribute("paiements", client == null
                ? List.of()
                : paiementService.listerPourClient(client.getId(), statut, mode, tri));
        request.setAttribute("statuts", StatutPaiement.values());
        request.setAttribute("modes", ModePaiement.values());
        request.setAttribute("statutSelectionne", statut);
        request.setAttribute("modeSelectionne", mode);
        request.setAttribute("triSelectionne", tri == null ? "echeance_desc" : tri);

        request.getRequestDispatcher(VUE).forward(request, response);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, valeur);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}