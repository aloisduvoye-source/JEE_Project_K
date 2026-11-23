package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.CommentaireSinistreDao;
import fr.cytech.assurance.dao.ContratDao;
import fr.cytech.assurance.dao.PaiementDao;
import fr.cytech.assurance.dao.SinistreDao;
import fr.cytech.assurance.dao.VersementDao;
import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.CommentaireSinistre;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutSinistre;
import fr.cytech.assurance.model.enums.TypeSinistre;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Règles métier des sinistres : déclaration par le client, traitement par un agent, discussion entre eux.
 * <p>
 * Cycle de vie : DECLARE → (prise en charge) EN_ANALYSE → ACCEPTE ou REFUSE → (indemnisation) INDEMNISE.
 * REFUSE et INDEMNISE sont définitifs : le dossier est clos, sa discussion aussi.
 * <p>
 * Les servlets vérifient le format des saisies (Validateur), puis appellent ce service.
 * Toute règle non respectée lève {@link SinistreInvalideException}, dont le message est affichable.
 */
public class SinistreService {

    public static final int LONGUEUR_MAX_DESCRIPTION = 2000;
    public static final int LONGUEUR_MAX_COMMENTAIRE = 2000;

    private final SinistreDao sinistreDao = new SinistreDao();
    private final ContratDao contratDao = new ContratDao();
    private final PaiementDao paiementDao = new PaiementDao();
    private final CommentaireSinistreDao commentaireDao = new CommentaireSinistreDao();
    private final VersementDao versementDao = new VersementDao();

    // ---------- Consultation ----------

    /**
     * Liste les sinistres d'un client, du plus récent au plus ancien.
     *
     * @param idClient identifiant du client
     * @return la liste des sinistres du client
     */
    public List<Sinistre> listerDuClient(Long idClient) {
        return sinistreDao.listerParClient(idClient);
    }

    /**
     * Recherche un sinistre avec ses détails uniquement s'il appartient bien au client donné.
     *
     * @param idSinistre identifiant du sinistre
     * @param idClient   identifiant du client
     * @return le sinistre avec ses détails, ou null s'il n'existe pas ou appartient à un autre client
     */
    public Sinistre trouverDuClient(Long idSinistre, Long idClient) {
        Sinistre sinistre = sinistreDao.trouverAvecDetails(idSinistre);
        return sinistre != null && sinistre.getContrat().getClient().getId().equals(idClient) ? sinistre : null;
    }

    /**
     * Recherche un sinistre avec tous ses détails (pour un agent ou un administrateur).
     *
     * @param idSinistre identifiant du sinistre
     * @return le sinistre avec ses détails, ou null s'il n'existe pas
     */
    public Sinistre trouverAvecDetails(Long idSinistre) {
        return sinistreDao.trouverAvecDetails(idSinistre);
    }

    /**
     * Liste les sinistres pour les agents avec filtres optionnels.
     *
     * @param statut  statut recherché (du plus ancien au plus récent), ou null pour tous
     * @param idAgent identifiant de l'agent responsable, ou null pour tous les dossiers
     * @return la liste des sinistres correspondants
     */
    public List<Sinistre> lister(StatutSinistre statut, Long idAgent) {
        List<Sinistre> sinistres = idAgent != null ? sinistreDao.listerParAgent(idAgent)
                : statut != null ? sinistreDao.listerParStatut(statut)
                : sinistreDao.listerTousAvecDetails();
        if (idAgent != null && statut != null) {
            sinistres = sinistres.stream().filter(s -> s.getStatut() == statut).toList();
        }
        return sinistres;
    }

    /**
     * Récupère les messages de la discussion d'un sinistre, du plus ancien au plus récent.
     *
     * @param idSinistre identifiant du sinistre
     * @return la liste des commentaires du sinistre
     */
    public List<CommentaireSinistre> discussion(Long idSinistre) {
        return commentaireDao.listerParSinistre(idSinistre);
    }

