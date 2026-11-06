package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.ClientDao;
import fr.cytech.assurance.model.Client;

import java.util.Comparator;
import java.util.List;

// Règles et lectures concernant les clients
public class ClientService {

    private final ClientDao clientDao = new ClientDao();

    // Tous les clients (comptes actifs ou désactivés), triés par nom puis prénom, avec leur compte déjà chargé
    public List<Client> lister() {
        return clientDao.listerAvecUtilisateur();
    }

    // Clients dont le compte est actif, triés par nom puis prénom
    public List<Client> listerActifs() {
        return clientDao.listerTous().stream()
                .filter(c -> c.getUtilisateur().isActif())
                .sorted(Comparator.comparing((Client c) -> c.getUtilisateur().getNom())
                        .thenComparing(c -> c.getUtilisateur().getPrenom()))
                .toList();
    }

    // Fiche client de l'utilisateur connecté, ou null s'il n'est pas client
    public Client trouverParUtilisateur(Long idUtilisateur) {
        return clientDao.trouverParUtilisateur(idUtilisateur);
    }
}
