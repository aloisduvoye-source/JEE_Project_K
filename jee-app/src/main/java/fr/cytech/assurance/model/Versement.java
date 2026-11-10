package fr.cytech.assurance.model;

import fr.cytech.assurance.model.enums.TypeVersement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Argent versé par l'assurance au client (l'inverse d'un {@link Paiement}) :
 * <ul>
 *     <li>INDEMNISATION : pour un sinistre, qui est alors renseigné ;</li>
 *     <li>REMBOURSEMENT : part de la prime non couverte, à la résiliation d'un contrat payé (pas de sinistre).</li>
 * </ul>
 */
@Entity
@Table(name = "versement")
public class Versement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeVersement type;

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_versement", nullable = false)
    private LocalDate dateVersement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    private Contrat contrat;

    // Sinistre indemnisé (null pour un remboursement)
    @OneToOne
    @JoinColumn(name = "id_sinistre", unique = true)
    private Sinistre sinistre;

    // Agent qui a enregistré le versement (null s'il a été calculé automatiquement)
    @ManyToOne
    @JoinColumn(name = "id_agent")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Utilisateur agent;

    public Versement() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public TypeVersement getType() {return type;}
    public void setType(TypeVersement type) {this.type = type;}
    public BigDecimal getMontant() {return montant;}
    public void setMontant(BigDecimal montant) {this.montant = montant;}
    public LocalDate getDateVersement() {return dateVersement;}
    public void setDateVersement(LocalDate dateVersement) {this.dateVersement = dateVersement;}
    public Contrat getContrat() {return contrat;}
    public void setContrat(Contrat contrat) {this.contrat = contrat;}
    public Sinistre getSinistre() {return sinistre;}
    public void setSinistre(Sinistre sinistre) {this.sinistre = sinistre;}
    public Utilisateur getAgent() {return agent;}
    public void setAgent(Utilisateur agent) {this.agent = agent;}
}
