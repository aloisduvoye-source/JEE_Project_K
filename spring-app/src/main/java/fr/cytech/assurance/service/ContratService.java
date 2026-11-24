package fr.cytech.assurance.service;

import fr.cytech.assurance.repository.ClientRepository;
import fr.cytech.assurance.repository.ContratRepository;
import fr.cytech.assurance.repository.PaiementRepository;
import fr.cytech.assurance.repository.TypeAssuranceRepository;
import fr.cytech.assurance.repository.VersementRepository;
import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import fr.cytech.assurance.repository.GarantieRepository;
import fr.cytech.assurance.repository.UtilisateurRepository;
import org.springframework.transaction.annotation.Transactional;

import fr.cytech.assurance.model.enums.TypeVersement;
import java.util.HashSet;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Règles métier des contrats.
 * Les servlets vérifient le format des saisies (Validateur), ce service applique les règles et passe par les DAO.
 */
@Service
public class ContratService {

    /** Durée de tous les contrats : la date de fin est calculée, la prime et le prix de base sont annuels. */
    public static final int DUREE_EN_MOIS = 12;

    private final ContratRepository contratRepository;
    private final ClientRepository clientRepository;
    private final TypeAssuranceRepository typeAssuranceRepository;
    private final PaiementRepository paiementRepository;
    private final VersementRepository versementRepository;
    private final GarantieRepository garantieRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Dépendances injectées par Spring. */
    public ContratService(ContratRepository contratRepository, ClientRepository clientRepository,
                          TypeAssuranceRepository typeAssuranceRepository, PaiementRepository paiementRepository,
                          VersementRepository versementRepository, GarantieRepository garantieRepository,
                          UtilisateurRepository utilisateurRepository) {
        this.contratRepository = contratRepository;
        this.clientRepository = clientRepository;
        this.typeAssuranceRepository = typeAssuranceRepository;
        this.paiementRepository = paiementRepository;
        this.versementRepository = versementRepository;
        this.garantieRepository = garantieRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * @return tous les contrats, du plus récent au plus ancien
     */
    public List<Contrat> listerContrats() {
        return contratRepository.listerTousAvecDetails();
    }

    /**
     * @return le contrat avec son client, son type et ses garanties, ou null s'il n'existe pas
     */
    public Contrat trouverAvecDetails(Long id) {
        return contratRepository.trouverAvecDetails(id);
    }

    /**
     * @return le contrat avec ses détails s'il appartient à ce client, sinon null
     */
    public Contrat trouverDuClient(Long idContrat, Long idClient) {
        Contrat contrat = contratRepository.trouverAvecDetails(idContrat);
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
                : contratRepository.rechercher(nettoye, statut, idType);
        return trierContrats(resultats, tri);
    }

    /**
     * Contrats d'un client, filtrés et triés comme {@link #rechercher}. Critères facultatifs (null : ignoré).
     */
    public List<Contrat> listerContratsDuClient(Long idClient, StatutContrat statut, Long idType, String tri) {
        if (idClient == null) {
            return List.of();
        }
        Stream<Contrat> flux = contratRepository.listerParClient(idClient).stream();
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
        return contratRepository.trouverRenouvellement(idContrat);
    }

    /**
     * @return le remboursement de prime versé à la résiliation, ou null s'il n'y en a pas eu
     */
    public Versement trouverRemboursement(Long idContrat) {
        return versementRepository.trouverRemboursement(idContrat);
    }

    /**
     * Crée un contrat EN_ATTENTE de {@link #DUREE_EN_MOIS}, avec son paiement unique (la prime entière).
     *
     * @throws ContratInvalideException si une règle n'est pas respectée (message affichable)
     */
    @Transactional
    public Contrat creerContrat(Long idClient, Long idTypeAssurance, Set<Long> idsGaranties,
                                LocalDate dateDebut, BigDecimal prime) {
        Client client = idClient == null ? null : clientRepository.findById(idClient).orElse(null);
        if (client == null) {
            throw new ContratInvalideException("Client inconnu.");
        }
        if (!client.getUtilisateur().isActif()) {
            throw new ContratInvalideException("Le compte de ce client est désactivé.");
        }

        TypeAssurance type = idTypeAssurance == null ? null : typeAssuranceRepository.trouverAvecGaranties(idTypeAssurance);
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
        return creerAvecGaranties(contrat, idClient, idTypeAssurance, idsGaranties, null);
    }

    /**
     * Modifie les garanties, la date de début et la prime d'un contrat pas encore payé ; son paiement suit.
     *
     * @throws ContratInvalideException si le contrat est payé, ni en attente ni suspendu, ou si une règle n'est pas respectée
     */
    @Transactional
    public void modifierContrat(Long idContrat, Set<Long> idsGaranties, LocalDate dateDebut, BigDecimal prime) {
        Contrat contrat = idContrat == null ? null : contratRepository.trouverAvecDetails(idContrat);
        if (contrat == null) {
            throw new ContratInvalideException("Contrat inconnu.");
        }
        if (!estModifiable(contrat)) {
            throw new ContratInvalideException("Seul un contrat en attente ou suspendu, pas encore payé, peut être modifié.");
        }
        if (estPaye(idContrat)) {
            throw new ContratInvalideException("Ce contrat a été payé : il ne peut plus être modifié.");
        }
        TypeAssurance type = typeAssuranceRepository.trouverAvecGaranties(contrat.getTypeAssurance().getId());
        verifierGarantiesDateEtPrime(type, idsGaranties, dateDebut, prime, prime != null
                && prime.compareTo(contrat.getPrime()) != 0);

        modifierAvantPaiement(idContrat, idsGaranties, dateDebut, dateFin(dateDebut), prime);
        // Modifications écrites en base avant le recalcul des statuts, qui passe par des requêtes UPDATE
        contratRepository.flush();
        // Un contrat suspendu dont la date de début est repoussée redevient EN_ATTENTE
        mettreAJourStatuts(LocalDate.now());
    }

    /** Vrai si le statut permet une modification (le paiement est vérifié à part, avec {@link #estPaye}). */
    public static boolean estModifiable(Contrat contrat) {
        return contrat.getStatut() == StatutContrat.EN_ATTENTE || contrat.getStatut() == StatutContrat.SUSPENDU;
    }

    /** Vrai si le paiement du contrat a été effectué. */
    public boolean estPaye(Long idContrat) {
        return paiementRepository.listerParContrat(idContrat).stream().anyMatch(p -> p.getStatut() == StatutPaiement.EFFECTUE);
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
    @Transactional
    public void resilierParAgent(Long idContrat, String motif, Long idAgent) {
        resilier(idContrat == null ? null : contratRepository.findById(idContrat).orElse(null), motif, idAgent);
    }

    /**
     * Résiliation par le client, de l'un de ses propres contrats.
     *
     * @throws ContratInvalideException si le contrat n'est pas le sien, n'existe pas ou est déjà terminé
     */
    @Transactional
    public void resilierParClient(Long idContrat, Long idClient, String motif) {
        Contrat contrat = idContrat == null ? null : contratRepository.findById(idContrat).orElse(null);
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
        enregistrerResiliation(contrat, aujourdhui, motif == null || motif.isBlank() ? null : motif,
                remboursement(contrat, aujourdhui), idAgent);
    }

    /**
     * Remboursement au prorata de la période non couverte si le contrat est résilié à cette date :
     * toute la prime s'il n'a pas commencé, rien s'il n'est pas payé.
     *
     * @return le montant à rembourser, arrondi au centime inférieur, zéro s'il n'y a rien à rembourser
     */
    public BigDecimal remboursement(Contrat contrat, LocalDate dateResiliation) {
        BigDecimal paye = paiementRepository.listerParContrat(contrat.getId()).stream()
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
        if (idContrat == null || contratRepository.suspendre(idContrat, LocalDate.now(), motif.trim()) == 0) {
            throw new ContratInvalideException("Seul un contrat actif peut être suspendu.");
        }
    }

    /** Lève la suspension d'un agent : le contrat redevient ACTIF, ou EXPIRE si sa fin est passée. */
    @Transactional
    public void leverSuspension(Long idContrat) {
        if (idContrat == null || contratRepository.leverSuspension(idContrat) == 0) {
            throw new ContratInvalideException("Ce contrat n'a pas été suspendu par un agent.");
        }
        mettreAJourStatuts(LocalDate.now());
    }

    // ---------- Renouvellement ----------

    /** Renouvellement par un agent, de n'importe quel contrat. */
    @Transactional
    public Contrat renouvelerParAgent(Long idContrat) {
        return renouveler(idContrat == null ? null : contratRepository.trouverAvecDetails(idContrat));
    }

    /** Renouvellement par le client, de l'un de ses propres contrats. */
    @Transactional
    public Contrat renouvelerParClient(Long idContrat, Long idClient) {
        Contrat contrat = idContrat == null ? null : contratRepository.trouverAvecDetails(idContrat);
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
        if (contratRepository.trouverRenouvellement(contrat.getId()) != null) {
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
        TypeAssurance type = typeAssuranceRepository.trouverAvecGaranties(ancien.getTypeAssurance().getId());
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
            return creerAvecGaranties(nouveau, ancien.getClient().getId(), type.getId(), garanties, ancien.getId());
        } catch (DataIntegrityViolationException e) {
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
     * Appelée au démarrage puis toutes les heures (config.MiseAJourStatuts), et après un paiement.
     *
     * @return le nombre de contrats dont le statut a changé
     */
    @Transactional
    public int mettreAJourStatuts(LocalDate aujourdhui) {
        paiementRepository.marquerEnRetard(aujourdhui);
        Set<Long> payes = paiementRepository.idsContratsPayes();
        int changes = 0;
        for (Contrat contrat : contratRepository.listerEnCours()) {
            StatutContrat attendu = statutAttendu(contrat, payes.contains(contrat.getId()), aujourdhui);
            if (attendu != contrat.getStatut()
                    && contratRepository.changerStatutSi(contrat.getId(), contrat.getStatut(), attendu) == 1) {
                changes++;
            }
        }
        return changes;
    }

    /** Fin d'un contrat : {@link #DUREE_EN_MOIS} après son début (le 29 février donne le 28 février). */
    public static LocalDate dateFin(LocalDate dateDebut) {
        return dateDebut.plusMonths(DUREE_EN_MOIS);
    }

    // ---------- Écritures en base (faites par le DAO dans jee-app), appelées par des méthodes @Transactional ----------

    /**
     * Enregistre un contrat relié à son client, son type et ses garanties, avec son paiement unique (la prime entière,
     * à régler à la date de début). Les règles métier ont déjà été vérifiées.
     *
     * @param idContratPrecedent contrat renouvelé par celui-ci, ou null
     * @throws IllegalArgumentException      si le client, le type, une garantie ou le contrat précédent n'existe pas
     * @throws DataIntegrityViolationException si le contrat précédent a déjà été renouvelé
     */
    private Contrat creerAvecGaranties(Contrat contrat, Long idClient, Long idTypeAssurance, Set<Long> idsGaranties,
                                       Long idContratPrecedent) {
        if (idContratPrecedent != null) {
            contrat.setContratPrecedent(contratRepository.findById(idContratPrecedent)
                    .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + idContratPrecedent)));
        }
        contrat.setClient(clientRepository.findById(idClient)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable : " + idClient)));
        contrat.setTypeAssurance(typeAssuranceRepository.findById(idTypeAssurance)
                .orElseThrow(() -> new IllegalArgumentException("Type d'assurance introuvable : " + idTypeAssurance)));
        List<Garantie> garanties = garantieRepository.findAllById(idsGaranties);
        if (garanties.size() != idsGaranties.size()) {
            throw new IllegalArgumentException("Garantie introuvable parmi : " + idsGaranties);
        }
        contrat.setGaranties(new HashSet<>(garanties));
        // Écrit tout de suite : un renouvellement en double est refusé ici par la contrainte d'unicité
        contratRepository.saveAndFlush(contrat);

        Paiement paiement = new Paiement();   // statut EN_ATTENTE par défaut
        paiement.setContrat(contrat);
        paiement.setMontant(contrat.getPrime());
        paiement.setDateEcheance(contrat.getDateDebut());
        paiementRepository.save(paiement);
        return contrat;
    }

    /**
     * Remplace les garanties, les dates et la prime d'un contrat non payé ; son paiement suit (montant, échéance).
     *
     * @throws ContratInvalideException si le contrat a été payé entre-temps
     */
    private void modifierAvantPaiement(Long id, Set<Long> idsGaranties, LocalDate dateDebut, LocalDate dateFin,
                                       BigDecimal prime) {
        Contrat contrat = contratRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable : " + id));
        List<Paiement> paiements = paiementRepository.listerParContrat(id);
        if (paiements.stream().anyMatch(p -> p.getStatut() == StatutPaiement.EFFECTUE)) {
            throw new ContratInvalideException("Ce contrat a été payé : il ne peut plus être modifié.");
        }
        List<Garantie> garanties = garantieRepository.findAllById(idsGaranties);
        if (garanties.size() != idsGaranties.size()) {
            throw new IllegalArgumentException("Garantie introuvable parmi : " + idsGaranties);
        }
        // Entités lues dans la transaction : leurs changements sont enregistrés automatiquement
        contrat.setGaranties(new HashSet<>(garanties));
        contrat.setDateDebut(dateDebut);
        contrat.setDateFin(dateFin);
        contrat.setPrime(prime);
        for (Paiement paiement : paiements) {
            if (paiement.getStatut() != StatutPaiement.ANNULE) {
                paiement.setMontant(prime);
                paiement.setDateEcheance(dateDebut);
                paiement.setStatut(StatutPaiement.EN_ATTENTE);
            }
        }
    }

    /**
     * Passe le contrat RESILIE à cette date, enregistre le remboursement s'il y en a un
     * et annule les paiements qui restaient à régler.
     *
     * @param remboursement montant remboursé au client, zéro ou null s'il n'y a rien à rembourser
     * @param idAgent       agent qui résilie, null si c'est le client
     */
    private void enregistrerResiliation(Contrat contrat, LocalDate date, String motif, BigDecimal remboursement,
                                        Long idAgent) {
        if (remboursement != null && remboursement.signum() > 0) {
            Versement versement = new Versement();
            versement.setType(TypeVersement.REMBOURSEMENT);
            versement.setMontant(remboursement);
            versement.setDateVersement(date);
            versement.setContrat(contrat);
            versement.setAgent(idAgent == null ? null : utilisateurRepository.getReferenceById(idAgent));
            versementRepository.save(versement);
        }
        contrat.setStatut(StatutContrat.RESILIE);
        contrat.setDateResiliation(date);
        contrat.setMotifResiliation(motif);
        contratRepository.save(contrat);
        paiementRepository.annulerNonRegles(contrat.getId());
    }
}
