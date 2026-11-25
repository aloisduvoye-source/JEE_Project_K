package fr.cytech.assurance.exception;

// Levée quand un contrat ne respecte pas une règle métier (garantie non proposée, dates incohérentes...)
// Le message est destiné à être affiché à l'utilisateur
public class ContratInvalideException extends RuntimeException {

    public ContratInvalideException(String message) {
        super(message);
    }
}
