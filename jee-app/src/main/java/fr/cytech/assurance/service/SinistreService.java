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

    // Sinistres du client, du plus récent au plus ancien
    public List<Sinistre> listerDuClient(Long idClient) {
        return sinistreDao.listerParClient(idClient);
    }

    // Sinistre avec ses détails s'il appartient à ce client, sinon null (inexistant ou d'un autre client)
    public Sinistre trouverDuClient(Long idSinistre, Long idClient) {
        Sinistre sinistre = sinistreDao.trouverAvecDetails(idSinistre);
        return sinistre != null && sinistre.getContrat().getClient().getId().equals(idClient) ? sinistre : null;
    }

    // Sinistre avec ses détails (pour un agent ou l'admin), ou null s'il n'existe pas
    public Sinistre trouverAvecDetails(Long idSinistre) {
        return sinistreDao.trouverAvecDetails(idSinistre);
    }

    /**
     * Sinistres pour la liste des agents.
     *
     * @param statut  seulement ce statut (du plus ancien au plus récent : ordre de traitement), ou null pour tous
     * @param idAgent seulement les dossiers de cet agent, ou null pour tous
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

    // Discussion du sinistre, du plus ancien message au plus récent
    public List<CommentaireSinistre> discussion(Long idSinistre) {
        return commentaireDao.listerParSinistre(idSinistre);
    }

    // Indemnisation versée pour ce sinistre, ou null s'il n'a pas été indemnisé
    public Versement indemnisation(Long idSinistre) {
        return versementDao.trouverParSinistre(idSinistre);
    }

    // REFUSE et INDEMNISE sont définitifs : plus d'action ni de message possible
    public static boolean estClos(Sinistre sinistre) {
        return sinistre.getStatut() == StatutSinistre.REFUSE || sinistre.getStatut() == StatutSinistre.INDEMNISE;
    }

    // ---------- Déclaration par le client ----------

    /**
     * Contrats sur lesquels le client peut déclarer un sinistre : payés et déjà commencés
     * (un contrat expiré ou résilié reste proposé, pour un sinistre survenu pendant sa période de couverture).
     * Les contrats actifs sont en premier.
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
     * Déclare un sinistre sur l'un des contrats du client.
     *
     * @param idClient      client connecté (vient de la session)
     * @param idContrat     contrat concerné
     * @param dateSinistre  date à laquelle le sinistre a eu lieu
     * @param type          type de sinistre
     * @param description   circonstances, déjà nettoyées et de longueur vérifiée
     * @param montantEstime estimation des dommages, ou null
     * @return le sinistre enregistré, avec son numéro
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

    // Un contrat couvre du jour de début jusqu'à la veille de sa fin, ou jusqu'au jour de sa résiliation
    static boolean couvre(Contrat contrat, LocalDate date) {
        return !date.isBefore(contrat.getDateDebut()) && !date.isAfter(finDeCouverture(contrat));
    }

    private static LocalDate finDeCouverture(Contrat contrat) {
        LocalDate veilleDeFin = contrat.getDateFin().minusDays(1);
        LocalDate resiliation = contrat.getDateResiliation();
        return resiliation != null && resiliation.isBefore(veilleDeFin) ? resiliation : veilleDeFin;
    }

    // ---------- Traitement par un agent ----------

    // DECLARE → EN_ANALYSE : l'agent devient responsable du dossier
    public void prendreEnCharge(Long idSinistre, Utilisateur agent) {
        faireAvancer(idSinistre, agent, StatutSinistre.DECLARE, StatutSinistre.EN_ANALYSE, null,
                "Seul un sinistre déclaré peut être pris en charge.");
    }

    // EN_ANALYSE → ACCEPTE : le sinistre est couvert, l'indemnisation reste à enregistrer
    public void accepter(Long idSinistre, Utilisateur agent) {
        faireAvancer(idSinistre, agent, StatutSinistre.EN_ANALYSE, StatutSinistre.ACCEPTE, null,
                "Seul un sinistre en analyse peut être accepté.");
    }

    // EN_ANALYSE → REFUSE, définitif : le motif est obligatoire et ajouté à la discussion, pour que le client le lise
    public void refuser(Long idSinistre, Utilisateur agent, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new SinistreInvalideException("Le motif du refus est obligatoire.");
        }
        faireAvancer(idSinistre, agent, StatutSinistre.EN_ANALYSE, StatutSinistre.REFUSE,
                "Sinistre refusé. Motif : " + motif,
                "Seul un sinistre en analyse peut être refusé.");
    }

    /**
     * ACCEPTE → INDEMNISE, définitif : enregistre le versement au client, daté du jour.
     *
     * @param montant montant versé, strictement positif (vérifié par le Validateur)
     */
    public Versement indemniser(Long idSinistre, Utilisateur agent, BigDecimal montant) {
        verifierAgent(agent);
        if (montant == null || montant.signum() <= 0) {
            throw new SinistreInvalideException("Le montant de l'indemnisation doit être supérieur à 0.");
        }
        Versement versement = idSinistre == null ? null
                : sinistreDao.indemniser(idSinistre, montant, LocalDate.now(), agent.getId());
        if (versement == null) {
            throw new SinistreInvalideException("Seul un sinistre accepté peut être indemnisé.");
        }
        return versement;
    }

    /**
     * Garanties du contrat du sinistre, chacune avec l'indemnisation suggérée à l'agent :
     * montant estimé moins la franchise, dans la limite du plafond (null si le client n'a pas estimé ses dommages).
     * C'est une aide : l'agent choisit la garantie qui s'applique et décide du montant.
     */
    public List<Proposition> propositions(Sinistre sinistre) {
        BigDecimal estime = sinistre.getMontantEstime();
        // Les garanties ne sont pas chargées avec le sinistre : on relit le contrat avec ses garanties
        return contratDao.trouverAvecDetails(sinistre.getContrat().getId()).getGaranties().stream()
                .sorted(Comparator.comparing(Garantie::getNom))
                .map(g -> new Proposition(g, estime == null ? null : calculer(estime, g)))
                .toList();
    }

    static BigDecimal calculer(BigDecimal estime, Garantie garantie) {
        BigDecimal montant = garantie.getFranchise() == null ? estime : estime.subtract(garantie.getFranchise());
        if (garantie.getPlafond() != null && montant.compareTo(garantie.getPlafond()) > 0) {
            montant = garantie.getPlafond();
        }
        return montant.max(BigDecimal.ZERO);
    }

    private void faireAvancer(Long idSinistre, Utilisateur agent, StatutSinistre attendu, StatutSinistre nouveau,
                              String commentaire, String messageSiImpossible) {
        verifierAgent(agent);
        if (idSinistre == null || !sinistreDao.faireAvancer(idSinistre, attendu, nouveau, agent.getId(), commentaire)) {
            throw new SinistreInvalideException(messageSiImpossible);
        }
    }

    // Seul un agent traite les sinistres : l'admin les consulte seulement
    private static void verifierAgent(Utilisateur utilisateur) {
        if (utilisateur == null || utilisateur.getRole() != Role.AGENT) {
            throw new SinistreInvalideException("Seul un agent peut traiter un sinistre.");
        }
    }

    // ---------- Discussion ----------

    /**
     * Ajoute un message à la discussion d'un sinistre.
     * Le client n'écrit que sur ses propres sinistres, un agent sur tous ; l'admin ne fait que lire.
     * Un dossier clos (refusé ou indemnisé) n'accepte plus de message.
     *
     * @param idClient client connecté si l'auteur est un client (vient de la session), sinon ignoré
     * @param contenu  message déjà nettoyé, non vide et de longueur vérifiée
     */
    public CommentaireSinistre commenter(Long idSinistre, Utilisateur auteur, Long idClient, String contenu) {
        Sinistre sinistre = ouvertEnEcriture(idSinistre, auteur, idClient, "la discussion est fermée");
        return commentaireDao.ajouter(sinistre.getId(), auteur.getId(), contenu);
    }

    /**
     * Sinistre que cet utilisateur peut compléter (message, pièce justificative) : règle commune à la discussion
     * et aux pièces. Le client ne complète que ses propres sinistres, un agent tous ; l'admin ne fait que lire.
     *
     * @param idClient  client connecté si l'utilisateur est un client (vient de la session), sinon ignoré
     * @param siClos    fin du message d'erreur si le dossier est clos ("la discussion est fermée"...)
     * @return le sinistre avec ses détails
     * @throws SinistreInvalideException si le sinistre est inconnu, n'est pas accessible ou est clos
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

    // Indemnisation suggérée pour une garantie (classe et non record : lisible par l'EL des JSP de Tomcat 10)
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
