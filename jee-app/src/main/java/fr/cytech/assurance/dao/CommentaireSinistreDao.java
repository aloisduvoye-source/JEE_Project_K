package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.CommentaireSinistre;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;

import java.util.List;

/**
 * Accès en base aux commentaires ajoutés par les agents lors de l'analyse d'un sinistre.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class CommentaireSinistreDao extends GenericDao<CommentaireSinistre> {

    public CommentaireSinistreDao() {
        super(CommentaireSinistre.class);
    }

    /**
     * Liste les commentaires d'un sinistre, du plus ancien au plus récent, avec leur auteur déjà chargé.
     *
     * @param idSinistre identifiant du sinistre
     * @return les commentaires du sinistre, liste vide s'il n'en a aucun
     */
    public List<CommentaireSinistre> listerParSinistre(Long idSinistre) {
        return enLecture(em -> em.createQuery(
                        "SELECT cs FROM CommentaireSinistre cs JOIN FETCH cs.auteur"
                                + " WHERE cs.sinistre.id = :idSinistre ORDER BY cs.dateCommentaire, cs.id",
                        CommentaireSinistre.class)
                .setParameter("idSinistre", idSinistre)
                .getResultList());
    }

    /**
     * Ajoute un commentaire à un sinistre, dans une seule transaction.
     *
     * @param idSinistre identifiant du sinistre commenté
     * @param idAuteur   identifiant de l'utilisateur (agent) qui écrit le commentaire
     * @param contenu    texte du commentaire
     * @return le commentaire enregistré
     * @throws IllegalArgumentException si le sinistre ou l'auteur n'existe pas
     */
    public CommentaireSinistre ajouter(Long idSinistre, Long idAuteur, String contenu) {
        return enTransaction(em -> {
            Sinistre sinistre = em.find(Sinistre.class, idSinistre);
            if (sinistre == null) {
                throw new IllegalArgumentException("Sinistre introuvable : " + idSinistre);
            }
            Utilisateur auteur = em.find(Utilisateur.class, idAuteur);
            if (auteur == null) {
                throw new IllegalArgumentException("Utilisateur introuvable : " + idAuteur);
            }
            CommentaireSinistre commentaire = new CommentaireSinistre();
            commentaire.setSinistre(sinistre);
            commentaire.setAuteur(auteur);
            commentaire.setContenu(contenu);
            em.persist(commentaire);
            return commentaire;
        });
    }
}
