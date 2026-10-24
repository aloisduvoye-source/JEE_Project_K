package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Client;
import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// CRUD (trouverParId, listerTous, creer, modifier, supprimer) hérité de GenericDao
public class ContratDao extends GenericDao<Contrat> {

    // Numéro du tout premier contrat ; les suivants sont incrémentés
    private static final long PREMIER_NUMERO = 100001;

    public ContratDao() {
        super(Contrat.class);
    }

    // Prochain numéro de contrat libre : le plus grand existant + 1
    public long prochainNumero() {
        Long max = enLecture(em -> em.createQuery("SELECT MAX(c.numero) FROM Contrat c", Long.class)
                .getSingleResult());
        return max == null ? PREMIER_NUMERO : max + 1;
    }

    // Crée le contrat en le reliant au client, au type d'assurance et aux garanties désignés par leur id,
    // dans une seule transaction : si l'un d'eux n'existe pas, rien n'est enregistré
    // Les règles métier (garanties proposées par le type, dates, prime...) sont vérifiées par le service
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

    // Contrat avec son client, son type et ses garanties déjà chargés (lisibles dans une JSP), ou null
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

    // Tous les contrats, du plus récent au plus ancien, avec leur client et leur type déjà chargés
    public List<Contrat> listerTousAvecDetails() {
        return enLecture(em -> em.createQuery(
                        "SELECT c FROM Contrat c"
                                + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
                                + " JOIN FETCH c.typeAssurance"
                                + " ORDER BY c.numero DESC", Contrat.class)
                .getResultList());
    }

    // Contrats d'un client, du plus récent au plus ancien, avec leur type et leurs garanties
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
