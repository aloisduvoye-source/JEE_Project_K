package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.dao.ContratDao;
import fr.cytech.assurance.dao.TypeAssuranceDao;
import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Règles métier des contrats. Les servlets vérifient le format des saisies (Validateur),
 * puis appellent ce service, qui applique les règles et passe par les DAO.
 */
public class ContratService {

    private final ContratDao contratDao = new ContratDao();
    private final ClientDao clientDao = new ClientDao();
    private final TypeAssuranceDao typeAssuranceDao = new TypeAssuranceDao();

    /**
     * Crée un contrat en EN_ATTENTE, avec un numéro attribué automatiquement.
     * Lève ContratInvalideException (message affichable) si une règle n'est pas respectée.
     */
    public Contrat creerContrat(Long idClient, Long idTypeAssurance, Set<Long> idsGaranties,
                                LocalDate dateDebut, LocalDate dateFin, BigDecimal prime) {
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

        if (dateDebut == null || dateFin == null) {
            throw new ContratInvalideException("Les dates de début et de fin sont obligatoires.");
        }
        if (!dateFin.isAfter(dateDebut)) {
            throw new ContratInvalideException("La date de fin doit être après la date de début.");
        }

        if (prime == null || prime.signum() <= 0) {
            throw new ContratInvalideException("La prime doit être supérieure à zéro.");
        }

        Contrat contrat = new Contrat();   // statut EN_ATTENTE par défaut
        contrat.setNumero(contratDao.prochainNumero());
        contrat.setDateDebut(dateDebut);
        contrat.setDateFin(dateFin);
        contrat.setPrime(prime);
        return contratDao.creerAvecGaranties(contrat, idClient, idTypeAssurance, idsGaranties);
    }
}
