package fr.cytech.assurance.exception;

// Levée quand une action sur un sinistre ne respecte pas une règle métier
// (contrat qui ne couvre pas la date, statut qui ne permet pas l'action, dossier clos...)
// Le message est destiné à être affiché à l'utilisateur
public class SinistreInvalideException extends RuntimeException {

    public SinistreInvalideException(String message) {
        super(message);
    }
}
