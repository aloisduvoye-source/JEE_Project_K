package fr.cytech.assurance.ai;

/**
 * Un message de la conversation tel qu'envoyé à l'API d'IA.
 * Indépendant de la base (MessageAssistant) : le client IA ne connaît ni JPA ni les entités.
 *
 * @param depuisClient true si le message a été écrit par le client, false s'il vient de l'assistant
 * @param texte        contenu du message
 */
public record MessageIA(boolean depuisClient, String texte) {

    public static MessageIA client(String texte) {
        return new MessageIA(true, texte);
    }

    public static MessageIA assistant(String texte) {
        return new MessageIA(false, texte);
    }
}
