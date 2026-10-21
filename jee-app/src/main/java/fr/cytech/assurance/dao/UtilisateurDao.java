package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Utilisateur;

// CRUD (trouverParId, listerTous, creer, modifier, supprimer) hérité de GenericDao
public class UtilisateurDao extends GenericDao<Utilisateur> {

    public UtilisateurDao() {
        super(Utilisateur.class);
    }

    // Renvoie null si aucun utilisateur n'a cet identifiant
    public Utilisateur trouverParIdentifiant(String identifiant) {
        return enLecture(em -> em.createQuery(
                        "SELECT u FROM Utilisateur u WHERE u.identifiant = :identifiant", Utilisateur.class)
                .setParameter("identifiant", identifiant)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }
}
