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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gère les règles et opérations liées aux paiements des contrats.
 * Chaque contrat a un seul paiement (la prime entière) créé à la souscription
 * et à régler au plus tard à la date de début du contrat.
 */
public class PaiementService {

    private final PaiementDao paiementDao = new PaiementDao();
    private final ContratDao contratDao = new ContratDao();
    private final ContratService contratService = new ContratService();

    /**
     * Trouve le paiement unique d'un contrat.
     *
     * @param idContrat identifiant du contrat
     * @return le paiement du contrat, ou null s'il n'en a pas
     */
    public Paiement trouverParContrat(Long idContrat) {
        return paiementDao.listerParContrat(idContrat).stream().findFirst().orElse(null);
    }

    /**
     * Enregistre le paiement d'un contrat par son client à la date du jour,
     * puis met à jour le statut du contrat (passe à ACTIF si sa date de début est atteinte).
     *
     * @param idContrat identifiant du contrat à payer
     * @param idClient  identifiant du client qui paie
     * @param mode      mode de paiement choisi (carte, virement, etc.)
     * @throws PaiementInvalideException si le contrat est introuvable, n'appartient pas au client,
     *                                   est déjà payé/annulé/résilié, ou si le mode est manquant
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

    /**
     * Liste les paiements d'un client avec filtrage et tri optionnels.
     *
     * @param idClient identifiant du client
     * @param statut   statut recherché (ou null pour tous les statuts)
     * @param mode     mode de paiement recherché (ou null pour tous les modes)
     * @param tri      ordre de tri (echeance_asc, montant_asc, montant_desc, paiement_desc)
     * @return la liste filtrée et triée des paiements du client
     */
    public List<Paiement> listerPourClient(Long idClient, StatutPaiement statut, ModePaiement mode, String tri) {
        if (idClient == null) {
            return List.of();
        }
        return filtrerEtTrier(paiementDao.listerParClient(idClient), statut, mode, tri);
    }

    /**
     * Liste tous les paiements (pour les agents et administrateurs) avec filtrage et tri optionnels.
     *
     * @param statut statut recherché (ou null pour tous les statuts)
     * @param mode   mode de paiement recherché (ou null pour tous les modes)
     * @param tri    ordre de tri (echeance_asc, montant_asc, montant_desc, paiement_desc)
     * @return la liste complète des paiements filtrée et triée
     */
    public List<Paiement> listerTous(StatutPaiement statut, ModePaiement mode, String tri) {
        return filtrerEtTrier(paiementDao.listerTousAvecDetails(), statut, mode, tri);
    }

    /**
     * Filtre une liste de paiements selon le statut et le mode, puis la trie.
     *
     * @param paiements liste des paiements à filtrer
     * @param statut    statut à garder (ignoré si null)
     * @param mode      mode à garder (ignoré si null)
     * @param tri       critère de tri choisi
     * @return la nouvelle liste filtrée et triée
     */
    private List<Paiement> filtrerEtTrier(List<Paiement> paiements, StatutPaiement statut, ModePaiement mode, String tri) {
        Stream<Paiement> flux = paiements.stream();

        if (statut != null) {
            flux = flux.filter(p -> p.getStatut() == statut);
        }
        if (mode != null) {
            flux = flux.filter(p -> p.getModePaiement() == mode);
        }

        Comparator<Paiement> comparateur = switch (tri == null ? "" : tri) {
            case "echeance_asc" -> Comparator.comparing(Paiement::getDateEcheance);
            case "montant_asc" -> Comparator.comparing(Paiement::getMontant);
            case "montant_desc" -> Comparator.comparing(Paiement::getMontant).reversed();
            case "paiement_desc" -> Comparator.comparing(
                    Paiement::getDatePaiement, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(Paiement::getDateEcheance).reversed(); // echeance_desc par défaut
        };

        return flux.sorted(comparateur).toList();
    }
}