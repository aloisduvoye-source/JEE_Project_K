package fr.cytech.assurance.ai;

import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.Sinistre;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.Versement;
import fr.cytech.assurance.model.enums.StatutContrat;
import fr.cytech.assurance.model.enums.StatutPaiement;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Construit les instructions envoyées au modèle d'IA : son rôle, les règles à respecter,
 * les connaissances générales (procédures, documents) et les données du client connecté.
 * <p>
 * Le modèle ne connaît pas la base : il ne peut répondre « Quels sont mes contrats actifs ? »
 * que parce que ses contrats sont écrits ici, en texte. Seules les données du client connecté
 * y figurent : il ne peut donc rien apprendre sur les autres clients.
 * <p>
 * Classe sans accès à la base : le service lui passe les données déjà chargées.
 */
public final class ContexteAssistant {

    private static final String FICHIER_CONNAISSANCES = "/assistant/connaissances.txt";
    private static final String CONNAISSANCES = chargerConnaissances();

    // Paiements réglés ou annulés plus anciens que ça : non envoyés au modèle. Ils s'accumulent d'année
    // en année et alourdiraient chaque question, alors que le client demande presque toujours les récents.
    static final int MOIS_DE_PAIEMENTS = 12;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ROLE_ET_REGLES = """
            Tu es l'assistant virtuel d'une compagnie d'assurance. Tu aides un client connecté à son espace en ligne.

            Règles à respecter :
            - Réponds toujours en français, en vouvoyant le client, de façon claire et concise.
            - Pour tout ce qui concerne le client (contrats, garanties, paiements, sinistres), base-toi uniquement
              sur la section « Données du client ». N'invente jamais un contrat, une garantie, un montant ou un statut.
              Si l'information n'y figure pas, dis-le simplement.
            - Pour les procédures et les définitions, base-toi sur la section « Connaissances générales ».
            - Pour comparer deux garanties, appuie-toi sur « Garanties proposées par la compagnie » :
              explique ce que couvre chacune, puis leurs différences (plafond et franchise compris),
              et précise si le client les a souscrites.
            - Quand le client parle de « mon assurance auto » ou de « mon dernier sinistre », retrouve le contrat
              ou le sinistre correspondant dans ses données (type d'assurance, date la plus récente).
            - Tu ne peux effectuer aucune action (déclarer, payer, résilier, modifier) : explique comment le faire
              depuis l'espace client, ou oriente-le vers un agent.
            - Ne prends aucune décision sur l'acceptation d'un sinistre ou le montant d'une indemnisation :
              seul un agent en décide.
            - Si la question n'a aucun rapport avec l'assurance, indique poliment que tu ne peux aider que sur ce sujet.
            - N'utilise pas de Markdown (pas d'astérisques, pas de dièses) : écris du texte simple.
              Pour une liste, commence chaque ligne par un tiret.
            """;

    private ContexteAssistant() {
    }

    /**
     * @param utilisateur compte du client connecté
     * @param contrats    ses contrats, avec leur type et leurs garanties chargés
     * @param paiements   ses paiements, avec leur contrat chargé
     * @param sinistres   ses sinistres, avec leur contrat et son type chargés
     * @param versements  les versements qu'il a reçus (indemnisations, remboursements), avec leur contrat chargé
     * @param catalogue   toutes les garanties proposées par la compagnie (pour comparer deux garanties,
     *                    même si le client ne les a pas souscrites)
     * @param aujourdhui  date du jour (pour que le modèle sache si une date est passée)
     * @return le texte des instructions
     */
    public static String instructions(Utilisateur utilisateur, List<Contrat> contrats,
                                      List<Paiement> paiements, List<Sinistre> sinistres,
                                      List<Versement> versements, List<Garantie> catalogue,
                                      LocalDate aujourdhui) {
        StringBuilder texte = new StringBuilder(ROLE_ET_REGLES);

        texte.append("\n=== Connaissances générales ===\n\n").append(CONNAISSANCES);
        decrireCatalogue(texte, catalogue);

        texte.append("\n\n=== Données du client ===\n\n");
        texte.append("Date du jour : ").append(date(aujourdhui)).append('\n');
        texte.append("Client : ").append(utilisateur.getPrenom()).append(' ').append(utilisateur.getNom()).append('\n');

        decrireContrats(texte, contrats);
        decrirePaiements(texte, paiements, aujourdhui);
        decrireSinistres(texte, sinistres);
        decrireVersements(texte, versements);
        return texte.toString();
    }

    private static void decrireCatalogue(StringBuilder texte, List<Garantie> catalogue) {
        texte.append("\n\n## Garanties proposées par la compagnie\n\n");
        if (catalogue.isEmpty()) {
            texte.append("Aucune garantie au catalogue.\n");
        }
        for (Garantie garantie : catalogue) {
            texte.append("- ");
            decrireGarantie(texte, garantie);
        }
    }

