package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.ContratDao;
import fr.cytech.assurance.dao.PaiementDao;
import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.dao.VersementDao;
import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;

import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Règles métier des contrats. Les servlets vérifient le format des saisies (Validateur),
 * puis appellent ce service, qui applique les règles et passe par les DAO.
 */
public class ContratService {

    // Tous les contrats durent un an : la date de fin est calculée, l'agent ne la saisit pas.
    // Le prix de base d'un type d'assurance et la prime d'un contrat sont donc des montants annuels
    public static final int DUREE_EN_MOIS = 12;

    private final ContratDao contratDao = new ContratDao();
    private final ClientDao clientDao = new ClientDao();
    private final TypeAssuranceDao typeAssuranceDao = new TypeAssuranceDao();
    private final PaiementDao paiementDao = new PaiementDao();
    private final VersementDao versementDao = new VersementDao();

    // Tous les contrats, du plus récent au plus ancien
    public List<Contrat> listerContrats() {
        return contratDao.listerTousAvecDetails();
    }

    // Contrat avec son client, son type et ses garanties, ou null s'il n'existe pas
    public Contrat trouverAvecDetails(Long id) {
        return contratDao.trouverAvecDetails(id);
    }

    // Contrat avec ses détails s'il appartient à ce client, sinon null (contrat inexistant ou d'un autre client)
    public Contrat trouverDuClient(Long idContrat, Long idClient) {
        Contrat contrat = contratDao.trouverAvecDetails(idContrat);
        return contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null;
    }

    /**
     * Recherche et tri de contrats pour les agents (§2.2, « rechercher un contrat »). Critères facultatifs (null : ignoré).
     *
     * @param texte  numéro de contrat, ou nom, prénom ou email du client (en partie)
     * @param statut statut des contrats
     * @param idType type d'assurance
     * @param tri    critère de tri (debut_desc, debut_asc, fin_asc, fin_desc, prime_desc, prime_asc)
     */
    public List<Contrat> rechercher(String texte, StatutContrat statut, Long idType, String tri) {
        String nettoye = texte == null || texte.isBlank() ? null : texte.trim();
        List<Contrat> resultats = (nettoye == null && statut == null && idType == null)
                ? listerContrats()
                : contratDao.rechercher(nettoye, statut, idType);
        return trierContrats(resultats, tri);
    }

    /**
     * Contrats d'un client, avec leur type et leurs garanties, filtrés et triés. Critères facultatifs (null : ignoré).
     */
    public List<Contrat> listerContratsDuClient(Long idClient, StatutContrat statut, Long idType, String tri) {
        if (idClient == null) {
            return List.of();
        }
        Stream<Contrat> flux = contratDao.listerParClient(idClient).stream();
        if (statut != null) {
            flux = flux.filter(c -> c.getStatut() == statut);
        }
        if (idType != null) {
            flux = flux.filter(c -> c.getTypeAssurance() != null && idType.equals(c.getTypeAssurance().getId()));
        }
        return trierContrats(flux.toList(), tri);
    }

    private List<Contrat> trierContrats(List<Contrat> contrats, String tri) {
        Comparator<Contrat> comparateur = switch (tri == null ? "" : tri) {
            case "debut_asc" -> Comparator.comparing(Contrat::getDateDebut);
            case "fin_asc" -> Comparator.comparing(Contrat::getDateFin);
            case "fin_desc" -> Comparator.comparing(Contrat::getDateFin).reversed();
            case "prime_asc" -> Comparator.comparing(Contrat::getPrime);
            case "prime_desc" -> Comparator.comparing(Contrat::getPrime).reversed();
            default -> Comparator.comparing(Contrat::getDateDebut).reversed()
                    .thenComparing(Comparator.comparing(Contrat::getId).reversed());
        };
        return contrats.stream().sorted(comparateur).toList();
    }

    // Contrat qui renouvelle celui-ci, ou null s'il n'a pas été renouvelé
    public Contrat trouverRenouvellement(Long idContrat) {
        return contratDao.trouverRenouvellement(idContrat);
    }

    // Remboursement de prime versé à la résiliation du contrat, ou null s'il n'y en a pas eu
    public Versement trouverRemboursement(Long idContrat) {
        return versementDao.trouverRemboursement(idContrat);
    }

