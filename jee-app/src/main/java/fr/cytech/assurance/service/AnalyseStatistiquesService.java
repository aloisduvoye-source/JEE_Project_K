package fr.cytech.assurance.service;

import fr.cytech.assurance.ai.ClientGemini;
import fr.cytech.assurance.ai.MessageIA;
import fr.cytech.assurance.exception.AssistantIndisponibleException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Synthèse des statistiques globales rédigée par l'IA (Gemini), pour les agents et l'administrateur.
 * <p>
 * Seuls des chiffres agrégés sont envoyés à l'API : aucune donnée personnelle de client.
 */
public class AnalyseStatistiquesService {

    // Évolutions envoyées : les 12 derniers mois suffisent pour dégager une tendance
    private static final int MOIS_ENVOYES = 12;

    private static final String INSTRUCTIONS = "Tu es un analyste décisionnel pour une compagnie d'assurance. "
            + "Réponds en français, de façon claire et concise (maximum 150 mots), "
            + "structurée en 3 points : 1) Bilan global, 2) Point de vigilance, 3) Recommandation concrète. "
            + "Base-toi uniquement sur les chiffres fournis. "
            + "Écris en texte brut, sans Markdown (pas d'astérisques, de dièses ni de tirets de liste).";

    private final ClientGemini clientGemini = ClientGemini.getInstance();
    private final StatistiqueService statistiqueService = new StatistiqueService();

    /**
     * Demande à l'IA une synthèse des statistiques actuelles.
     *
     * @return le texte de la synthèse, ou un message expliquant pourquoi elle n'est pas disponible
     */
    public String analyser() {
        if (!clientGemini.estConfigure()) {
            return "L'assistant IA n'est pas configuré (clé AI_API_KEY manquante dans .env).";
        }
        try {
            return clientGemini.generer(INSTRUCTIONS, List.of(MessageIA.client(donnees())));
        } catch (AssistantIndisponibleException e) {
            return e.getMessage();
        }
    }

    // Toutes les statistiques de la page, en texte
    private String donnees() {
        BigDecimal ratio = statistiqueService.calculerRatioIndemnisations();
        return "Voici les statistiques actuelles de l'assurance.\n"
                + "Clients : " + statistiqueService.compterTotalClients() + " au total, dont "
                + statistiqueService.compterNouveauxClients() + " inscrits ce mois-ci.\n"
                + "Clients ayant un contrat actif, par type d'assurance : "
                + statistiqueService.repartitionClientsParType().stream()
                        .map(l -> l[0] + " = " + l[1]).collect(Collectors.joining(", ")) + ".\n"
                + "Contrats : " + statistiqueService.compterContratsActifs() + " actifs, "
                + statistiqueService.compterContratsExpires() + " expirés, "
                + statistiqueService.compterContratsResilies() + " résiliés.\n"
                + "Contrats actifs en fin de mois (12 derniers mois) : "
                + derniersMois(statistiqueService.evolutionContratsParMois()) + ".\n"
                + "Finances : chiffre d'affaires encaissé = " + statistiqueService.calculerChiffreAffaires() + " €, "
                + "remboursements de résiliation = " + statistiqueService.calculerTotalRemboursements() + " €, "
                + "chiffre d'affaires net = " + statistiqueService.calculerChiffreAffairesNet() + " €, "
                + "primes des contrats actifs = " + statistiqueService.calculerTotalPrimes() + " €, "
                + "indemnisations versées = " + statistiqueService.calculerTotalIndemnisations() + " €, "
                + "ratio indemnisations / chiffre d'affaires net = " + (ratio == null ? "non calculable" : ratio + " %")
                + ".\n"
                + "Par mois (12 derniers mois), encaissé / indemnisations / remboursements en € : "
                + derniersMois(statistiqueService.evolutionFinancesParMois()) + ".";
    }

    // Lignes {"MM/AAAA", valeur, ...} des MOIS_ENVOYES derniers mois -> "MM/AAAA : valeur / ... ; ..."
    private static String derniersMois(List<Object[]> lignes) {
        return lignes.stream()
                .skip(Math.max(0, lignes.size() - MOIS_ENVOYES))
                .map(l -> l[0] + " : " + Arrays.stream(l, 1, l.length).map(String::valueOf)
                        .collect(Collectors.joining(" / ")))
                .collect(Collectors.joining(" ; "));
    }
}
