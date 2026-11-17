package fr.cytech.assurance.ai;

/**
 * Un message de la conversation envoyé à l'API d'IA, indépendant des entités JPA.
 *
 * @param depuisClient vrai si le message vient du client, faux s'il vient de l'assistant
 * @param texte        contenu du message
 */
public record MessageIA(boolean depuisClient, String texte) {

    /** Message écrit par le client. */
    public static MessageIA client(String texte) {
        return new MessageIA(true, texte);
    }

    /** Message écrit par l'assistant. */
    public static MessageIA assistant(String texte) {
        return new MessageIA(false, texte);
    }
}
