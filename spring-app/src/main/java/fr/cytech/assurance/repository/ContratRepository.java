package fr.cytech.assurance.repository;

import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.enums.StatutContrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Accès aux contrats (remplace ContratDao).
 * Créer un contrat avec son paiement, le modifier avant paiement et le résilier : dans le service (@Transactional),
 * car ces opérations touchent plusieurs tables.
 */
public interface ContratRepository extends JpaRepository<Contrat, Long> {

    /** @return le contrat avec son client, son type et ses garanties, ou null */
    @Query("SELECT DISTINCT c FROM Contrat c"
            + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " JOIN FETCH c.typeAssurance"
            + " LEFT JOIN FETCH c.garanties"
            + " WHERE c.id = :id")
    Contrat trouverAvecDetails(Long id);

    /** @return tous les contrats avec client et type, du plus récent au plus ancien */
    @Query("SELECT c FROM Contrat c"
            + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " JOIN FETCH c.typeAssurance"
            + " ORDER BY c.id DESC")
    List<Contrat> listerTousAvecDetails();

    /** @return les contrats du client avec leur type et leurs garanties, par date de début décroissante */
    @Query("SELECT DISTINCT c FROM Contrat c"
            + " JOIN FETCH c.typeAssurance"
            + " LEFT JOIN FETCH c.garanties"
            + " WHERE c.client.id = :idClient"
            + " ORDER BY c.dateDebut DESC")
    List<Contrat> listerParClient(Long idClient);

    /** @return les contrats de ce statut avec client et type, du plus récent au plus ancien */
    @Query("SELECT c FROM Contrat c"
            + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur"
            + " JOIN FETCH c.typeAssurance"
            + " WHERE c.statut = :statut"
            + " ORDER BY c.id DESC")
    List<Contrat> listerParStatut(StatutContrat statut);

    /** @return les contrats dont le statut peut encore changer : EN_ATTENTE, ACTIF et SUSPENDU */
    @Query("SELECT c FROM Contrat c WHERE c.statut IN (fr.cytech.assurance.model.enums.StatutContrat.EN_ATTENTE,"
            + " fr.cytech.assurance.model.enums.StatutContrat.ACTIF, fr.cytech.assurance.model.enums.StatutContrat.SUSPENDU)")
    List<Contrat> listerEnCours();

    /** @return le contrat qui renouvelle celui-ci, ou null */
    @Query("SELECT c FROM Contrat c WHERE c.contratPrecedent.id = :idContrat")
    Contrat trouverRenouvellement(Long idContrat);

    /**
     * Recherche (utilisée par rechercher) : chaque critère null est ignoré.
     *
     * @param motif  texte en minuscules entouré de %, ou null
     * @param numero numéro de contrat saisi, ou null
     */
    @Query("SELECT c FROM Contrat c"
            + " JOIN FETCH c.client cl JOIN FETCH cl.utilisateur u"
            + " JOIN FETCH c.typeAssurance t"
            + " WHERE (:motif IS NULL OR LOWER(u.nom) LIKE :motif OR LOWER(u.prenom) LIKE :motif"
            + "   OR LOWER(u.email) LIKE :motif OR LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE :motif"
            + "   OR LOWER(CONCAT(u.nom, ' ', u.prenom)) LIKE :motif OR c.id = :numero)"
            + " AND (:statut IS NULL OR c.statut = :statut)"
            + " AND (:idType IS NULL OR t.id = :idType)"
            + " ORDER BY c.id DESC")
    List<Contrat> rechercherAvec(String motif, Long numero, StatutContrat statut, Long idType);

    /**
     * Recherche de contrats pour les agents. Critères facultatifs (null : ignoré).
     *
     * @param texte numéro de contrat ("100001" ou "#100001"), ou morceau du nom, du prénom ou de l'email du client
     */
    default List<Contrat> rechercher(String texte, StatutContrat statut, Long idType) {
        if (texte == null) {
            return rechercherAvec(null, null, statut, idType);
        }
        Long numero;
        try {
            numero = Long.valueOf(texte.startsWith("#") ? texte.substring(1) : texte);
        } catch (NumberFormatException e) {
            numero = null;
        }
        return rechercherAvec("%" + texte.toLowerCase() + "%", numero, statut, idType);
    }

    /** Change le statut seulement s'il vaut toujours {@code ancien}. @return 1 si le statut a changé, sinon 0 */
    @Transactional
    @Modifying
    @Query("UPDATE Contrat c SET c.statut = :nouveau WHERE c.id = :id AND c.statut = :ancien")
    int changerStatutSi(Long id, StatutContrat ancien, StatutContrat nouveau);

    /** Change le statut, sans condition. @return 0 si le contrat n'existe pas */
    @Transactional
    @Modifying
    @Query("UPDATE Contrat c SET c.statut = :statut WHERE c.id = :id")
    int changerStatut(Long id, StatutContrat statut);

    /** Suspend un contrat ACTIF (décision d'un agent). @return 0 si le contrat n'existe pas ou n'est pas actif */
    @Transactional
    @Modifying
    @Query("UPDATE Contrat c SET c.statut = fr.cytech.assurance.model.enums.StatutContrat.SUSPENDU,"
            + " c.dateSuspension = :date, c.motifSuspension = :motif"
            + " WHERE c.id = :id AND c.statut = fr.cytech.assurance.model.enums.StatutContrat.ACTIF")
    int suspendre(Long id, LocalDate date, String motif);

    /**
     * Lève une suspension décidée par un agent. Le statut ACTIF est provisoire :
     * ContratService.mettreAJourStatuts le corrige aussitôt.
     *
     * @return 0 si le contrat n'existe pas ou n'est pas suspendu par un agent
     */
    @Transactional
    @Modifying
    @Query("UPDATE Contrat c SET c.statut = fr.cytech.assurance.model.enums.StatutContrat.ACTIF,"
            + " c.dateSuspension = NULL, c.motifSuspension = NULL"
            + " WHERE c.id = :id AND c.dateSuspension IS NOT NULL"
            + " AND c.statut = fr.cytech.assurance.model.enums.StatutContrat.SUSPENDU")
    int leverSuspension(Long id);
}
