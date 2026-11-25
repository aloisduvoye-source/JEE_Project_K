package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.TypeVersement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Accès aux versements de la compagnie : indemnisations et remboursements (remplace VersementDao).
 */
public interface VersementRepository extends JpaRepository<Versement, Long> {

    /** @return les versements reçus par le client, avec contrat, type, sinistre et garantie, du plus récent au plus ancien */
    @Query("SELECT v FROM Versement v JOIN FETCH v.contrat c JOIN FETCH c.typeAssurance"
            + " LEFT JOIN FETCH v.sinistre LEFT JOIN FETCH v.garantie"
            + " WHERE c.client.id = :idClient ORDER BY v.dateVersement DESC, v.id DESC")
    List<Versement> listerParClient(Long idClient);

    /** Premier versement de ce type pour le contrat (utilisé par trouverRemboursement). */
    Versement findFirstByContratIdAndType(Long idContrat, TypeVersement type);

    /** @return le remboursement de prime versé à la résiliation du contrat, ou null */
    default Versement trouverRemboursement(Long idContrat) {
        return findFirstByContratIdAndType(idContrat, TypeVersement.REMBOURSEMENT);
    }

    /** @return l'indemnisation du sinistre avec sa garantie, ou null (une seule par sinistre) */
    @Query("SELECT v FROM Versement v LEFT JOIN FETCH v.garantie WHERE v.sinistre.id = :idSinistre")
    Versement trouverParSinistre(Long idSinistre);
}
