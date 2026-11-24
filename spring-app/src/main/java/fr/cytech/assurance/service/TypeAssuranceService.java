package fr.cytech.assurance.service;

import fr.cytech.assurance.repository.GarantieRepository;
import fr.cytech.assurance.repository.TypeAssuranceRepository;
import fr.cytech.assurance.exception.TypeAssuranceInvalideException;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.TypeAssurance;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gère les règles métier et la consultation du catalogue des types d'assurance.
 * Les servlets vérifient le format des saisies (Validateur), puis ce service applique les règles de gestion.
 */
@Service
public class TypeAssuranceService {

    private final TypeAssuranceRepository typeAssuranceRepository;
    private final GarantieRepository garantieRepository;

    /** Dépendances injectées par Spring. */
    public TypeAssuranceService(TypeAssuranceRepository typeAssuranceRepository, GarantieRepository garantieRepository) {
        this.typeAssuranceRepository = typeAssuranceRepository;
        this.garantieRepository = garantieRepository;
    }

    /**
     * Liste les types d'assurance actifs (non archivés), triés par nom,
     * pouvant être proposés pour un nouveau contrat.
     *
     * @return la liste des types d'assurance actifs
     */
    public List<TypeAssurance> listerActifs() {
        return typeAssuranceRepository.listerActifs();
    }


    /**
     * Liste les types d'assurance avec leurs garanties disponibles déjà chargées.
     *
     * @return la liste des types d'assurance avec leurs garanties
     */
    public List<TypeAssurance> listerAvecGaranties() {
        return typeAssuranceRepository.listerAvecGaranties();
    }

    /**
     * Récupère les identifiants des types d'assurance utilisés par au moins un contrat
     * (ces types ne peuvent pas être supprimés, uniquement archivés).
     *
     * @return l'ensemble des identifiants des types utilisés par un contrat
     */
    public Set<Long> idsUtilisesParContrat() {
        return typeAssuranceRepository.idsUtilisesParContrat();
    }

    /**
     * Recherche un type d'assurance par son identifiant avec ses garanties disponibles.
     *
     * @param id identifiant du type d'assurance
     * @return le type d'assurance avec ses garanties, ou null s'il n'existe pas
     */
    public TypeAssurance trouverAvecGaranties(Long id) {
        return typeAssuranceRepository.trouverAvecGaranties(id);
    }

    /**
     * Liste toutes les garanties existantes triées par nom afin de choisir celles à associer à un type.
     *
     * @return la liste complète des garanties triées par nom
     */
    public List<Garantie> listerGaranties() {
        return garantieRepository.listerParNom();
    }

    /**
     * Crée un nouveau type d'assurance avec les garanties associées en une seule opération.
     *
     * @param nom          nom du type d'assurance (doit être unique)
     * @param description  description optionnelle du type d'assurance
     * @param prixBase     tarif de base annuel (doit être strictement positif)
     * @param idsGaranties ensemble des identifiants des garanties proposées (au moins une requise)
     * @return le type d'assurance créé et enregistré
     * @throws TypeAssuranceInvalideException si le nom est déjà pris, si le prix est invalide,
     *                                        si aucune garantie n'est choisie ou si une garantie n'existe pas
     */
    @Transactional
    public TypeAssurance creer(String nom, String description, BigDecimal prixBase, Set<Long> idsGaranties) {
        verifierNomDisponible(nom, null);
        verifierPrixBase(prixBase);
        if (idsGaranties == null || idsGaranties.isEmpty()) {
            throw new TypeAssuranceInvalideException("Choisissez au moins une garantie.");
        }

        TypeAssurance type = new TypeAssurance();
        type.setNom(nom);
        type.setDescription(descriptionOuNull(description));
        type.setPrixBase(prixBase);
        for (Long idGarantie : idsGaranties) {
            Garantie garantie = garantieRepository.findById(idGarantie).orElse(null);
            if (garantie == null) {
                throw new TypeAssuranceInvalideException("Une des garanties choisies n'existe pas.");
            }
            type.getGaranties().add(garantie);
        }
        // Le type et ses lignes dans type_assurance_garantie sont enregistrés dans la même transaction
        return typeAssuranceRepository.save(type);
    }

    /**
     * Modifie le nom, la description et le prix de base d'un type d'assurance existant
     * (les garanties proposées et les contrats déjà signés ne sont pas modifiés).
     *
     * @param id          identifiant du type d'assurance à modifier
     * @param nom         nouveau nom du type d'assurance
     * @param description nouvelle description optionnelle
     * @param prixBase    nouveau prix de base (strictement positif)
     * @throws TypeAssuranceInvalideException si le type est introuvable, si le nom est déjà pris ou si le prix est invalide
     */
    public void modifier(Long id, String nom, String description, BigDecimal prixBase) {
        verifierNomDisponible(nom, id);
        verifierPrixBase(prixBase);

        // modifierInfos et non save (merge) : merge effacerait les garanties non chargées
        if (typeAssuranceRepository.modifierInfos(id, nom, descriptionOuNull(description), prixBase) == 0) {
            throw typeIntrouvable();
        }
    }

    /**
     * Supprime définitivement un type d'assurance et ses liens vers les garanties
     * (les garanties elles-mêmes sont conservées).
     *
     * @param id identifiant du type d'assurance à supprimer
     * @throws TypeAssuranceInvalideException si le type n'existe pas ou s'il est utilisé par au moins un contrat
     */
    @Transactional
    public void supprimer(Long id) {
        if (typeAssuranceRepository.findById(id).orElse(null) == null) {
            throw typeIntrouvable();
        }
        if (typeAssuranceRepository.estUtiliseParContrat(id)) {
            throw typeUtiliseParContrat();
        }
        try {
            typeAssuranceRepository.deleteById(id);
            // Suppression envoyée tout de suite à la base, pour intercepter ici le refus de la clé étrangère
            typeAssuranceRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // Un contrat a été créé avec ce type entre la vérification et la suppression :
            // la clé étrangère de la table contrat refuse la suppression
            throw typeUtiliseParContrat();
        }
    }

