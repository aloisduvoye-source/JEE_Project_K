package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.PieceJustificative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Accès aux pièces justificatives des sinistres (remplace PieceJustificativeDao).
 * Ajouter une pièce : le service lui affecte son sinistre puis appelle save.
 */
public interface PieceJustificativeRepository extends JpaRepository<PieceJustificative, Long> {

    /** @return les pièces du sinistre, de la plus ancienne à la plus récente */
    @Query("SELECT p FROM PieceJustificative p WHERE p.sinistre.id = :idSinistre ORDER BY p.dateAjout, p.id")
    List<PieceJustificative> listerParSinistre(Long idSinistre);

    /** @return la pièce avec son sinistre, son contrat et son client (pour vérifier le propriétaire), ou null */
    @Query("SELECT p FROM PieceJustificative p"
            + " JOIN FETCH p.sinistre s JOIN FETCH s.contrat c JOIN FETCH c.client"
            + " WHERE p.id = :id")
    PieceJustificative trouverAvecProprietaire(Long id);
}
