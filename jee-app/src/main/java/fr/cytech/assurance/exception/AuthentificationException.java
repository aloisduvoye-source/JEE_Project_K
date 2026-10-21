package fr.cytech.assurance.exception;

// Levée quand une connexion est refusée : identifiants incorrects ou compte désactivé
// Le message est destiné à être affiché à l'utilisateur
public class AuthentificationException extends RuntimeException {

    public AuthentificationException(String message) {
        super(message);
    }
}
