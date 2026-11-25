package fr.cytech.assurance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Compte de connexion du client (nom, email, mot de passe...)
    // Supprimer l'utilisateur supprime aussi le client (ON DELETE CASCADE)
    @OneToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Utilisateur utilisateur;

    @Column(name = "telephone", length = 255)
    private String telephone;

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    public Client() {
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public Utilisateur getUtilisateur() {return utilisateur;}
    public void setUtilisateur(Utilisateur utilisateur) {this.utilisateur = utilisateur;}
    public String getTelephone() {return telephone;}
    public void setTelephone(String telephone) {this.telephone = telephone;}
    public String getAdresse() {return adresse;}
    public void setAdresse(String adresse) {this.adresse = adresse;}
    public LocalDate getDateNaissance() {return dateNaissance;}
    public void setDateNaissance(LocalDate dateNaissance) {this.dateNaissance = dateNaissance;}
}
