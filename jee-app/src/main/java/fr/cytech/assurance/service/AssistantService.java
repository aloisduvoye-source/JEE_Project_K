package fr.cytech.assurance.service;

import fr.cytech.assurance.ai.ClientGemini;
import fr.cytech.assurance.ai.ContexteAssistant;
import fr.cytech.assurance.ai.MessageIA;
import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.ContratDao;
import fr.cytech.assurance.dao.GarantieDao;
import fr.cytech.assurance.dao.MessageAssistantDao;
import fr.cytech.assurance.dao.PaiementDao;
import fr.cytech.assurance.dao.SinistreDao;
import fr.cytech.assurance.exception.AssistantIndisponibleException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.MessageAssistant;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.AuteurMessage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Assistant IA des clients : répond à une question à partir des données du client connecté,
 * et conserve la conversation en base.
 * <p>
 * Le modèle n'a pas de mémoire : à chaque question, on lui renvoie les instructions (avec les données
 * du client, relues en base pour être à jour) et les derniers messages de la conversation.
 */
public class AssistantService {

    // Longueur maximale d'une question (évite d'envoyer un texte énorme à l'API)
    public static final int LONGUEUR_MAX_QUESTION = 1000;

    // Nombre de messages précédents renvoyés au modèle pour qu'il suive la conversation
    // (pair : chaque échange enregistré est une question suivie de sa réponse)
    private static final int MESSAGES_DE_CONTEXTE = 10;

    private final ClientGemini clientIA = ClientGemini.getInstance();
    private final MessageAssistantDao messageDao = new MessageAssistantDao();
    private final ClientDao clientDao = new ClientDao();
    private final ContratDao contratDao = new ContratDao();
    private final PaiementDao paiementDao = new PaiementDao();
    private final SinistreDao sinistreDao = new SinistreDao();
    private final GarantieDao garantieDao = new GarantieDao();

    // Toute la conversation de l'utilisateur, du plus ancien message au plus récent
    public List<MessageAssistant> historique(Long idUtilisateur) {
        return messageDao.listerParUtilisateur(idUtilisateur);
    }

    /**
     * Pose une question à l'assistant et enregistre la question et sa réponse.
     * <p>
     * Rien n'est enregistré si l'assistant ne répond pas : le client peut reposer sa question
     * sans qu'elle apparaisse deux fois dans l'historique.
     *
     * @param utilisateur client connecté
     * @param question    question déjà nettoyée et validée (non vide, au plus LONGUEUR_MAX_QUESTION caractères)
     * @return la réponse enregistrée
     * @throws AssistantIndisponibleException si l'API d'IA ne répond pas
     */
    public MessageAssistant poserQuestion(Utilisateur utilisateur, String question) {
        String instructions = construireInstructions(utilisateur);

        List<MessageIA> conversation = new ArrayList<>();
        for (MessageAssistant message : messageDao.listerDerniers(utilisateur.getId(), MESSAGES_DE_CONTEXTE)) {
            // La conversation envoyée doit commencer par un message du client
            if (conversation.isEmpty() && message.getAuteur() != AuteurMessage.CLIENT) {
                continue;
            }
            conversation.add(new MessageIA(message.getAuteur() == AuteurMessage.CLIENT, message.getContenu()));
        }
        conversation.add(MessageIA.client(question));

        String reponse = clientIA.generer(instructions, conversation);

        messageDao.ajouter(utilisateur.getId(), AuteurMessage.CLIENT, question);
        return messageDao.ajouter(utilisateur.getId(), AuteurMessage.ASSISTANT, reponse);
    }

    // Efface toute la conversation : l'assistant repart de zéro
    public void effacerHistorique(Long idUtilisateur) {
        messageDao.supprimerHistorique(idUtilisateur);
    }

    private String construireInstructions(Utilisateur utilisateur) {
        List<Garantie> catalogue = garantieDao.listerParNom();
        Client client = clientDao.trouverParUtilisateur(utilisateur.getId());
        if (client == null) {
            // Compte CLIENT sans fiche client : l'assistant répond sans données personnelles
            return ContexteAssistant.instructions(utilisateur, List.of(), List.of(), List.of(), catalogue,
                    LocalDate.now());
        }
        return ContexteAssistant.instructions(utilisateur,
                contratDao.listerParClient(client.getId()),
                paiementDao.listerParClient(client.getId()),
                sinistreDao.listerParClient(client.getId()),
                catalogue,
                LocalDate.now());
    }
}
