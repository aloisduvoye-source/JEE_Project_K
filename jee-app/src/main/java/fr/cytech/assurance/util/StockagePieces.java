package fr.cytech.assurance.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Enregistrement des pièces justificatives sur le disque, hors du .war (écrasé à chaque déploiement).
 * <p>
 * Dossier : variable DOSSIER_PIECES (/data/pieces dans Docker, sur un volume qui survit aux redémarrages),
 * ou à défaut un dossier temporaire de la machine.
 * <p>
 * Chaque fichier est enregistré sous un nom aléatoire (UUID) : le nom choisi par le client, gardé en base,
 * n'est jamais utilisé comme chemin, ce qui évite les collisions et les noms piégés comme "../../web.xml".
 * <p>
 * Le type est reconnu d'après le contenu (signature au début du fichier) et non d'après l'extension,
 * qu'il suffit de renommer pour falsifier.
 */
public final class StockagePieces {

    private static final Path DOSSIER = dossier();

    private StockagePieces() {
    }

    // Types acceptés : type MIME renvoyé au navigateur, extension sur le disque, signature du contenu
    public enum TypeFichier {
        PDF("application/pdf", ".pdf", new byte[]{'%', 'P', 'D', 'F', '-'}),
        PNG("image/png", ".png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
        JPEG("image/jpeg", ".jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        private final String typeMime;
        private final String extension;
        private final byte[] signature;

        TypeFichier(String typeMime, String extension, byte[] signature) {
            this.typeMime = typeMime;
            this.extension = extension;
            this.signature = signature;
        }

        public String getTypeMime() {return typeMime;}

        // Type correspondant au début du fichier, ou null s'il n'est pas accepté
        public static TypeFichier reconnaitre(byte[] debut) {
            for (TypeFichier type : values()) {
                if (debut.length >= type.signature.length
                        && Arrays.equals(Arrays.copyOf(debut, type.signature.length), type.signature)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Enregistre un fichier déjà reconnu.
     *
     * @param contenu flux du fichier envoyé (lu jusqu'au bout, non fermé)
     * @param type    type reconnu d'après son début
     * @return le nom du fichier sur le disque (à garder dans la colonne "chemin")
     */
    public static String enregistrer(InputStream contenu, TypeFichier type) {
        String nom = UUID.randomUUID() + type.extension;
        try {
            Files.copy(contenu, DOSSIER.resolve(nom), StandardCopyOption.REPLACE_EXISTING);
            return nom;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer la pièce " + nom, e);
        }
    }

    /**
     * Chemin d'un fichier enregistré. Refuse tout nom qui sortirait du dossier des pièces.
     *
     * @param nom valeur de la colonne "chemin"
     * @return le chemin du fichier, ou null si le nom est invalide
     */
    public static Path chemin(String nom) {
        Path chemin = DOSSIER.resolve(nom).normalize();
        return chemin.getParent().equals(DOSSIER) ? chemin : null;
    }

    // Supprime un fichier enregistré (si l'enregistrement en base a échoué, par exemple). Sans erreur s'il n'existe pas
    public static void supprimer(String nom) {
        Path chemin = chemin(nom);
        try {
            if (chemin != null) {
                Files.deleteIfExists(chemin);
            }
        } catch (IOException e) {
            // Fichier orphelin sans conséquence : il n'est référencé nulle part
        }
    }

    private static Path dossier() {
        String valeur = Dotenv.configure().ignoreIfMissing().load().get("DOSSIER_PIECES");
        Path dossier = (valeur == null || valeur.isBlank()
                ? Paths.get(System.getProperty("java.io.tmpdir"), "assurance-pieces")
                : Paths.get(valeur.trim())).toAbsolutePath().normalize();
        try {
            return Files.createDirectories(dossier);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de créer le dossier des pièces justificatives " + dossier, e);
        }
    }
}
