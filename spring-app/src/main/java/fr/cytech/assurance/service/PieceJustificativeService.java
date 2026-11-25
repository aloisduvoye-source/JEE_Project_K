package fr.cytech.assurance.service;

import fr.cytech.assurance.repository.PieceJustificativeRepository;
import fr.cytech.assurance.exception.SinistreInvalideException;
import fr.cytech.assurance.model.PieceJustificative;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.util.StockagePieces;
import fr.cytech.assurance.util.StockagePieces.TypeFichier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Pièces justificatives d'un sinistre (photos, constat, dépôt de plainte...).
 * <p>
 * Qui peut ajouter une pièce : les mêmes que pour la discussion (le client sur ses sinistres, un agent sur tous),
 * tant que le dossier n'est pas clos. Qui peut la télécharger : le client du sinistre, les agents et l'admin.
 * <p>
 * Le fichier est écrit sur le disque par {@link StockagePieces}, la base ne garde que son nom d'origine,
 * son nom sur le disque et son type.
 */
@Service
public class PieceJustificativeService {

    public static final long TAILLE_MAX = 5L * 1024 * 1024;   // 5 Mo par fichier
    public static final int NOMBRE_MAX_PAR_SINISTRE = 20;
    private static final int LONGUEUR_MAX_NOM = 255;          // colonne nom_fichier

    private final PieceJustificativeRepository pieceRepository;
    private final SinistreService sinistreService;
    private final StockagePieces stockagePieces;

    /** Dépendances injectées par Spring. */
    public PieceJustificativeService(PieceJustificativeRepository pieceRepository, SinistreService sinistreService,
                                     StockagePieces stockagePieces) {
        this.pieceRepository = pieceRepository;
        this.sinistreService = sinistreService;
        this.stockagePieces = stockagePieces;
    }

    /**
     * Liste les pièces justificatives d'un sinistre, de la plus ancienne à la plus récente.
     *
     * @param idSinistre identifiant du sinistre
     * @return la liste des pièces justificatives associées au sinistre
     */
    public List<PieceJustificative> lister(Long idSinistre) {
        return pieceRepository.listerParSinistre(idSinistre);
    }

    /**
     * Joint un fichier (PDF, JPG ou PNG de 5 Mo maximum) à un sinistre.
     *
     * @param idSinistre  identifiant du sinistre concerné
     * @param utilisateur utilisateur qui effectue l'ajout
     * @param idClient    identifiant du client connecté (si l'utilisateur est un client), sinon ignoré
     * @param nomOriginal nom du fichier sur l'ordinateur de l'utilisateur
     * @param taille      taille annoncée du fichier en octets
     * @param contenu     flux contenant les données du fichier
     * @return la pièce justificative enregistrée
     * @throws SinistreInvalideException si le sinistre est introuvable, non autorisé ou clos,
     *                                   si le fichier est vide, trop gros (plus de 5 Mo),
     *                                   d'un format refusé, ou si la limite de 20 pièces est atteinte
     */
    public PieceJustificative ajouter(Long idSinistre, Utilisateur utilisateur, Long idClient,
                                      String nomOriginal, long taille, InputStream contenu) {
        Sinistre sinistre = sinistreService.ouvertEnEcriture(idSinistre, utilisateur, idClient,
                "aucune pièce ne peut plus être ajoutée");
        String nom = nettoyerNom(nomOriginal);
        if (taille <= 0) {
            throw new SinistreInvalideException("Le fichier « " + nom + " » est vide.");
        }
        if (taille > TAILLE_MAX) {
            throw new SinistreInvalideException("Le fichier « " + nom + " » dépasse 5 Mo.");
        }
        if (pieceRepository.listerParSinistre(sinistre.getId()).size() >= NOMBRE_MAX_PAR_SINISTRE) {
            throw new SinistreInvalideException("Ce sinistre a déjà " + NOMBRE_MAX_PAR_SINISTRE + " pièces justificatives.");
        }

        try {
            // Le type est lu dans les premiers octets, puis le flux est rembobiné pour être enregistré en entier
            BufferedInputStream flux = new BufferedInputStream(contenu);
            flux.mark(16);
            TypeFichier type = TypeFichier.reconnaitre(flux.readNBytes(16));
            flux.reset();
            if (type == null) {
                throw new SinistreInvalideException("Le fichier « " + nom + " » n'est ni un PDF, ni une image JPG ou PNG.");
            }

            String chemin = stockagePieces.enregistrer(flux, type);
            PieceJustificative piece = new PieceJustificative();
            piece.setNomFichier(nom);
            piece.setChemin(chemin);
            piece.setTypeMime(type.getTypeMime());
            try {
                piece.setSinistre(sinistre);
                return pieceRepository.save(piece);
            } catch (RuntimeException e) {
                // Pas de fichier orphelin si l'enregistrement en base échoue
                stockagePieces.supprimer(chemin);
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Récupère une pièce justificative à télécharger si l'utilisateur a le droit de la voir
     * et que le fichier existe bien sur le disque.
     *
     * @param idPiece     identifiant de la pièce justificative
     * @param utilisateur utilisateur qui demande le téléchargement
     * @param idClient    identifiant du client connecté (si l'utilisateur est un client), sinon ignoré
     * @return la pièce justificative, ou null si elle n'existe pas, n'est pas autorisée ou si le fichier est absent
     */
    public PieceJustificative trouverPourTelechargement(Long idPiece, Utilisateur utilisateur, Long idClient) {
        PieceJustificative piece = idPiece == null ? null : pieceRepository.trouverAvecProprietaire(idPiece);
        if (piece == null) {
            return null;
        }
        boolean autorise = utilisateur.getRole() != Role.CLIENT
                || piece.getSinistre().getContrat().getClient().getId().equals(idClient);
        Path fichier = stockagePieces.chemin(piece.getChemin());
        return autorise && fichier != null && Files.isRegularFile(fichier) ? piece : null;
    }

    // Fichier d'une pièce trouvée par trouverPourTelechargement
    public Path fichier(PieceJustificative piece) {
        return stockagePieces.chemin(piece.getChemin());
    }

    // Nom affiché : sans le chemin que certains navigateurs envoient, sans caractères de contrôle, de longueur limitée
    static String nettoyerNom(String nom) {
        String propre = nom == null ? "" : nom.replace('\\', '/');
        propre = propre.substring(propre.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (propre.isEmpty()) {
            propre = "piece";
        }
        return propre.length() > LONGUEUR_MAX_NOM ? propre.substring(propre.length() - LONGUEUR_MAX_NOM) : propre;
    }
}
