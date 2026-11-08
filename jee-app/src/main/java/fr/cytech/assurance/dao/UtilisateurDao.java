package fr.cytech.assurance.dao;

import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;

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
     * Crée un compte utilisateur seul (agent, administrateur), après avoir vérifié
     * que son identifiant et son email sont libres.
     *
     * @param utilisateur compte à créer, sans id, mot de passe déjà haché
     * @throws UtilisateurDejaExistantException si l'identifiant ou l'email est déjà pris
     */
    public void creerSansDoublon(Utilisateur utilisateur) {
        try {
            enTransaction(em -> {
                verifierDisponible(em, utilisateur);
                em.persist(utilisateur);
                return null;
            });
        } catch (PersistenceException e) {
            throw doublonOuErreur(e);
        }
    }

    /**
     * Liste tous les utilisateurs, tous rôles confondus, triés par nom puis prénom.
     *
     * @return les utilisateurs triés, liste vide s'il n'y en a aucun
     */
    public List<Utilisateur> listerTousTries() {
        return enLecture(em -> em.createQuery(
                        "SELECT u FROM Utilisateur u ORDER BY u.nom, u.prenom", Utilisateur.class)
                .getResultList());
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
     * Indique si un utilisateur a déjà cet identifiant de connexion.
     *
     * @param identifiant identifiant de connexion à vérifier
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
     * @param id          id de l'utilisateur
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
     * @param id     id de l'utilisateur
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

    /**
     * Liste les utilisateurs ayant ce rôle, triés par nom puis prénom.
     *
     * @param role rôle recherché (client, agent ou administrateur)
     * @return les utilisateurs triés, liste vide si aucun n'a ce rôle
     */
    public List<Utilisateur> listerParRole(Role role) {
        return enLecture(em -> em.createQuery(
                        "SELECT u FROM Utilisateur u WHERE u.role = :role ORDER BY u.nom, u.prenom", Utilisateur.class)
                .setParameter("role", role)
                .getResultList());
    }

    /**
     * Active ou désactive un compte. Un compte désactivé ne peut plus se connecter.
     *
     * @param id    id de l'utilisateur
     * @param actif {@code true} pour activer le compte, {@code false} pour le désactiver
     * @return {@code false} si aucun utilisateur n'a cet id
     */
    public boolean changerEtat(Long id, boolean actif) {
        return mettreAJour(id, u -> u.setActif(actif));
    }

    /**
     * Supprime un compte s'il n'a aucun historique, sinon le désactive seulement.
     * <p>
     * L'historique, c'est ce que l'assurance doit garder et que la base refuserait de perdre :
     * les contrats d'un client, les commentaires et les sinistres traités par un agent.
     * Sans historique, le compte est effacé ; la base efface avec lui la fiche client
     * et les messages de l'assistant (ON DELETE CASCADE).
     *
     * @param id id de l'utilisateur
     * @return {@code true} si le compte a été effacé, {@code false} s'il a été désactivé ou n'existe pas
     */
    public boolean supprimerSiSansHistorique(Long id) {
        return enTransaction(em -> {
            Utilisateur utilisateur = em.find(Utilisateur.class, id);
            if (utilisateur == null) {
                return false;
            }
            if (aUnHistorique(em, id)) {
                utilisateur.setActif(false);
                return false;
            }
            em.remove(utilisateur);
            return true;
        });
    }

    // Vrai si le compte a des contrats (client), des commentaires ou des sinistres à traiter (agent)
    private static boolean aUnHistorique(EntityManager em, Long id) {
        String[] requetes = {
                "SELECT COUNT(c) FROM Contrat c WHERE c.client.utilisateur.id = :id",
                "SELECT COUNT(c) FROM CommentaireSinistre c WHERE c.auteur.id = :id",
                "SELECT COUNT(s) FROM Sinistre s WHERE s.agent.id = :id"
        };
        for (String requete : requetes) {
            if (em.createQuery(requete, Long.class).setParameter("id", id).getSingleResult() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie, dans la transaction de création en cours, que l'identifiant et l'email sont libres.
     * Partagé avec {@link ClientDao#creerAvecUtilisateur}.
     *
     * @throws UtilisateurDejaExistantException si l'un des deux est déjà pris
     */
    static void verifierDisponible(EntityManager em, Utilisateur utilisateur) {
        if (existe(em, "identifiant", utilisateur.getIdentifiant())) {
            throw new UtilisateurDejaExistantException("identifiant");
        }
        if (existe(em, "email", utilisateur.getEmail())) {
            throw new UtilisateurDejaExistantException("email");
        }
    }

    /**
     * Deux créations simultanées peuvent passer {@link #verifierDisponible} : la contrainte UNIQUE
     * de la base refuse alors la seconde, qu'on signale comme un doublon. Toute autre erreur est rendue telle quelle.
     */
    static RuntimeException doublonOuErreur(PersistenceException e) {
        if (e.getCause() instanceof ConstraintViolationException) {
            return new UtilisateurDejaExistantException(null);
        }
        return e;
    }

    // Vrai si un utilisateur a déjà cette valeur dans ce champ ("identifiant" ou "email")
    private static boolean existe(EntityManager em, String champ, String valeur) {
        return em.createQuery("SELECT COUNT(u) FROM Utilisateur u WHERE u." + champ + " = :valeur", Long.class)
                .setParameter("valeur", valeur)
                .getSingleResult() > 0;
    }
}
