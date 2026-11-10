package fr.cytech.assurance.model;

import fr.cytech.assurance.model.enums.StatutSinistre;
import fr.cytech.assurance.model.enums.TypeSinistre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sinistre")
public class Sinistre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    private Contrat contrat;

    // Date à laquelle le sinistre a eu lieu
    @Column(name = "date_sinistre", nullable = false)
    private LocalDate dateSinistre;

    // Date à laquelle le client l'a déclaré (remplie automatiquement)
    @CreationTimestamp
    @Column(name = "date_declaration", nullable = false, updatable = false)
    private LocalDateTime dateDeclaration;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sinistre", nullable = false)
    private TypeSinistre typeSinistre;

    @Column(name = "montant_estime", precision = 12, scale = 2)
    private BigDecimal montantEstime;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutSinistre statut = StatutSinistre.DECLARE;

    // Étapes du traitement, remplies par SinistreDao.changerStatut (null tant que l'étape n'est pas atteinte).
    // L'indemnisation (montant, date) est un Versement.
    @Column(name = "date_prise_en_charge")
    private LocalDateTime datePriseEnCharge;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    // Agent qui traite le dossier (null tant qu'il n'est pas attribué)
    @ManyToOne
    @JoinColumn(name = "id_agent")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Utilisateur agent;

    public Sinistre() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Contrat getContrat() {return contrat;}
    public void setContrat(Contrat contrat) {this.contrat = contrat;}
    public LocalDate getDateSinistre() {return dateSinistre;}
    public void setDateSinistre(LocalDate dateSinistre) {this.dateSinistre = dateSinistre;}
    public LocalDateTime getDateDeclaration() {return dateDeclaration;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public TypeSinistre getTypeSinistre() {return typeSinistre;}
    public void setTypeSinistre(TypeSinistre typeSinistre) {this.typeSinistre = typeSinistre;}
    public BigDecimal getMontantEstime() {return montantEstime;}
    public void setMontantEstime(BigDecimal montantEstime) {this.montantEstime = montantEstime;}
    public StatutSinistre getStatut() {return statut;}
    public void setStatut(StatutSinistre statut) {this.statut = statut;}
    public LocalDateTime getDatePriseEnCharge() {return datePriseEnCharge;}
    public void setDatePriseEnCharge(LocalDateTime datePriseEnCharge) {this.datePriseEnCharge = datePriseEnCharge;}
    public LocalDateTime getDateDecision() {return dateDecision;}
    public void setDateDecision(LocalDateTime dateDecision) {this.dateDecision = dateDecision;}
    public Utilisateur getAgent() {return agent;}
    public void setAgent(Utilisateur agent) {this.agent = agent;}
}
