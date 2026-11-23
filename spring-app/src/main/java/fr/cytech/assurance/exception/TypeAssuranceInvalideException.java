package fr.cytech.assurance.exception;

// Levée quand une opération sur un type d'assurance ne respecte pas une règle métier
// (nom déjà pris, prix incorrect, type utilisé par un contrat...)
// Le message est destiné à être affiché à l'utilisateur
public class TypeAssuranceInvalideException extends RuntimeException {

    public TypeAssuranceInvalideException(String message) {
        super(message);
    }
}
