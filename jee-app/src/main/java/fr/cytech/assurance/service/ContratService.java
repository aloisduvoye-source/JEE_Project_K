package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.ContratDao;
import fr.cytech.assurance.dao.PaiementDao;
import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.model.enums.StatutContrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Contrats d'un client, du plus récent au plus ancien, avec leur type et leurs garanties
    public List<Contrat> listerContratsDuClient(Long idClient) {
        return contratDao.listerParClient(idClient);
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
        if (prime.compareTo(type.getPrixBase()) < 0) {
            throw new ContratInvalideException("La prime ne peut pas être inférieure au prix de base de l'"
                    + type.getNom().toLowerCase() + " (" + type.getPrixBase().toPlainString() + " €).");
        }

        Contrat contrat = new Contrat();   // statut EN_ATTENTE par défaut
        contrat.setDateDebut(dateDebut);
        contrat.setDateFin(dateFin(dateDebut));
        contrat.setPrime(prime);
        return contratDao.creerAvecGaranties(contrat, idClient, idTypeAssurance, idsGaranties);
    }

    // Résiliation par un agent, de n'importe quel contrat en cours
    // Lève ContratInvalideException si le contrat n'existe pas ou est déjà terminé
    public void resilierParAgent(Long idContrat, String motif) {
        resilier(idContrat == null ? null : contratDao.trouverParId(idContrat), motif);
    }

    // Résiliation par le client, seulement de l'un de ses propres contrats
    // Lève ContratInvalideException si le contrat n'est pas le sien, n'existe pas ou est déjà terminé
    public void resilierParClient(Long idContrat, Long idClient, String motif) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverParId(idContrat);
        // Même message pour un contrat d'un autre client : on ne révèle pas qu'il existe
        resilier(contrat != null && contrat.getClient().getId().equals(idClient) ? contrat : null, motif);
    }

    // Un contrat EXPIRE ou RESILIE est terminé : il ne peut plus être résilié
    private void resilier(Contrat contrat, String motif) {
        if (contrat == null) {
            throw new ContratInvalideException("Contrat inconnu.");
        }
        if (contrat.getStatut() == StatutContrat.RESILIE) {
            throw new ContratInvalideException("Ce contrat est déjà résilié.");
        }
        if (contrat.getStatut() == StatutContrat.EXPIRE) {
            throw new ContratInvalideException("Ce contrat est expiré : il ne peut plus être résilié.");
        }
        contratDao.resilier(contrat.getId(), LocalDate.now(), motif == null || motif.isBlank() ? null : motif);
    }

    /**
     * Statut qu'un contrat doit avoir à la date donnée. Seul endroit où cette règle est écrite :
     *   - RESILIE reste RESILIE (décision d'un agent, jamais annulée automatiquement)
     *   - date de fin atteinte : EXPIRE, payé ou non
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
