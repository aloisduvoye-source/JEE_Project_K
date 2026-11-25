package fr.cytech.assurance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Pages d'accueil : la page publique et celle de chaque espace (remplace AccueilServlet et EspaceAccueilServlet).
 * L'accès aux espaces par rôle est vérifié par RoleIntercepteur.
 */
@Controller
public class AccueilController {

    /** Page d'accueil publique. */
    @GetMapping("/")
    public String accueil() {
        return "common/accueil";
    }

    /** "/client/accueil" affiche "client/accueil.jsp", et de même pour agent et admin. */
    @GetMapping("/{espace:client|agent|admin}/accueil")
    public String espace(@PathVariable String espace) {
        return espace + "/accueil";
    }
}
