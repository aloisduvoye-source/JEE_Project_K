package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.enums.ModePaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accès aux paiements des contrats (remplace PaiementDao).
 */
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    /** @return les paiements du contrat, par date d'échéance */
    @Query("SELECT p FROM Paiement p WHERE p.contrat.id = :idContrat ORDER BY p.dateEcheance")
    List<Paiement> listerParContrat(Long idContrat);

    /** @return les paiements du client avec leur contrat et son type, du plus récent au plus ancien */
    @Query("SELECT p FROM Paiement p JOIN FETCH p.contrat c JOIN FETCH c.typeAssurance"
            + " WHERE c.client.id = :idClient ORDER BY p.dateEcheance DESC")
    List<Paiement> listerParClient(Long idClient);

    /** @return les paiements en retard avec le contrat et le client, par date d'échéance */
    @Query("SELECT p FROM Paiement p"
            + " JOIN FETCH p.contrat c JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " WHERE p.statut = fr.cytech.assurance.model.enums.StatutPaiement.EN_RETARD ORDER BY p.dateEcheance")
    List<Paiement> listerEnRetard();

    /** @return tous les paiements avec contrat, type et client, du plus récent au plus ancien */
    @Query("SELECT p FROM Paiement p"
            + " JOIN FETCH p.contrat c JOIN FETCH c.typeAssurance"
            + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " ORDER BY p.dateEcheance DESC")
    List<Paiement> listerTousAvecDetails();

    /** Ids des contrats ayant un paiement effectué (utilisé par idsContratsPayes). */
    @Query("SELECT p.contrat.id FROM Paiement p WHERE p.statut = fr.cytech.assurance.model.enums.StatutPaiement.EFFECTUE")
    List<Long> listerIdsContratsPayes();

    /** @return les ids des contrats dont le paiement a été effectué */
    default Set<Long> idsContratsPayes() {
        return new HashSet<>(listerIdsContratsPayes());
    }

    /** Passe EN_RETARD les paiements EN_ATTENTE dont l'échéance est dépassée. @return le nombre de paiements modifiés */
    @Transactional
    @Modifying
    @Query("UPDATE Paiement p SET p.statut = fr.cytech.assurance.model.enums.StatutPaiement.EN_RETARD"
            + " WHERE p.statut = fr.cytech.assurance.model.enums.StatutPaiement.EN_ATTENTE"
            + " AND p.dateEcheance < :aujourdhui")
    int marquerEnRetard(LocalDate aujourdhui);

    /** Enregistre le paiement comme EFFECTUE à cette date et avec ce mode. @return 0 si le paiement n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Paiement p SET p.datePaiement = :datePaiement, p.modePaiement = :mode,"
            + " p.statut = fr.cytech.assurance.model.enums.StatutPaiement.EFFECTUE WHERE p.id = :id")
    int enregistrerPaiement(Long id, LocalDate datePaiement, ModePaiement mode);

    /** Passe le paiement ANNULE. @return 0 si le paiement n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Paiement p SET p.statut = fr.cytech.assurance.model.enums.StatutPaiement.ANNULE WHERE p.id = :id")
    int annuler(Long id);

    /** Annule les paiements EN_ATTENTE ou EN_RETARD du contrat (à la résiliation). @return le nombre de paiements annulés */
    @Transactional
    @Modifying
    @Query("UPDATE Paiement p SET p.statut = fr.cytech.assurance.model.enums.StatutPaiement.ANNULE"
            + " WHERE p.contrat.id = :idContrat AND p.statut IN"
            + " (fr.cytech.assurance.model.enums.StatutPaiement.EN_ATTENTE, fr.cytech.assurance.model.enums.StatutPaiement.EN_RETARD)")
    int annulerNonRegles(Long idContrat);
}
