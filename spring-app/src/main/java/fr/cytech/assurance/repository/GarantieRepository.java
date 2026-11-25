package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Garantie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Accès aux garanties (remplace GarantieDao).
 * findById, findAll, findAllById, save et deleteById sont fournis par JpaRepository.
 */
public interface GarantieRepository extends JpaRepository<Garantie, Long> {

    /** @return la garantie de ce nom, ou null */
    @Query("SELECT g FROM Garantie g WHERE g.nom = :nom")
    Garantie trouverParNom(String nom);

    /** @return les garanties triées par nom */
    @Query("SELECT g FROM Garantie g ORDER BY g.nom")
    List<Garantie> listerParNom();

    /** @return true si une autre garantie que idExclu (null à la création) porte déjà ce nom */
    default boolean existeAvecNom(String nom, Long idExclu) {
        Garantie garantie = trouverParNom(nom);
        return garantie != null && !garantie.getId().equals(idExclu);
    }

    /** Nombre de contrats qui ont souscrit cette garantie (utilisé par estUtiliseeParContrat). */
    @Query("SELECT COUNT(c) FROM Contrat c JOIN c.garanties g WHERE g.id = :id")
    long compterContrats(Long id);

    /** @return true si au moins un contrat a souscrit cette garantie */
    default boolean estUtiliseeParContrat(Long id) {
        return compterContrats(id) > 0;
    }

    /** Modifie nom, description, plafond et franchise. @return 0 si la garantie n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Garantie g SET g.nom = :nom, g.description = :description,"
            + " g.plafond = :plafond, g.franchise = :franchise WHERE g.id = :id")
    int modifierInfos(Long id, String nom, String description, BigDecimal plafond, BigDecimal franchise);
}
