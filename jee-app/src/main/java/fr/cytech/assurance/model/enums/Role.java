package fr.cytech.assurance.model.enums;

public enum Role {
    CLIENT,
    AGENT,
    ADMIN;

    // Page d'accueil de l'espace du rôle : "/client/accueil", "/agent/accueil", "/admin/accueil"
    // (utilisable dans une JSP avec ${utilisateur.role.accueil})
    public String getAccueil() {return "/" + name().toLowerCase() + "/accueil";}
}