    /**
     * Archive un type d'assurance afin qu'il ne soit plus proposé pour les nouveaux contrats
     * (possible même si des contrats existants l'utilisent).
     *
     * @param id identifiant du type d'assurance à archiver
     * @throws TypeAssuranceInvalideException si le type n'existe pas
     */
    public void archiver(Long id) {
        if (typeAssuranceRepository.changerEtat(id, false) == 0) {
            throw typeIntrouvable();
        }
    }

    /**
     * Réactive un type d'assurance archivé pour qu'il soit de nouveau proposé aux nouveaux contrats.
     *
     * @param id identifiant du type d'assurance à réactiver
     * @throws TypeAssuranceInvalideException si le type n'existe pas
     */
    public void reactiver(Long id) {
        if (typeAssuranceRepository.changerEtat(id, true) == 0) {
            throw typeIntrouvable();
        }
    }

    /**
     * Associe une garantie existante à un type d'assurance (sans effet si elle était déjà proposée).
     *
     * @param idType     identifiant du type d'assurance
     * @param idGarantie identifiant de la garantie à ajouter
     * @throws TypeAssuranceInvalideException si le type d'assurance ou la garantie n'existe pas
     */
    @Transactional
    public void ajouterGarantie(Long idType, Long idGarantie) {
        TypeAssurance type = typeAssuranceRepository.trouverAvecGaranties(idType);
        Garantie garantie = garantieRepository.findById(idGarantie).orElse(null);
        if (type == null || garantie == null) {
            throw new TypeAssuranceInvalideException("Ce type d'assurance ou cette garantie n'existe pas.");
        }
        // Type lu dans cette transaction : la ligne de type_assurance_garantie est enregistrée à la fin de la méthode
        type.getGaranties().add(garantie);
    }

    /**
     * Retire une garantie proposée pour un type d'assurance (les contrats existants la conservent).
     *
     * @param idType     identifiant du type d'assurance
     * @param idGarantie identifiant de la garantie à retirer
     * @throws TypeAssuranceInvalideException si le type n'existe pas, si la garantie n'y est pas associée
     *                                        ou s'il s'agit de la dernière garantie du type
     */
    @Transactional
    public void retirerGarantie(Long idType, Long idGarantie) {
        TypeAssurance type = typeAssuranceRepository.trouverAvecGaranties(idType);
        if (type == null) {
            throw typeIntrouvable();
        }
        boolean proposee = type.getGaranties().stream().anyMatch(g -> g.getId().equals(idGarantie));
        if (!proposee) {
            throw new TypeAssuranceInvalideException("Cette garantie n'est pas proposée pour ce type d'assurance.");
        }
        if (type.getGaranties().size() == 1) {
            throw new TypeAssuranceInvalideException(
                    "Un type d'assurance doit proposer au moins une garantie : celle-ci ne peut pas être retirée.");
        }
        type.getGaranties().removeIf(g -> g.getId().equals(idGarantie));
    }

    /**
     * Vérifie que le nom choisi n'est pas déjà utilisé par un autre type d'assurance.
     *
     * @param nom     nom à vérifier
     * @param idExclu identifiant du type en cours de modification (ou null lors d'une création)
     * @throws TypeAssuranceInvalideException si un autre type porte déjà ce nom
     */
    private void verifierNomDisponible(String nom, Long idExclu) {
        if (typeAssuranceRepository.existeAvecNom(nom, idExclu)) {
            throw new TypeAssuranceInvalideException("Un type d'assurance nommé « " + nom + " » existe déjà.");
        }
    }

    /**
     * Vérifie que le prix de base est renseigné et strictement supérieur à zéro.
     *
     * @param prixBase prix de base à contrôler
     * @throws TypeAssuranceInvalideException si le prix est null ou inférieur ou égal à 0
     */
    private void verifierPrixBase(BigDecimal prixBase) {
        if (prixBase == null || prixBase.signum() <= 0) {
            throw new TypeAssuranceInvalideException("Le prix de base doit être supérieur à 0.");
        }
    }

    /**
     * Construit l'exception standard lorsqu'un type d'assurance est introuvable.
     *
     * @return une TypeAssuranceInvalideException indiquant que le type n'existe pas
     */
    private TypeAssuranceInvalideException typeIntrouvable() {
        return new TypeAssuranceInvalideException("Ce type d'assurance n'existe pas.");
    }

    /**
     * Construit l'exception standard lorsqu'on tente de supprimer un type lié à des contrats.
     *
     * @return une TypeAssuranceInvalideException expliquant qu'il faut archiver le type plutôt que le supprimer
     */
    private TypeAssuranceInvalideException typeUtiliseParContrat() {
        return new TypeAssuranceInvalideException(
                "Ce type d'assurance est utilisé par au moins un contrat : il ne peut pas être supprimé. "
                        + "Archivez-le pour ne plus le proposer.");
    }

    /**
     * Normalise une description vide ou composée uniquement d'espaces en valeur null.
     *
     * @param description texte saisi
     * @return la description telle quelle, ou null si elle est vide
     */
    private String descriptionOuNull(String description) {
        return description == null || description.isBlank() ? null : description;
    }
}
