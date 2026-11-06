package fr.cytech.assurance.exception;

// Levée quand une opération sur une garantie ne respecte pas une règle métier
// (nom déjà pris, garantie souscrite par un contrat...)
// Le message est destiné à être affiché à l'utilisateur
public class GarantieInvalideException extends RuntimeException {

    public GarantieInvalideException(String message) {
        super(message);
    }
}
