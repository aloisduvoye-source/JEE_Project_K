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
 * Client de l'API Google Gemini : envoie une conversation et renvoie la réponse du modèle, sans logique métier.
 * Configuré par AI_API_KEY (obligatoire), AI_MODEL et AI_MODEL_SECOURS (.env). Une seule instance : {@link #getInstance()}.
 */
public final class ClientGemini {

    private static final Logger LOG = Logger.getLogger(ClientGemini.class.getName());

    private static final String URL_API = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    /** Modèle principal : version « lite », rapide et rarement surchargée (alias « latest » : version courante). */
    private static final String MODELE_PAR_DEFAUT = "gemini-flash-lite-latest";

    /** Modèle de secours, si le principal ne répond pas. */
    private static final String MODELE_SECOURS_PAR_DEFAUT = "gemini-flash-latest";

    /** Essais de chaque modèle avant de passer au suivant. */
    private static final int TENTATIVES_PAR_MODELE = 2;
    private static final Duration ATTENTE_ENTRE_TENTATIVES = Duration.ofSeconds(1);

    /** Délai court : un modèle surchargé peut mettre très longtemps, mieux vaut passer au suivant. */
    private static final Duration DELAI_REPONSE = Duration.ofSeconds(15);

    /** Température faible : réponses factuelles, l'assistant ne doit pas inventer. */
    private static final double TEMPERATURE = 0.3;
    private static final int TOKENS_MAX = 2048;

    private static final ClientGemini INSTANCE = new ClientGemini();

    private final String cleApi;
    private final List<String> modeles;
    private final HttpClient http;

    private ClientGemini() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        // Clé absente : pas d'exception, l'application fonctionne sans l'assistant (erreur au premier appel)
        this.cleApi = valeurOuNull(dotenv.get("AI_API_KEY"));
        this.modeles = List.of(
                valeurOuDefaut(dotenv.get("AI_MODEL"), MODELE_PAR_DEFAUT),
                valeurOuDefaut(dotenv.get("AI_MODEL_SECOURS"), MODELE_SECOURS_PAR_DEFAUT));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Instance unique, partagée par toute l'application (HttpClient est thread-safe). */
    public static ClientGemini getInstance() {
        return INSTANCE;
    }

    /** Vrai si une clé est configurée, sans garantir qu'elle soit valide. */
    public boolean estConfigure() {
        return cleApi != null;
    }

    /**
     * Envoie la conversation au modèle principal, puis au modèle de secours s'il échoue.
     *
     * @param instructions consignes et contexte donnés au modèle
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
                    LOG.log(Level.WARNING, "Gemini " + modele + " indisponible (tentative " + tentative + ") : "
                            + e.getMessage());
                    derniereErreur = new AssistantIndisponibleException(
                            "L'assistant est momentanément surchargé, veuillez réessayer dans quelques instants.", e);
                    if (tentative < TENTATIVES_PAR_MODELE) {
                        attendre();
                    }
                } catch (ErreurQuotaJournalier e) {
                    // Quota compté par modèle : le modèle de secours peut encore répondre
                    LOG.warning("Quota journalier de l'API Gemini épuisé pour le modèle " + modele + " : "
                            + e.getMessage());
                    derniereErreur = new AssistantIndisponibleException(
                            "L'assistant a atteint sa limite d'utilisation pour aujourd'hui. "
                                    + "Veuillez réessayer demain.", e);
                    break;
                } catch (ErreurModele e) {
                    LOG.severe("Erreur de l'API Gemini avec le modèle " + modele + " : " + e.getMessage());
                    derniereErreur = new AssistantIndisponibleException(
                            "L'assistant est indisponible pour le moment.", e);
                    break;
                }
            }
        }
        throw derniereErreur;
    }

    /** Un appel HTTP au modèle ; l'exception levée indique s'il faut réessayer, changer de modèle ou abandonner. */
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
            throw new ErreurTemporaire("problème réseau : " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistantIndisponibleException("La demande à l'assistant a été interrompue.", e);
        }

        int statut = reponse.statusCode();
        if (statut == 200) {
            return extraireTexte(modele, reponse.body());
        }
        String detail = "HTTP " + statut + " : " + messageErreur(reponse.body());
        if (statut == 429 && estQuotaJournalier(reponse.body())) {
            throw new ErreurQuotaJournalier(detail);
        }
        // Autre 429 : quota par minute, passager
        if (statut == 429 || statut == 500 || statut == 503 || statut == 504) {
            throw new ErreurTemporaire(detail, null);
        }
        // 404 : modèle inconnu ou retiré ; 400 : paramètre refusé par ce modèle
        if (statut == 400 || statut == 404) {
            throw new ErreurModele(detail);
        }
        // 401/403 (clé invalide ou sans droits) et autres : aucun modèle ne répondra
        LOG.severe("Erreur de l'API Gemini avec le modèle " + modele + " : " + detail);
        throw new AssistantIndisponibleException("L'assistant est indisponible pour le moment.");
    }

    /** Corps JSON de la requête : instructions système, conversation et paramètres de génération. */
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
        // Raisonnement minimal : ses tokens sont pris sur maxOutputTokens et pourraient couper la réponse.
        // thinkingLevel et non thinkingBudget = 0, refusé (HTTP 400) par les modèles récents
        JsonObject raisonnement = new JsonObject();
        raisonnement.addProperty("thinkingLevel", "low");
        configuration.add("thinkingConfig", raisonnement);
        requete.add("generationConfig", configuration);

        return requete;
    }

    /** Tableau « parts » de l'API, avec ce seul texte. */
    private JsonArray parties(String texte) {
        JsonObject partie = new JsonObject();
        partie.addProperty("text", texte);
        JsonArray parties = new JsonArray();
        parties.add(partie);
        return parties;
    }

    /** Texte de la réponse, sans le raisonnement du modèle (visible dans le paquetage pour les tests). */
    static String extraireTexte(String modele, String json) {
        try {
            JsonObject racine = JsonParser.parseString(json).getAsJsonObject();
            journaliserConsommation(modele, racine);
            JsonArray candidats = racine.getAsJsonArray("candidates");
            if (candidats == null || candidats.isEmpty()) {
                // Question bloquée par les filtres de sécurité
                LOG.warning("Réponse Gemini sans candidat : " + json);
                throw new AssistantIndisponibleException("L'assistant n'a pas pu répondre à cette question.");
            }
            JsonObject candidat = candidats.get(0).getAsJsonObject();
            // STOP : réponse complète ; MAX_TOKENS : coupée faute de place ; SAFETY : bloquée par un filtre
            String finPrevue = candidat.has("finishReason") ? candidat.get("finishReason").getAsString() : "STOP";
            if (!"STOP".equals(finPrevue)) {
                LOG.warning("Réponse Gemini interrompue (finishReason = " + finPrevue + ") : " + json);
            }
            JsonObject contenu = candidat.getAsJsonObject("content");
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

    /** Journalise les tokens consommés par l'appel, pour suivre l'utilisation du quota. */
    private static void journaliserConsommation(String modele, JsonObject racine) {
        JsonObject usage = racine.getAsJsonObject("usageMetadata");
        if (usage == null) {
            return;
        }
        LOG.info("Gemini " + modele + " : " + entier(usage, "totalTokenCount") + " tokens ("
                + entier(usage, "promptTokenCount") + " en entrée dont " + entier(usage, "cachedContentTokenCount")
                + " en cache, " + entier(usage, "candidatesTokenCount") + " de réponse, "
                + entier(usage, "thoughtsTokenCount") + " de raisonnement)");
    }

    /** Valeur entière du champ, ou 0 s'il est absent. */
    private static int entier(JsonObject objet, String champ) {
        return objet.has(champ) ? objet.get(champ).getAsInt() : 0;
    }

    /** Vrai si l'erreur 429 vient du quota par jour et non par minute (visible dans le paquetage pour les tests). */
    static boolean estQuotaJournalier(String json) {
        try {
            JsonArray details = JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonObject("error").getAsJsonArray("details");
            if (details == null) {
                return false;
            }
            for (JsonElement detail : details) {
                JsonArray violations = detail.getAsJsonObject().getAsJsonArray("violations");
                if (violations == null) {
                    continue;
                }
                for (JsonElement violation : violations) {
                    JsonElement quota = violation.getAsJsonObject().get("quotaId");
                    if (quota != null && quota.getAsString().contains("PerDay")) {
                        return true;
                    }
                }
            }
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Message d'une réponse d'erreur de l'API, ou la réponse entière s'il est illisible. */
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

    /** Valeur sans espaces autour, ou null si elle est vide. */
    private static String valeurOuNull(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static String valeurOuDefaut(String valeur, String defaut) {
        String nettoyee = valeurOuNull(valeur);
        return nettoyee != null ? nettoyee : defaut;
    }

    /** Erreur passagère (surcharge, quota par minute, délai, réseau) : un nouvel essai peut réussir. */
    private static final class ErreurTemporaire extends RuntimeException {
        ErreurTemporaire(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Quota du jour du modèle épuisé : seul un autre modèle peut réussir avant demain. */
    private static final class ErreurQuotaJournalier extends RuntimeException {
        ErreurQuotaJournalier(String message) {
            super(message);
        }
    }

    /** Erreur propre au modèle appelé (inconnu, paramètre refusé) : seul un autre modèle peut réussir. */
    private static final class ErreurModele extends RuntimeException {
        ErreurModele(String message) {
            super(message);
        }
    }
}
