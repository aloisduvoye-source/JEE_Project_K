package fr.cytech.assurance.service;

import fr.cytech.assurance.ai.ClientGemini;
import fr.cytech.assurance.ai.ContexteAssistant;
import fr.cytech.assurance.ai.LimiteurQuestions;
import fr.cytech.assurance.ai.MessageIA;
import fr.cytech.assurance.repository.ClientRepository;
import fr.cytech.assurance.repository.ContratRepository;
import fr.cytech.assurance.repository.GarantieRepository;
import fr.cytech.assurance.repository.MessageAssistantRepository;
import fr.cytech.assurance.repository.PaiementRepository;
import fr.cytech.assurance.repository.SinistreRepository;
import fr.cytech.assurance.repository.VersementRepository;
import fr.cytech.assurance.exception.AssistantIndisponibleException;
import fr.cytech.assurance.exception.TropDeQuestionsException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.MessageAssistant;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.AuteurMessage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Assistant IA des clients : répond à partir des données du client connecté et conserve la conversation en base.
 * Le modèle n'a pas de mémoire : chaque question lui renvoie les données du client et les derniers messages.
 */
@Service
public class AssistantService {

    /** Longueur maximale d'une question envoyée à l'API. */
    public static final int LONGUEUR_MAX_QUESTION = 1000;

    /** Nombre de messages précédents renvoyés au modèle (pair : une question puis sa réponse). */
    private static final int MESSAGES_DE_CONTEXTE = 10;

    private final ClientGemini clientIA;
    private final LimiteurQuestions limiteur;
    private final MessageAssistantRepository messageRepository;
    private final ClientRepository clientRepository;
    private final ContratRepository contratRepository;
    private final PaiementRepository paiementRepository;
    private final SinistreRepository sinistreRepository;
    private final VersementRepository versementRepository;
    private final GarantieRepository garantieRepository;

    /** Dépendances injectées par Spring. */
    public AssistantService(ClientGemini clientIA, LimiteurQuestions limiteur,
                            MessageAssistantRepository messageRepository, ClientRepository clientRepository,
                            ContratRepository contratRepository, PaiementRepository paiementRepository,
                            SinistreRepository sinistreRepository, VersementRepository versementRepository,
                            GarantieRepository garantieRepository) {
        this.clientIA = clientIA;
        this.limiteur = limiteur;
        this.messageRepository = messageRepository;
        this.clientRepository = clientRepository;
        this.contratRepository = contratRepository;
        this.paiementRepository = paiementRepository;
        this.sinistreRepository = sinistreRepository;
        this.versementRepository = versementRepository;
        this.garantieRepository = garantieRepository;
    }

    /**
     * @return toute la conversation de l'utilisateur, du plus ancien message au plus récent
     */
    public List<MessageAssistant> historique(Long idUtilisateur) {
        return messageRepository.listerParUtilisateur(idUtilisateur);
    }

    /**
     * Pose une question à l'assistant ; la question et la réponse ne sont enregistrées que si l'assistant répond.
     *
     * @param utilisateur client connecté
     * @param question    question déjà nettoyée et validée
     * @return la réponse enregistrée
     * @throws TropDeQuestionsException       si le client a posé trop de questions
     * @throws AssistantIndisponibleException si l'API d'IA ne répond pas
     */
    public MessageAssistant poserQuestion(Utilisateur utilisateur, String question) {
        // Avant l'appel à l'API : une question refusée ne consomme pas le quota commun
        limiteur.enregistrer(utilisateur.getId());
        String instructions = construireInstructions(utilisateur);

        List<MessageIA> conversation = new ArrayList<>();
        for (MessageAssistant message : messageRepository.listerDerniers(utilisateur.getId(), MESSAGES_DE_CONTEXTE)) {
            // La conversation envoyée doit commencer par un message du client
            if (conversation.isEmpty() && message.getAuteur() != AuteurMessage.CLIENT) {
                continue;
            }
            conversation.add(new MessageIA(message.getAuteur() == AuteurMessage.CLIENT, message.getContenu()));
        }
        conversation.add(MessageIA.client(question));

        String reponse = clientIA.generer(instructions, conversation);

        ajouterMessage(utilisateur, AuteurMessage.CLIENT, question);
        return ajouterMessage(utilisateur, AuteurMessage.ASSISTANT, reponse);
    }

    /** Efface toute la conversation : l'assistant repart de zéro. */
    public void effacerHistorique(Long idUtilisateur) {
        messageRepository.supprimerHistorique(idUtilisateur);
    }

    /**
     * @return les instructions du modèle, avec les données du client relues en base
     */
    private String construireInstructions(Utilisateur utilisateur) {
        List<Garantie> catalogue = garantieRepository.listerParNom();
        Client client = clientRepository.trouverParUtilisateur(utilisateur.getId());
        if (client == null) {
            return ContexteAssistant.instructions(utilisateur, List.of(), List.of(), List.of(), List.of(), catalogue,
                    LocalDate.now());
        }
        return ContexteAssistant.instructions(utilisateur,
                contratRepository.listerParClient(client.getId()),
                paiementRepository.listerParClient(client.getId()),
                sinistreRepository.listerParClient(client.getId()),
                versementRepository.listerParClient(client.getId()),
                catalogue,
                LocalDate.now());
    }

    /** Enregistre un message de la conversation (la date est remplie par Hibernate). */
    private MessageAssistant ajouterMessage(Utilisateur utilisateur, AuteurMessage auteur, String contenu) {
        MessageAssistant message = new MessageAssistant();
        message.setUtilisateur(utilisateur);
        message.setAuteur(auteur);
        message.setContenu(contenu);
        return messageRepository.save(message);
    }
}
