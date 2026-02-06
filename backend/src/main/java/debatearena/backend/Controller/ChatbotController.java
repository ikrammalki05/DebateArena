package debatearena.backend.Controller;

import debatearena.backend.Service.DebatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot", description = "API de diagnostic et test du service chatbot")
public class ChatbotController {

    private final DebatService debatService;

    public ChatbotController(DebatService debatService) {
        this.debatService = debatService;
    }

    @Operation(
            summary = "Vérifier l'état du chatbot",
            description = "Retourne l'état de santé du service chatbot et le nombre de sessions actives"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "État du chatbot récupéré",
                    content = @Content(schema = @Schema(example = """
                {
                  "status": "healthy",
                  "service": "chatbot",
                  "active_sessions": 5
                }
                """))
            )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getChatbotHealth() {
        Map<String, Object> status = debatService.getChatbotStatus();
        return ResponseEntity.ok(status);
    }

    @Operation(
            summary = "Tester le chatbot",
            description = "Envoie un message directement au chatbot pour tester sa connectivité et ses réponses"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Test effectué",
                    content = @Content(schema = @Schema(example = """
                {
                  "test_result": "✅ Test réussi!\\nSession: chat-session-12345\\nRéponse: Bonjour, je suis prêt à débattre!",
                  "timestamp": "2024-01-15T14:30:00"
                }
                """))
            )
    })
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testChatbot(
            @Parameter(
                    description = "Message à envoyer au chatbot",
                    required = true,
                    schema = @Schema(example = "{\"message\": \"Bonjour, peux-tu débattre?\"}")
            )
            @RequestBody Map<String, String> request
    ) {
        String message = request.get("message");
        String result = debatService.testerChatbot(message);

        return ResponseEntity.ok(Map.of(
                "test_result", result,
                "timestamp", LocalDateTime.now()
        ));
    }
}
