package fr.cytech.assurance.dao;

import fr.cytech.assurance.exception.ContratInvalideException;
import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.TypeAssurance;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;
import fr.cytech.assurance.model.enums.TypeVersement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accès en base aux contrats.
 * <p>
 * Le CRUD (trouverParId, listerTous, creer, modifier, supprimer) est hérité de {@link GenericDao}.
 */
public class ContratDao extends GenericDao<Contrat> {

    public ContratDao() {
        super(Contrat.class);
    }

    /**
     * Crée un contrat relié à un client, un type d'assurance et des garanties désignés par leur id,
     * avec son paiement unique : la prime entière, à régler au plus tard à la date de début (statut EN_ATTENTE).
     * <p>
     * Tout se fait dans une seule transaction : si l'un d'eux n'existe pas, rien n'est enregistré
     * (pas de contrat sans paiement, ni de paiement sans contrat).
     * Les règles métier (garanties proposées par le type, dates, prime...) sont vérifiées par le service.
     *
     * @param contrat         contrat à créer, sans client, type ni garanties
     * @param idClient        id du client
     * @param idTypeAssurance id du type d'assurance
     * @param idsGaranties    ids des garanties souscrites
     * @return le contrat enregistré
     * @throws IllegalArgumentException si le client, le type ou l'une des garanties n'existe pas
     */
    public Contrat creerAvecGaranties(Contrat contrat, Long idClient, Long idTypeAssurance, Set<Long> idsGaranties) {
        return creerAvecGaranties(contrat, idClient, idTypeAssurance, idsGaranties, null);
    }

    /**
     * Comme {@link #creerAvecGaranties(Contrat, Long, Long, Set)}, pour le renouvellement d'un contrat :
     * le nouveau contrat garde le lien vers celui qu'il renouvelle.
     *
     * @param idContratPrecedent contrat renouvelé, ou {@code null} pour un premier contrat
     * @throws IllegalArgumentException si le client, le type, une garantie ou le contrat précédent n'existe pas
     * @throws jakarta.persistence.PersistenceException si le contrat précédent a déjà été renouvelé
     *                                                  (colonne id_contrat_precedent unique)
     */
    public Contrat creerAvecGaranties(Contrat contrat, Long idClient, Long idTypeAssurance, Set<Long> idsGaranties,
                                      Long idContratPrecedent) {
        return enTransaction(em -> {
            if (idContratPrecedent != null) {
                Contrat precedent = em.find(Contrat.class, idContratPrecedent);
                if (precedent == null) {
                    throw new IllegalArgumentException("Contrat introuvable : " + idContratPrecedent);
                }
                contrat.setContratPrecedent(precedent);
            }
            Client client = em.find(Client.class, idClient);
            if (client == null) {
                throw new IllegalArgumentException("Client introuvable : " + idClient);
            }
            TypeAssurance type = em.find(TypeAssurance.class, idTypeAssurance);
            if (type == null) {
                throw new IllegalArgumentException("Type d'assurance introuvable : " + idTypeAssurance);
            }
            List<Garantie> garanties = em.createQuery("SELECT g FROM Garantie g WHERE g.id IN :ids", Garantie.class)
                    .setParameter("ids", idsGaranties)
                    .getResultList();
            if (garanties.size() != idsGaranties.size()) {
                throw new IllegalArgumentException("Garantie introuvable parmi : " + idsGaranties);
            }

            contrat.setClient(client);
            contrat.setTypeAssurance(type);
            contrat.setGaranties(new HashSet<>(garanties));
            em.persist(contrat);

            Paiement paiement = new Paiement();   // statut EN_ATTENTE par défaut
            paiement.setContrat(contrat);
            paiement.setMontant(contrat.getPrime());
            paiement.setDateEcheance(contrat.getDateDebut());
            em.persist(paiement);
            return contrat;
        });
    }

