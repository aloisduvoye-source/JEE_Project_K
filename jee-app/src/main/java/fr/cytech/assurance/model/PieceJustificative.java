package fr.cytech.assurance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// Fichier joint à une déclaration de sinistre : le fichier est sur le disque, seul son chemin est en base
@Entity
@Table(name = "piece_justificative")
public class PieceJustificative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sinistre", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sinistre sinistre;

    // Nom d'origine du fichier envoyé par le client
    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "chemin", nullable = false, length = 500)
    private String chemin;

    @Column(name = "type_mime", length = 100)
    private String typeMime;

    @CreationTimestamp
    @Column(name = "date_ajout", nullable = false, updatable = false)
    private LocalDateTime dateAjout;

    public PieceJustificative() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Sinistre getSinistre() {
        return sinistre;
    }

    public void setSinistre(Sinistre sinistre) {
        this.sinistre = sinistre;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public String getChemin() {
        return chemin;
    }

    public void setChemin(String chemin) {
        this.chemin = chemin;
    }

    public String getTypeMime() {
        return typeMime;
    }

    public void setTypeMime(String typeMime) {
        this.typeMime = typeMime;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }
}
