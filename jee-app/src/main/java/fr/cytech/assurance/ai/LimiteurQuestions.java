package fr.cytech.assurance.ai;

import fr.cytech.assurance.exception.TropDeQuestionsException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de questions posées à l'assistant par chaque client.
 * <p>
 * Le quota de l'API d'IA est commun à toute l'application : sans limite, un seul client qui enchaîne
 * les questions l'épuiserait et l'assistant ne répondrait plus à personne.
 * <p>
 * Compteurs en mémoire : ils repartent de zéro au redémarrage de l'application, ce qui suffit ici.
 * Une seule instance pour toute l'application : utiliser {@link #getInstance()}.
 */
public final class LimiteurQuestions {

    public static final int QUESTIONS_PAR_MINUTE = 5;
    public static final int QUESTIONS_PAR_JOUR = 50;

    private static final Duration MINUTE = Duration.ofMinutes(1);

    private static final LimiteurQuestions INSTANCE = new LimiteurQuestions(Clock.systemDefaultZone());

    private final Clock horloge;
    private final Map<Long, Compteur> compteurs = new ConcurrentHashMap<>();

    // Horloge en paramètre : un test peut faire avancer le temps sans attendre
    LimiteurQuestions(Clock horloge) {
        this.horloge = horloge;
    }

    public static LimiteurQuestions getInstance() {
        return INSTANCE;
    }

    /**
     * Compte une question de l'utilisateur, ou la refuse s'il a atteint une limite.
     * Une question refusée n'est pas comptée.
     *
     * @throws TropDeQuestionsException si la limite par minute ou par jour est atteinte
     */
    public void enregistrer(Long idUtilisateur) {
        Compteur compteur = compteurs.computeIfAbsent(idUtilisateur, id -> new Compteur());
        Instant maintenant = horloge.instant();
        LocalDate aujourdhui = LocalDate.now(horloge);

        // Un même client peut envoyer deux questions en même temps (deux onglets)
        synchronized (compteur) {
            if (!aujourdhui.equals(compteur.jour)) {
                compteur.jour = aujourdhui;
                compteur.questionsDuJour = 0;
            }
            if (compteur.questionsDuJour >= QUESTIONS_PAR_JOUR) {
                throw new TropDeQuestionsException("Vous avez atteint la limite de " + QUESTIONS_PAR_JOUR
                        + " questions par jour. Vous pourrez de nouveau interroger l'assistant demain.");
            }

            // On ne garde que les questions de la dernière minute
            while (!compteur.derniereMinute.isEmpty()
                    && !compteur.derniereMinute.peekFirst().isAfter(maintenant.minus(MINUTE))) {
                compteur.derniereMinute.pollFirst();
            }
            if (compteur.derniereMinute.size() >= QUESTIONS_PAR_MINUTE) {
                throw new TropDeQuestionsException("Vous avez posé beaucoup de questions en peu de temps. "
                        + "Veuillez patienter une minute avant d'en poser une nouvelle.");
            }

            compteur.derniereMinute.addLast(maintenant);
            compteur.questionsDuJour++;
        }
    }

    private static final class Compteur {
        private final Deque<Instant> derniereMinute = new ArrayDeque<>();
        private LocalDate jour;
        private int questionsDuJour;
    }
}
