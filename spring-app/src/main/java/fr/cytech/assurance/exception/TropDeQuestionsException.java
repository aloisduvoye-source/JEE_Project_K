package fr.cytech.assurance.exception;

// Levée quand un client a posé trop de questions à l'assistant en peu de temps (par minute ou par jour)
// Le message est destiné à être affiché à l'utilisateur
public class TropDeQuestionsException extends RuntimeException {

    public TropDeQuestionsException(String message) {
        super(message);
    }
}
