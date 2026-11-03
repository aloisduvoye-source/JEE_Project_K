package fr.cytech.assurance.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Vérification des saisies d'un formulaire, commune à toutes les servlets.
 * Chaque méthode ajoute un message au champ concerné ; seule la première erreur d'un champ est gardée.
 *
 * <pre>{@code
 *   Validateur v = new Validateur();
 *   v.obligatoire("nom", nom, "Le nom", 100);
 *   v.email("email", email);
 *   if (!v.aErreur("email") && emailDejaPris) {v.erreur("email", "Cet email est déjà utilisé.");}
 *   if (!v.estValide()) {request.setAttribute("erreurs", v.getErreurs()); ...}
 * }</pre>
 */
public class Validateur {

    public static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;
    private static final Pattern FORMAT_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern FORMAT_TELEPHONE = Pattern.compile("^[0-9 +.()-]{6,20}$");

    private final Map<String, String> erreurs = new HashMap<>();

    // Paramètre de formulaire sans espaces autour, jamais null
    public static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    // Ajoute un message, sauf si le champ a déjà une erreur
    public void erreur(String champ, String message) {
        erreurs.putIfAbsent(champ, message);
    }

    public boolean aErreur(String champ) {return erreurs.containsKey(champ);}

    public boolean estValide() {return erreurs.isEmpty();}

    public Map<String, String> getErreurs() {return erreurs;}

    // Texte obligatoire, de longueur limitée. libelle : "Le nom", "L'identifiant"...
    public void obligatoire(String champ, String valeur, String libelle, int longueurMax) {
        if (valeur.isEmpty()) {
            erreur(champ, libelle + " est obligatoire.");
        } else {
            longueur(champ, valeur, libelle, longueurMax);
        }
    }

    // Texte facultatif : seule la longueur est vérifiée
    public void longueur(String champ, String valeur, String libelle, int longueurMax) {
        if (valeur.length() > longueurMax) {
            erreur(champ, libelle + " ne doit pas dépasser " + longueurMax + " caractères.");
        }
    }

    // Email obligatoire et bien formé
    public void email(String champ, String valeur) {
        if (valeur.isEmpty()) {
            erreur(champ, "L'email est obligatoire.");
        } else if (valeur.length() > 180 || !FORMAT_EMAIL.matcher(valeur).matches()) {
            erreur(champ, "L'email n'est pas valide.");
        }
    }

    // Téléphone facultatif
    public void telephone(String champ, String valeur) {
        if (!valeur.isEmpty() && !FORMAT_TELEPHONE.matcher(valeur).matches()) {
            erreur(champ, "Le numéro de téléphone n'est pas valide.");
        }
    }

    // Date facultative, qui ne peut pas être dans le futur (ex. date de naissance)
    // Renvoie la date lue, ou null si le champ est vide ou invalide
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

    // Identifiant choisi dans une liste (client, type d'assurance...) : obligatoire et numérique
    // Renvoie l'identifiant lu, ou null s'il est absent ou invalide
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

    // Date obligatoire (au format du champ HTML type="date" : AAAA-MM-JJ)
    // Renvoie la date lue, ou null si le champ est vide ou invalide
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

    // Montant obligatoire, avec au plus 2 décimales (virgule ou point acceptés)
    // Renvoie le montant lu, ou null si le champ est vide ou invalide
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
            return montant;
        } catch (NumberFormatException e) {
            erreur(champ, libelle + " n'est pas un montant valide.");
            return null;
        }
    }

    // Montant obligatoire et strictement positif (prix, prime...), avec au plus 2 décimales
    // Renvoie le montant lu, ou null si le champ est vide, invalide, nul ou négatif
    public BigDecimal montantPositif(String champ, String valeur, String libelle) {
        BigDecimal montant = montant(champ, valeur, libelle);
        if (montant != null && montant.signum() <= 0) {
            erreur(champ, libelle + " doit être supérieur à 0.");
            return null;
        }
        return montant;
    }

    // Nouveau mot de passe : longueur minimale, puis confirmation identique
    public void nouveauMotDePasse(String champ, String valeur, String libelle,
                                  String champConfirmation, String confirmation) {
        if (valeur == null || valeur.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            erreur(champ, libelle + " doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        } else if (!valeur.equals(confirmation)) {
            erreur(champConfirmation, "La confirmation ne correspond pas.");
        }
    }
}
