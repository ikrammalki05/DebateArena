package debatearena.backend.Service;

import debatearena.backend.Entity.PasswordResetToken;
import debatearena.backend.Entity.Utilisateur;
import debatearena.backend.Exceptions.BadRequestException;
import debatearena.backend.Exceptions.NotFoundException;
import debatearena.backend.Exceptions.UnauthorizedException;
import debatearena.backend.Repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
//@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepo, UtilisateurService utilisateurService, PasswordEncoder passwordEncoder) {
        this.tokenRepo = tokenRepo;
        this.utilisateurService = utilisateurService;
        this.passwordEncoder = passwordEncoder;
    }

    public void createPasswordResetToken(String email) {
        // Utiliser Optional pour gérer proprement
        Utilisateur user = utilisateurService.findUtilisateurByEmail(email)
                .orElseThrow(() -> new NotFoundException("Aucun compte associé à cet email"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUtilisateur(user);
        resetToken.setToken(token);
        resetToken.setExpiration(LocalDateTime.now().plusHours(1));

        tokenRepo.save(resetToken);

        // TODO: envoyer email avec le lien
        // Exemple: http://localhost:3000/reset-password?token=XXXX
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("Le token est obligatoire");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("Le nouveau mot de passe est obligatoire");
        }

        if (newPassword.length() < 6) {
            throw new BadRequestException("Le mot de passe doit contenir au moins 6 caractères");
        }

        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token de réinitialisation invalide"));

        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Le token de réinitialisation a expiré");
        }

        Utilisateur user = resetToken.getUtilisateur();
        user.setPassword(passwordEncoder.encode(newPassword));
        utilisateurService.save(user);

        // Supprimer le token après usage
        tokenRepo.delete(resetToken);
    }

    // Méthode supplémentaire pour valider un token
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        return tokenRepo.findByToken(token)
                .map(resetToken -> !resetToken.getExpiration().isBefore(LocalDateTime.now()))
                .orElse(false);
    }

    // Méthode pour obtenir l'email associé à un token
    public String getEmailFromToken(String token) {
        return tokenRepo.findByToken(token)
                .map(resetToken -> resetToken.getUtilisateur().getEmail())
                .orElseThrow(() -> new BadRequestException("Token invalide"));
    }
}
