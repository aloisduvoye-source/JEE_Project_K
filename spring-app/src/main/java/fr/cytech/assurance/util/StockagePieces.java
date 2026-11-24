package fr.cytech.assurance.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 * Pièces justificatives sur le disque, hors du .war : dossier DOSSIER_PIECES (assurance.dossier-pieces),
 * ou à défaut un dossier temporaire.
 * Nom aléatoire (UUID) et type reconnu d'après le contenu : un nom ou une extension piégés sont sans effet.
 */
@Component
public class StockagePieces {

    private final Path dossier;

    /** @param dossier valeur de DOSSIER_PIECES, vide pour utiliser le dossier temporaire */
    public StockagePieces(@Value("${assurance.dossier-pieces:}") String dossier) {
        this.dossier = dossier(dossier);
    }

    /** Types acceptés : type MIME renvoyé au navigateur, extension sur le disque, signature du contenu. */
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

        /** Type correspondant au début du fichier, ou null s'il n'est pas accepté. */
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
     * Enregistre un fichier déjà reconnu, sous un nom aléatoire.
     *
     * @param contenu flux du fichier, lu jusqu'au bout mais non fermé
     * @return le nom du fichier sur le disque (colonne "chemin")
     */
    public String enregistrer(InputStream contenu, TypeFichier type) {
        String nom = UUID.randomUUID() + type.extension;
        try {
            Files.copy(contenu, this.dossier.resolve(nom), StandardCopyOption.REPLACE_EXISTING);
            return nom;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer la pièce " + nom, e);
        }
    }

    /** Chemin d'un fichier enregistré, ou null si le nom sortirait du dossier des pièces. */
    public Path chemin(String nom) {
        Path chemin = this.dossier.resolve(nom).normalize();
        return chemin.getParent().equals(this.dossier) ? chemin : null;
    }

    /** Supprime un fichier enregistré, sans erreur s'il n'existe pas. */
    public void supprimer(String nom) {
        Path chemin = chemin(nom);
        try {
            if (chemin != null) {
                Files.deleteIfExists(chemin);
            }
        } catch (IOException e) {
            // Fichier orphelin sans conséquence : il n'est référencé nulle part
        }
    }

    /** Dossier des pièces, créé s'il n'existe pas. */
    private static Path dossier(String valeur) {
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
