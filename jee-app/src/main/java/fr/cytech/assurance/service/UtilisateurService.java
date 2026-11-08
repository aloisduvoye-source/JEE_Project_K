package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.UtilisateurDao;
import fr.cytech.assurance.exception.AuthentificationException;
import fr.cytech.assurance.exception.MotDePasseIncorrectException;
import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.exception.UtilisateurInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * Règles métier des comptes utilisateurs : connexion, inscription, profil, mot de passe.
 * Les servlets vérifient le format des saisies (Validateur), puis appellent ce service ;
 * le service applique les règles et passe par les DAO pour la base.
 * Le hachage des mots de passe (BCrypt) n'est fait qu'ici.
 */
public class UtilisateurService {

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final ClientDao clientDao = new ClientDao();

    public Utilisateur trouverParId(Long id) {
        return utilisateurDao.trouverParId(id);
    }

    // Renvoie l'utilisateur si l'identifiant et le mot de passe sont bons et le compte actif
    public Utilisateur authentifier(String identifiant, String motDePasse) {
        Utilisateur utilisateur = utilisateurDao.trouverParIdentifiant(identifiant);
        // Même message que l'identifiant ou le mot de passe soit faux : on ne révèle pas lequel
        if (utilisateur == null || !BCrypt.checkpw(motDePasse, utilisateur.getMotDePasseHash())) {
            throw new AuthentificationException("Identifiant ou mot de passe incorrect.");
        }
        if (!utilisateur.isActif()) {
            throw new AuthentificationException("Ce compte est désactivé.");
        }
        return utilisateur;
    }

    public boolean identifiantDisponible(String identifiant) {
        return !utilisateurDao.identifiantExiste(identifiant);
    }

    public boolean emailDisponible(String email) {
        return !utilisateurDao.emailExiste(email);
    }

    // Disponible si personne ne l'utilise, ou si c'est déjà l'email de cet utilisateur
    public boolean emailDisponiblePour(String email, Long idUtilisateur) {
        return !utilisateurDao.emailExistePourUnAutre(email, idUtilisateur);
    }

    // Crée un compte client : rôle toujours CLIENT, mot de passe haché
    // Lève UtilisateurDejaExistantException si l'identifiant ou l'email est déjà pris
    public void inscrireClient(Utilisateur utilisateur, Client client, String motDePasse) {
        utilisateur.setRole(Role.CLIENT);
        utilisateur.setMotDePasseHash(hacher(motDePasse));
        clientDao.creerAvecUtilisateur(client, utilisateur);
    }

    // Crée un compte agent (par l'administrateur) : rôle toujours AGENT, mot de passe haché, pas de fiche client
    // Lève UtilisateurDejaExistantException si l'identifiant ou l'email est déjà pris
    public void creerAgent(Utilisateur utilisateur, String motDePasse) {
        utilisateur.setRole(Role.AGENT);
        utilisateur.setMotDePasseHash(hacher(motDePasse));
        utilisateurDao.creerSansDoublon(utilisateur);
    }

    // Désactivation ou réactivation d'un compte par l'administrateur.
    // Le compte administrateur ne peut pas être désactivé : sinon plus personne ne pourrait gérer l'application
    // Lève UtilisateurInvalideException si le compte n'existe pas ou est celui de l'administrateur
    public void changerEtat(Long id, boolean actif) {
        Utilisateur utilisateur = utilisateurDao.trouverParId(id);
        if (utilisateur == null) {
            throw new UtilisateurInvalideException("Ce compte n'existe pas.");
        }
        if (utilisateur.getRole() == Role.ADMIN) {
            throw new UtilisateurInvalideException("Le compte administrateur ne peut pas être désactivé.");
        }
        utilisateurDao.changerEtat(id, actif);
    }

    // Suppression de son propre compte (client ou agent), depuis « Mon compte ».
    // Le mot de passe est exigé : une session laissée ouverte ne suffit pas pour supprimer le compte.
    // Compte sans historique : effacé (renvoie true). Avec contrats ou sinistres : désactivé (renvoie false)
    // Lève UtilisateurInvalideException pour l'administrateur, MotDePasseIncorrectException si le mot de passe est faux
    public boolean supprimerCompte(Long id, String motDePasse) {
        Utilisateur utilisateur = utilisateurDao.trouverParId(id);   // hash relu en base
        if (utilisateur == null) {
            throw new UtilisateurInvalideException("Ce compte n'existe pas.");
        }
        if (utilisateur.getRole() == Role.ADMIN) {
            throw new UtilisateurInvalideException("Le compte administrateur ne peut pas être supprimé.");
        }
        if (motDePasse == null || !BCrypt.checkpw(motDePasse, utilisateur.getMotDePasseHash())) {
            throw new MotDePasseIncorrectException();
        }
        return utilisateurDao.supprimerSiSansHistorique(id);
    }

    // Utilisateurs triés par nom puis prénom : tous si role est null, sinon ceux de ce rôle
    public List<Utilisateur> lister(Role role) {
        return role == null ? utilisateurDao.listerTousTries() : utilisateurDao.listerParRole(role);
    }

    // Lève UtilisateurDejaExistantException si l'email appartient à un autre compte
    public void modifierProfil(Long id, String nom, String prenom, String email) {
        if (!emailDisponiblePour(email, id)) {
            throw new UtilisateurDejaExistantException("email");
        }
        utilisateurDao.modifierProfil(id, nom, prenom, email);
    }

    // Le mot de passe actuel est exigé : une session laissée ouverte ne suffit pas pour le changer
    // Lève MotDePasseIncorrectException si le mot de passe actuel est faux
    public void changerMotDePasse(Long id, String actuel, String nouveau) {
        Utilisateur utilisateur = utilisateurDao.trouverParId(id);   // hash relu en base
        if (actuel == null || !BCrypt.checkpw(actuel, utilisateur.getMotDePasseHash())) {
            throw new MotDePasseIncorrectException();
        }
        utilisateurDao.changerMotDePasse(id, hacher(nouveau));
    }

    private static String hacher(String motDePasse) {
        return BCrypt.hashpw(motDePasse, BCrypt.gensalt());
    }
}
