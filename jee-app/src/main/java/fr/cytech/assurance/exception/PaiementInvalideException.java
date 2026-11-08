package fr.cytech.assurance.exception;

// Levée quand un paiement ne peut pas être enregistré (contrat déjà payé, résilié, expiré...)
// Le message est destiné à être affiché à l'utilisateur
public class PaiementInvalideException extends RuntimeException {

    public PaiementInvalideException(String message) {
        super(message);
    }
}
