package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.StatutSinistre;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * @param id identifiant du sinistre
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
     * @param idContrat identifiant du contrat
     * @return les sinistres avec leurs détails, liste vide si le contrat n'en a aucun
     */
    public List<Sinistre> listerParContrat(Long idContrat) {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " WHERE c.id = :idContrat ORDER BY s.dateDeclaration DESC", Sinistre.class)
                .setParameter("idContrat", idContrat)
                .getResultList());
    }

    /**
     * Liste les sinistres de tous les contrats d'un client, du plus récent au plus ancien.
     *
     * @param idClient identifiant du client
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
     * @param idAgent identifiant de l'utilisateur agent
     * @return les sinistres avec leurs détails, liste vide si aucun ne lui est attribué
     */
    public List<Sinistre> listerParAgent(Long idAgent) {
        return enLecture(em -> em.createQuery(
                        AVEC_DETAILS + " WHERE s.agent.id = :idAgent ORDER BY s.dateDeclaration", Sinistre.class)
                .setParameter("idAgent", idAgent)
                .getResultList());
    }

    /**
     * Change le statut d'un sinistre.
     * <p>
     * Les transitions autorisées (par exemple DECLARE vers EN_ANALYSE) sont vérifiées par le service.
     *
     * @param id     identifiant du sinistre
     * @param statut nouveau statut
     * @return {@code false} si aucun sinistre n'a cet id
     */
    public boolean changerStatut(Long id, StatutSinistre statut) {
        return mettreAJour(id, s -> s.setStatut(statut));
    }

    /**
     * Attribue un sinistre à un agent, qui en assure le traitement.
     * <p>
     * Le rôle de l'utilisateur (agent) est vérifié par le service.
     *
     * @param id      identifiant du sinistre
     * @param idAgent identifiant de l'utilisateur agent
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
     * Enregistre l'indemnisation d'un sinistre et le passe au statut INDEMNISE.
     *
     * @param id      identifiant du sinistre
     * @param montant montant versé au client
     * @param date    date du versement
     * @return {@code false} si aucun sinistre n'a cet id
     */
    public boolean indemniser(Long id, BigDecimal montant, LocalDate date) {
        return mettreAJour(id, s -> {
            s.setMontantIndemnisation(montant);
            s.setDateIndemnisation(date);
            s.setStatut(StatutSinistre.INDEMNISE);
        });
    }
}
