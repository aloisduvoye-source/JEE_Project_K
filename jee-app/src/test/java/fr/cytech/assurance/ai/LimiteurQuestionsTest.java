package fr.cytech.assurance.ai;

import fr.cytech.assurance.exception.TropDeQuestionsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimiteurQuestionsTest {

    private static final Long CLIENT = 1L;

    // Horloge que le test fait avancer à la main
    private static final class HorlogeReglable extends Clock {
        private Instant maintenant = Instant.parse("2026-11-13T09:00:00Z");

        void avancer(Duration duree) {
            maintenant = maintenant.plus(duree);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Europe/Paris");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return maintenant;
        }
    }

    private HorlogeReglable horloge;
    private LimiteurQuestions limiteur;

    @BeforeEach
    void preparer() {
        horloge = new HorlogeReglable();
        limiteur = new LimiteurQuestions(horloge);
    }

    private void poser(int nombre, Duration entreDeux) {
        for (int i = 0; i < nombre; i++) {
            limiteur.enregistrer(CLIENT);
            horloge.avancer(entreDeux);
        }
    }

    @Test
    void refuseLaQuestionDeTropDansLaMinute() {
        poser(LimiteurQuestions.QUESTIONS_PAR_MINUTE, Duration.ofSeconds(1));

        assertThrows(TropDeQuestionsException.class, () -> limiteur.enregistrer(CLIENT));
    }

    @Test
    void accepteDeNouveauUneMinuteApres() {
        poser(LimiteurQuestions.QUESTIONS_PAR_MINUTE, Duration.ofSeconds(1));
        horloge.avancer(Duration.ofMinutes(1));

        assertDoesNotThrow(() -> limiteur.enregistrer(CLIENT));
    }

    @Test
    void uneQuestionRefuseeNEstPasComptee() {
        poser(LimiteurQuestions.QUESTIONS_PAR_MINUTE, Duration.ZERO);
        // Le client insiste : ses essais refusés ne doivent pas prolonger l'attente
        for (int i = 0; i < 10; i++) {
            assertThrows(TropDeQuestionsException.class, () -> limiteur.enregistrer(CLIENT));
        }
        horloge.avancer(Duration.ofMinutes(1).plusSeconds(1));

        assertDoesNotThrow(() -> limiteur.enregistrer(CLIENT));
    }

    @Test
    void refuseAuDelaDeLaLimiteDuJour() {
        // Une question toutes les 15 s : jamais plus de 4 par minute
        poser(LimiteurQuestions.QUESTIONS_PAR_JOUR, Duration.ofSeconds(15));

        assertThrows(TropDeQuestionsException.class, () -> limiteur.enregistrer(CLIENT));
    }

    @Test
    void repartDeZeroLeLendemain() {
        poser(LimiteurQuestions.QUESTIONS_PAR_JOUR, Duration.ofSeconds(15));
        horloge.avancer(Duration.ofDays(1));

        assertDoesNotThrow(() -> limiteur.enregistrer(CLIENT));
    }

    @Test
    void chaqueClientASaPropreLimite() {
        poser(LimiteurQuestions.QUESTIONS_PAR_MINUTE, Duration.ZERO);

        assertDoesNotThrow(() -> limiteur.enregistrer(2L));
    }
}
