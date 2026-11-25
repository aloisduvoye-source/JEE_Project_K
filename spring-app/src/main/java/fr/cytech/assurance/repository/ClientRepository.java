package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Accès aux fiches clients (remplace ClientDao).
 * La création du compte et de sa fiche (creerAvecUtilisateur) se fait dans le service, en une transaction.
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    /** @return la fiche client de ce compte, ou null */
    @Query("SELECT c FROM Client c WHERE c.utilisateur.id = :idUtilisateur")
    Client trouverParUtilisateur(Long idUtilisateur);

    /** @return les clients avec leur compte, triés par nom puis prénom */
    @Query("SELECT c FROM Client c JOIN FETCH c.utilisateur u ORDER BY u.nom, u.prenom")
    List<Client> listerAvecUtilisateur();

    /** @return les clients dont le nom, le prénom ou l'email contient ce mot-clé (sans tenir compte de la casse) */
    @Query("SELECT c FROM Client c JOIN FETCH c.utilisateur u"
            + " WHERE LOWER(u.nom) LIKE LOWER(CONCAT('%', :motCle, '%'))"
            + " OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :motCle, '%'))"
            + " OR LOWER(u.email) LIKE LOWER(CONCAT('%', :motCle, '%'))"
            + " ORDER BY u.nom, u.prenom")
    List<Client> rechercher(String motCle);
}
