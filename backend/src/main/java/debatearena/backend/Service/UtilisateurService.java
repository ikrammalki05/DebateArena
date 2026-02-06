package debatearena.backend.Service;

import debatearena.backend.DTO.*;
import debatearena.backend.Entity.Debat;
import debatearena.backend.Entity.Utilisateur;
import debatearena.backend.Entity.role_enum;
import debatearena.backend.Exceptions.UnauthorizedException;
import debatearena.backend.Exceptions.NotFoundException;
import debatearena.backend.Repository.*;
import debatearena.backend.Utils.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final ImageStorageService imageStorageService;
    private final DebatRepository debatRepository;
    private final TestRepository testRepository;

    // ========== MÉTHODES EXISTANTES ==========

    public Optional<Utilisateur> findUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return utilisateurRepository.findByEmail(email).isPresent();
    }

    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur non trouvé"));
    }

    @Transactional
    public void updateScore(Long userId, Integer points) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));
        user.setScore(user.getScore() + points);
        utilisateurRepository.save(user);
    }

    // ========== MÉTHODES POUR DÉBATS ==========

    /**
     * Met à jour le score d'un utilisateur
     */
    @Transactional
    public void updateScore(Utilisateur utilisateur, Integer points) {
        utilisateur.setScore(utilisateur.getScore() + points);
        utilisateurRepository.save(utilisateur);
    }

    /**
     * Calcule le niveau de l'utilisateur
     */
    public String calculerNiveau(Utilisateur utilisateur) {
        return calculerNiveau(utilisateur.getScore());
    }

    /**
     * Vérifie si un utilisateur peut accéder à un sujet
     */
    public boolean peutAccederAuSujet(Utilisateur utilisateur, String difficulteSujet) {
        String niveauUser = calculerNiveau(utilisateur.getScore());
        return estAccessible(niveauUser, difficulteSujet);
    }

    /**
     * Récupère l'utilisateur CHATBOT
     */
    public Utilisateur getChatbotUser() {
        return utilisateurRepository.findByRole(role_enum.CHATBOT)
                .orElseThrow(() -> new NotFoundException("Utilisateur CHATBOT non trouvé"));
    }

    // ========== PROFIL PERSONNEL (/me) ==========

    /**
     * Récupère le profil personnel (sans données de jeu)
     */
    public UtilisateurProfile getMyProfile() {
        Utilisateur user = getCurrentUser();

        return new UtilisateurProfile(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getRole().name(),
                user.getImagePath()
        );
    }

    /**
     * Met à jour le profil personnel
     */
    public UtilisateurProfile updateMyProfile(UpdateProfileRequest request) throws IOException {
        Utilisateur user = getCurrentUser();

        if (request.getNom() != null && !request.getNom().isEmpty()) {
            user.setNom(request.getNom());
        }

        if (request.getPrenom() != null && !request.getPrenom().isEmpty()) {
            user.setPrenom(request.getPrenom());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imagePath = imageStorageService.saveImage(request.getImage());
            user.setImagePath(imagePath);
        }

        utilisateurRepository.save(user);
        return getMyProfile();
    }

    /**
     * Met à jour uniquement l'image de profil
     */
    public UtilisateurProfile updateProfileImage(MultipartFile image) throws IOException {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setImage(image);
        return updateMyProfile(request);
    }

    // ========== DASHBOARD (/dashboard) ==========

    /**
     * Récupère toutes les données du tableau de bord
     */
    public Dashboard getDashboard() {
        Utilisateur user = getCurrentUser();
        Long userId = user.getId();

        // 1. Statistiques de base
        Integer totalDebats = debatRepository.countByUtilisateurId(userId);
        Integer debatsGagnes = testRepository.countDebatsGagnesByUserId(userId);
        Double tauxReussite = totalDebats > 0 ?
                (debatsGagnes * 100.0) / totalDebats : 0.0;

        // 2. Notes (note sur 20 dans ta base)
        Integer moyenneNotes = testRepository.getMoyenneNotesByUserId(userId);
        Integer meilleureNote = testRepository.getMeilleureNoteByUserId(userId);

        // 3. Niveau et progression
        String niveau = calculerNiveau(user.getScore());
        Double progressionPourcentage = calculerProgression(user.getScore());
        Integer pointsPourNiveauSuivant = calculerPointsPourNiveauSuivant(user.getScore(), niveau);

        // 4. Historique récent des débats (5 derniers)
        List<DebatRecap> debatsRecents = getDebatsRecents(userId, 5);

        return new Dashboard(
                niveau,
                progressionPourcentage,
                user.getScore(),
                pointsPourNiveauSuivant,
                user.getBadge(),
                totalDebats,
                debatsGagnes,
                tauxReussite,
                moyenneNotes != null ? moyenneNotes : 0,
                meilleureNote != null ? meilleureNote : 0,
                debatsRecents
        );
    }

    // ========== MÉTHODES UTILITAIRES PRIVÉES ==========

    /**
     * Calcule le niveau basé sur le score (selon ton schéma)
     */
    private String calculerNiveau(Integer score) {
        if (score >= 1000) return "EXPERT";
        else if (score >= 500) return "AVANCE";
        else if (score >= 200) return "INTERMEDIAIRE";
        else return "DEBUTANT";
    }

    /**
     * Calcule la progression vers le niveau suivant (0-100%)
     */
    private Double calculerProgression(Integer score) {
        String niveau = calculerNiveau(score);

        switch (niveau) {
            case "DEBUTANT":
                return (score * 100.0) / 200;
            case "INTERMEDIAIRE":
                return ((score - 200) * 100.0) / 300; // 200 → 500
            case "AVANCE":
                return ((score - 500) * 100.0) / 500; // 500 → 1000
            case "EXPERT":
                return 100.0;
            default:
                return 0.0;
        }
    }

    /**
     * Calcule les points manquants pour le niveau suivant
     */
    private Integer calculerPointsPourNiveauSuivant(Integer score, String niveau) {
        switch (niveau) {
            case "DEBUTANT":
                return Math.max(0, 200 - score);
            case "INTERMEDIAIRE":
                return Math.max(0, 500 - score);
            case "AVANCE":
                return Math.max(0, 1000 - score);
            case "EXPERT":
                return 0;
            default:
                return 0;
        }
    }

    /**
     * Vérifie l'accès à un sujet
     */
    private boolean estAccessible(String niveauUser, String difficulteSujet) {
        if ("EXPERT".equals(niveauUser)) return true;
        if ("AVANCE".equals(niveauUser)) return !"EXPERT".equals(difficulteSujet);
        if ("INTERMEDIAIRE".equals(niveauUser)) {
            return "DEBUTANT".equals(difficulteSujet) || "INTERMEDIAIRE".equals(difficulteSujet);
        }
        if ("DEBUTANT".equals(niveauUser)) {
            return "DEBUTANT".equals(difficulteSujet);
        }
        return false;
    }

    /**
     * Récupère les débats récents formatés
     */
    private List<DebatRecap> getDebatsRecents(Long userId, int limit) {
        List<Object[]> results = debatRepository.findRecentDebatsByUtilisateurId(userId);

        // Limiter manuellement car LIMIT n'est pas supporté dans JPQL avec ORDER BY
        return results.stream()
                .limit(limit)
                .map(this::mapToDebatRecap)
                .collect(Collectors.toList());
    }

    /**
     * Mappe un résultat SQL à un DebatRecap
     */
    private DebatRecap mapToDebatRecap(Object[] result) {
        // Récupérer l'ID du débat
        Long debatId = ((Number) result[0]).longValue();

        // Récupérer le débat complet (nécessite une requête supplémentaire)
        Debat debat = debatRepository.findById(debatId).orElseThrow(
                () -> new NotFoundException("Débat non trouvé")
        );

        return new DebatRecap(
                debatId,                                    // id
                (String) result[1],                         // titre
                ((Enum<?>) result[2]).name(),               // categorie
                ((Enum<?>) result[3]).name(),               // difficulte
                testRepository.existsByDebat(debat) ? "TEST" : "ENTRAINEMENT", // type
                debat.getChoixUtilisateur(),                // choix utilisateur
                result[4] != null ? ((Number) result[4]).intValue() : null, // note
                (LocalDateTime) result[5],                  // date
                formatDuree(result[6] != null ? ((Number) result[6]).intValue() : null) // duree
        );
    }

    /**
     * Formate la durée en minutes/secondes
     */
    private String formatDuree(Integer dureeSeconds) {
        if (dureeSeconds == null) return "N/A";

        int minutes = dureeSeconds / 60;
        int seconds = dureeSeconds % 60;
        return minutes + "min " + seconds + "s";
    }
}
