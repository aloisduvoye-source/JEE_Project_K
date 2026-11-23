package fr.cytech.assurance.model;

import fr.cytech.assurance.model.enums.AuteurMessage;
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

import java.time.LocalDateTime;

// Un message de la conversation entre un client et l'assistant IA
@Entity
@Table(name = "message_assistant")
public class MessageAssistant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Utilisateur utilisateur;

    // Qui a écrit le message : le client ou l'assistant
    @Enumerated(EnumType.STRING)
    @Column(name = "auteur", nullable = false)
    private AuteurMessage auteur;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @CreationTimestamp
    @Column(name = "date_message", nullable = false, updatable = false)
    private LocalDateTime dateMessage;

    public MessageAssistant() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Utilisateur getUtilisateur() {return utilisateur;}
    public void setUtilisateur(Utilisateur utilisateur) {this.utilisateur = utilisateur;}
    public AuteurMessage getAuteur() {return auteur;}
    public void setAuteur(AuteurMessage auteur) {this.auteur = auteur;}
    public String getContenu() {return contenu;}
    public void setContenu(String contenu) {this.contenu = contenu;}
    public LocalDateTime getDateMessage() {return dateMessage;}
}