    /**
     * Modifie les garanties, les dates et la prime d'un contrat pas encore payé, et son paiement avec :
     * montant = prime, échéance = date de début, de nouveau EN_ATTENTE (il était peut-être EN_RETARD).
     * <p>
     * Tout se fait dans une seule transaction. Le paiement est relu dans la transaction : si le client
     * vient de payer, la modification est refusée (le prix d'un contrat payé ne peut plus changer).
     * Les autres règles (garanties proposées par le type, dates, prime) sont vérifiées par le service.
     *
     * @throws ContratInvalideException si le contrat a été payé entre-temps
     * @throws IllegalArgumentException si le contrat ou l'une des garanties n'existe pas
     */
    public void modifierAvantPaiement(Long id, Set<Long> idsGaranties, LocalDate dateDebut, LocalDate dateFin,
                                      BigDecimal prime) {
        enTransaction(em -> {
            Contrat contrat = em.find(Contrat.class, id);
            if (contrat == null) {
                throw new IllegalArgumentException("Contrat introuvable : " + id);
            }
            List<Paiement> paiements = em.createQuery("SELECT p FROM Paiement p WHERE p.contrat.id = :id", Paiement.class)
                    .setParameter("id", id)
                    .getResultList();
            if (paiements.stream().anyMatch(p -> p.getStatut() == StatutPaiement.EFFECTUE)) {
                throw new ContratInvalideException("Ce contrat a été payé : il ne peut plus être modifié.");
            }
            List<Garantie> garanties = em.createQuery("SELECT g FROM Garantie g WHERE g.id IN :ids", Garantie.class)
                    .setParameter("ids", idsGaranties)
                    .getResultList();
            if (garanties.size() != idsGaranties.size()) {
                throw new IllegalArgumentException("Garantie introuvable parmi : " + idsGaranties);
            }

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
            return null;
        });
    }

