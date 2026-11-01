package fr.cytech.assurance.dao;

import fr.cytech.assurance.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Opérations CRUD communes à toutes les entités.
 * Chaque DAO hérite de cette classe et n'ajoute que ses requêtes spécifiques :
 *
 * <pre>{@code
 *   public class ContratDao extends GenericDao<Contrat> {
 *       public ContratDao() { super(Contrat.class); }
 *   }
 * }</pre>
 * <p>
 * Attention : l'EntityManager est fermé à la fin de chaque méthode. Les collections chargées
 * à la demande (@ManyToMany, ex. contrat.getGaranties()) doivent être récupérées dans la requête
 * avec "JOIN FETCH" si une servlet ou une JSP doit les lire.
 */
public abstract class GenericDao<T> {

    private final Class<T> classe;

    protected GenericDao(Class<T> classe) {
        this.classe = classe;
    }

    /**
     * Recherche une entité par son identifiant.
     *
     * @param id identifiant de l'entité
     * @return l'entité, ou {@code null} si aucune n'a cet id
     */
    public T trouverParId(Long id) {
        return enLecture(em -> em.find(classe, id));
    }

    /**
     * Liste toutes les entités, sans ordre garanti.
     *
     * @return les entités, liste vide s'il n'y en a aucune
     */
    public List<T> listerTous() {
        return enLecture(em -> em.createQuery("SELECT e FROM " + classe.getSimpleName() + " e", classe)
                .getResultList());
    }

    /**
     * Enregistre une nouvelle entité.
     *
     * @param entite entité à créer, sans id
     * @return la même entité, dont l'id a été généré par la base
     */
    public T creer(T entite) {
        return enTransaction(em -> {
            em.persist(entite);
            return entite;
        });
    }

    /**
     * Enregistre toutes les valeurs d'une entité existante ({@code merge}).
     * <p>
     * Tous les champs sont écrasés, y compris les collections : pour ne changer que certains champs,
     * préférer une méthode basée sur {@link #mettreAJour}.
     *
     * @param entite entité à enregistrer, avec son id
     * @return l'entité telle qu'enregistrée en base
     */
    public T modifier(T entite) {
        return enTransaction(em -> em.merge(entite));
    }

    /**
     * Supprime une entité. Sans effet si aucune n'a cet id.
     *
     * @param id identifiant de l'entité à supprimer
     */
    public void supprimer(Long id) {
        enTransaction(em -> {
            T entite = em.find(classe, id);
            if (entite != null) {
                em.remove(entite);
            }
            return null;
        });
    }

    /**
     * Modifie seulement ce que fait {@code modification} sur l'entité, dans une transaction.
     * <p>
     * L'entité est chargée dans la transaction et Hibernate enregistre les changements au commit :
     * contrairement à {@link #modifier}, rien d'autre n'est écrasé.
     *
     * @param id           identifiant de l'entité
     * @param modification changements à appliquer à l'entité chargée
     * @return {@code false} si aucune entité n'a cet id
     */
    protected boolean mettreAJour(Long id, Consumer<T> modification) {
        return enTransaction(em -> {
            T entite = em.find(classe, id);
            if (entite == null) {return false;}
            modification.accept(entite);
            return true;
        });
    }

    /**
     * Exécute une lecture seule (sans transaction) avec un EntityManager fermé à la fin.
     *
     * @param operation requête à exécuter
     * @param <R>       type du résultat
     * @return le résultat de {@code operation}
     */
    protected <R> R enLecture(Function<EntityManager, R> operation) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return operation.apply(em);
        }
    }

    /**
     * Exécute une écriture dans une transaction, avec un EntityManager fermé à la fin.
     * <p>
     * La transaction est validée si tout s'est bien passé, et annulée en cas d'erreur.
     *
     * @param operation opération à exécuter
     * @param <R>       type du résultat
     * @return le résultat de {@code operation}
     * @throws RuntimeException toute exception levée par {@code operation}, relancée après l'annulation
     */
    protected <R> R enTransaction(Function<EntityManager, R> operation) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                R resultat = operation.apply(em);
                tx.commit();
                return resultat;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }
}
