package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.CommentaireSinistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Accès aux messages échangés sur un sinistre (remplace CommentaireSinistreDao).
 * Ajouter un message : le service le remplit (sinistre, auteur, contenu) puis appelle save.
 */
public interface CommentaireSinistreRepository extends JpaRepository<CommentaireSinistre, Long> {

    /** @return les messages du sinistre avec leur auteur, du plus ancien au plus récent */
    @Query("SELECT cs FROM CommentaireSinistre cs JOIN FETCH cs.auteur"
            + " WHERE cs.sinistre.id = :idSinistre ORDER BY cs.dateCommentaire, cs.id")
    List<CommentaireSinistre> listerParSinistre(Long idSinistre);
}
