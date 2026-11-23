package fr.cytech.assurance.exception;

// Levée quand une opération sur un compte ne respecte pas une règle métier
// (compte introuvable, compte administrateur désactivé ou supprimé...)
// Le message est destiné à être affiché à l'utilisateur
public class UtilisateurInvalideException extends RuntimeException {

    public UtilisateurInvalideException(String message) {
        super(message);
    }
}
