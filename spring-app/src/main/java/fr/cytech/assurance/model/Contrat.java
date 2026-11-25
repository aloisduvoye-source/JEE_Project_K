package fr.cytech.assurance.model;

import fr.cytech.assurance.model.enums.StatutContrat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contrat")
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_assurance", nullable = false)
    private TypeAssurance typeAssurance;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "prime", nullable = false, precision = 12, scale = 2)
    private BigDecimal prime;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutContrat statut = StatutContrat.EN_ATTENTE;

    @Column(name = "date_resiliation")
    private LocalDate dateResiliation;

    @Column(name = "motif_resiliation", length = 255)
    private String motifResiliation;

    // Suspension décidée par un agent : renseignée tant qu'elle dure (null pour une suspension faute de paiement,
    // qui est automatique). La mise à jour automatique des statuts ne lève jamais une suspension par un agent
    @Column(name = "date_suspension")
    private LocalDate dateSuspension;

    @Column(name = "motif_suspension", length = 255)
    private String motifSuspension;

    // Contrat renouvelé par celui-ci (null pour un premier contrat)
    @ManyToOne
    @JoinColumn(name = "id_contrat_precedent", unique = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Contrat contratPrecedent;

    // Garanties souscrites (table contrat_garantie) : au moins une, vérifié côté service
    @ManyToMany
    @JoinTable(name = "contrat_garantie",
               joinColumns = @JoinColumn(name = "id_contrat"),
               inverseJoinColumns = @JoinColumn(name = "id_garantie"))
    private Set<Garantie> garanties = new HashSet<>();

    public Contrat() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Client getClient() {return client;}
    public void setClient(Client client) {this.client = client;}
    public TypeAssurance getTypeAssurance() {return typeAssurance;}
    public void setTypeAssurance(TypeAssurance typeAssurance) {this.typeAssurance = typeAssurance;}
    public LocalDate getDateDebut() {return dateDebut;}
    public void setDateDebut(LocalDate dateDebut) {this.dateDebut = dateDebut;}
    public LocalDate getDateFin() {return dateFin;}
    public void setDateFin(LocalDate dateFin) {this.dateFin = dateFin;}
    public BigDecimal getPrime() {return prime;}
    public void setPrime(BigDecimal prime) {this.prime = prime;}
    public StatutContrat getStatut() {return statut;}
    public void setStatut(StatutContrat statut) {this.statut = statut;}
    public LocalDate getDateResiliation() {return dateResiliation;}
    public void setDateResiliation(LocalDate dateResiliation) {this.dateResiliation = dateResiliation;}
    public String getMotifResiliation() {return motifResiliation;}
    public void setMotifResiliation(String motifResiliation) {this.motifResiliation = motifResiliation;}
    public LocalDate getDateSuspension() {return dateSuspension;}
    public void setDateSuspension(LocalDate dateSuspension) {this.dateSuspension = dateSuspension;}
    public String getMotifSuspension() {return motifSuspension;}
    public void setMotifSuspension(String motifSuspension) {this.motifSuspension = motifSuspension;}
    // true si un agent a suspendu le contrat (et non le défaut de paiement)
    public boolean isSuspenduParAgent() {return dateSuspension != null && statut == StatutContrat.SUSPENDU;}
    public Contrat getContratPrecedent() {return contratPrecedent;}
    public void setContratPrecedent(Contrat contratPrecedent) {this.contratPrecedent = contratPrecedent;}
    public Set<Garantie> getGaranties() {return garanties;}
    public void setGaranties(Set<Garantie> garanties) {this.garanties = garanties;}
}
