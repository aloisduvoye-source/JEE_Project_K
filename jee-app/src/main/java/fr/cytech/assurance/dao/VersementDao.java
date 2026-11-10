package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Versement;

import java.util.List;

/**
 * Accès en base aux versements de l'assurance vers ses clients (indemnisations et remboursements).
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 * Une indemnisation est créée par {@link SinistreDao#indemniser}, en même temps que le changement de statut.
 */
public class VersementDao extends GenericDao<Versement> {

    public VersementDao() {
        super(Versement.class);
    }

    /**
     * Liste les versements reçus par un client, du plus récent au plus ancien.
     * <p>
     * Le contrat, son type et le sinistre éventuel sont déjà chargés (lisibles dans une JSP).
     *
     * @param idClient id du client
     * @return les versements du client, liste vide s'il n'en a reçu aucun
     */
    public List<Versement> listerParClient(Long idClient) {
        return enLecture(em -> em.createQuery(
                        "SELECT v FROM Versement v JOIN FETCH v.contrat c JOIN FETCH c.typeAssurance"
                                + " LEFT JOIN FETCH v.sinistre"
                                + " WHERE c.client.id = :idClient ORDER BY v.dateVersement DESC, v.id DESC",
                        Versement.class)
                .setParameter("idClient", idClient)
                .getResultList());
    }

    /**
     * Recherche l'indemnisation d'un sinistre.
     *
     * @param idSinistre id du sinistre
     * @return l'indemnisation, ou {@code null} si le sinistre n'a pas encore été indemnisé
     */
    public Versement trouverParSinistre(Long idSinistre) {
        return enLecture(em -> em.createQuery(
                        "SELECT v FROM Versement v WHERE v.sinistre.id = :idSinistre", Versement.class)
                .setParameter("idSinistre", idSinistre)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }
}
