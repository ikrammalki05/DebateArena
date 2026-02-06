package debatearena.backend.Service;

import debatearena.backend.Entity.PasswordResetToken;
import debatearena.backend.Entity.Utilisateur;
import debatearena.backend.Exceptions.*;
import debatearena.backend.Repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;

    public void createPasswordResetToken(String email) {
        Utilisateur user = utilisateurService.findUtilisateurByEmail(email)
                .orElseThrow(() -> new NotFoundException("Aucun compte associé à cet email"));

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUtilisateur(user);
        resetToken.setToken(token);
        resetToken.setExpiration(LocalDateTime.now().plusHours(1));

        tokenRepo.save(resetToken);

        // TODO: envoyer email avec le lien de reset
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) throw new BadRequestException("Token obligatoire");
        if (newPassword == null || newPassword.trim().isEmpty()) throw new BadRequestException("Mot de passe obligatoire");
        if (newPassword.length() < 6) throw new BadRequestException("Mot de passe trop court");

        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token invalide"));

        if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Token expiré");
        }

        Utilisateur user = resetToken.getUtilisateur();
        user.setPassword(passwordEncoder.encode(newPassword));
        utilisateurService.save(user);

        tokenRepo.delete(resetToken);
    }

    public boolean validateToken(String token) {
        return token != null && tokenRepo.findByToken(token)
                .map(t -> !t.getExpiration().isBefore(LocalDateTime.now()))
                .orElse(false);
    }

    public String getEmailFromToken(String token) {
        return tokenRepo.findByToken(token)
                .map(t -> t.getUtilisateur().getEmail())
                .orElseThrow(() -> new BadRequestException("Token invalide"));
    }
}
