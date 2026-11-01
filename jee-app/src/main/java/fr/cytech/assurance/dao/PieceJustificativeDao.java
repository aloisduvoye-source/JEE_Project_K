package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.PieceJustificative;
import fr.cytech.assurance.model.Sinistre;

import java.util.List;

/**
 * Accès en base aux pièces justificatives jointes aux sinistres.
 * <p>
 * Seul le chemin du fichier est en base : l'enregistrement et la suppression du fichier sur le disque
 * sont à la charge du service.
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class PieceJustificativeDao extends GenericDao<PieceJustificative> {

    public PieceJustificativeDao() {
        super(PieceJustificative.class);
    }

    /**
     * Liste les pièces jointes à un sinistre, de la plus ancienne à la plus récente.
     *
     * @param idSinistre identifiant du sinistre
     * @return les pièces du sinistre, liste vide s'il n'en a aucune
     */
    public List<PieceJustificative> listerParSinistre(Long idSinistre) {
        return enLecture(em -> em.createQuery(
                        "SELECT p FROM PieceJustificative p WHERE p.sinistre.id = :idSinistre ORDER BY p.dateAjout, p.id",
                        PieceJustificative.class)
                .setParameter("idSinistre", idSinistre)
                .getResultList());
    }

    /**
     * Recherche une pièce avec son sinistre, le contrat et le client déjà chargés.
     * <p>
     * Permet de vérifier, avant un téléchargement, que la pièce appartient bien au client connecté.
     *
     * @param id identifiant de la pièce
     * @return la pièce avec son propriétaire, ou {@code null} si elle n'existe pas
     */
    public PieceJustificative trouverAvecProprietaire(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT p FROM PieceJustificative p"
                                + " JOIN FETCH p.sinistre s JOIN FETCH s.contrat c JOIN FETCH c.client"
                                + " WHERE p.id = :id", PieceJustificative.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Joint une pièce à un sinistre, dans une seule transaction.
     *
     * @param idSinistre identifiant du sinistre
     * @param piece      pièce à enregistrer (nom, chemin et type du fichier déjà remplis)
     * @return la pièce enregistrée
     * @throws IllegalArgumentException si le sinistre n'existe pas
     */
    public PieceJustificative ajouter(Long idSinistre, PieceJustificative piece) {
        return enTransaction(em -> {
            Sinistre sinistre = em.find(Sinistre.class, idSinistre);
            if (sinistre == null) {
                throw new IllegalArgumentException("Sinistre introuvable : " + idSinistre);
            }
            piece.setSinistre(sinistre);
            em.persist(piece);
            return piece;
        });
    }
}
