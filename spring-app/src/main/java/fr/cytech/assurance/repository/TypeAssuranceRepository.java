package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.TypeAssurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accès aux types d'assurance (remplace TypeAssuranceDao).
 * Ajouter ou retirer une garantie proposée : le service modifie type.getGaranties() dans une transaction.
 */
public interface TypeAssuranceRepository extends JpaRepository<TypeAssurance, Long> {

    /** @return le type de ce nom, ou null */
    @Query("SELECT t FROM TypeAssurance t WHERE t.nom = :nom")
    TypeAssurance trouverParNom(String nom);

    /** @return les types non archivés, triés par nom */
    @Query("SELECT t FROM TypeAssurance t WHERE t.actif = true ORDER BY t.nom")
    List<TypeAssurance> listerActifs();

    /** @return tous les types avec leurs garanties proposées, triés par nom */
    @Query("SELECT DISTINCT t FROM TypeAssurance t LEFT JOIN FETCH t.garanties ORDER BY t.nom")
    List<TypeAssurance> listerAvecGaranties();

    /** @return le type avec ses garanties proposées, ou null */
    @Query("SELECT DISTINCT t FROM TypeAssurance t LEFT JOIN FETCH t.garanties WHERE t.id = :id")
    TypeAssurance trouverAvecGaranties(Long id);

    /** @return true si un autre type que idExclu (null à la création) porte déjà ce nom */
    default boolean existeAvecNom(String nom, Long idExclu) {
        TypeAssurance type = trouverParNom(nom);
        return type != null && !type.getId().equals(idExclu);
    }

    /** Nombre de contrats de ce type (utilisé par estUtiliseParContrat). */
    @Query("SELECT COUNT(c) FROM Contrat c WHERE c.typeAssurance.id = :id")
    long compterContrats(Long id);

    /** @return true si au moins un contrat est de ce type */
    default boolean estUtiliseParContrat(Long id) {
        return compterContrats(id) > 0;
    }

    /** Ids des types ayant au moins un contrat (utilisé par idsUtilisesParContrat). */
    @Query("SELECT DISTINCT c.typeAssurance.id FROM Contrat c")
    List<Long> listerIdsUtilisesParContrat();

    /** @return les ids des types qui ont au moins un contrat */
    default Set<Long> idsUtilisesParContrat() {
        return new HashSet<>(listerIdsUtilisesParContrat());
    }

    /** Modifie nom, description et prix de base. @return 0 si le type n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE TypeAssurance t SET t.nom = :nom, t.description = :description, t.prixBase = :prixBase WHERE t.id = :id")
    int modifierInfos(Long id, String nom, String description, BigDecimal prixBase);

    /** Archive (false) ou réactive (true) le type. @return 0 si le type n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE TypeAssurance t SET t.actif = :actif WHERE t.id = :id")
    int changerEtat(Long id, boolean actif);
}
