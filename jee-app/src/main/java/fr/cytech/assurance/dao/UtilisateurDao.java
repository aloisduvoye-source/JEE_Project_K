package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.util.JpaUtil;
import jakarta.persistence.EntityManager;

public class UtilisateurDao {

    // Renvoie null si aucun utilisateur n'a cet identifiant
    public Utilisateur trouverParIdentifiant(String identifiant) {
        try (EntityManager em = JpaUtil.getEntityManager()) {
            return em.createQuery("SELECT u FROM Utilisateur u WHERE u.identifiant = :identifiant", Utilisateur.class)
                    .setParameter("identifiant", identifiant)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        }
    }
}