    // "Vol (plafond : 20 000,00 €, franchise : 150,00 €) : Vol ou tentative de vol des biens assurés."
    private static void decrireGarantie(StringBuilder texte, Garantie garantie) {
        texte.append(garantie.getNom())
                .append(" (plafond : ").append(montantOuNonPrecise(garantie.getPlafond()))
                .append(", franchise : ").append(montantOuNonPrecise(garantie.getFranchise())).append(')');
        if (garantie.getDescription() != null) {
            texte.append(" : ").append(surUneLigne(garantie.getDescription()));
        }
        texte.append('\n');
    }

    private static void decrireContrats(StringBuilder texte, List<Contrat> contrats) {
        texte.append("\nContrats (").append(contrats.size()).append(") :\n");
        if (contrats.isEmpty()) {
            texte.append("Aucun contrat.\n");
            return;
        }
        for (Contrat contrat : contrats) {
            texte.append("- Contrat n° ").append(contrat.getId())
                    .append(" : assurance ").append(contrat.getTypeAssurance().getNom())
                    .append(", statut ").append(libelle(contrat.getStatut()))
                    .append(", du ").append(date(contrat.getDateDebut()))
                    .append(" au ").append(date(contrat.getDateFin()))
                    .append(", prime annuelle ").append(montant(contrat.getPrime()));
            if (contrat.isSuspenduParAgent()) {
                texte.append(", suspendu par un agent depuis le ").append(date(contrat.getDateSuspension()))
                        .append(" (motif : ").append(contrat.getMotifSuspension()).append(')');
            } else if (contrat.getStatut() == StatutContrat.SUSPENDU) {
                texte.append(", suspendu faute de paiement");
            }
            if (contrat.getContratPrecedent() != null) {
                texte.append(", renouvelle le contrat n° ").append(contrat.getContratPrecedent().getId());
            }
            if (contrat.getDateResiliation() != null) {
                texte.append(", résilié le ").append(date(contrat.getDateResiliation()));
                if (contrat.getMotifResiliation() != null) {
                    texte.append(" (motif : ").append(contrat.getMotifResiliation()).append(')');
                }
            }
            texte.append('\n');

            // Ordre alphabétique : la réponse ne change pas d'une question à l'autre
            List<Garantie> garanties = contrat.getGaranties().stream()
                    .sorted(Comparator.comparing(Garantie::getNom))
                    .toList();
            texte.append("  Garanties souscrites :").append(garanties.isEmpty() ? " aucune" : "").append('\n');
            for (Garantie garantie : garanties) {
                texte.append("  * ");
                decrireGarantie(texte, garantie);
            }
        }
    }

    private static void decrirePaiements(StringBuilder texte, List<Paiement> paiements, LocalDate aujourdhui) {
        LocalDate limite = aujourdhui.minusMonths(MOIS_DE_PAIEMENTS);
        List<Paiement> envoyes = paiements.stream()
                .filter(paiement -> estAEnvoyer(paiement, limite))
                .toList();
        int anciens = paiements.size() - envoyes.size();

        texte.append("\nPaiements des ").append(MOIS_DE_PAIEMENTS).append(" derniers mois et paiements non réglés (")
                .append(envoyes.size()).append(") :\n");
        if (envoyes.isEmpty()) {
            texte.append("Aucun paiement.\n");
        }
        for (Paiement paiement : envoyes) {
            texte.append("- Contrat n° ").append(paiement.getContrat().getId())
                    .append(" : ").append(montant(paiement.getMontant()))
                    .append(", échéance le ").append(date(paiement.getDateEcheance()))
                    .append(", statut ").append(libelle(paiement.getStatut()));
            if (paiement.getDatePaiement() != null) {
                texte.append(", payé le ").append(date(paiement.getDatePaiement()));
            }
            if (paiement.getModePaiement() != null) {
                texte.append(" par ").append(libelle(paiement.getModePaiement()));
            }
            texte.append('\n');
        }
        // Le modèle doit savoir que la liste n'est pas complète, pour ne pas affirmer qu'il n'y a rien eu avant
        if (anciens > 0) {
            texte.append('(').append(anciens).append(anciens > 1
                            ? " paiements plus anciens, réglés ou annulés, ne sont pas listés"
                            : " paiement plus ancien, réglé ou annulé, n'est pas listé")
                    .append(" : le client peut tous les consulter dans la section « Mes paiements » de son espace.)\n");
        }
    }

    // Un paiement en attente ou en retard est toujours envoyé, quelle que soit son échéance
    private static boolean estAEnvoyer(Paiement paiement, LocalDate limite) {
        if (paiement.getStatut() == StatutPaiement.EN_ATTENTE || paiement.getStatut() == StatutPaiement.EN_RETARD) {
            return true;
        }
        return paiement.getDateEcheance() == null || !paiement.getDateEcheance().isBefore(limite);
    }

