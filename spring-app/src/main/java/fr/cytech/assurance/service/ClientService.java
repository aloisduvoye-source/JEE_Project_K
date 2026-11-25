package fr.cytech.assurance.service;

import fr.cytech.assurance.repository.ClientRepository;
import fr.cytech.assurance.model.Client;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Règles et lectures concernant les clients.
 */
@Service
public class ClientService {

    private final ClientRepository clientRepository;

    /** Dépendances injectées par Spring. */
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * @return tous les clients, actifs ou désactivés, triés par nom puis prénom, avec leur compte chargé
     */
    public List<Client> lister() {
        return clientRepository.listerAvecUtilisateur();
    }

    /**
     * @param motCle texte cherché dans le nom, le prénom ou l'email, sans tenir compte des majuscules
     * @return les clients correspondants, ou tous si le mot-clé est vide
     */
    public List<Client> rechercher(String motCle) {
        return motCle.isEmpty() ? lister() : clientRepository.rechercher(motCle);
    }

    /**
     * @return les clients dont le compte est actif, triés par nom puis prénom
     */
    public List<Client> listerActifs() {
        return clientRepository.findAll().stream()
                .filter(c -> c.getUtilisateur().isActif())
                .sorted(Comparator.comparing((Client c) -> c.getUtilisateur().getNom())
                        .thenComparing(c -> c.getUtilisateur().getPrenom()))
                .toList();
    }

    /**
     * @return la fiche client de cet utilisateur, ou null s'il n'est pas client
     */
    public Client trouverParUtilisateur(Long idUtilisateur) {
        return clientRepository.trouverParUtilisateur(idUtilisateur);
    }
}
