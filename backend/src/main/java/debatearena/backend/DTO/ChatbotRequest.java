package debatearena.backend.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requête pour envoyer un message au chatbot")
public class ChatbotRequest {

    @Schema(
            description = "Message texte à envoyer au chatbot",
            example = "Quel est votre avis sur l'intelligence artificielle?",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Le message ne peut pas être vide")
    private String message;

    @Schema(
            description = "Identifiant unique de la session de conversation",
            example = "session-123456789",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "L'ID de session est requis")
    private String session_id;

    @Schema(
            description = "Mode de conversation ou type de traitement demandé au chatbot",
            example = "score",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String mode;  // <-- Ajout du champ mode

    // Constructeurs
    public ChatbotRequest() {}

    public ChatbotRequest(String message, String session_id) {
        this.message = message;
        this.session_id = session_id;
    }

    public ChatbotRequest(String message, String session_id, String mode) {
        this.message = message;
        this.session_id = session_id;
        this.mode = mode;
    }

    // Getters et Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSession_id() { return session_id; }
    public void setSession_id(String session_id) { this.session_id = session_id; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
