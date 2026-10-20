package fr.cytech.assurance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

// Un type d'assurance est une ligne en base : en ajouter un ne demande pas de modifier le code
@Entity
@Table(name = "type_assurance")
public class TypeAssurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // BigDecimal (et non double) pour les montants : pas d'erreur d'arrondi
    @Column(name = "prix_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixBase;

    // Garanties que l'on peut souscrire avec ce type d'assurance (table type_assurance_garantie)
    @ManyToMany
    @JoinTable(name = "type_assurance_garantie",
               joinColumns = @JoinColumn(name = "id_type_assurance"),
               inverseJoinColumns = @JoinColumn(name = "id_garantie"))
    private Set<Garantie> garanties = new HashSet<>();

    public TypeAssurance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrixBase() {
        return prixBase;
    }

    public void setPrixBase(BigDecimal prixBase) {
        this.prixBase = prixBase;
    }

    public Set<Garantie> getGaranties() {
        return garanties;
    }

    public void setGaranties(Set<Garantie> garanties) {
        this.garanties = garanties;
    }
}
