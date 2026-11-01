package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Utilisateur;

/**
 * Accès en base aux comptes utilisateurs.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class UtilisateurDao extends GenericDao<Utilisateur> {

    public UtilisateurDao() {
        super(Utilisateur.class);
    }

    /**
     * Recherche un utilisateur par son identifiant de connexion.
     *
     * @param identifiant identifiant de connexion
     * @return l'utilisateur, ou {@code null} si aucun n'a cet identifiant
     */
    public Utilisateur trouverParIdentifiant(String identifiant) {
        return enLecture(em -> em.createQuery(
                        "SELECT u FROM Utilisateur u WHERE u.identifiant = :identifiant", Utilisateur.class)
                .setParameter("identifiant", identifiant)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Recherche un utilisateur par son adresse email.
     *
     * @param email adresse email
     * @return l'utilisateur, ou {@code null} si aucun n'a cet email
     */
    public Utilisateur trouverParEmail(String email) {
        return enLecture(em -> em.createQuery(
                        "SELECT u FROM Utilisateur u WHERE u.email = :email", Utilisateur.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Indique si un utilisateur a déjà cet identifiant.
     *
     * @param identifiant identifiant à vérifier
     * @return {@code true} si l'identifiant est déjà pris
     */
    public boolean identifiantExiste(String identifiant) {
        return trouverParIdentifiant(identifiant) != null;
    }

    /**
     * Indique si un utilisateur a déjà cet email.
     *
     * @param email email à vérifier
     * @return {@code true} si l'email est déjà pris
     */
    public boolean emailExiste(String email) {
        return trouverParEmail(email) != null;
    }

    /**
     * Indique si un autre utilisateur que celui donné a déjà cet email.
     * <p>
     * À utiliser lors d'une modification de profil : l'utilisateur peut garder son propre email.
     *
     * @param email         email à vérifier
     * @param idUtilisateur id de l'utilisateur qui modifie son profil
     * @return {@code true} si un utilisateur d'id différent a cet email
     */
    public boolean emailExistePourUnAutre(String email, Long idUtilisateur) {
        Utilisateur utilisateur = trouverParEmail(email);
        return utilisateur != null && !utilisateur.getId().equals(idUtilisateur);
    }

    /**
     * Remplace le mot de passe d'un utilisateur.
     *
     * @param id          identifiant de l'utilisateur
     * @param nouveauHash mot de passe déjà haché ({@code BCrypt.hashpw}), jamais en clair
     * @return {@code false} si aucun utilisateur n'a cet id
     */
    public boolean changerMotDePasse(Long id, String nouveauHash) {
        return mettreAJour(id, u -> u.setMotDePasseHash(nouveauHash));
    }

    /**
     * Modifie le nom, le prénom et l'email d'un utilisateur.
     * <p>
     * Le rôle, le mot de passe et l'état du compte ne sont pas modifiés.
     *
     * @param id     identifiant de l'utilisateur
     * @param nom    nouveau nom
     * @param prenom nouveau prénom
     * @param email  nouvel email
     * @return {@code false} si aucun utilisateur n'a cet id
     */
    public boolean modifierProfil(Long id, String nom, String prenom, String email) {
        return mettreAJour(id, u -> {
            u.setNom(nom);
            u.setPrenom(prenom);
            u.setEmail(email);
        });
    }
}
