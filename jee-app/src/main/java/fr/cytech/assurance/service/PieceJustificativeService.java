package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.PieceJustificativeDao;
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

/**
 * Pièces justificatives d'un sinistre (photos, constat, dépôt de plainte...).
 * <p>
 * Qui peut ajouter une pièce : les mêmes que pour la discussion (le client sur ses sinistres, un agent sur tous),
 * tant que le dossier n'est pas clos. Qui peut la télécharger : le client du sinistre, les agents et l'admin.
 * <p>
 * Le fichier est écrit sur le disque par {@link StockagePieces}, la base ne garde que son nom d'origine,
 * son nom sur le disque et son type.
 */
public class PieceJustificativeService {

    public static final long TAILLE_MAX = 5L * 1024 * 1024;   // 5 Mo par fichier
    public static final int NOMBRE_MAX_PAR_SINISTRE = 20;
    private static final int LONGUEUR_MAX_NOM = 255;          // colonne nom_fichier

    private final PieceJustificativeDao pieceDao = new PieceJustificativeDao();
    private final SinistreService sinistreService = new SinistreService();

    // Pièces du sinistre, de la plus ancienne à la plus récente
    public List<PieceJustificative> lister(Long idSinistre) {
        return pieceDao.listerParSinistre(idSinistre);
    }

    /**
     * Joint un fichier à un sinistre.
     *
     * @param idClient   client connecté si l'utilisateur est un client (vient de la session), sinon ignoré
     * @param nomOriginal nom du fichier sur l'ordinateur de l'utilisateur
     * @param taille     taille annoncée du fichier, en octets
     * @param contenu    contenu du fichier (non fermé)
     * @return la pièce enregistrée
     * @throws SinistreInvalideException si le sinistre n'est pas accessible ou est clos,
     *                                   ou si le fichier est vide, trop gros ou d'un type refusé
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
        if (pieceDao.listerParSinistre(sinistre.getId()).size() >= NOMBRE_MAX_PAR_SINISTRE) {
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

            String chemin = StockagePieces.enregistrer(flux, type);
            PieceJustificative piece = new PieceJustificative();
            piece.setNomFichier(nom);
            piece.setChemin(chemin);
            piece.setTypeMime(type.getTypeMime());
            try {
                return pieceDao.ajouter(sinistre.getId(), piece);
            } catch (RuntimeException e) {
                // Pas de fichier orphelin si l'enregistrement en base échoue
                StockagePieces.supprimer(chemin);
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Pièce à télécharger, si cet utilisateur a le droit de la voir.
     *
     * @param idClient client connecté si l'utilisateur est un client (vient de la session), sinon ignoré
     * @return la pièce, ou null si elle n'existe pas, n'est pas accessible ou si son fichier a disparu
     */
    public PieceJustificative trouverPourTelechargement(Long idPiece, Utilisateur utilisateur, Long idClient) {
        PieceJustificative piece = idPiece == null ? null : pieceDao.trouverAvecProprietaire(idPiece);
        if (piece == null) {
            return null;
        }
        boolean autorise = utilisateur.getRole() != Role.CLIENT
                || piece.getSinistre().getContrat().getClient().getId().equals(idClient);
        Path fichier = StockagePieces.chemin(piece.getChemin());
        return autorise && fichier != null && Files.isRegularFile(fichier) ? piece : null;
    }

    // Fichier d'une pièce trouvée par trouverPourTelechargement
    public Path fichier(PieceJustificative piece) {
        return StockagePieces.chemin(piece.getChemin());
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