    /**
     * Récupère le versement d'indemnisation effectué pour un sinistre.
     *
     * @param idSinistre identifiant du sinistre
     * @return le versement associé, ou null si le sinistre n'a pas été indemnisé
     */
    public Versement indemnisation(Long idSinistre) {
        return versementDao.trouverParSinistre(idSinistre);
    }

    /**
     * Indique si un sinistre est définitivement clos (statut REFUSE ou INDEMNISE).
     *
     * @param sinistre sinistre à vérifier
     * @return true si le dossier est clos, false sinon
     */
    public static boolean estClos(Sinistre sinistre) {
        return sinistre.getStatut() == StatutSinistre.REFUSE || sinistre.getStatut() == StatutSinistre.INDEMNISE;
    }

    // ---------- Déclaration par le client ----------

    /**
     * Liste les contrats sur lesquels un client peut déclarer un sinistre (payés et déjà commencés).
     * Les contrats actifs sont placés en premier, suivis des contrats expirés ou résiliés.
     *
     * @param idClient identifiant du client
     * @return la liste des contrats éligibles à une déclaration
     */
    public List<Contrat> contratsDeclarables(Long idClient) {
        Set<Long> payes = paiementDao.idsContratsPayes();
        LocalDate aujourdhui = LocalDate.now();
        return contratDao.listerParClient(idClient).stream()
                .filter(c -> payes.contains(c.getId()) && !c.getDateDebut().isAfter(aujourdhui))
                .sorted(Comparator.comparing((Contrat c) -> c.getStatut() != StatutContrat.ACTIF))
                .toList();
    }

    /**
     * Déclare un nouveau sinistre sur l'un des contrats du client.
     *
     * @param idClient      identifiant du client connecté
     * @param idContrat     identifiant du contrat concerné
     * @param dateSinistre  date à laquelle le sinistre s'est produit
     * @param type          type de sinistre
     * @param description   description des circonstances
     * @param montantEstime montant estimé des dommages (ou null)
     * @return le sinistre enregistré avec son numéro
     * @throws SinistreInvalideException si le contrat est introuvable ou non payé,
     *                                   si la date est dans le futur, ou si le contrat ne couvrait pas cette date
     */
    public Sinistre declarer(Long idClient, Long idContrat, LocalDate dateSinistre, TypeSinistre type,
                             String description, BigDecimal montantEstime) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverParId(idContrat);
        // Même message pour un contrat d'un autre client : on ne révèle pas qu'il existe
        if (contrat == null || !contrat.getClient().getId().equals(idClient)) {
            throw new SinistreInvalideException("Contrat inconnu.");
        }
        if (!paiementDao.idsContratsPayes().contains(contrat.getId())) {
            throw new SinistreInvalideException("Ce contrat n'a pas été payé : il ne couvre aucun sinistre.");
        }
        if (dateSinistre.isAfter(LocalDate.now())) {
            throw new SinistreInvalideException("La date du sinistre ne peut pas être dans le futur.");
        }
        // Suspension décidée par un agent : plus de couverture à partir de sa date, tant qu'elle dure
        if (contrat.getDateSuspension() != null && !dateSinistre.isBefore(contrat.getDateSuspension())) {
            throw new SinistreInvalideException("Votre contrat est suspendu depuis le " + contrat.getDateSuspension()
                    + " : il ne couvre pas un sinistre survenu à partir de cette date.");
        }
        if (!couvre(contrat, dateSinistre)) {
            throw new SinistreInvalideException("Votre contrat ne couvrait pas cette date (du "
                    + contrat.getDateDebut() + " au " + finDeCouverture(contrat) + ").");
        }

