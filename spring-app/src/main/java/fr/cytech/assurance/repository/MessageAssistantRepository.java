package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.MessageAssistant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accès à l'historique de l'assistant IA (remplace MessageAssistantDao).
 * Ajouter un message : le service le remplit (utilisateur, auteur, contenu) puis appelle save.
 */
public interface MessageAssistantRepository extends JpaRepository<MessageAssistant, Long> {

    /** @return toute la conversation de l'utilisateur, du plus ancien au plus récent */
    @Query("SELECT m FROM MessageAssistant m WHERE m.utilisateur.id = :idUtilisateur ORDER BY m.dateMessage, m.id")
    List<MessageAssistant> listerParUtilisateur(Long idUtilisateur);

    /** Messages du plus récent au plus ancien, limités par page (utilisé par listerDerniers). */
    @Query("SELECT m FROM MessageAssistant m WHERE m.utilisateur.id = :idUtilisateur"
            + " ORDER BY m.dateMessage DESC, m.id DESC")
    List<MessageAssistant> listerPlusRecents(Long idUtilisateur, Pageable page);

    /** @return les {@code nombre} derniers messages, remis dans l'ordre de la conversation */
    default List<MessageAssistant> listerDerniers(Long idUtilisateur, int nombre) {
        List<MessageAssistant> derniers = new ArrayList<>(listerPlusRecents(idUtilisateur, PageRequest.of(0, nombre)));
        Collections.reverse(derniers);
        return derniers;
    }

    /** Efface la conversation de l'utilisateur. @return le nombre de messages supprimés */
    @Transactional
    @Modifying
    @Query("DELETE FROM MessageAssistant m WHERE m.utilisateur.id = :idUtilisateur")
    int supprimerHistorique(Long idUtilisateur);
}
