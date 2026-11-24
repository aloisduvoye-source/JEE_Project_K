package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Accès aux comptes utilisateurs (remplace UtilisateurDao).
 * Création sans doublon et suppression d'un compte sans historique : dans le service, avec les méthodes ci-dessous.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /** @return tous les comptes triés par nom puis prénom */
    @Query("SELECT u FROM Utilisateur u ORDER BY u.nom, u.prenom")
    List<Utilisateur> listerTousTries();

    /** @return les comptes de ce rôle, triés par nom puis prénom */
    @Query("SELECT u FROM Utilisateur u WHERE u.role = :role ORDER BY u.nom, u.prenom")
    List<Utilisateur> listerParRole(Role role);

    /** @return le compte de cet identifiant, ou null */
    @Query("SELECT u FROM Utilisateur u WHERE u.identifiant = :identifiant")
    Utilisateur trouverParIdentifiant(String identifiant);

    /** @return le compte de cet email, ou null */
    @Query("SELECT u FROM Utilisateur u WHERE u.email = :email")
    Utilisateur trouverParEmail(String email);

    /** @return true si un compte a déjà cet identifiant */
    default boolean identifiantExiste(String identifiant) {
        return trouverParIdentifiant(identifiant) != null;
    }

    /** @return true si un compte a déjà cet email */
    default boolean emailExiste(String email) {
        return trouverParEmail(email) != null;
    }

    /** @return true si un autre compte que idUtilisateur a déjà cet email */
    default boolean emailExistePourUnAutre(String email, Long idUtilisateur) {
        Utilisateur utilisateur = trouverParEmail(email);
        return utilisateur != null && !utilisateur.getId().equals(idUtilisateur);
    }

    /** 1 si le compte a des contrats (client), des commentaires ou des sinistres suivis (agent), sinon 0. */
    @Query("SELECT COUNT(u) FROM Utilisateur u WHERE u.id = :id AND ("
            + " EXISTS (SELECT c.id FROM Contrat c WHERE c.client.utilisateur.id = :id)"
            + " OR EXISTS (SELECT cs.id FROM CommentaireSinistre cs WHERE cs.auteur.id = :id)"
            + " OR EXISTS (SELECT s.id FROM Sinistre s WHERE s.agent.id = :id))")
    long compterHistorique(Long id);

    /** @return true si le compte a un historique : on le désactive alors au lieu de le supprimer */
    default boolean aUnHistorique(Long id) {
        return compterHistorique(id) > 0;
    }

    /** Remplace le hash du mot de passe. @return 0 si le compte n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Utilisateur u SET u.motDePasseHash = :nouveauHash WHERE u.id = :id")
    int changerMotDePasse(Long id, String nouveauHash);

    /** Modifie nom, prénom et email. @return 0 si le compte n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Utilisateur u SET u.nom = :nom, u.prenom = :prenom, u.email = :email WHERE u.id = :id")
    int modifierProfil(Long id, String nom, String prenom, String email);

    /** Active (true) ou désactive (false) le compte. @return 0 si le compte n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Utilisateur u SET u.actif = :actif WHERE u.id = :id")
    int changerEtat(Long id, boolean actif);
}
