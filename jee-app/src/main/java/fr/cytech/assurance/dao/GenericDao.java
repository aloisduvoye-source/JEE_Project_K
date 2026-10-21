package fr.cytech.assurance.dao;

import fr.cytech.assurance.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.function.Function;

/**
 * Opérations CRUD communes à toutes les entités.
 * Chaque DAO hérite de cette classe et n'ajoute que ses requêtes spécifiques :
 *
 *   public class ContratDao extends GenericDao<Contrat> {
 *       public ContratDao() { super(Contrat.class); }
 *   }
 *
 * Attention : l'EntityManager est fermé à la fin de chaque méthode. Les collections chargées
 * à la demande (@ManyToMany, ex. contrat.getGaranties()) doivent être récupérées dans la requête
 * avec "JOIN FETCH" si une servlet ou une JSP doit les lire.
 */
public abstract class GenericDao<T> {

    private final Class<T> classe;

    protected GenericDao(Class<T> classe) {
        this.classe = classe;
    }

    public T trouverParId(Long id) {
        return enLecture(em -> em.find(classe, id));
    }

    public List<T> listerTous() {
        return enLecture(em -> em.createQuery("SELECT e FROM " + classe.getSimpleName() + " e", classe)
                .getResultList());
    }

    public T creer(T entite) {
        return enTransaction(em -> {
            em.persist(entite);
            return entite;
        });
    }

    public T modifier(T entite) {
        return enTransaction(em -> em.merge(entite));
    }

    public void supprimer(Long id) {
        enTransaction(em -> {
            T entite = em.find(classe, id);
            if (entite != null) {
                em.remove(entite);
            }
            return null;
        });
    }

    // Lecture seule : pas besoin de transaction
    protected <R> R enLecture(Function<EntityManager, R> operation) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return operation.apply(em);
        }
    }

    // Écriture : validée si tout s'est bien passé, annulée en cas d'erreur (cohérence des données)
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
