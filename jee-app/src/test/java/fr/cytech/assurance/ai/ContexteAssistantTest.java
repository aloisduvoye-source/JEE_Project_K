package fr.cytech.assurance.ai;

import fr.cytech.assurance.model.Contrat;
import fr.cytech.assurance.model.Garantie;
import fr.cytech.assurance.model.Paiement;
import fr.cytech.assurance.model.Utilisateur;
import fr.cytech.assurance.model.enums.StatutPaiement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContexteAssistantTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 13);

    private static Utilisateur client() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setPrenom("Bob");
        utilisateur.setNom("Bill");
        return utilisateur;
    }

    private static Paiement paiement(String montant, LocalDate echeance, StatutPaiement statut) {
        Contrat contrat = new Contrat();
        contrat.setId(7L);
        Paiement paiement = new Paiement();
        paiement.setContrat(contrat);
        paiement.setMontant(new BigDecimal(montant));
        paiement.setDateEcheance(echeance);
        paiement.setStatut(statut);
        return paiement;
    }

    private static String instructions(List<Paiement> paiements, List<Garantie> catalogue) {
        return ContexteAssistant.instructions(client(), List.of(), paiements, List.of(), List.of(), catalogue,
                AUJOURDHUI);
    }

    @Test
    void contientLesDonneesDuClient() {
        String texte = instructions(List.of(), List.of());

        assertTrue(texte.contains("Client : Bob Bill"));
        assertTrue(texte.contains("Date du jour : 13/11/2026"));
        assertTrue(texte.contains("Aucun contrat."));
        assertTrue(texte.contains("Aucun sinistre déclaré."));
    }

    @Test
    void decritLesGarantiesDuCatalogue() {
        Garantie vol = new Garantie();
        vol.setNom("Vol");
        vol.setPlafond(new BigDecimal("20000"));
        vol.setFranchise(new BigDecimal("150"));
        vol.setDescription("Vol ou tentative\nde vol des biens assurés.");

        String texte = instructions(List.of(), List.of(vol));

        // Montants au format français, description remise sur une ligne
        assertTrue(texte.contains("- Vol (plafond : 20 000,00 €, franchise : 150,00 €) : "
                + "Vol ou tentative de vol des biens assurés."), texte);
    }

    @Test
    void envoieLesPaiementsRecents() {
        String texte = instructions(List.of(
                paiement("50.00", AUJOURDHUI.minusMonths(2), StatutPaiement.EFFECTUE)), List.of());

        assertTrue(texte.contains("Paiements des 12 derniers mois et paiements non réglés (1)"));
        assertTrue(texte.contains("échéance le 13/09/2026"));
        assertFalse(texte.contains("pas listé"));
    }

    @Test
    void ometLesAnciensPaiementsReglesEnLeSignalant() {
        String texte = instructions(List.of(
                paiement("50.00", AUJOURDHUI.minusMonths(2), StatutPaiement.EFFECTUE),
                paiement("60.00", AUJOURDHUI.minusYears(2), StatutPaiement.EFFECTUE),
                paiement("70.00", AUJOURDHUI.minusYears(3), StatutPaiement.ANNULE)), List.of());

        assertTrue(texte.contains("(1)"));
        assertFalse(texte.contains("13/11/2024"));
        assertFalse(texte.contains("13/11/2023"));
        assertTrue(texte.contains("2 paiements plus anciens, réglés ou annulés, ne sont pas listés"));
    }

    @Test
    void gardeLesPaiementsNonReglesQuelleQueSoitLEcheance() {
        String texte = instructions(List.of(
                paiement("80.00", AUJOURDHUI.minusYears(2), StatutPaiement.EN_RETARD),
                paiement("90.00", AUJOURDHUI.minusYears(2), StatutPaiement.EN_ATTENTE)), List.of());

        assertTrue(texte.contains("statut en retard"));
        assertTrue(texte.contains("statut en attente"));
        assertFalse(texte.contains("pas listé"));
    }
}
