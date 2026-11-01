package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Garantie;

import java.math.BigDecimal;
import java.util.List;

/**
 * Accès en base aux garanties (responsabilité civile, vol, incendie...).
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class GarantieDao extends GenericDao<Garantie> {

    public GarantieDao() {
        super(Garantie.class);
    }

    /**
     * Recherche une garantie par son nom exact (unique en base).
     *
     * @param nom nom de la garantie
     * @return la garantie portant ce nom, ou {@code null} si elle n'existe pas
     */
    public Garantie trouverParNom(String nom) {
        return enLecture(em -> em.createQuery(
                        "SELECT g FROM Garantie g WHERE g.nom = :nom", Garantie.class)
                .setParameter("nom", nom)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Liste toutes les garanties, triées par nom.
     *
     * @return les garanties triées, liste vide s'il n'y en a aucune
     */
    public List<Garantie> listerParNom() {
        return enLecture(em -> em.createQuery(
                        "SELECT g FROM Garantie g ORDER BY g.nom", Garantie.class)
                .getResultList());
    }

    /**
     * Indique si une autre garantie que celle d'id {@code idExclu} porte déjà ce nom.
     * <p>
     * À appeler avant une création ou une modification, le nom étant unique en base.
     *
     * @param nom     nom à vérifier
     * @param idExclu id de la garantie en cours de modification, ou {@code null} à la création
     * @return {@code true} si une garantie d'id différent de {@code idExclu} porte ce nom
     */
    public boolean existeAvecNom(String nom, Long idExclu) {
        Garantie garantie = trouverParNom(nom);
        return garantie != null && !garantie.getId().equals(idExclu);
    }

    /**
     * Indique si au moins un contrat a souscrit cette garantie.
     * <p>
     * Une garantie souscrite ne peut pas être supprimée (clé étrangère dans contrat_garantie).
     * Une garantie seulement proposée par un type d'assurance peut l'être : le lien est supprimé avec elle.
     *
     * @param id identifiant de la garantie
     * @return {@code true} si au moins un contrat a souscrit cette garantie
     */
    public boolean estUtiliseeParContrat(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Contrat c JOIN c.garanties g WHERE g.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult()) > 0;
    }

    /**
     * Modifie le nom, la description, le plafond et la franchise d'une garantie.
     *
     * @param id          identifiant de la garantie
     * @param nom         nouveau nom
     * @param description nouvelle description
     * @param plafond     nouveau montant maximal remboursé, ou {@code null} s'il n'y en a pas
     * @param franchise   nouveau montant à la charge du client, ou {@code null} s'il n'y en a pas
     * @return {@code false} si aucune garantie n'a cet id
     */
    public boolean modifierInfos(Long id, String nom, String description, BigDecimal plafond, BigDecimal franchise) {
        return mettreAJour(id, g -> {
            g.setNom(nom);
            g.setDescription(description);
            g.setPlafond(plafond);
            g.setFranchise(franchise);
        });
    }
}
