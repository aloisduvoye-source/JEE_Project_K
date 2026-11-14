package fr.cytech.assurance.service;

import fr.cytech.assurance.ai.ClientGemini;
import fr.cytech.assurance.ai.MessageIA;
import fr.cytech.assurance.exception.AssistantIndisponibleException;

import java.math.BigDecimal;
import java.util.List;

public class AnalyseStatistiquesService {

    private final ClientGemini clientGemini = ClientGemini.getInstance();

    public String analyser(
            int totalClients,
            int nouveauxClients,
            int contratsActifs,
            int contratsExpires,
            int contratsResilies,
            BigDecimal chiffreAffaires,
            BigDecimal totalPrimes,
            BigDecimal totalIndemnisations) {

        if (!clientGemini.estConfigure()) {
            return "L'assistant IA n'est pas configuré (clé AI_API_KEY manquante dans .env).";
        }

        String instructions = "Tu es un analyste décisionnel pour une compagnie d'assurance. "
                + "Réponds en français, de façon claire et concise (maximum 150 mots), "
                + "structurée en 3 points : 1) Bilan global, 2) Point de vigilance, 3) Recommandation concrète. "
                + "Base-toi uniquement sur les chiffres fournis.";

        String donnees = String.format(
                "Voici les statistiques actuelles de l'assurance : "
                + "Total clients = %d (dont %d nouveaux ce mois-ci) ; "
                + "Contrats actifs = %d, expirés = %d, résiliés = %d ; "
                + "Chiffre d'affaires encaissé = %s €, primes actives = %s €, indemnisations versées = %s €.",
                totalClients, nouveauxClients,
                contratsActifs, contratsExpires, contratsResilies,
                chiffreAffaires, totalPrimes, totalIndemnisations
        );

        try {
            return clientGemini.generer(instructions, List.of(MessageIA.client(donnees)));
        } catch (AssistantIndisponibleException e) {
            return e.getMessage();
        }
    }
}
