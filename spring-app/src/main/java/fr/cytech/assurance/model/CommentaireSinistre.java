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

// Commentaire ajouté par un agent lors de l'analyse d'un sinistre
@Entity
@Table(name = "commentaire_sinistre")
public class CommentaireSinistre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sinistre", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Sinistre sinistre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_auteur", nullable = false)
    private Utilisateur auteur;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @CreationTimestamp
    @Column(name = "date_commentaire", nullable = false, updatable = false)
    private LocalDateTime dateCommentaire;

    public CommentaireSinistre() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Sinistre getSinistre() {return sinistre;}
    public void setSinistre(Sinistre sinistre) {this.sinistre = sinistre;}
    public Utilisateur getAuteur() {return auteur;}
    public void setAuteur(Utilisateur auteur) {this.auteur = auteur;}
    public String getContenu() {return contenu;}
    public void setContenu(String contenu) {this.contenu = contenu;}
    public LocalDateTime getDateCommentaire() {return dateCommentaire;}
}