    /**
     * Recherche un contrat avec son client, son type et ses garanties déjà chargés (lisibles dans une JSP).
     *
     * @param id id du contrat
     * @return le contrat avec ses détails, ou {@code null} s'il n'existe pas
     */
    public Contrat trouverAvecDetails(Long id) {
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT c FROM Contrat c"
                                + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
                                + " JOIN FETCH c.typeAssurance"
                                + " LEFT JOIN FETCH c.garanties"
                                + " WHERE c.id = :id", Contrat.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Liste tous les contrats, du plus récent au plus ancien, avec leur client et leur type déjà chargés.
     *
     * @return les contrats triés par id décroissant, liste vide s'il n'y en a aucun
     */
    public List<Contrat> listerTousAvecDetails() {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Contrat c"
                                + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
                                + " JOIN FETCH c.typeAssurance"
                                + " ORDER BY c.id DESC", Contrat.class)
                .getResultList());
    }

    /**
     * Liste les contrats d'un client, du plus récent au plus ancien, avec leur type et leurs garanties.
     *
     * @param idClient id du client
     * @return les contrats triés par date de début décroissante, liste vide si le client n'en a aucun
     */
    public List<Contrat> listerParClient(Long idClient) {
        return enLecture(em -> em.createQuery(
                        "SELECT DISTINCT c FROM Contrat c"
                                + " JOIN FETCH c.typeAssurance"
                                + " LEFT JOIN FETCH c.garanties"
                                + " WHERE c.client.id = :idClient"
                                + " ORDER BY c.dateDebut DESC", Contrat.class)
                .setParameter("idClient", idClient)
                .getResultList());
    }

    /**
     * Liste les contrats ayant ce statut, du plus récent au plus ancien, avec leur client et leur type déjà chargés.
     *
     * @param statut statut recherché
     * @return les contrats triés par id décroissant, liste vide si aucun n'a ce statut
     */
    public List<Contrat> listerParStatut(StatutContrat statut) {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Contrat c"
                                + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
                                + " JOIN FETCH c.typeAssurance"
                                + " WHERE c.statut = :statut"
                                + " ORDER BY c.id DESC", Contrat.class)
                .setParameter("statut", statut)
                .getResultList());
    }

    /**
     * Liste les contrats dont le statut peut encore changer avec le temps : EN_ATTENTE, ACTIF et SUSPENDU.
     * <p>
     * Les contrats EXPIRE et RESILIE sont terminés : leur statut ne change plus.
     *
     * @return les contrats en cours, sans leurs détails (seuls les dates et le statut servent)
     */
    public List<Contrat> listerEnCours() {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Contrat c WHERE c.statut IN :statuts", Contrat.class)
                .setParameter("statuts", List.of(StatutContrat.EN_ATTENTE, StatutContrat.ACTIF, StatutContrat.SUSPENDU))
                .getResultList());
    }

    /**
     * Change le statut d'un contrat seulement s'il a toujours le statut {@code ancien}.
     * <p>
     * Utilisé par la mise à jour automatique : si un agent a résilié le contrat entre-temps,
     * la résiliation n'est pas écrasée.
     *
     * @param id      id du contrat
     * @param ancien  statut lu avant le calcul
     * @param nouveau statut calculé
     * @return {@code true} si le statut a été changé
     */
    public boolean changerStatutSi(Long id, StatutContrat ancien, StatutContrat nouveau) {
        return enTransaction(em -> em.createQuery(
                        "UPDATE Contrat c SET c.statut = :nouveau WHERE c.id = :id AND c.statut = :ancien")
                .setParameter("nouveau", nouveau)
                .setParameter("id", id)
                .setParameter("ancien", ancien)
                .executeUpdate() == 1);
    }

    /**
     * Recherche des contrats, du plus récent au plus ancien, avec leur client et leur type déjà chargés.
     * Chaque critère est facultatif ({@code null} : pas de filtre).
     *
     * @param texte   numéro de contrat exact, ou morceau du nom, du prénom ou de l'email du client
     * @param statut  statut recherché
     * @param idType  id du type d'assurance
     * @return les contrats correspondants, liste vide s'il n'y en a aucun
     */
    public List<Contrat> rechercher(String texte, StatutContrat statut, Long idType) {
        Long numero = numeroOuNull(texte);
        return enLecture(em -> {
            StringBuilder jpql = new StringBuilder("SELECT c FROM Contrat c"
                    + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur u"
                    + " JOIN FETCH c.typeAssurance t WHERE 1 = 1");
            if (texte != null) {
                jpql.append(" AND (LOWER(u.nom) LIKE :motif OR LOWER(u.prenom) LIKE :motif OR LOWER(u.email) LIKE :motif"
                        + " OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE :motif OR LOWER(CONCAT(u.nom, ' ', u.prenom)) LIKE :motif"
                        + (numero != null ? " OR c.id = :numero" : "") + ")");
            }
            if (statut != null) {
                jpql.append(" AND c.statut = :statut");
            }
            if (idType != null) {
                jpql.append(" AND t.id = :idType");
            }
            jpql.append(" ORDER BY c.id DESC");

            var requete = em.createQuery(jpql.toString(), Contrat.class);
            if (texte != null) {
                requete.setParameter("motif", "%" + texte.toLowerCase() + "%");
                if (numero != null) {
                    requete.setParameter("numero", numero);
                }
            }
            if (statut != null) {
                requete.setParameter("statut", statut);
            }
            if (idType != null) {
                requete.setParameter("idType", idType);
            }
            return requete.getResultList();
        });
    }

    // Texte saisi qui est un numéro de contrat ("100001" ou "#100001"), sinon null
    private static Long numeroOuNull(String texte) {
        if (texte == null) {
            return null;
        }
        try {
            return Long.valueOf(texte.startsWith("#") ? texte.substring(1) : texte);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Contrat qui renouvelle celui-ci, ou {@code null} s'il n'a pas été renouvelé.
     *
     * @param idContrat id du contrat renouvelé
     */
    public Contrat trouverRenouvellement(Long idContrat) {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Contrat c WHERE c.contratPrecedent.id = :id", Contrat.class)
                .setParameter("id", idContrat)
                .getResultStream()
                .findFirst()
                .orElse(null));
    }

    /**
     * Suspend un contrat sur décision d'un agent : statut SUSPENDU, avec la date et le motif.
     * Le statut est relu dans la transaction : seul un contrat ACTIF peut être suspendu.
     *
     * @return {@code false} si le contrat n'existe pas ou n'est pas (ou plus) ACTIF
     */
    public boolean suspendre(Long id, LocalDate date, String motif) {
        return enTransaction(em -> {
            Contrat contrat = em.find(Contrat.class, id);
            if (contrat == null || contrat.getStatut() != StatutContrat.ACTIF) {
                return false;
            }
            contrat.setStatut(StatutContrat.SUSPENDU);
            contrat.setDateSuspension(date);
            contrat.setMotifSuspension(motif);
            return true;
        });
    }

    /**
     * Lève la suspension décidée par un agent : efface la date et le motif.
     * Le statut est ensuite recalculé par la mise à jour automatique (ACTIF, ou EXPIRE si la fin est passée).
     *
     * @return {@code false} si le contrat n'existe pas ou n'est pas suspendu par un agent
     */
    public boolean leverSuspension(Long id) {
        return enTransaction(em -> {
            Contrat contrat = em.find(Contrat.class, id);
            if (contrat == null || !contrat.isSuspenduParAgent()) {
                return false;
            }
            contrat.setDateSuspension(null);
            contrat.setMotifSuspension(null);
            // Statut provisoire, corrigé aussitôt par ContratService.mettreAJourStatuts
            contrat.setStatut(StatutContrat.ACTIF);
            return true;
        });
    }

    /**
     * Change le statut d'un contrat (activation, suspension, expiration).
     * <p>
     * Pour une résiliation, utiliser {@link #resilier}, qui enregistre aussi la date et le motif.
     * Les transitions autorisées sont vérifiées par le service.
     *
     * @param id     id du contrat
     * @param statut nouveau statut
     * @return {@code false} si aucun contrat n'a cet id
     */
    public boolean changerStatut(Long id, StatutContrat statut) {
        return mettreAJour(id, c -> c.setStatut(statut));
    }

    /**
     * Résilie un contrat : statut RESILIE, avec la date et le motif de résiliation.
     * Dans la même transaction :
     * <ul>
     *     <li>son paiement, s'il n'est pas encore réglé (EN_ATTENTE ou EN_RETARD), est annulé :
     *         un contrat résilié ne doit plus rien ;</li>
     *     <li>la part de la prime déjà payée qui n'est plus couverte est remboursée (versement REMBOURSEMENT),
     *         si {@code remboursement} est positif. Le montant est calculé par le service.</li>
     * </ul>
     * Le statut est relu dans la transaction : un contrat déjà résilié ou expiré n'est pas modifié.
     *
     * @param id            id du contrat
     * @param date          date de résiliation (obligatoire en base pour un contrat résilié)
     * @param motif         raison de la résiliation, ou {@code null}
     * @param remboursement montant à rembourser au client, ou {@code null} / zéro s'il n'y a rien à rembourser
     * @param idAgent       agent qui résilie, ou {@code null} si c'est le client
     * @return {@code false} si aucun contrat n'a cet id, ou s'il est déjà résilié ou expiré
     */
    public boolean resilier(Long id, LocalDate date, String motif, BigDecimal remboursement, Long idAgent) {
        return enTransaction(em -> {
            Contrat contrat = em.find(Contrat.class, id);
            if (contrat == null || contrat.getStatut() == StatutContrat.RESILIE
                    || contrat.getStatut() == StatutContrat.EXPIRE) {
                return false;
            }
            if (remboursement != null && remboursement.signum() > 0) {
                Versement versement = new Versement();
                versement.setType(TypeVersement.REMBOURSEMENT);
                versement.setMontant(remboursement);
                versement.setDateVersement(date);
                versement.setContrat(contrat);
                versement.setAgent(idAgent == null ? null : em.find(Utilisateur.class, idAgent));
                em.persist(versement);
            }
            contrat.setStatut(StatutContrat.RESILIE);
            contrat.setDateResiliation(date);
            contrat.setMotifResiliation(motif);
            em.createQuery("UPDATE Paiement p SET p.statut = :annule"
                            + " WHERE p.contrat.id = :id AND p.statut IN :nonRegles")
                    .setParameter("annule", StatutPaiement.ANNULE)
                    .setParameter("id", id)
                    .setParameter("nonRegles", List.of(StatutPaiement.EN_ATTENTE, StatutPaiement.EN_RETARD))
                    .executeUpdate();
            return true;
        });
    }
}
