package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.TypeAssurance;

// CRUD (trouverParId, listerTous, creer, modifier, supprimer) hérité de GenericDao
public class TypeAssuranceDao extends GenericDao<TypeAssurance> {

    public TypeAssuranceDao() {
        super(TypeAssurance.class);
    }

    public TypeAssurance trouverAvecNom(String nom){
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT t FROM TypeAssurance t WHERE t.nom = :nom",
                        TypeAssurance.class)
                .setParameter("nom", nom),
                .getResultStream(),
                .findFirst(),
                orElse(null));
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
}
