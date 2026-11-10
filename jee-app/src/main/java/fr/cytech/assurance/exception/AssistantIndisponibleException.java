package fr.cytech.assurance.exception;

// Levée quand l'API d'IA ne peut pas répondre (clé absente ou invalide, service surchargé, réseau...)
// Le message est destiné à être affiché à l'utilisateur ; la cause technique est conservée pour les logs
public class AssistantIndisponibleException extends RuntimeException {

    public AssistantIndisponibleException(String message) {
        super(message);
    }

    public AssistantIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
