package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.enums.StatutSinistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Accès aux sinistres (remplace SinistreDao).
 * Déclarer, faire avancer, affecter un agent et indemniser : dans le service (@Transactional),
 * en modifiant le sinistre chargé avec findById.
 */
public interface SinistreRepository extends JpaRepository<Sinistre, Long> {

    /** Début commun des requêtes qui chargent le contrat, son client, son type et l'agent (lisibles dans une JSP). */
    String AVEC_DETAILS = "SELECT s FROM Sinistre s"
            + " JOIN FETCH s.contrat c JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " JOIN FETCH c.typeAssurance"
            + " LEFT JOIN FETCH s.agent";

    /** @return le sinistre avec ses détails, ou null */
    @Query(AVEC_DETAILS + " WHERE s.id = :id")
    Sinistre trouverAvecDetails(Long id);

    /** @return tous les sinistres avec leurs détails, du plus récent au plus ancien */
    @Query(AVEC_DETAILS + " ORDER BY s.dateDeclaration DESC")
    List<Sinistre> listerTousAvecDetails();

    /** @return les sinistres du contrat, du plus récent au plus ancien */
    @Query(AVEC_DETAILS + " WHERE c.id = :idContrat ORDER BY s.dateDeclaration DESC")
    List<Sinistre> listerParContrat(Long idContrat);

    /** @return les sinistres du client, du plus récent au plus ancien */
    @Query(AVEC_DETAILS + " WHERE cl.id = :idClient ORDER BY s.dateDeclaration DESC")
    List<Sinistre> listerParClient(Long idClient);

    /** @return les sinistres de ce statut, du plus ancien au plus récent */
    @Query(AVEC_DETAILS + " WHERE s.statut = :statut ORDER BY s.dateDeclaration")
    List<Sinistre> listerParStatut(StatutSinistre statut);

    /** @return les sinistres suivis par cet agent, du plus ancien au plus récent */
    @Query(AVEC_DETAILS + " WHERE s.agent.id = :idAgent ORDER BY s.dateDeclaration")
    List<Sinistre> listerParAgent(Long idAgent);
}
