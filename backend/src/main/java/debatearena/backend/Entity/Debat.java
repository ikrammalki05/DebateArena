package debatearena.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "debat")
public class Debat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "duree")
    private Integer duree;

    // NOUVEAU : Choix de l'utilisateur (POUR ou CONTRE)
    @Column(name = "choix_utilisateur", nullable = false, length = 10)
    private String choixUtilisateur = "POUR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sujet", nullable = false)
    private Sujet sujet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    // Constructeur pratique
    public Debat(Sujet sujet, Utilisateur utilisateur, String choixUtilisateur) {
        this.dateDebut = LocalDateTime.now();
        this.sujet = sujet;
        this.utilisateur = utilisateur;
        this.choixUtilisateur = choixUtilisateur;
        this.duree = null;
    }

    public Debat() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Integer getDuree() {
        return duree;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }

    public String getChoixUtilisateur() {
        return choixUtilisateur;
    }

    public void setChoixUtilisateur(String choixUtilisateur) {
        this.choixUtilisateur = choixUtilisateur;
    }

    public Sujet getSujet() {
        return sujet;
    }

    public void setSujet(Sujet sujet) {
        this.sujet = sujet;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}
