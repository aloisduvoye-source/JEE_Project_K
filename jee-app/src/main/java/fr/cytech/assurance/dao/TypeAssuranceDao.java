package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Accès en base aux types d'assurance.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class TypeAssuranceDao extends GenericDao<TypeAssurance> {

    public TypeAssuranceDao() {
        super(TypeAssurance.class);
    }

    /**
     * Recherche un type d'assurance par son nom exact (unique en base).
     *
     * @param nom nom du type
     * @return le type portant ce nom, ou {@code null} s'il n'existe pas
     */
    public TypeAssurance trouverParNom(String nom) {
        return enLecture(em -> em.createQuery(
                        "SELECT t FROM TypeAssurance t WHERE t.nom = :nom",
                        TypeAssurance.class)
                .setParameter("nom", nom)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Liste tous les types d'assurance, triés par nom.
     *
     * @return les types triés, liste vide s'il n'y en a aucun
     */
    public List<TypeAssurance> listerParNom() {
        return enLecture(em -> em.createQuery(
                        "SELECT t FROM TypeAssurance t ORDER BY t.nom", TypeAssurance.class)
                .getResultList());
    }

    /**
     * Liste tous les types d'assurance triés par nom, avec leurs garanties déjà chargées.
     * <p>
     * Les garanties restent lisibles après la fermeture de l'EntityManager (dans une JSP par exemple).
     *
     * @return les types triés avec leurs garanties, liste vide s'il n'y en a aucun
     */
    public List<TypeAssurance> listerAvecGaranties() {
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT t FROM TypeAssurance t LEFT JOIN FETCH t.garanties ORDER BY t.nom",
                        TypeAssurance.class)
                .getResultList());
    }

    /**
     * Recherche les types dont le nom contient un mot-clé, sans tenir compte des majuscules.
     *
     * @param motCle texte à chercher dans le nom ({@code %} et {@code _} servent de jokers)
     * @return les types correspondants triés par nom, liste vide si aucun ne correspond
     */
    public List<TypeAssurance> rechercher(String motCle) {
        return enLecture(em -> em.createQuery(
                        "SELECT t FROM TypeAssurance t WHERE LOWER(t.nom) LIKE LOWER(:motif) ORDER BY t.nom",
                        TypeAssurance.class)
                .setParameter("motif", "%" + motCle + "%")
                .getResultList());
    }

    /**
     * Recherche un type d'assurance avec ses garanties disponibles déjà chargées.
     *
     * @param id id du type
     * @return le type avec ses garanties, ou {@code null} s'il n'existe pas
     */
    public TypeAssurance trouverAvecGaranties(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT t FROM TypeAssurance t LEFT JOIN FETCH t.garanties WHERE t.id = :id",
                        TypeAssurance.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Indique si un autre type d'assurance porte déjà ce nom.
     * <p>
     * À appeler avant une création ou une modification, le nom étant unique en base.
     *
     * @param nom     nom à vérifier
     * @param idExclu id du type en cours de modification, ou {@code null} à la création
     * @return {@code true} si un type d'id différent de {@code idExclu} porte ce nom
     */
    public boolean existeAvecNom(String nom, Long idExclu) {
        TypeAssurance typeAssurance = trouverParNom(nom);
        return typeAssurance != null && !typeAssurance.getId().equals(idExclu);
    }

    /**
     * Indique si au moins un contrat utilise ce type d'assurance.
     * <p>
     * Un type utilisé ne peut pas être supprimé (clé étrangère dans la table contrat).
     *
     * @param id id du type
     * @return {@code true} si au moins un contrat utilise ce type
     */
    public boolean estUtiliseParContrat(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Contrat c WHERE c.typeAssurance.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult()) > 0;
    }

    /**
     * Modifie le nom, la description et le prix de base d'un type d'assurance.
     * <p>
     * Les garanties proposées ne sont pas modifiées, contrairement à {@link #modifier}.
     *
     * @param id          id du type
     * @param nom         nouveau nom
     * @param description nouvelle description
     * @param prixBase    nouveau prix de base
     * @return {@code false} si aucun type n'a cet id
     */
    public boolean modifierInfos(Long id, String nom, String description, BigDecimal prixBase) {
        return mettreAJour(id, t -> {
            t.setNom(nom);
            t.setDescription(description);
            t.setPrixBase(prixBase);
        });
    }

    /**
     * Ajoute une garantie à celles proposées par un type d'assurance.
     * <p>
     * Sans effet si la garantie était déjà proposée.
     *
     * @param idType     id du type
     * @param idGarantie id de la garantie à ajouter
     * @return {@code false} si le type ou la garantie n'existe pas
     */
    public boolean ajouterGarantie(Long idType, Long idGarantie) {
        return enTransaction(em -> {
            TypeAssurance type = em.find(TypeAssurance.class, idType);
            Garantie garantie = em.find(Garantie.class, idGarantie);
            if (type == null || garantie == null) {return false;}
            type.getGaranties().add(garantie);
            return true;
        });
    }

    /**
     * Retire une garantie de celles proposées par un type d'assurance.
     * <p>
     * Les contrats existants conservent leurs garanties.
     *
     * @param idType     id du type
     * @param idGarantie id de la garantie à retirer
     * @return {@code false} si aucun type n'a cet id
     */
    public boolean retirerGarantie(Long idType, Long idGarantie) {
        return mettreAJour(idType, t -> t.getGaranties().removeIf(g -> g.getId().equals(idGarantie)));
    }
}
