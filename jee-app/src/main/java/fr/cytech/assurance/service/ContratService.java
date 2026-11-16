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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Règles métier des contrats.
 * Les servlets vérifient le format des saisies (Validateur), ce service applique les règles et passe par les DAO.
 */
public class ContratService {

    /** Durée de tous les contrats : la date de fin est calculée, la prime et le prix de base sont annuels. */
    public static final int DUREE_EN_MOIS = 12;

    private final ContratDao contratDao = new ContratDao();
    private final ClientDao clientDao = new ClientDao();
    private final TypeAssuranceDao typeAssuranceDao = new TypeAssuranceDao();
    private final PaiementDao paiementDao = new PaiementDao();
    private final VersementDao versementDao = new VersementDao();

    /**
     * @return tous les contrats, du plus récent au plus ancien
     */
    public List<Contrat> listerContrats() {
        return contratDao.listerTousAvecDetails();
    }

    /**
     * @return le contrat avec son client, son type et ses garanties, ou null s'il n'existe pas
     */
    public Contrat trouverAvecDetails(Long id) {
        return contratDao.trouverAvecDetails(id);
    }

    /**
     * @return le contrat avec ses détails s'il appartient à ce client, sinon null
     */
    public Contrat trouverDuClient(Long idContrat, Long idClient) {
        Contrat contrat = contratDao.trouverAvecDetails(idContrat);
        return contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null;
    }

    /**
     * Recherche de contrats pour les agents. Critères facultatifs (null : ignoré).
     *
     * @param texte numéro de contrat, ou nom, prénom ou email du client (en partie)
     * @param tri   debut_desc (par défaut), debut_asc, fin_asc, fin_desc, prime_desc ou prime_asc
     */
    public List<Contrat> rechercher(String texte, StatutContrat statut, Long idType, String tri) {
        String nettoye = texte == null || texte.isBlank() ? null : texte.trim();
        List<Contrat> resultats = (nettoye == null && statut == null && idType == null)
                ? listerContrats()
                : contratDao.rechercher(nettoye, statut, idType);
        return trierContrats(resultats, tri);
    }

    /**
     * Contrats d'un client, filtrés et triés comme {@link #rechercher}. Critères facultatifs (null : ignoré).
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

    /** Trie les contrats selon tri ; par défaut, du plus récent au plus ancien. */
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

    /**
     * @return le contrat qui renouvelle celui-ci, ou null s'il n'a pas été renouvelé
     */
    public Contrat trouverRenouvellement(Long idContrat) {
        return contratDao.trouverRenouvellement(idContrat);
    }

    /**
     * @return le remboursement de prime versé à la résiliation, ou null s'il n'y en a pas eu
     */
    public Versement trouverRemboursement(Long idContrat) {
        return versementDao.trouverRemboursement(idContrat);
    }

    /**
     * Crée un contrat EN_ATTENTE de {@link #DUREE_EN_MOIS}, avec son paiement unique (la prime entière).
     *
     * @throws ContratInvalideException si une règle n'est pas respectée (message affichable)
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
        if (!type.isActif()) {
            throw new ContratInvalideException("Ce type d'assurance est archivé : il n'est plus proposé.");
        }

        verifierGarantiesDateEtPrime(type, idsGaranties, dateDebut, prime, true);

        Contrat contrat = new Contrat();
        contrat.setDateDebut(dateDebut);
        contrat.setDateFin(dateFin(dateDebut));
        contrat.setPrime(prime);
        return contratDao.creerAvecGaranties(contrat, idClient, idTypeAssurance, idsGaranties);
    }

    /**
     * Modifie les garanties, la date de début et la prime d'un contrat pas encore payé ; son paiement suit.
     *
     * @throws ContratInvalideException si le contrat est payé, ni en attente ni suspendu, ou si une règle n'est pas respectée
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
        verifierGarantiesDateEtPrime(type, idsGaranties, dateDebut, prime, prime != null
                && prime.compareTo(contrat.getPrime()) != 0);

        contratDao.modifierAvantPaiement(idContrat, idsGaranties, dateDebut, dateFin(dateDebut), prime);
        // Un contrat suspendu dont la date de début est repoussée redevient EN_ATTENTE
        mettreAJourStatuts(LocalDate.now());
    }

    /** Vrai si le statut permet une modification (le paiement est vérifié à part, avec {@link #estPaye}). */
    public static boolean estModifiable(Contrat contrat) {
        return contrat.getStatut() == StatutContrat.EN_ATTENTE || contrat.getStatut() == StatutContrat.SUSPENDU;
    }

    /** Vrai si le paiement du contrat a été effectué. */
    public boolean estPaye(Long idContrat) {
        return paiementDao.listerParContrat(idContrat).stream().anyMatch(p -> p.getStatut() == StatutPaiement.EFFECTUE);
    }

