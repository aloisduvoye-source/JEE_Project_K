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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limite les questions de chaque client, pour qu'un seul ne puisse pas épuiser le quota commun de l'API.
 * Compteurs en mémoire, remis à zéro au redémarrage. Bean Spring : une seule instance pour toute l'application.
 */
@Component
public class LimiteurQuestions {

    public static final int QUESTIONS_PAR_MINUTE = 5;
    public static final int QUESTIONS_PAR_JOUR = 50;

    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final Clock horloge;
    private final Map<Long, Compteur> compteurs = new ConcurrentHashMap<>();

    /** Horloge en paramètre : un test peut faire avancer le temps sans attendre. */
    LimiteurQuestions(Clock horloge) {
        this.horloge = horloge;
    }

    /** Constructeur utilisé par Spring, avec l'horloge du système. */
    @Autowired
    public LimiteurQuestions() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Compte une question de l'utilisateur, ou la refuse sans la compter s'il a atteint une limite.
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

    /** Questions d'un client : heures de celles de la dernière minute, et nombre de celles du jour. */
    private static final class Compteur {
        private final Deque<Instant> derniereMinute = new ArrayDeque<>();
        private LocalDate jour;
        private int questionsDuJour;
    }
}
