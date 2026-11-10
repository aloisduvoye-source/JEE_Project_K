package fr.cytech.assurance.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.cytech.assurance.exception.AssistantIndisponibleException;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client de l'API Google Gemini (generateContent) : envoie une conversation, renvoie la réponse du modèle.
 * <p>
 * Ne contient aucune logique métier : le service de l'assistant construit les instructions
 * (données du client, procédures) et l'historique, cette classe ne fait que l'appel HTTP.
 * <p>
 * Configuration (fichier .env ou variables d'environnement du docker-compose.yml) :
 * <ul>
 *     <li>AI_API_KEY : clé de l'API, obligatoire (créée sur https://aistudio.google.com/apikey)</li>
 *     <li>AI_MODEL : modèle principal, facultatif</li>
 *     <li>AI_MODEL_SECOURS : modèle utilisé si le principal est surchargé, facultatif</li>
 * </ul>
 * Les modèles par défaut sont des alias « latest » : Google les fait pointer vers la version courante,
 * le code ne casse donc pas quand une ancienne version est retirée.
 * <p>
 * Une seule instance suffit pour toute l'application (HttpClient est thread-safe) : utiliser {@link #getInstance()}.
 */
public final class ClientGemini {

    private static final Logger LOG = Logger.getLogger(ClientGemini.class.getName());

    private static final String URL_API = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    // Version « lite » en principal : quelques secondes par réponse et rarement surchargée,
    // ce qui suffit pour répondre à partir des données fournies. La version complète sert de secours.
    private static final String MODELE_PAR_DEFAUT = "gemini-flash-lite-latest";
    private static final String MODELE_SECOURS_PAR_DEFAUT = "gemini-flash-latest";

    // Le modèle principal est réessayé une fois avant de passer au modèle de secours
    private static final int TENTATIVES_PAR_MODELE = 2;
    private static final Duration ATTENTE_ENTRE_TENTATIVES = Duration.ofSeconds(1);
    // Un modèle surchargé peut mettre très longtemps avant de répondre (ou d'échouer) :
    // mieux vaut abandonner vite et essayer le suivant que faire attendre le client
    private static final Duration DELAI_REPONSE = Duration.ofSeconds(15);

    // Réponses courtes et factuelles : un assistant d'assurance ne doit pas inventer
    private static final double TEMPERATURE = 0.3;
    private static final int TOKENS_MAX = 1024;

    private static final ClientGemini INSTANCE = new ClientGemini();

    private final String cleApi;
    private final List<String> modeles;
    private final HttpClient http;

    private ClientGemini() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        // Pas d'exception si la clé manque : le reste de l'application doit fonctionner sans l'assistant.
        // L'erreur est signalée au premier appel.
        this.cleApi = valeurOuNull(dotenv.get("AI_API_KEY"));
        this.modeles = List.of(
                valeurOuDefaut(dotenv.get("AI_MODEL"), MODELE_PAR_DEFAUT),
                valeurOuDefaut(dotenv.get("AI_MODEL_SECOURS"), MODELE_SECOURS_PAR_DEFAUT));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static ClientGemini getInstance() {
        return INSTANCE;
    }

    // true si une clé est configurée (ne garantit pas qu'elle soit valide)
    public boolean estConfigure() {
        return cleApi != null;
    }

    /**
     * Envoie la conversation au modèle et renvoie sa réponse.
     *
     * @param instructions consignes et contexte donnés au modèle (rôle, données du client, procédures)
     * @param conversation messages dans l'ordre chronologique, le dernier étant la question du client
     * @return le texte de la réponse
     * @throws AssistantIndisponibleException si aucune réponse n'a pu être obtenue
     */
    public String generer(String instructions, List<MessageIA> conversation) {
        if (cleApi == null) {
            throw new AssistantIndisponibleException(
                    "L'assistant n'est pas configuré (variable AI_API_KEY absente).");
        }
        if (conversation == null || conversation.isEmpty()) {
            throw new IllegalArgumentException("La conversation doit contenir au moins un message");
        }

        String corps = construireRequete(instructions, conversation).toString();
        AssistantIndisponibleException derniereErreur = null;

        for (String modele : modeles) {
            for (int tentative = 1; tentative <= TENTATIVES_PAR_MODELE; tentative++) {
                try {
                    return appeler(modele, corps);
                } catch (ErreurTemporaire e) {
                    // Surcharge (503), quota par minute (429) ou délai dépassé : on réessaie puis on change de modèle
                    LOG.log(Level.WARNING, "Gemini " + modele + " indisponible (tentative " + tentative + ") : "
                            + e.getMessage());
                    derniereErreur = new AssistantIndisponibleException(
                            "L'assistant est momentanément surchargé, veuillez réessayer dans quelques instants.", e);
                    attendre();
                }
            }
        }
        throw derniereErreur;
    }

    // Un appel HTTP au modèle donné. Lève ErreurTemporaire si un nouvel essai a des chances de réussir
    private String appeler(String modele, String corps) {
        HttpRequest requete = HttpRequest.newBuilder()
                .uri(URI.create(String.format(URL_API, modele)))
                .timeout(DELAI_REPONSE)
                // La clé dans un en-tête et non dans l'URL : elle n'apparaît pas dans les logs d'accès
                .header("x-goog-api-key", cleApi)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(corps, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> reponse;
        try {
            reponse = http.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ErreurTemporaire("délai de réponse dépassé", e);
        } catch (IOException e) {
            throw new AssistantIndisponibleException("Impossible de joindre l'assistant (problème réseau).", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistantIndisponibleException("La demande à l'assistant a été interrompue.", e);
        }

        int statut = reponse.statusCode();
        if (statut == 200) {
            return extraireTexte(reponse.body());
        }
        String detail = "HTTP " + statut + " : " + messageErreur(reponse.body());
        if (statut == 429 || statut == 500 || statut == 503 || statut == 504) {
            throw new ErreurTemporaire(detail, null);
        }
        // 400 (requête invalide), 403 (clé invalide), 404 (modèle inconnu) : inutile de réessayer
        LOG.severe("Erreur de l'API Gemini avec le modèle " + modele + " : " + detail);
        throw new AssistantIndisponibleException("L'assistant est indisponible pour le moment.");
    }

    /*
     * Format attendu par l'API :
     * {
     *   "systemInstruction": {"parts": [{"text": "..."}]},
     *   "contents": [{"role": "user" | "model", "parts": [{"text": "..."}]}, ...],
     *   "generationConfig": {"temperature": 0.3, "maxOutputTokens": 1024}
     * }
     */
    private JsonObject construireRequete(String instructions, List<MessageIA> conversation) {
        JsonObject requete = new JsonObject();

        if (instructions != null && !instructions.isBlank()) {
            JsonObject systeme = new JsonObject();
            systeme.add("parts", parties(instructions));
            requete.add("systemInstruction", systeme);
        }

        JsonArray contenus = new JsonArray();
        for (MessageIA message : conversation) {
            JsonObject contenu = new JsonObject();
            contenu.addProperty("role", message.depuisClient() ? "user" : "model");
            contenu.add("parts", parties(message.texte()));
            contenus.add(contenu);
        }
        requete.add("contents", contenus);

        JsonObject configuration = new JsonObject();
        configuration.addProperty("temperature", TEMPERATURE);
        configuration.addProperty("maxOutputTokens", TOKENS_MAX);
        requete.add("generationConfig", configuration);

        return requete;
    }

    private JsonArray parties(String texte) {
        JsonObject partie = new JsonObject();
        partie.addProperty("text", texte);
        JsonArray parties = new JsonArray();
        parties.add(partie);
        return parties;
    }

    // Réponse : {"candidates": [{"content": {"parts": [{"text": "..."}, ...]}, "finishReason": "STOP"}]}
    private String extraireTexte(String json) {
        try {
            JsonObject racine = JsonParser.parseString(json).getAsJsonObject();
            JsonArray candidats = racine.getAsJsonArray("candidates");
            if (candidats == null || candidats.isEmpty()) {
                // Question bloquée par les filtres de sécurité : promptFeedback.blockReason est renseigné
                LOG.warning("Réponse Gemini sans candidat : " + json);
                throw new AssistantIndisponibleException("L'assistant n'a pas pu répondre à cette question.");
            }
            JsonObject contenu = candidats.get(0).getAsJsonObject().getAsJsonObject("content");
            StringBuilder texte = new StringBuilder();
            if (contenu != null && contenu.has("parts")) {
                for (JsonElement partie : contenu.getAsJsonArray("parts")) {
                    JsonObject objet = partie.getAsJsonObject();
                    // Les parties "thought" sont le raisonnement interne du modèle, pas la réponse
                    if (objet.has("text") && !(objet.has("thought") && objet.get("thought").getAsBoolean())) {
                        texte.append(objet.get("text").getAsString());
                    }
                }
            }
            if (texte.isEmpty()) {
                throw new AssistantIndisponibleException("L'assistant n'a pas pu répondre à cette question.");
            }
            return texte.toString().trim();
        } catch (RuntimeException e) {
            if (e instanceof AssistantIndisponibleException aie) {
                throw aie;
            }
            LOG.log(Level.SEVERE, "Réponse Gemini illisible : " + json, e);
            throw new AssistantIndisponibleException("L'assistant a renvoyé une réponse illisible.", e);
        }
    }

    // Erreur : {"error": {"code": 503, "message": "...", "status": "UNAVAILABLE"}}
    private String messageErreur(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonObject("error").get("message").getAsString();
        } catch (RuntimeException e) {
            return json;
        }
    }

    private void attendre() {
        try {
            Thread.sleep(ATTENTE_ENTRE_TENTATIVES.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String valeurOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static String valeurOuDefaut(String valeur, String defaut) {
        String nettoyee = valeurOuNull(valeur);
        return nettoyee != null ? nettoyee : defaut;
    }

    // Erreur passagère (surcharge, quota, délai) : un nouvel essai ou un autre modèle peut réussir
    private static final class ErreurTemporaire extends RuntimeException {
        ErreurTemporaire(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
