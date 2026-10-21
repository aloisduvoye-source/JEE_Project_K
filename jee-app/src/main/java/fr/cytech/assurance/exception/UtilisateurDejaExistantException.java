package fr.cytech.assurance.exception;

// Levée quand on tente de créer un utilisateur dont l'identifiant ou l'email est déjà pris
public class UtilisateurDejaExistantException extends RuntimeException {

    // "identifiant", "email", ou null si la base a refusé sans qu'on sache lequel
    private final String champ;

    public UtilisateurDejaExistantException(String champ) {
        super(champ == null ? "Identifiant ou email déjà utilisé" : champ + " déjà utilisé");
        this.champ = champ;
    }

    public String getChamp() {return champ;}
}