    /**
     * Crée un contrat en EN_ATTENTE, avec un numéro attribué automatiquement.
     * Il dure DUREE_EN_MOIS à partir de dateDebut : la date de fin est calculée ici.
     * Son paiement unique (la prime entière, à régler avant dateDebut) est créé en même temps.
     * Lève ContratInvalideException (message affichable) si une règle n'est pas respectée.
     */
    public Contrat creerContrat(Long idClient, Long idTypeAssurance, Set<Long> idsGaranties,
                                LocalDate dateDebut, BigDecimal prime) {
        Client client = idClient == null ? null : clientDao.trouverParId(idClient);
        if (client == null) {
            throw new ContratInvalideException("Client inconnu.");
        }
        if (!client.getUtilisateur().isActif()) {
            throw new ContratInvalideException("Le compte de ce client est désactivé.");
        }

        TypeAssurance type = idTypeAssurance == null ? null : typeAssuranceDao.trouverAvecGaranties(idTypeAssurance);
        if (type == null) {
            throw new ContratInvalideException("Type d'assurance inconnu.");
        }
        // Un type archivé n'est plus proposé : il reste seulement lié aux contrats déjà existants
        if (!type.isActif()) {
            throw new ContratInvalideException("Ce type d'assurance est archivé : il n'est plus proposé.");
        }

        verifierGarantiesDateEtPrime(type, idsGaranties, dateDebut, prime, true);

        Contrat contrat = new Contrat();   // statut EN_ATTENTE par défaut
        contrat.setDateDebut(dateDebut);
        contrat.setDateFin(dateFin(dateDebut));
        contrat.setPrime(prime);
        return contratDao.creerAvecGaranties(contrat, idClient, idTypeAssurance, idsGaranties);
    }

    /**
     * Modifie un contrat pas encore payé (EN_ATTENTE ou SUSPENDU) : garanties, date de début, prime.
     * Le client et le type ne changent pas (sinon, c'est un autre contrat). La date de fin est recalculée,
     * et le paiement suit : nouveau montant, nouvelle échéance, de nouveau EN_ATTENTE.
     * Un contrat payé ne peut plus être modifié : il n'y a qu'un seul paiement, son prix ne peut plus changer.
     * Lève ContratInvalideException (message affichable) si une règle n'est pas respectée.
     */
    public void modifierContrat(Long idContrat, Set<Long> idsGaranties, LocalDate dateDebut, BigDecimal prime) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverAvecDetails(idContrat);
        if (contrat == null) {
            throw new ContratInvalideException("Contrat inconnu.");
        }
        if (!estModifiable(contrat)) {
            throw new ContratInvalideException("Seul un contrat en attente ou suspendu, pas encore payé, peut être modifié.");
        }
        if (estPaye(idContrat)) {
            throw new ContratInvalideException("Ce contrat a été payé : il ne peut plus être modifié.");
        }
        TypeAssurance type = typeAssuranceDao.trouverAvecGaranties(contrat.getTypeAssurance().getId());
        // Prix de base vérifié seulement si la prime change : une hausse du prix de base
        // ne s'applique qu'aux nouveaux contrats (page de modification d'un type d'assurance)
        verifierGarantiesDateEtPrime(type, idsGaranties, dateDebut, prime, prime != null
                && prime.compareTo(contrat.getPrime()) != 0);

