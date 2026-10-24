package fr.cytech.assurance.dao;

import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

// CRUD (trouverParId, listerTous, creer, modifier, supprimer) hérité de GenericDao
public class ClientDao extends GenericDao<Client> {

    public ClientDao() {
        super(Client.class);
    }

    // Crée le compte utilisateur et la fiche client dans la même transaction :
    // si l'une des deux insertions échoue, rien n'est enregistré
    // Lève UtilisateurDejaExistantException si l'identifiant ou l'email est déjà pris
    public void creerAvecUtilisateur(Client client, Utilisateur utilisateur) {
        try {
            enTransaction(em -> {
                if (existe(em, "identifiant", utilisateur.getIdentifiant())) {
                    throw new UtilisateurDejaExistantException("identifiant");
                }
                if (existe(em, "email", utilisateur.getEmail())) {
                    throw new UtilisateurDejaExistantException("email");
                }
                em.persist(utilisateur);
                client.setUtilisateur(utilisateur);
                em.persist(client);
                return null;
            });
        } catch (PersistenceException e) {
            // Deux inscriptions simultanées peuvent passer la vérification : la contrainte UNIQUE
            // de la base refuse alors la seconde, qu'on signale de la même façon
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new UtilisateurDejaExistantException(null);
            }
            throw e;
        }
    }

    // Fiche client d'un utilisateur (celui qui est connecté, par exemple), ou null s'il n'est pas client
    public Client trouverParUtilisateur(Long idUtilisateur) {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Client c WHERE c.utilisateur.id = :idUtilisateur", Client.class)
                .setParameter("idUtilisateur", idUtilisateur)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    // Vrai si un utilisateur a déjà cette valeur dans ce champ ("identifiant" ou "email")
    private static boolean existe(EntityManager em, String champ, String valeur) {
        return em.createQuery("SELECT COUNT(u) FROM Utilisateur u WHERE u." + champ + " = :valeur", Long.class)
                .setParameter("valeur", valeur)
                .getSingleResult() > 0;
    }
}
