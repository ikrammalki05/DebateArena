package debatearena.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "debats")
public class Debat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut = LocalDateTime.now();

    @Column(name = "duree")
    private Integer duree; // en secondes

    @Column(name = "choix_utilisateur", nullable = false, length = 10)
    private String choixUtilisateur = "POUR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sujet_id", nullable = false)
    private Sujet sujet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    public Debat(Sujet sujet, Utilisateur utilisateur, String choixUtilisateur) {
        this.sujet = sujet;
        this.utilisateur = utilisateur;
        this.choixUtilisateur = choixUtilisateur;
        this.dateDebut = LocalDateTime.now();
    }
}
