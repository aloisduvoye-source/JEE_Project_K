package fr.cytech.assurance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

// Couverture proposée dans un contrat : responsabilité civile, vol, incendie...
@Entity
@Table(name = "garantie")
public class Garantie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Montant maximal remboursé
    @Column(name = "plafond", precision = 12, scale = 2)
    private BigDecimal plafond;

    // Montant restant à la charge du client
    @Column(name = "franchise", precision = 12, scale = 2)
    private BigDecimal franchise;

    public Garantie() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getNom() {return nom;}
    public void setNom(String nom) {this.nom = nom;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public BigDecimal getPlafond() {return plafond;}
    public void setPlafond(BigDecimal plafond) {this.plafond = plafond;}
    public BigDecimal getFranchise() {return franchise;}
    public void setFranchise(BigDecimal franchise) {this.franchise = franchise;}
}
