package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.UtilisateurDao;
import fr.cytech.assurance.exception.AuthentificationException;
import fr.cytech.assurance.exception.MotDePasseIncorrectException;
import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import org.mindrot.jbcrypt.BCrypt;

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
        utilisateur.setMotDePasseHash(BCrypt.hashpw(motDePasse, BCrypt.gensalt()));
        clientDao.creerAvecUtilisateur(client, utilisateur);
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
        utilisateurDao.changerMotDePasse(id, BCrypt.hashpw(nouveau, BCrypt.gensalt()));
    }
}