        Sinistre sinistre = new Sinistre();
        sinistre.setDateSinistre(dateSinistre);
        sinistre.setTypeSinistre(type);
        sinistre.setDescription(description);
        sinistre.setMontantEstime(montantEstime);
        return sinistreDao.declarer(sinistre, contrat.getId());
    }

    /**
     * Vérifie si un contrat couvre une date donnée (entre la date de début et la fin de couverture).
     *
     * @param contrat contrat à vérifier
     * @param date    date du sinistre
     * @return true si le contrat était actif à cette date, false sinon
     */
    static boolean couvre(Contrat contrat, LocalDate date) {
        return !date.isBefore(contrat.getDateDebut()) && !date.isAfter(finDeCouverture(contrat));
    }

    /**
     * Détermine le dernier jour de couverture d'un contrat (veille de la date de fin ou date de résiliation).
     *
     * @param contrat contrat concerné
     * @return la date de fin effective de couverture
     */
    private static LocalDate finDeCouverture(Contrat contrat) {
        LocalDate veilleDeFin = contrat.getDateFin().minusDays(1);
        LocalDate resiliation = contrat.getDateResiliation();
        return resiliation != null && resiliation.isBefore(veilleDeFin) ? resiliation : veilleDeFin;
    }

    // ---------- Traitement par un agent ----------

    // DECLARE → EN_ANALYSE : l'agent devient responsable du dossier

    /**
     * Fait passer un sinistre de DECLARE à EN_ANALYSE et attribue le dossier à l'agent.
     *
     * @param idSinistre identifiant du sinistre
     * @param agent      agent qui prend en charge le dossier
     * @throws SinistreInvalideException si l'utilisateur n'est pas un agent ou si le sinistre n'est pas en statut DECLARE
     */
    public void prendreEnCharge(Long idSinistre, Utilisateur agent) {
        faireAvancer(idSinistre, agent, StatutSinistre.DECLARE, StatutSinistre.EN_ANALYSE, null,
                "Seul un sinistre déclaré peut être pris en charge.");
    }

    // EN_ANALYSE → ACCEPTE : le sinistre est couvert, l'indemnisation reste à enregistrer

    /**
     * Fait passer un sinistre de EN_ANALYSE à ACCEPTE (l'indemnisation restera ensuite à calculer).
     *
     * @param idSinistre identifiant du sinistre
     * @param agent      agent qui accepte le dossier
     * @throws SinistreInvalideException si l'utilisateur n'est pas un agent ou si le sinistre n'est pas en analyse
     */
    public void accepter(Long idSinistre, Utilisateur agent) {
        faireAvancer(idSinistre, agent, StatutSinistre.EN_ANALYSE, StatutSinistre.ACCEPTE, null,
                "Seul un sinistre en analyse peut être accepté.");
    }

    // EN_ANALYSE → REFUSE, définitif : le motif est obligatoire et ajouté à la discussion, pour que le client le lise
    
    /**
     * Fait passer définitivement un sinistre de EN_ANALYSE à REFUSE et ajoute le motif à la discussion.
     *
     * @param idSinistre identifiant du sinistre
     * @param agent      agent qui refuse le dossier
     * @param motif      raison du refus (obligatoire, visible par le client)
     * @throws SinistreInvalideException si le motif est vide, si l'utilisateur n'est pas un agent
     *                                   ou si le sinistre n'est pas en analyse
     */
    public void refuser(Long idSinistre, Utilisateur agent, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new SinistreInvalideException("Le motif du refus est obligatoire.");
        }
        faireAvancer(idSinistre, agent, StatutSinistre.EN_ANALYSE, StatutSinistre.REFUSE,
                "Sinistre refusé. Motif : " + motif,
                "Seul un sinistre en analyse peut être refusé.");
    }

    /**
     * Fait passer définitivement un sinistre de ACCEPTE à INDEMNISE, calcule le montant
     * selon la garantie choisie (dommage - franchise, limité au plafond) et enregistre le versement.
     *
     * @param idSinistre identifiant du sinistre accepté
     * @param agent      agent qui valide l'indemnisation
     * @param idGarantie identifiant de la garantie du contrat appliquée
     * @param dommage    montant du dommage retenu par l'agent (strictement positif)
     * @return le versement enregistré
     * @throws SinistreInvalideException si l'utilisateur n'est pas un agent, si le dommage est invalide
     *                                   ou inférieur à la franchise, si le sinistre n'est pas accepté,
     *                                   ou si la garantie n'appartient pas au contrat
     */
    public Versement indemniser(Long idSinistre, Utilisateur agent, Long idGarantie, BigDecimal dommage) {
        verifierAgent(agent);
        if (dommage == null || dommage.signum() <= 0) {
            throw new SinistreInvalideException("Le dommage retenu doit être supérieur à 0.");
        }
        Sinistre sinistre = idSinistre == null ? null : sinistreDao.trouverAvecDetails(idSinistre);
        if (sinistre == null || sinistre.getStatut() != StatutSinistre.ACCEPTE) {
            throw new SinistreInvalideException("Seul un sinistre accepté peut être indemnisé.");
        }
        Garantie garantie = contratDao.trouverAvecDetails(sinistre.getContrat().getId()).getGaranties().stream()
                .filter(g -> g.getId().equals(idGarantie))
                .findFirst()
                .orElseThrow(() -> new SinistreInvalideException("Cette garantie ne fait pas partie du contrat."));

        BigDecimal franchise = garantie.getFranchise() == null ? BigDecimal.ZERO : garantie.getFranchise();
        BigDecimal montant = calculer(dommage, garantie);
        if (montant.signum() <= 0) {
            throw new SinistreInvalideException("Le dommage retenu (" + dommage.toPlainString()
                    + " €) ne dépasse pas la franchise de la garantie " + garantie.getNom() + " ("
                    + franchise.toPlainString() + " €) : il n'y a rien à verser. "
                    + "Ce sinistre ne peut pas être indemnisé : refusez-le avec ce motif.");
        }

        Versement versement = new Versement();
        versement.setMontant(montant);
        versement.setMontantDommage(dommage);
        versement.setFranchiseAppliquee(franchise);
        versement.setPlafondApplique(garantie.getPlafond());
        versement.setDateVersement(LocalDate.now());
        Versement enregistre = sinistreDao.indemniser(sinistre.getId(), versement, garantie.getId(), agent.getId());
        if (enregistre == null) {
            // Un autre agent l'a indemnisé entre la vérification et l'enregistrement
            throw new SinistreInvalideException("Seul un sinistre accepté peut être indemnisé.");
        }
        return enregistre;
    }

    /**
     * Calcule pour chaque garantie du contrat l'indemnisation estimée à partir du montant déclaré par le client.
     *
     * @param sinistre sinistre concerné
     * @return la liste des garanties associées au montant d'indemnisation calculé (ou null si non estimé)
     */
    public List<Proposition> propositions(Sinistre sinistre) {
        BigDecimal estime = sinistre.getMontantEstime();
        // Les garanties ne sont pas chargées avec le sinistre : on relit le contrat avec ses garanties
        return contratDao.trouverAvecDetails(sinistre.getContrat().getId()).getGaranties().stream()
                .sorted(Comparator.comparing(Garantie::getNom))
                .map(g -> new Proposition(g, estime == null ? null : calculer(estime, g)))
                .toList();
    }

    /**
     * Calcule le montant de l'indemnisation selon la formule : min(dommage - franchise, plafond), jamais négatif.
     *
     * @param dommage  montant du dommage
     * @param garantie garantie appliquée (contenant franchise et plafond)
     * @return le montant final à indemniser
     */
    static BigDecimal calculer(BigDecimal dommage, Garantie garantie) {
        BigDecimal montant = garantie.getFranchise() == null ? dommage : dommage.subtract(garantie.getFranchise());
        if (garantie.getPlafond() != null && montant.compareTo(garantie.getPlafond()) > 0) {
            montant = garantie.getPlafond();
        }
        return montant.max(BigDecimal.ZERO);
    }

    /**
     * Vérifie que l'utilisateur est un agent et fait évoluer le statut du sinistre en base.
     *
     * @param idSinistre          identifiant du sinistre
     * @param agent               agent effectuant l'action
     * @param attendu             statut actuel requis
     * @param nouveau             nouveau statut à appliquer
     * @param commentaire         message optionnel à ajouter à la discussion
     * @param messageSiImpossible message d'erreur si la transition échoue
     * @throws SinistreInvalideException si l'utilisateur n'est pas un agent ou si le changement de statut est impossible
     */
    private void faireAvancer(Long idSinistre, Utilisateur agent, StatutSinistre attendu, StatutSinistre nouveau,
                              String commentaire, String messageSiImpossible) {
        verifierAgent(agent);
        if (idSinistre == null || !sinistreDao.faireAvancer(idSinistre, attendu, nouveau, agent.getId(), commentaire)) {
            throw new SinistreInvalideException(messageSiImpossible);
        }
    }

    /**
     * Vérifie que l'utilisateur connecté possède bien le rôle AGENT.
     *
     * @param utilisateur utilisateur à contrôler
     * @throws SinistreInvalideException si l'utilisateur est null ou n'est pas un agent
     */
    private static void verifierAgent(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() != Role.AGENT) {
            throw new SinistreInvalideException("Seul un agent peut traiter un sinistre.");
        }
    }

    // ---------- Discussion ----------

    /**
     * Ajoute un message dans la discussion d'un sinistre ouvert (client titulaire ou agent uniquement).
     *
     * @param idSinistre identifiant du sinistre
     * @param auteur     utilisateur qui écrit le message
     * @param idClient   identifiant du client connecté (si l'auteur est un client), sinon ignoré
     * @param contenu    texte du message à publier
     * @return le commentaire enregistré
     * @throws SinistreInvalideException si le sinistre est introuvable, non autorisé ou déjà clos
     */
    public CommentaireSinistre commenter(Long idSinistre, Utilisateur auteur, Long idClient, String contenu) {
        Sinistre sinistre = ouvertEnEcriture(idSinistre, auteur, idClient, "la discussion est fermée");
        return commentaireDao.ajouter(sinistre.getId(), auteur.getId(), contenu);
    }

    /**
     * Vérifie qu'un sinistre existe, qu'il est encore ouvert et que l'utilisateur a le droit de le modifier.
     *
     * @param idSinistre  identifiant du sinistre
     * @param utilisateur utilisateur connecté
     * @param idClient    identifiant du client connecté (si l'utilisateur est un client), sinon ignoré
     * @param siClos      fin du message d'erreur affiché si le dossier est clos
     * @return le sinistre avec ses détails
     * @throws SinistreInvalideException si le sinistre est inconnu, inaccessible ou clos
     */
    public Sinistre ouvertEnEcriture(Long idSinistre, Utilisateur utilisateur, Long idClient, String siClos) {
        Sinistre sinistre = idSinistre == null ? null : sinistreDao.trouverAvecDetails(idSinistre);
        boolean autorise = sinistre != null && switch (utilisateur.getRole()) {
            case CLIENT -> sinistre.getContrat().getClient().getId().equals(idClient);
            case AGENT -> true;
            case ADMIN -> false;
        };
        // Même message pour un sinistre d'un autre client : on ne révèle pas qu'il existe
        if (!autorise) {
            throw new SinistreInvalideException("Sinistre inconnu.");
        }
        if (estClos(sinistre)) {
            throw new SinistreInvalideException("Ce dossier est clos : " + siClos + ".");
        }
        return sinistre;
    }

    /**
     * Représente une proposition d'indemnisation associant une garantie au montant estimé.
     */
    public static final class Proposition {
        private final Garantie garantie;
        private final BigDecimal montant;

        Proposition(Garantie garantie, BigDecimal montant) {
            this.garantie = garantie;
            this.montant = montant;
        }

        public Garantie getGarantie() {return garantie;}
        public BigDecimal getMontant() {return montant;}
    }
}
