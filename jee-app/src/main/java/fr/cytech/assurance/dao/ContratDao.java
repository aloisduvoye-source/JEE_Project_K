package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;

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
     * Crée un contrat relié à un client, un type d'assurance et des garanties désignés par leur id.
     * <p>
     * Tout se fait dans une seule transaction : si l'un d'eux n'existe pas, rien n'est enregistré.
     * Les règles métier (garanties proposées par le type, dates, prime...) sont vérifiées par le service.
     *
     * @param contrat         contrat à créer, sans client, type ni garanties
     * @param idClient        identifiant du client
     * @param idTypeAssurance identifiant du type d'assurance
     * @param idsGaranties    identifiants des garanties souscrites
     * @return le contrat enregistré
     * @throws IllegalArgumentException si le client, le type ou l'une des garanties n'existe pas
     */
    public Contrat creerAvecGaranties(Contrat contrat, Long idClient, Long idTypeAssurance, Set<Long> idsGaranties) {
        return enTransaction(em -> {
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
            return contrat;
        });
    }

    /**
     * Recherche un contrat avec son client, son type et ses garanties déjà chargés (lisibles dans une JSP).
     *
     * @param id identifiant du contrat
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
     * @param idClient identifiant du client
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
}