    /**
     * Règles communes à la création et à la modification : garanties, date de début, prime.
     *
     * @param verifierPrixBase vrai pour refuser une prime inférieure au prix de base du type
     */
    private static void verifierGarantiesDateEtPrime(TypeAssurance type, Set<Long> idsGaranties,
                                                     LocalDate dateDebut, BigDecimal prime, boolean verifierPrixBase) {
        if (idsGaranties == null || idsGaranties.isEmpty()) {
            throw new ContratInvalideException("Choisissez au moins une garantie.");
        }
        Set<Long> disponibles = type.getGaranties().stream().map(Garantie::getId).collect(Collectors.toSet());
        if (!disponibles.containsAll(idsGaranties)) {
            throw new ContratInvalideException("Une des garanties choisies n'est pas proposée pour l'"
                    + type.getNom().toLowerCase() + ".");
        }

        if (dateDebut == null) {
            throw new ContratInvalideException("La date de début est obligatoire.");
        }
        if (dateDebut.isBefore(LocalDate.now())) {
            throw new ContratInvalideException("La date de début ne peut pas être dans le passé.");
        }

        if (prime == null || prime.signum() <= 0) {
            throw new ContratInvalideException("La prime doit être supérieure à zéro.");
        }

        if (verifierPrixBase && prime.compareTo(type.getPrixBase()) < 0) {
            throw new ContratInvalideException("La prime ne peut pas être inférieure au prix de base de l'"
                    + type.getNom().toLowerCase() + " (" + type.getPrixBase().toPlainString() + " €).");
        }
    }

    // ---------- Résiliation ----------

    /**
     * Résiliation par un agent, de n'importe quel contrat en cours.
     *
     * @throws ContratInvalideException si le contrat n'existe pas ou est déjà terminé
     */
    public void resilierParAgent(Long idContrat, String motif, Long idAgent) {
        resilier(idContrat == null ? null : contratDao.trouverParId(idContrat), motif, idAgent);
    }

    /**
     * Résiliation par le client, de l'un de ses propres contrats.
     *
     * @throws ContratInvalideException si le contrat n'est pas le sien, n'existe pas ou est déjà terminé
     */
    public void resilierParClient(Long idContrat, Long idClient, String motif) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverParId(idContrat);
        resilier(contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null, motif, null);
    }

    /** Résilie le contrat à la date du jour avec son remboursement ; un contrat EXPIRE ou RESILIE est terminé. */
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
     * Remboursement au prorata de la période non couverte si le contrat est résilié à cette date :
     * toute la prime s'il n'a pas commencé, rien s'il n'est pas payé.
     *
     * @return le montant à rembourser, arrondi au centime inférieur, zéro s'il n'y a rien à rembourser
     */
    public BigDecimal remboursement(Contrat contrat, LocalDate dateResiliation) {
        BigDecimal paye = paiementDao.listerParContrat(contrat.getId()).stream()
                .filter(p -> p.getStatut() == StatutPaiement.EFFECTUE)
                .map(Paiement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return calculerRemboursement(paye, contrat.getDateDebut(), contrat.getDateFin(), dateResiliation);
    }

    /** Couvert du début à la veille de la fin : (fin - début) jours, dont (fin - résiliation - 1) non couverts. */
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
     * Suspend un contrat ACTIF sur décision d'un agent ; seul un agent peut lever la suspension.
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

    /** Lève la suspension d'un agent : le contrat redevient ACTIF, ou EXPIRE si sa fin est passée. */
    public void leverSuspension(Long idContrat) {
        if (idContrat == null || !contratDao.leverSuspension(idContrat)) {
            throw new ContratInvalideException("Ce contrat n'a pas été suspendu par un agent.");
        }
        mettreAJourStatuts(LocalDate.now());
    }

    // ---------- Renouvellement ----------

    /** Renouvellement par un agent, de n'importe quel contrat. */
    public Contrat renouvelerParAgent(Long idContrat) {
        return renouveler(idContrat == null ? null : contratDao.trouverAvecDetails(idContrat));
    }

    /** Renouvellement par le client, de l'un de ses propres contrats. */
    public Contrat renouvelerParClient(Long idContrat, Long idClient) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverAvecDetails(idContrat);
        // Même message pour un contrat d'un autre client : on ne révèle pas qu'il existe
        return renouveler(contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null);
    }

    /**
     * @return la raison pour laquelle ce contrat ne peut pas être renouvelé, ou null s'il peut l'être
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
     * Crée le contrat suivant, EN_ATTENTE : même client, même type, garanties encore proposées,
     * prime relevée au prix de base si besoin, début à la fin de l'ancien (ou aujourd'hui s'il est expiré).
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

        Contrat nouveau = new Contrat();
        nouveau.setDateDebut(debut);
        nouveau.setDateFin(dateFin(debut));
        nouveau.setPrime(ancien.getPrime().max(type.getPrixBase()));
        try {
            return contratDao.creerAvecGaranties(nouveau, ancien.getClient().getId(), type.getId(), garanties,
                    ancien.getId());
        } catch (PersistenceException e) {
            throw new ContratInvalideException("Ce contrat a déjà été renouvelé.");
        }
    }

    /**
     * @return le statut que ce contrat doit avoir à cette date (seul endroit où cette règle est écrite)
     */
    static StatutContrat statutAttendu(Contrat contrat, boolean paye, LocalDate aujourdhui) {
        if (contrat.getStatut() == StatutContrat.RESILIE) {
            return StatutContrat.RESILIE;
        }
        if (!aujourdhui.isBefore(contrat.getDateFin())) {
            return StatutContrat.EXPIRE;
        }
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
     * Met à jour le statut des contrats en cours et passe en retard les paiements dont l'échéance est dépassée.
     * Appelée au démarrage puis toutes les heures (DemarrageApplication), et après un paiement.
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

    /** Fin d'un contrat : {@link #DUREE_EN_MOIS} après son début (le 29 février donne le 28 février). */
    public static LocalDate dateFin(LocalDate dateDebut) {
        return dateDebut.plusMonths(DUREE_EN_MOIS);
    }
}
