package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.CommentaireSinistre;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.StatutSinistre;
import fr.cytech.assurance.model.enums.TypeVersement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Accès en base aux sinistres déclarés sur les contrats.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 * Les commentaires et pièces justificatives d'un sinistre ont leurs propres DAO.
 */
public class SinistreDao extends GenericDao<Sinistre> {

    // Début commun des requêtes qui chargent le contrat, son client et son type (lisibles dans une JSP)
    private static final String AVEC_DETAILS = "SELECT s FROM Sinistre s"
            + " JOIN FETCH s.contrat c JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " JOIN FETCH c.typeAssurance"
            + " LEFT JOIN FETCH s.agent";

    public SinistreDao() {
        super(Sinistre.class);
    }

    /**
     * Recherche un sinistre avec son contrat, le client, le type d'assurance et l'agent déjà chargés.
     *
     * @param id id du sinistre
     * @return le sinistre avec ses détails, ou {@code null} s'il n'existe pas
     */
    public Sinistre trouverAvecDetails(Long id) {
        return enLecture(em -> em.createQuery(AVEC_DETAILS + " WHERE s.id = :id", Sinistre.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Liste les sinistres d'un contrat, du plus récent au plus ancien.
     *
     * @param idContrat id du contrat
     * @return les sinistres avec leurs détails, liste vide si le contrat n'en a aucun
     */
    public List<Sinistre> listerParContrat(Long idContrat) {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " WHERE c.id = :idContrat ORDER BY s.dateDeclaration DESC", Sinistre.class)
                .setParameter("idContrat", idContrat)
                .getResultList());
    }

    /**
     * Liste tous les sinistres, du plus récent au plus ancien, avec leurs détails.
     *
     * @return les sinistres, liste vide s'il n'y en a aucun
     */
    public List<Sinistre> listerTousAvecDetails() {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " ORDER BY s.dateDeclaration DESC", Sinistre.class)
                .getResultList());
    }

    /**
     * Enregistre la déclaration d'un sinistre sur un contrat (statut DECLARE).
     * <p>
     * Les règles (contrat du client, payé, date couverte) sont vérifiées par le service.
     *
     * @param sinistre  sinistre à enregistrer (date, type, description, montant estimé)
     * @param idContrat id du contrat concerné
     * @return le sinistre enregistré, avec son numéro
     * @throws IllegalArgumentException si le contrat n'existe pas
     */
    public Sinistre declarer(Sinistre sinistre, Long idContrat) {
        return enTransaction(em -> {
            Contrat contrat = em.find(Contrat.class, idContrat);
            if (contrat == null) {
                throw new IllegalArgumentException("Contrat introuvable : " + idContrat);
            }
            sinistre.setContrat(contrat);
            sinistre.setStatut(StatutSinistre.DECLARE);
            em.persist(sinistre);
            return sinistre;
        });
    }

    /**
     * Liste les sinistres de tous les contrats d'un client, du plus récent au plus ancien.
     *
     * @param idClient id du client
     * @return les sinistres avec leurs détails, liste vide si le client n'en a aucun
     */
    public List<Sinistre> listerParClient(Long idClient) {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " WHERE cl.id = :idClient ORDER BY s.dateDeclaration DESC", Sinistre.class)
                .setParameter("idClient", idClient)
                .getResultList());
    }

    /**
     * Liste les sinistres ayant ce statut, du plus ancien au plus récent (ordre de traitement).
     *
     * @param statut statut recherché
     * @return les sinistres avec leurs détails, liste vide si aucun n'a ce statut
     */
    public List<Sinistre> listerParStatut(StatutSinistre statut) {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " WHERE s.statut = :statut ORDER BY s.dateDeclaration", Sinistre.class)
                .setParameter("statut", statut)
                .getResultList());
    }

    /**
     * Liste les sinistres attribués à un agent, du plus ancien au plus récent (ordre de traitement).
     *
     * @param idAgent id de l'utilisateur agent
     * @return les sinistres avec leurs détails, liste vide si aucun ne lui est attribué
     */
    public List<Sinistre> listerParAgent(Long idAgent) {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " WHERE s.agent.id = :idAgent ORDER BY s.dateDeclaration", Sinistre.class)
                .setParameter("idAgent", idAgent)
                .getResultList());
    }

    /**
     * Change le statut d'un sinistre et date l'étape atteinte :
     * prise en charge (EN_ANALYSE) ou décision (ACCEPTE, REFUSE).
     * <p>
     * Les transitions autorisées (par exemple DECLARE vers EN_ANALYSE) sont vérifiées par le service.
     * Pour passer à INDEMNISE, utiliser {@link #indemniser}, qui enregistre aussi le versement.
     *
     * @param id     id du sinistre
     * @param statut nouveau statut
     * @return {@code false} si aucun sinistre n'a cet id
     */
    public boolean changerStatut(Long id, StatutSinistre statut) {
        return mettreAJour(id, s -> {
            s.setStatut(statut);
            dater(s, LocalDateTime.now());
        });
    }

    // Une étape déjà datée garde sa date. Une décision sans prise en charge explicite date aussi celle-ci
    // (exigé par la base : un sinistre décidé a forcément été pris en charge).
    private static void dater(Sinistre s, LocalDateTime maintenant) {
        if (s.getStatut() != StatutSinistre.DECLARE && s.getDatePriseEnCharge() == null) {
            s.setDatePriseEnCharge(maintenant);
        }
        if (s.getStatut() != StatutSinistre.DECLARE && s.getStatut() != StatutSinistre.EN_ANALYSE
                && s.getDateDecision() == null) {
            s.setDateDecision(maintenant);
        }
    }

    /**
     * Attribue un sinistre à un agent, qui en assure le traitement.
     * <p>
     * Le rôle de l'utilisateur (agent) est vérifié par le service.
     *
     * @param id      id du sinistre
     * @param idAgent id de l'utilisateur agent
     * @return {@code false} si le sinistre ou l'utilisateur n'existe pas
     */
    public boolean affecterAgent(Long id, Long idAgent) {
        return enTransaction(em -> {
            Sinistre sinistre = em.find(Sinistre.class, id);
            Utilisateur agent = em.find(Utilisateur.class, idAgent);
            if (sinistre == null || agent == null) {return false;}
            sinistre.setAgent(agent);
            return true;
        });
    }

    /**
     * Fait avancer un sinistre d'un statut à un autre, dans une seule transaction :
     * l'agent est attribué au dossier s'il ne l'était pas, l'étape est datée,
     * et un commentaire de l'agent (motif d'un refus...) peut être ajouté à la discussion.
     * <p>
     * Le statut est relu dans la transaction : si un autre agent a déjà fait avancer le dossier,
     * rien n'est modifié.
     *
     * @param id          id du sinistre
     * @param attendu     statut que le sinistre doit avoir
     * @param nouveau     statut à lui donner
     * @param idAgent     agent qui fait l'action
     * @param commentaire texte ajouté à la discussion, ou {@code null}
     * @return {@code false} si le sinistre ou l'agent n'existe pas, ou si le sinistre n'a plus le statut attendu
     */
    public boolean faireAvancer(Long id, StatutSinistre attendu, StatutSinistre nouveau,
                                Long idAgent, String commentaire) {
        return enTransaction(em -> {
            Sinistre sinistre = em.find(Sinistre.class, id);
            Utilisateur agent = em.find(Utilisateur.class, idAgent);
            if (sinistre == null || agent == null || sinistre.getStatut() != attendu) {return false;}
            sinistre.setStatut(nouveau);
            if (sinistre.getAgent() == null) {
                sinistre.setAgent(agent);
            }
            dater(sinistre, LocalDateTime.now());
            if (commentaire != null) {
                CommentaireSinistre message = new CommentaireSinistre();
                message.setSinistre(sinistre);
                message.setAuteur(agent);
                message.setContenu(commentaire);
                em.persist(message);
            }
            return true;
        });
    }

    /**
     * Indemnise un sinistre : enregistre le versement et passe le sinistre au statut INDEMNISE, dans une seule transaction.
     * <p>
     * Le montant est calculé par le service (dommage, franchise, plafond). Le statut ACCEPTE est relu
     * dans la transaction : un sinistre ne peut pas être indemnisé deux fois.
     *
     * @param id         id du sinistre
     * @param versement  versement à enregistrer : montant, dommage, franchise, plafond et date déjà remplis
     * @param idGarantie garantie appliquée
     * @param idAgent    agent qui enregistre l'indemnisation
     * @return le versement enregistré, ou {@code null} si le sinistre, la garantie ou l'agent n'existe pas,
     *         ou si le sinistre n'est pas (ou plus) au statut ACCEPTE
     */
    public Versement indemniser(Long id, Versement versement, Long idGarantie, Long idAgent) {
        return enTransaction(em -> {
            Sinistre sinistre = em.find(Sinistre.class, id);
            Garantie garantie = em.find(Garantie.class, idGarantie);
            Utilisateur agent = em.find(Utilisateur.class, idAgent);
            if (sinistre == null || garantie == null || agent == null
                    || sinistre.getStatut() != StatutSinistre.ACCEPTE) {
                return null;
            }
            sinistre.setStatut(StatutSinistre.INDEMNISE);
            dater(sinistre, LocalDateTime.now());

            versement.setType(TypeVersement.INDEMNISATION);
            versement.setContrat(sinistre.getContrat());
            versement.setSinistre(sinistre);
            versement.setGarantie(garantie);
            versement.setAgent(agent);
            em.persist(versement);
            return versement;
        });
    }
}
