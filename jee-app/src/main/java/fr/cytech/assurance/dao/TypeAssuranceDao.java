package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;

import java.math.BigDecimal;
import java.util.List;

// CRUD (trouverParId, listerTous, creer, modifier, supprimer) hérité de GenericDao
public class TypeAssuranceDao extends GenericDao<TypeAssurance> {

    public TypeAssuranceDao() {
        super(TypeAssurance.class);
    }

    // Type portant ce nom (unique en base), ou null
    public TypeAssurance trouverParNom(String nom) {
        return enLecture(em -> em.createQuery(
                        "SELECT t FROM TypeAssurance t WHERE t.nom = :nom",
                        TypeAssurance.class)
                .setParameter("nom", nom)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    // Tous les types d'assurance, triés par nom
    public List<TypeAssurance> listerParNom() {
        return enLecture(em -> em.createQuery(
                        "SELECT t FROM TypeAssurance t ORDER BY t.nom", TypeAssurance.class)
                .getResultList());
    }

    // Tous les types triés par nom, avec leurs garanties déjà chargées (lisibles dans une JSP)
    public List<TypeAssurance> listerAvecGaranties() {
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT t FROM TypeAssurance t LEFT JOIN FETCH t.garanties ORDER BY t.nom",
                        TypeAssurance.class)
                .getResultList());
    }

    // Types dont le nom contient motCle (sans tenir compte des majuscules), triés par nom
    public List<TypeAssurance> rechercher(String motCle) {
        return enLecture(em -> em.createQuery(
                        "SELECT t FROM TypeAssurance t WHERE LOWER(t.nom) LIKE LOWER(:motif) ORDER BY t.nom",
                        TypeAssurance.class)
                .setParameter("motif", "%" + motCle + "%")
                .getResultList());
    }

    // Type avec ses garanties disponibles déjà chargées (lisibles hors du DAO), ou null
    public TypeAssurance trouverAvecGaranties(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT t FROM TypeAssurance t LEFT JOIN FETCH t.garanties WHERE t.id = :id",
                        TypeAssurance.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    // Vrai si un autre type que celui d'id idExclu porte déjà ce nom
    // idExclu = null à la création, id du type modifié lors d'une modification
    public boolean existeAvecNom(String nom, Long idExclu) {
        TypeAssurance typeAssurance = trouverParNom(nom);
        return typeAssurance != null && !typeAssurance.getId().equals(idExclu);
    }

    // Vrai si au moins un contrat utilise ce type : il ne peut alors pas être supprimé (clé étrangère)
    public boolean estUtiliseParContrat(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT COUNT(c) FROM Contrat c WHERE c.typeAssurance.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult()) > 0;
    }

    // Modifie uniquement nom, description et prix de base (les garanties proposées ne sont pas touchées)
    // Renvoie false si aucun type n'a cet id
    public boolean modifierInfos(Long id, String nom, String description, BigDecimal prixBase) {
        return mettreAJour(id, t -> {
            t.setNom(nom);
            t.setDescription(description);
            t.setPrixBase(prixBase);
        });
    }

    // Propose cette garantie avec ce type (sans effet si elle l'était déjà)
    // Renvoie false si le type ou la garantie n'existe pas
    public boolean ajouterGarantie(Long idType, Long idGarantie) {
        return enTransaction(em -> {
            TypeAssurance type = em.find(TypeAssurance.class, idType);
            Garantie garantie = em.find(Garantie.class, idGarantie);
            if (type == null || garantie == null) {return false;}
            type.getGaranties().add(garantie);
            return true;
        });
    }

    // Ne propose plus cette garantie avec ce type (les contrats existants gardent leurs garanties)
    // Renvoie false si aucun type n'a cet id
    public boolean retirerGarantie(Long idType, Long idGarantie) {
        return mettreAJour(idType, t -> t.getGaranties().removeIf(g -> g.getId().equals(idGarantie)));
    }
}
