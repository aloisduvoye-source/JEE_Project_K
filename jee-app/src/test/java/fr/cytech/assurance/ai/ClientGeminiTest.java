package fr.cytech.assurance.ai;

import fr.cytech.assurance.exception.AssistantIndisponibleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Analyse des réponses de l'API Gemini, sans appel réseau
class ClientGeminiTest {

    private static final String MODELE = "modele-test";

    private static String erreur429(String quotaId) {
        return """
                {"error": {"code": 429, "message": "You exceeded your current quota", "status": "RESOURCE_EXHAUSTED",
                  "details": [
                    {"@type": "type.googleapis.com/google.rpc.QuotaFailure",
                     "violations": [{"quotaMetric": "generate_content_free_tier_requests", "quotaId": "%s"}]},
                    {"@type": "type.googleapis.com/google.rpc.RetryInfo", "retryDelay": "40s"}]}}
                """.formatted(quotaId);
    }

    @Test
    void reconnaitLeQuotaJournalier() {
        assertTrue(ClientGemini.estQuotaJournalier(erreur429("GenerateRequestsPerDayPerProjectPerModel-FreeTier")));
    }

    @Test
    void leQuotaParMinuteNEstPasJournalier() {
        assertFalse(ClientGemini.estQuotaJournalier(erreur429("GenerateRequestsPerMinutePerProjectPerModel-FreeTier")));
    }

    @Test
    void uneErreurSansDetailNEstPasUnQuotaJournalier() {
        assertFalse(ClientGemini.estQuotaJournalier("{\"error\": {\"code\": 429, \"message\": \"Too many requests\"}}"));
        assertFalse(ClientGemini.estQuotaJournalier("pas du JSON"));
    }

    @Test
    void extraitLeTexteDeLaReponse() {
        String json = """
                {"candidates": [{"content": {"parts": [{"text": "Bonjour, "}, {"text": "que puis-je faire ?"}],
                  "role": "model"}, "finishReason": "STOP"}],
                 "usageMetadata": {"promptTokenCount": 10, "candidatesTokenCount": 5, "totalTokenCount": 15}}
                """;

        assertEquals("Bonjour, que puis-je faire ?", ClientGemini.extraireTexte(MODELE, json));
    }

    @Test
    void ignoreLeRaisonnementDuModele() {
        String json = """
                {"candidates": [{"content": {"parts": [
                  {"text": "Je dois chercher les contrats actifs...", "thought": true},
                  {"text": "Vous avez deux contrats actifs."}]}, "finishReason": "STOP"}]}
                """;

        assertEquals("Vous avez deux contrats actifs.", ClientGemini.extraireTexte(MODELE, json));
    }

    @Test
    void echoueSansCandidat() {
        // Question bloquée par les filtres de sécurité
        String json = "{\"promptFeedback\": {\"blockReason\": \"SAFETY\"}}";

        assertThrows(AssistantIndisponibleException.class, () -> ClientGemini.extraireTexte(MODELE, json));
    }

    @Test
    void echoueSurUneReponseIllisible() {
        assertThrows(AssistantIndisponibleException.class, () -> ClientGemini.extraireTexte(MODELE, "<html>"));
    }
}
