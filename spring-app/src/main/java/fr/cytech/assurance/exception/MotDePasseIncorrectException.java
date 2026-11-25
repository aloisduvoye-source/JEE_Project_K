package fr.cytech.assurance.exception;

// Levée quand le mot de passe actuel fourni pour une opération sensible est faux
public class MotDePasseIncorrectException extends RuntimeException {

    public MotDePasseIncorrectException() {
        super("Mot de passe actuel incorrect.");
    }
}