        contratDao.modifierAvantPaiement(idContrat, idsGaranties, dateDebut, dateFin(dateDebut), prime);
        // Un contrat suspendu dont la date de début est repoussée redevient EN_ATTENTE
        mettreAJourStatuts(LocalDate.now());
    }

    // Vrai si le statut permet une modification (le paiement est vérifié à part, avec estPaye)
    public static boolean estModifiable(Contrat contrat) {
        return contrat.getStatut() == StatutContrat.EN_ATTENTE || contrat.getStatut() == StatutContrat.SUSPENDU;
    }

    // Vrai si le paiement du contrat a été effectué
    public boolean estPaye(Long idContrat) {
        return paiementDao.listerParContrat(idContrat).stream().anyMatch(p -> p.getStatut() == StatutPaiement.EFFECTUE);
    }

    // Règles communes à la création et à la modification : garanties, date de début, prime
    private static void verifierGarantiesDateEtPrime(TypeAssurance type, Set<Long> idsGaranties,
                                                     LocalDate dateDebut, BigDecimal prime, boolean verifierPrixBase) {
        // Le sujet : un contrat contient une ou plusieurs garanties
        if (idsGaranties == null || idsGaranties.isEmpty()) {
            throw new ContratInvalideException("Choisissez au moins une garantie.");
        }
        // Seules les garanties proposées par ce type d'assurance sont acceptées
        Set<Long> disponibles = type.getGaranties().stream().map(Garantie::getId).collect(Collectors.toSet());
        if (!disponibles.containsAll(idsGaranties)) {
            throw new ContratInvalideException("Une des garanties choisies n'est pas proposée pour l'"
                    + type.getNom().toLowerCase() + ".");
        }

        if (dateDebut == null) {
            throw new ContratInvalideException("La date de début est obligatoire.");
        }
        // Un contrat commencé et pas encore payé serait suspendu dès sa création
        if (dateDebut.isBefore(LocalDate.now())) {
            throw new ContratInvalideException("La date de début ne peut pas être dans le passé.");
        }

        if (prime == null || prime.signum() <= 0) {
            throw new ContratInvalideException("La prime doit être supérieure à zéro.");
        }
        // Le prix de base du type est un minimum : l'agent peut l'augmenter (garanties, profil), pas le baisser
        if (verifierPrixBase && prime.compareTo(type.getPrixBase()) < 0) {
            throw new ContratInvalideException("La prime ne peut pas être inférieure au prix de base de l'"
                    + type.getNom().toLowerCase() + " (" + type.getPrixBase().toPlainString() + " €).");
        }
    }

    // ---------- Résiliation ----------

    // Résiliation par un agent, de n'importe quel contrat en cours
    // Lève ContratInvalideException si le contrat n'existe pas ou est déjà terminé
    public void resilierParAgent(Long idContrat, String motif, Long idAgent) {
        resilier(idContrat == null ? null : contratDao.trouverParId(idContrat), motif, idAgent);
    }

    // Résiliation par le client, seulement de l'un de ses propres contrats
    // Lève ContratInvalideException si le contrat n'est pas le sien, n'existe pas ou est déjà terminé
    public void resilierParClient(Long idContrat, Long idClient, String motif) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverParId(idContrat);
        // Même message pour un contrat d'un autre client : on ne révèle pas qu'il existe
        resilier(contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null, motif, null);
    }

    // Un contrat EXPIRE ou RESILIE est terminé : il ne peut plus être résilié
    private void resilier(Contrat contrat, String motif, Long idAgent) {
        if (contrat == null) {
            throw new ContratInvalideException("Contrat inconnu.");
        }
        if (contrat.getStatut() == StatutContrat.RESILIE) {
            throw new ContratInvalideException("Ce contrat est déjà résilié.");
        }
        if (contrat.getStatut() == StatutContrat.EXPIRE) {
            throw new ContratInvalideException("Ce contrat est expiré : il ne peut plus être résilié.");
        }
        LocalDate aujourdhui = LocalDate.now();
        if (!contratDao.resilier(contrat.getId(), aujourdhui, motif == null || motif.isBlank() ? null : motif,
                remboursement(contrat, aujourdhui), idAgent)) {
            throw new ContratInvalideException("Ce contrat est déjà terminé.");
        }
    }

    /**
     * Remboursement dû si le contrat est résilié à cette date : la part de la prime payée qui correspond
     * à la période qui ne sera plus couverte (remboursement au prorata temporis).
     * <ul>
     *     <li>contrat pas payé : rien (son paiement est annulé à la résiliation) ;</li>
     *     <li>résilié avant sa date de début : toute la prime ;</li>
     *     <li>sinon : prime × jours restants / jours du contrat. Le jour de la résiliation est encore couvert.</li>
     * </ul>
     * Arrondi au centime inférieur.
     *
     * @return le montant à rembourser, zéro s'il n'y a rien à rembourser
     */
    public BigDecimal remboursement(Contrat contrat, LocalDate dateResiliation) {
        BigDecimal paye = paiementDao.listerParContrat(contrat.getId()).stream()
                .filter(p -> p.getStatut() == StatutPaiement.EFFECTUE)
                .map(Paiement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return calculerRemboursement(paye, contrat.getDateDebut(), contrat.getDateFin(), dateResiliation);
    }

    // Couvert du jour de début à la veille de la fin : (fin - début) jours, dont (fin - résiliation - 1) non couverts
    static BigDecimal calculerRemboursement(BigDecimal paye, LocalDate debut, LocalDate fin, LocalDate dateResiliation) {
        if (paye.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (dateResiliation.isBefore(debut)) {
            return paye;
        }
        long total = ChronoUnit.DAYS.between(debut, fin);
        long restants = ChronoUnit.DAYS.between(dateResiliation, fin) - 1;
        if (total <= 0 || restants <= 0) {
            return BigDecimal.ZERO;
        }
        return paye.multiply(BigDecimal.valueOf(restants))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.DOWN);
    }

    // ---------- Suspension par un agent ----------

    /**
     * Suspend un contrat ACTIF sur décision d'un agent (fraude, enquête, pièces manquantes...).
     * Pendant la suspension, le contrat ne couvre plus les nouveaux sinistres. Elle ne prend fin
     * que quand un agent la lève : la mise à jour automatique des statuts ne la touche pas.
     *
     * @param motif raison de la suspension, obligatoire (affichée au client)
     */
    public void suspendre(Long idContrat, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new ContratInvalideException("Le motif de la suspension est obligatoire.");
        }
        if (idContrat == null || !contratDao.suspendre(idContrat, LocalDate.now(), motif.trim())) {
            throw new ContratInvalideException("Seul un contrat actif peut être suspendu.");
        }
    }

    // Lève la suspension décidée par un agent : le contrat redevient ACTIF (ou EXPIRE si sa fin est passée)
    public void leverSuspension(Long idContrat) {
        if (idContrat == null || !contratDao.leverSuspension(idContrat)) {
            throw new ContratInvalideException("Ce contrat n'a pas été suspendu par un agent.");
        }
        mettreAJourStatuts(LocalDate.now());
    }

    // ---------- Renouvellement ----------

    // Renouvellement par un agent, de n'importe quel contrat
    public Contrat renouvelerParAgent(Long idContrat) {
        return renouveler(idContrat == null ? null : contratDao.trouverAvecDetails(idContrat));
    }

    // Renouvellement par le client, seulement de l'un de ses propres contrats
    public Contrat renouvelerParClient(Long idContrat, Long idClient) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverAvecDetails(idContrat);
        // Même message pour un contrat d'un autre client : on ne révèle pas qu'il existe
        return renouveler(contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null);
    }

    /**
     * Raison pour laquelle ce contrat ne peut pas être renouvelé, ou null s'il peut l'être.
     * Sert à la fiche (afficher ou non le bouton) et au renouvellement lui-même.
     */
    public String raisonNonRenouvelable(Contrat contrat) {
        if (contrat.getStatut() != StatutContrat.ACTIF && contrat.getStatut() != StatutContrat.EXPIRE) {
            return "Seul un contrat actif ou expiré peut être renouvelé.";
        }
        if (!estPaye(contrat.getId())) {
            return "Ce contrat n'a pas été payé : il ne peut pas être renouvelé.";
        }
        if (contratDao.trouverRenouvellement(contrat.getId()) != null) {
            return "Ce contrat a déjà été renouvelé.";
        }
        if (!contrat.getTypeAssurance().isActif()) {
            return "Ce type d'assurance n'est plus proposé : le contrat ne peut pas être renouvelé.";
        }
        if (!contrat.getClient().getUtilisateur().isActif()) {
            return "Le compte de ce client est désactivé.";
        }
        return null;
    }

    /**
     * Crée le contrat qui prend la suite de celui-ci, pour DUREE_EN_MOIS de plus :
     * <ul>
     *     <li>même client et même type d'assurance ;</li>
     *     <li>mêmes garanties, sauf celles que le type ne propose plus (au moins une doit rester) ;</li>
     *     <li>même prime, relevée au prix de base du type s'il a augmenté ;</li>
     *     <li>début à la fin de l'ancien contrat, ou aujourd'hui s'il est déjà expiré (pas de période rétroactive).</li>
     * </ul>
     * Le nouveau contrat est EN_ATTENTE, avec son paiement à régler, comme un contrat créé par un agent
     * (qui peut encore le modifier tant qu'il n'est pas payé). Un contrat ne peut être renouvelé qu'une fois.
     */
    private Contrat renouveler(Contrat ancien) {
        if (ancien == null) {
            throw new ContratInvalideException("Contrat inconnu.");
        }
        String raison = raisonNonRenouvelable(ancien);
        if (raison != null) {
            throw new ContratInvalideException(raison);
        }
        TypeAssurance type = typeAssuranceDao.trouverAvecGaranties(ancien.getTypeAssurance().getId());
        Set<Long> proposees = type.getGaranties().stream().map(Garantie::getId).collect(Collectors.toSet());
        Set<Long> garanties = ancien.getGaranties().stream().map(Garantie::getId)
                .filter(proposees::contains).collect(Collectors.toSet());
        if (garanties.isEmpty()) {
            throw new ContratInvalideException("Aucune des garanties de ce contrat n'est encore proposée : "
                    + "créez plutôt un nouveau contrat.");
        }
        LocalDate aujourdhui = LocalDate.now();
        LocalDate debut = ancien.getDateFin().isBefore(aujourdhui) ? aujourdhui : ancien.getDateFin();

        Contrat nouveau = new Contrat();   // statut EN_ATTENTE par défaut
        nouveau.setDateDebut(debut);
        nouveau.setDateFin(dateFin(debut));
        nouveau.setPrime(ancien.getPrime().max(type.getPrixBase()));
        try {
            return contratDao.creerAvecGaranties(nouveau, ancien.getClient().getId(), type.getId(), garanties,
                    ancien.getId());
        } catch (PersistenceException e) {
            // Renouvelé au même moment par quelqu'un d'autre : la colonne id_contrat_precedent est unique
            throw new ContratInvalideException("Ce contrat a déjà été renouvelé.");
        }
    }

    /**
     * Statut qu'un contrat doit avoir à la date donnée. Seul endroit où cette règle est écrite :
     *   - RESILIE reste RESILIE (décision d'un agent, jamais annulée automatiquement)
     *   - date de fin atteinte : EXPIRE, payé ou non
     *   - suspendu par un agent : SUSPENDU, jusqu'à ce qu'un agent lève la suspension
     *   - avant la date de début : EN_ATTENTE, payé ou non
     *   - à partir de la date de début, payé : ACTIF
     *   - non payé : EN_ATTENTE le jour de la date de début (dernier jour pour payer), SUSPENDU après
     */
    static StatutContrat statutAttendu(Contrat contrat, boolean paye, LocalDate aujourdhui) {
        if (contrat.getStatut() == StatutContrat.RESILIE) {
            return StatutContrat.RESILIE;
        }
        if (!aujourdhui.isBefore(contrat.getDateFin())) {
            return StatutContrat.EXPIRE;
        }
        // Suspension décidée par un agent : seul un agent la lève (leverSuspension)
        if (contrat.getDateSuspension() != null) {
            return StatutContrat.SUSPENDU;
        }
        if (aujourdhui.isBefore(contrat.getDateDebut())) {
            return StatutContrat.EN_ATTENTE;
        }
        if (paye) {
            return StatutContrat.ACTIF;
        }
        return aujourdhui.isAfter(contrat.getDateDebut()) ? StatutContrat.SUSPENDU : StatutContrat.EN_ATTENTE;
    }

    /**
     * Met à jour le statut de tous les contrats en cours et passe en retard les paiements dont l'échéance est dépassée.
     * Appelée au démarrage de l'application puis toutes les heures (DemarrageApplication),
     * et après l'enregistrement d'un paiement.
     *
     * @return le nombre de contrats dont le statut a changé
     */
    public int mettreAJourStatuts(LocalDate aujourdhui) {
        paiementDao.marquerEnRetard(aujourdhui);
        Set<Long> payes = paiementDao.idsContratsPayes();
        int changes = 0;
        for (Contrat contrat : contratDao.listerEnCours()) {
            StatutContrat attendu = statutAttendu(contrat, payes.contains(contrat.getId()), aujourdhui);
            if (attendu != contrat.getStatut()
                    && contratDao.changerStatutSi(contrat.getId(), contrat.getStatut(), attendu)) {
                changes++;
            }
        }
        return changes;
    }

    // Fin d'un contrat qui commence à dateDebut : DUREE_EN_MOIS plus tard (le 29 février donne le 28 février)
    public static LocalDate dateFin(LocalDate dateDebut) {
        return dateDebut.plusMonths(DUREE_EN_MOIS);
    }
}
