package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ContratDao;
import fr.cytech.assurance.dao.PaiementDao;
import fr.cytech.assurance.exception.PaiementInvalideException;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.enums.ModePaiement;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;

import java.time.LocalDate;

/**
 * Règles des paiements. Chaque contrat a un seul paiement : la prime entière,
 * créé avec le contrat (ContratDao.creerAvecGaranties), à régler au plus tard à la date de début.
 */
public class PaiementService {

    private final PaiementDao paiementDao = new PaiementDao();
    private final ContratDao contratDao = new ContratDao();
    private final ContratService contratService = new ContratService();

    // Paiement unique du contrat, ou null s'il n'en a pas (contrat créé avant la règle du paiement unique)
    public Paiement trouverParContrat(Long idContrat) {
        return paiementDao.listerParContrat(idContrat).stream().findFirst().orElse(null);
    }

    /**
     * Paiement du contrat par son client, à la date du jour, puis mise à jour du statut :
     * il devient ACTIF tout de suite si sa date de début est atteinte (EN_ATTENTE ou SUSPENDU),
     * sinon à sa date de début.
     * Lève PaiementInvalideException (message affichable) si le paiement ne peut pas être enregistré,
     * notamment si le contrat n'appartient pas à ce client.
     */
    public void payer(Long idContrat, Long idClient, ModePaiement mode) {
        Contrat contrat = idContrat == null ? null : contratDao.trouverParId(idContrat);
        // Même message pour un contrat d'un autre client : on ne révèle pas qu'il existe
        if (contrat == null || !contrat.getClient().getId().equals(idClient)) {
            throw new PaiementInvalideException("Contrat inconnu.");
        }
        if (contrat.getStatut() == StatutContrat.RESILIE || contrat.getStatut() == StatutContrat.EXPIRE) {
            throw new PaiementInvalideException("Ce contrat est " + (contrat.getStatut() == StatutContrat.RESILIE
                    ? "résilié" : "expiré") + " : son paiement ne peut plus être enregistré.");
        }
        Paiement paiement = trouverParContrat(idContrat);
        if (paiement == null) {
            throw new PaiementInvalideException("Ce contrat n'a pas de paiement à enregistrer.");
        }
        if (paiement.getStatut() == StatutPaiement.EFFECTUE) {
            throw new PaiementInvalideException("Ce contrat est déjà payé.");
        }
        if (paiement.getStatut() == StatutPaiement.ANNULE) {
            throw new PaiementInvalideException("Le paiement de ce contrat a été annulé.");
        }
        if (mode == null) {
            throw new PaiementInvalideException("Le mode de paiement est obligatoire.");
        }
        LocalDate aujourdhui = LocalDate.now();
        paiementDao.enregistrerPaiement(paiement.getId(), aujourdhui, mode);
        // Sans attendre la prochaine mise à jour automatique (toutes les heures)
        contratService.mettreAJourStatuts(aujourdhui);
    }
}
