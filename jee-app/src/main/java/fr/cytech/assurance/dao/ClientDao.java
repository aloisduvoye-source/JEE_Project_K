package fr.cytech.assurance.dao;

import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;

/**
 * Accès en base aux fiches clients.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class ClientDao extends GenericDao<Client> {

    public ClientDao() {
        super(Client.class);
    }

    /**
     * Crée le compte utilisateur et la fiche client associée dans une seule transaction.
     * <p>
     * Si l'une des deux insertions échoue, rien n'est enregistré.
     *
     * @param client      fiche client à créer
     * @param utilisateur compte utilisateur à créer et à relier au client
     * @throws UtilisateurDejaExistantException si l'identifiant ou l'email est déjà pris
     */
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

    /**
     * Recherche la fiche client d'un utilisateur (celui qui est connecté, par exemple).
     *
     * @param idUtilisateur id de l'utilisateur
     * @return la fiche client, ou {@code null} si l'utilisateur n'est pas client
     */
    public Client trouverParUtilisateur(Long idUtilisateur) {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Client c WHERE c.utilisateur.id = :idUtilisateur", Client.class)
                .setParameter("idUtilisateur", idUtilisateur)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Liste tous les clients triés par nom puis prénom, avec leur compte utilisateur déjà chargé.
     * <p>
     * Le nom, le prénom et l'email sont dans l'utilisateur : ils restent ainsi lisibles dans une JSP.
     *
     * @return les clients triés, liste vide s'il n'y en a aucun
     */
    public List<Client> listerAvecUtilisateur() {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Client c JOIN FETCH c.utilisateur u ORDER BY u.nom, u.prenom", Client.class)
                .getResultList());
    }

    /**
     * Recherche les clients dont le nom, le prénom ou l'email contient un mot-clé, sans tenir compte des majuscules.
     *
     * @param motCle texte à chercher ({@code %} et {@code _} servent de jokers)
     * @return les clients correspondants triés par nom puis prénom, avec leur utilisateur déjà chargé
     */
    public List<Client> rechercher(String motCle) {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Client c JOIN FETCH c.utilisateur u"
                                + " WHERE LOWER(u.nom) LIKE LOWER(:motif)"
                                + " OR LOWER(u.prenom) LIKE LOWER(:motif)"
                                + " OR LOWER(u.email) LIKE LOWER(:motif)"
                                + " ORDER BY u.nom, u.prenom", Client.class)
                .setParameter("motif", "%" + motCle + "%")
                .getResultList());
    }

    // Vrai si un utilisateur a déjà cette valeur dans ce champ ("identifiant" ou "email")
    private static boolean existe(EntityManager em, String champ, String valeur) {
        return em.createQuery("SELECT COUNT(u) FROM Utilisateur u WHERE u." + champ + " = :valeur", Long.class)
                .setParameter("valeur", valeur)
                .getSingleResult() > 0;
    }
}
