package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.TypeAssurance;

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
}