    private static void decrireSinistres(StringBuilder texte, List<Sinistre> sinistres) {
        texte.append("\nSinistres, du plus récent au plus ancien (").append(sinistres.size()).append(") :\n");
        if (sinistres.isEmpty()) {
            texte.append("Aucun sinistre déclaré.\n");
            return;
        }
        for (Sinistre sinistre : sinistres) {
            texte.append("- Sinistre n° ").append(sinistre.getId())
                    .append(" sur le contrat n° ").append(sinistre.getContrat().getId())
                    .append(" (").append(sinistre.getContrat().getTypeAssurance().getNom()).append(')')
                    .append(" : ").append(libelle(sinistre.getTypeSinistre()))
                    .append(" survenu le ").append(date(sinistre.getDateSinistre()))
                    .append(", déclaré le ").append(date(sinistre.getDateDeclaration().toLocalDate()))
                    .append(", statut ").append(libelle(sinistre.getStatut()));
            if (sinistre.getMontantEstime() != null) {
                texte.append(", montant estimé ").append(montant(sinistre.getMontantEstime()));
            }
            if (sinistre.getDatePriseEnCharge() != null) {
                texte.append(", pris en charge le ").append(date(sinistre.getDatePriseEnCharge().toLocalDate()));
            }
            if (sinistre.getDateDecision() != null) {
                texte.append(", décision le ").append(date(sinistre.getDateDecision().toLocalDate()));
            }
            texte.append(sinistre.getAgent() != null ? ", dossier suivi par un agent" : ", pas encore attribué à un agent");
            texte.append(". Description : ").append(surUneLigne(sinistre.getDescription())).append('\n');
        }
    }

    private static void decrireVersements(StringBuilder texte, List<Versement> versements) {
        texte.append("\nVersements reçus de l'assurance, indemnisations et remboursements (")
                .append(versements.size()).append(") :\n");
        if (versements.isEmpty()) {
            texte.append("Aucun versement.\n");
            return;
        }
        for (Versement versement : versements) {
            texte.append("- ").append(montant(versement.getMontant()))
                    .append(" le ").append(date(versement.getDateVersement())).append(" : ");
            if (versement.getSinistre() != null) {
                texte.append("indemnisation du sinistre n° ").append(versement.getSinistre().getId());
                // Détail du calcul, pour que l'assistant puisse expliquer le montant au client
                if (versement.getGarantie() != null) {
                    texte.append(", garantie ").append(versement.getGarantie().getNom())
                            .append(" : dommage retenu ").append(montant(versement.getMontantDommage()))
                            .append(" moins la franchise de ").append(montantOuNonPrecise(versement.getFranchiseAppliquee()));
                    if (versement.isPlafonne()) {
                        texte.append(", limité au plafond de ").append(montant(versement.getPlafondApplique()));
                    }
                }
            } else {
                texte.append("remboursement de prime à la résiliation");
            }
            texte.append(" (contrat n° ").append(versement.getContrat().getId()).append(")\n");
        }
    }

    // EN_ANALYSE -> "en analyse", DEGATS_DES_EAUX -> "dégâts des eaux"
    private static String libelle(Enum<?> valeur) {
        if (valeur == null) {return "non précisé";}
        String texte = valeur.name().toLowerCase(Locale.FRENCH).replace('_', ' ');
        return switch (texte) {
            case "degats des eaux" -> "dégâts des eaux";
            case "declare" -> "déclaré";
            case "accepte" -> "accepté";
            case "refuse" -> "refusé";
            case "indemnise" -> "indemnisé";
            case "resilie" -> "résilié";
            case "expire" -> "expiré";
            case "effectue" -> "effectué";
            case "annule" -> "annulé";
            case "prelevement" -> "prélèvement";
            case "cheque" -> "chèque";
            default -> texte;
        };
    }

    private static String date(LocalDate date) {
        return date == null ? "date non précisée" : date.format(FORMAT_DATE);
    }

    // 1234.5 -> "1 234,50 €"
    private static String montant(BigDecimal montant) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(montant);
    }

    private static String montantOuNonPrecise(BigDecimal montant) {
        return montant == null ? "non précisé" : montant(montant);
    }

    // Une description sur plusieurs lignes casserait la mise en forme de la liste
    private static String surUneLigne(String texte) {
        return texte.replaceAll("\\s+", " ").trim();
    }

    private static String chargerConnaissances() {
        try (InputStream flux = ContexteAssistant.class.getResourceAsStream(FICHIER_CONNAISSANCES)) {
            if (flux == null) {
                throw new IllegalStateException("Fichier " + FICHIER_CONNAISSANCES + " absent des ressources");
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(ligne -> !ligne.startsWith("# "))
                    .collect(Collectors.joining("\n"))
                    .trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
