package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.MessageAssistant;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.AuteurMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accès en base à l'historique des conversations entre les clients et l'assistant IA.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class MessageAssistantDao extends GenericDao<MessageAssistant> {

    public MessageAssistantDao() {
        super(MessageAssistant.class);
    }

    /**
     * Liste toute la conversation d'un utilisateur, du plus ancien message au plus récent.
     *
     * @param idUtilisateur id de l'utilisateur
     * @return les messages, liste vide si l'utilisateur n'a jamais écrit à l'assistant
     */
    public List<MessageAssistant> listerParUtilisateur(Long idUtilisateur) {
        return enLecture(em -> em.createQuery(
                        "SELECT m FROM MessageAssistant m WHERE m.utilisateur.id = :idUtilisateur"
                                + " ORDER BY m.dateMessage, m.id", MessageAssistant.class)
                .setParameter("idUtilisateur", idUtilisateur)
                .getResultList());
    }

    /**
     * Liste les derniers messages d'un utilisateur, du plus ancien au plus récent.
     * <p>
     * Sert à donner le contexte récent de la conversation à l'assistant sans renvoyer tout l'historique.
     *
     * @param idUtilisateur id de l'utilisateur
     * @param nombre        nombre maximal de messages à renvoyer
     * @return au plus {@code nombre} messages, liste vide s'il n'y en a aucun
     */
    public List<MessageAssistant> listerDerniers(Long idUtilisateur, int nombre) {
        List<MessageAssistant> derniers = new ArrayList<>(enLecture(em -> em.createQuery(
                        "SELECT m FROM MessageAssistant m WHERE m.utilisateur.id = :idUtilisateur"
                                + " ORDER BY m.dateMessage DESC, m.id DESC", MessageAssistant.class)
                .setParameter("idUtilisateur", idUtilisateur)
                .setMaxResults(nombre)
                .getResultList()));
        // La requête prend les plus récents en premier : on remet la conversation dans l'ordre
        Collections.reverse(derniers);
        return derniers;
    }

    /**
     * Ajoute un message à la conversation d'un utilisateur, dans une seule transaction.
     *
     * @param idUtilisateur id de l'utilisateur
     * @param auteur        qui a écrit le message : le client ou l'assistant
     * @param contenu       texte du message
     * @return le message enregistré
     * @throws IllegalArgumentException si l'utilisateur n'existe pas
     */
    public MessageAssistant ajouter(Long idUtilisateur, AuteurMessage auteur, String contenu) {
        return enTransaction(em -> {
            Utilisateur utilisateur = em.find(Utilisateur.class, idUtilisateur);
            if (utilisateur == null) {
                throw new IllegalArgumentException("Utilisateur introuvable : " + idUtilisateur);
            }
            MessageAssistant message = new MessageAssistant();
            message.setUtilisateur(utilisateur);
            message.setAuteur(auteur);
            message.setContenu(contenu);
            em.persist(message);
            return message;
        });
    }

    /**
     * Supprime toute la conversation d'un utilisateur.
     *
     * @param idUtilisateur id de l'utilisateur
     * @return le nombre de messages supprimés
     */
    public int supprimerHistorique(Long idUtilisateur) {
        return enTransaction(em -> em.createQuery(
                        "DELETE FROM MessageAssistant m WHERE m.utilisateur.id = :idUtilisateur")
                .setParameter("idUtilisateur", idUtilisateur)
                .executeUpdate());
    }
}
