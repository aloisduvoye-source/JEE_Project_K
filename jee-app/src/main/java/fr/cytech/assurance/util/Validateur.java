package fr.cytech.assurance.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Vérification des saisies d'un formulaire, commune à toutes les servlets.
 * Chaque méthode ajoute un message au champ concerné, qui commence par son libellé (« Le nom »...) ;
 * seule la première erreur d'un champ est gardée.
 */
public class Validateur {

    public static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    /** Montants en DECIMAL(12,2) en base : au plus 10 chiffres avant la virgule. */
    private static final int CHIFFRES_ENTIERS_MAX_MONTANT = 10;
    private static final Pattern FORMAT_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern FORMAT_TELEPHONE = Pattern.compile("^[0-9 +.()-]{6,20}$");

    private final Map<String, String> erreurs = new HashMap<>();

    /** Paramètre de formulaire sans espaces autour, jamais null. */
    public static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    /** Identifiant facultatif lu dans l'adresse (?id=...), ou null s'il est absent ou non numérique, sans erreur. */
    public static Long idOuNull(String valeur) {
        try {
            return Long.valueOf(nettoyer(valeur));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Valeur d'énumération facultative lue dans l'adresse (?statut=...), ou null si elle est absente ou inconnue. */
    public static <E extends Enum<E>> E enumOuNull(Class<E> type, String valeur) {
        try {
            return Enum.valueOf(type, nettoyer(valeur));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Ajoute un message, sauf si le champ a déjà une erreur. */
    public void erreur(String champ, String message) {
        erreurs.putIfAbsent(champ, message);
    }

    /** Vrai si le champ a déjà une erreur. */
    public boolean aErreur(String champ) {return erreurs.containsKey(champ);}

    /** Vrai si aucune erreur n'a été ajoutée. */
    public boolean estValide() {return erreurs.isEmpty();}

    /** Messages d'erreur par nom de champ. */
    public Map<String, String> getErreurs() {return erreurs;}

    /** Texte obligatoire, de longueur limitée. */
    public void obligatoire(String champ, String valeur, String libelle, int longueurMax) {
        if (valeur.isEmpty()) {
            erreur(champ, libelle + " est obligatoire.");
        } else {
            longueur(champ, valeur, libelle, longueurMax);
        }
    }

    /** Texte facultatif : seule la longueur est vérifiée. */
    public void longueur(String champ, String valeur, String libelle, int longueurMax) {
        if (valeur.length() > longueurMax) {
            erreur(champ, libelle + " ne doit pas dépasser " + longueurMax + " caractères.");
        }
    }

    /** Email obligatoire et bien formé. */
    public void email(String champ, String valeur) {
        if (valeur.isEmpty()) {
            erreur(champ, "L'email est obligatoire.");
        } else if (valeur.length() > 180 || !FORMAT_EMAIL.matcher(valeur).matches()) {
            erreur(champ, "L'email n'est pas valide.");
        }
    }

    /** Téléphone facultatif, bien formé s'il est renseigné. */
    public void telephone(String champ, String valeur) {
        if (!valeur.isEmpty() && !FORMAT_TELEPHONE.matcher(valeur).matches()) {
            erreur(champ, "Le numéro de téléphone n'est pas valide.");
        }
    }

    /** Date facultative, pas dans le futur (date de naissance) : la date lue, ou null si elle est vide ou invalide. */
    public LocalDate datePassee(String champ, String valeur, String libelle) {
        if (valeur.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(valeur);
            if (date.isAfter(LocalDate.now())) {
                erreur(champ, libelle + " ne peut pas être dans le futur.");
            }
            return date;
        } catch (DateTimeParseException e) {
            erreur(champ, libelle + " n'est pas valide.");
            return null;
        }
    }

    /** Identifiant obligatoire choisi dans une liste : l'identifiant lu, ou null s'il est absent ou invalide. */
    public Long identifiant(String champ, String valeur, String libelle) {
        if (valeur.isEmpty()) {
            erreur(champ, libelle + " est obligatoire.");
            return null;
        }
        try {
            return Long.valueOf(valeur);
        } catch (NumberFormatException e) {
            erreur(champ, libelle + " n'est pas valide.");
            return null;
        }
    }

    /** Identifiants des cases cochées (valeurs null si aucune) : les identifiants valides, vide si rien n'est coché. */
    public Set<Long> identifiants(String champ, String[] valeurs, String libelle) {
        Set<Long> ids = new HashSet<>();
        if (valeurs != null) {
            for (String valeur : valeurs) {
                Long id = identifiant(champ, nettoyer(valeur), libelle);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    /** Date obligatoire au format AAAA-MM-JJ : la date lue, ou null si elle est vide ou invalide. */
    public LocalDate date(String champ, String valeur, String libelle) {
        if (valeur.isEmpty()) {
            erreur(champ, libelle + " est obligatoire.");
            return null;
        }
        try {
            return LocalDate.parse(valeur);
        } catch (DateTimeParseException e) {
            erreur(champ, libelle + " n'est pas valide.");
            return null;
        }
    }

    /** Montant obligatoire (virgule ou point, 2 décimales au plus) : le montant lu, ou null s'il est vide ou invalide. */
    public BigDecimal montant(String champ, String valeur, String libelle) {
        if (valeur.isEmpty()) {
            erreur(champ, libelle + " est obligatoire.");
            return null;
        }
        try {
            BigDecimal montant = new BigDecimal(valeur.replace(',', '.'));
            if (montant.scale() > 2) {
                erreur(champ, libelle + " ne doit pas avoir plus de 2 décimales.");
                return null;
            }
            // precision - scale = nombre de chiffres avant la virgule
            if (montant.precision() - montant.scale() > CHIFFRES_ENTIERS_MAX_MONTANT) {
                erreur(champ, libelle + " ne doit pas dépasser 9 999 999 999,99 €.");
                return null;
            }
            return montant;
        } catch (NumberFormatException e) {
            erreur(champ, libelle + " n'est pas un montant valide.");
            return null;
        }
    }

    /** Montant obligatoire et strictement positif (prix, prime...), ou null s'il est vide, invalide, nul ou négatif. */
    public BigDecimal montantPositif(String champ, String valeur, String libelle) {
        BigDecimal montant = montant(champ, valeur, libelle);
        if (montant != null && montant.signum() <= 0) {
            erreur(champ, libelle + " doit être supérieur à 0.");
            return null;
        }
        return montant;
    }

    /** Montant facultatif, positif ou nul (plafond, franchise...), ou null s'il est vide, invalide ou négatif. */
    public BigDecimal montantFacultatif(String champ, String valeur, String libelle) {
        if (valeur.isEmpty()) {
            return null;
        }
        BigDecimal montant = montant(champ, valeur, libelle);
        if (montant != null && montant.signum() < 0) {
            erreur(champ, libelle + " ne peut pas être un montant négatif.");
            return null;
        }
        return montant;
    }

    /** Nouveau mot de passe : longueur minimale, puis confirmation identique. */
    public void nouveauMotDePasse(String champ, String valeur, String libelle,
                                  String champConfirmation, String confirmation) {
        if (valeur == null || valeur.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            erreur(champ, libelle + " doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        } else if (!valeur.equals(confirmation)) {
            erreur(champConfirmation, "La confirmation ne correspond pas.");
        }
    }
}
