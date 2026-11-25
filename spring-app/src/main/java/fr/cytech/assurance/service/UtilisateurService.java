package fr.cytech.assurance.service;

import fr.cytech.assurance.repository.ClientRepository;
import fr.cytech.assurance.repository.UtilisateurRepository;
import fr.cytech.assurance.exception.AuthentificationException;
import fr.cytech.assurance.exception.MotDePasseIncorrectException;
import fr.cytech.assurance.exception.UtilisateurDejaExistantException;
import fr.cytech.assurance.exception.UtilisateurInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Règles métier des comptes utilisateurs : connexion, inscription, profil, mot de passe.
 * Les servlets vérifient le format des saisies (Validateur), puis appellent ce service ;
 * le service applique les règles et passe par les DAO pour la base.
 * Le hachage des mots de passe (BCrypt) n'est fait qu'ici.
 */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final ClientRepository clientRepository;

    /** Dépendances injectées par Spring. */
    public UtilisateurService(UtilisateurRepository utilisateurRepository, ClientRepository clientRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.clientRepository = clientRepository;
    }
    
    /**
     * Recherche un utilisateur par son identifiant unique.
     *
     * @param id identifiant de l'utilisateur
     * @return l'utilisateur correspondant, ou null s'il n'existe pas
     */
    public Utilisateur trouverParId(Long id) {
        return utilisateurRepository.findById(id).orElse(null);
    }

    /**
     * Vérifie l'identifiant et le mot de passe d'un utilisateur pour autoriser sa connexion.
     *
     * @param identifiant identifiant de connexion saisi
     * @param motDePasse  mot de passe en clair saisi
     * @return l'utilisateur authentifié si les identifiants sont valides et le compte actif
     * @throws AuthentificationException si l'identifiant ou le mot de passe est incorrect,
     *                                   ou si le compte est désactivé
     */
    public Utilisateur authentifier(String identifiant, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.trouverParIdentifiant(identifiant);
        // Même message que l'identifiant ou le mot de passe soit faux : on ne révèle pas lequel
        if (utilisateur == null || !BCrypt.checkpw(motDePasse, utilisateur.getMotDePasseHash())) {
            throw new AuthentificationException("Identifiant ou mot de passe incorrect.");
        }
        if (!utilisateur.isActif()) {
            throw new AuthentificationException("Ce compte est désactivé.");
        }
        return utilisateur;
    }

    /**
     * Vérifie si un identifiant de connexion est libre (non encore utilisé).
     *
     * @param identifiant identifiant à vérifier
     * @return true si l'identifiant est disponible, false sinon
     */
    public boolean identifiantDisponible(String identifiant) {
        return !utilisateurRepository.identifiantExiste(identifiant);
    }

    /**
     * Vérifie si une adresse email est libre (non encore utilisée).
     *
     * @param email adresse email à vérifier
     * @return true si l'email est disponible, false sinon
     */
    public boolean emailDisponible(String email) {
        return !utilisateurRepository.emailExiste(email);
    }

    /**
     * Vérifie si une adresse email est disponible pour un utilisateur donné
     * (libre ou déjà associée à ce même utilisateur).
     *
     * @param email         adresse email à vérifier
     * @param idUtilisateur identifiant de l'utilisateur concerné
     * @return true si l'email peut être utilisé par ce compte, false s'il appartient à un autre compte
     */    
    public boolean emailDisponiblePour(String email, Long idUtilisateur) {
        return !utilisateurRepository.emailExistePourUnAutre(email, idUtilisateur);
    }

    /**
     * Inscrit un nouveau client en créant son compte utilisateur (rôle CLIENT, mot de passe haché)
     * ainsi que sa fiche client associée.
     *
     * @param utilisateur informations du compte utilisateur à créer
     * @param client      informations de la fiche client associée
     * @param motDePasse  mot de passe en clair à hacher avant enregistrement
     * @throws UtilisateurDejaExistantException si l'identifiant ou l'email est déjà utilisé
     */
    @Transactional
    public void inscrireClient(Utilisateur utilisateur, Client client, String motDePasse) {
        utilisateur.setRole(Role.CLIENT);
        utilisateur.setMotDePasseHash(hacher(motDePasse));
        verifierDisponible(utilisateur);
        try {
            utilisateurRepository.save(utilisateur);
            client.setUtilisateur(utilisateur);
            clientRepository.saveAndFlush(client);
        } catch (DataIntegrityViolationException e) {
            throw doublon();
        }
    }

    /**
     * Crée un compte agent (action réservée à l'administrateur) avec le rôle AGENT et un mot de passe haché.
     *
     * @param utilisateur informations du compte agent à créer
     * @param motDePasse  mot de passe en clair à hacher avant enregistrement
     * @throws UtilisateurDejaExistantException si l'identifiant ou l'email est déjà utilisé
     */
    @Transactional
    public void creerAgent(Utilisateur utilisateur, String motDePasse) {
        utilisateur.setRole(Role.AGENT);
        utilisateur.setMotDePasseHash(hacher(motDePasse));
        verifierDisponible(utilisateur);
        try {
            utilisateurRepository.saveAndFlush(utilisateur);
        } catch (DataIntegrityViolationException e) {
            throw doublon();
        }
    }

    /**
     * Active ou désactive un compte utilisateur (action réservée à l'administrateur).
     * Le compte administrateur ne peut pas être désactivé.
     *
     * @param id    identifiant du compte utilisateur à modifier
     * @param actif true pour activer le compte, false pour le désactiver
     * @throws UtilisateurInvalideException si le compte n'existe pas ou s'il s'agit du compte administrateur
     */
    public void changerEtat(Long id, boolean actif) {
        Utilisateur utilisateur = utilisateurRepository.findById(id).orElse(null);
        if (utilisateur == null) {
            throw new UtilisateurInvalideException("Ce compte n'existe pas.");
        }
        if (utilisateur.getRole() == Role.ADMIN) {
            throw new UtilisateurInvalideException("Le compte administrateur ne peut pas être désactivé.");
        }
        utilisateurRepository.changerEtat(id, actif);
    }

    /**
     * Supprime le compte de l'utilisateur connecté (client ou agent) après vérification de son mot de passe.
     * Si le compte n'a aucun historique (contrats ou sinistres), il est supprimé définitivement ;
     * sinon il est simplement désactivé.
     *
     * @param id         identifiant du compte à supprimer
     * @param motDePasse mot de passe actuel saisi pour confirmer l'opération
     * @return true si le compte a été définitivement supprimé, false s'il a seulement été désactivé
     * @throws UtilisateurInvalideException si le compte est introuvable ou s'il s'agit du compte administrateur
     * @throws MotDePasseIncorrectException si le mot de passe saisi est incorrect
     */
    @Transactional
    public boolean supprimerCompte(Long id, String motDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findById(id).orElse(null);   // hash relu en base
        if (utilisateur == null) {
            throw new UtilisateurInvalideException("Ce compte n'existe pas.");
        }
        if (utilisateur.getRole() == Role.ADMIN) {
            throw new UtilisateurInvalideException("Le compte administrateur ne peut pas être supprimé.");
        }
        if (motDePasse == null || !BCrypt.checkpw(motDePasse, utilisateur.getMotDePasseHash())) {
            throw new MotDePasseIncorrectException();
        }
        if (utilisateurRepository.aUnHistorique(id)) {
            // Contrats, messages ou sinistres à conserver : le compte est seulement désactivé
            utilisateur.setActif(false);
            return false;
        }
        utilisateurRepository.delete(utilisateur);
        return true;
    }

    /**
     * Liste les utilisateurs triés par nom puis prénom, éventuellement filtrés par rôle.
     *
     * @param role rôle recherché (CLIENT, AGENT, ADMIN), ou null pour récupérer tous les utilisateurs
     * @return la liste des utilisateurs triés
     */
    public List<Utilisateur> lister(Role role) {
        return role == null ? utilisateurRepository.listerTousTries() : utilisateurRepository.listerParRole(role);
    }

    /**
     * Modifie le nom, le prénom et l'adresse email d'un compte utilisateur.
     *
     * @param id     identifiant de l'utilisateur à modifier
     * @param nom    nouveau nom
     * @param prenom nouveau prénom
     * @param email  nouvelle adresse email
     * @throws UtilisateurDejaExistantException si l'adresse email est déjà utilisée par un autre compte
     */
    public void modifierProfil(Long id, String nom, String prenom, String email) {
        if (!emailDisponiblePour(email, id)) {
            throw new UtilisateurDejaExistantException("email");
        }
        utilisateurRepository.modifierProfil(id, nom, prenom, email);
    }

    /**
     * Modifie le mot de passe d'un utilisateur après vérification de son mot de passe actuel.
     *
     * @param id      identifiant de l'utilisateur
     * @param actuel  mot de passe actuel en clair
     * @param nouveau nouveau mot de passe en clair à enregistrer
     * @throws MotDePasseIncorrectException si le mot de passe actuel est incorrect
     */
    public void changerMotDePasse(Long id, String actuel, String nouveau) {
        Utilisateur utilisateur = utilisateurRepository.findById(id).orElse(null);   // hash relu en base
        if (actuel == null || !BCrypt.checkpw(actuel, utilisateur.getMotDePasseHash())) {
            throw new MotDePasseIncorrectException();
        }
        utilisateurRepository.changerMotDePasse(id, hacher(nouveau));
    }

    /**
     * Hache un mot de passe en clair avec l'algorithme BCrypt.
     *
     * @param motDePasse mot de passe en clair
     * @return le mot de passe haché
     */
    /**
     * @throws UtilisateurDejaExistantException si l'identifiant (champ "identifiant") ou l'email ("email") est déjà pris
     */
    private void verifierDisponible(Utilisateur utilisateur) {
        if (utilisateurRepository.identifiantExiste(utilisateur.getIdentifiant())) {
            throw new UtilisateurDejaExistantException("identifiant");
        }
        if (utilisateurRepository.emailExiste(utilisateur.getEmail())) {
            throw new UtilisateurDejaExistantException("email");
        }
    }

    /** Identifiant ou email pris entre la vérification et l'enregistrement (inscription simultanée). */
    private static UtilisateurDejaExistantException doublon() {
        return new UtilisateurDejaExistantException(null);
    }

    /**
     * @return le hash BCrypt du mot de passe
     */
    private static String hacher(String motDePasse) {
        return BCrypt.hashpw(motDePasse, BCrypt.gensalt());
    }
}
