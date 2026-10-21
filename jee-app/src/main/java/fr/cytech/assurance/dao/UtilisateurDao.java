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

    // Renvoie null si aucun utilisateur n'a cet email
    public Utilisateur trouverParEmail(String email) {
        return enLecture(em -> em.createQuery(
                        "SELECT u FROM Utilisateur u WHERE u.email = :email", Utilisateur.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    public boolean identifiantExiste(String identifiant) {
        return trouverParIdentifiant(identifiant) != null;
    }

    public boolean emailExiste(String email) {
        return trouverParEmail(email) != null;
    }


    public boolean emailExistePourUnAutre(String email, Long idUtilisateur) {
        Utilisateur utilisateur = trouverParEmail(email);
        return utilisateur != null && !utilisateur.getId().equals(idUtilisateur);
    }

    // Remplace le mot de passe : nouveauHash doit déjà être haché (BCrypt.hashpw), jamais en clair
    // Renvoie false si aucun utilisateur n'a cet id
    public boolean changerMotDePasse(Long id, String nouveauHash) {
        return enTransaction(em -> {
            Utilisateur utilisateur = em.find(Utilisateur.class, id);
            if (utilisateur == null) {return false;}
            utilisateur.setMotDePasseHash(nouveauHash);   // enregistré en base à la validation de la transaction
            return true;
        });
    }
}
