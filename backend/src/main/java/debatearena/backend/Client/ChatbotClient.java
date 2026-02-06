package debatearena.backend.Client;

import debatearena.backend.DTO.ChatbotRequest;
import debatearena.backend.DTO.ChatbotResponse;
import debatearena.backend.DTO.ChatbotHealthResponse;
import debatearena.backend.Exceptions.ChatbotServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

@Component
public class ChatbotClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ChatbotClient(RestTemplateBuilder restTemplateBuilder,
                         @Value("${app.chatbot.base-url:http://chatbot:5005}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isHealthy() {
        try {
            String url = baseUrl + "/health";
            ResponseEntity<ChatbotHealthResponse> response =
                    restTemplate.getForEntity(url, ChatbotHealthResponse.class);

            return response.getBody() != null &&
                    "healthy".equalsIgnoreCase(response.getBody().getStatus());

        } catch (Exception e) {
            return false;
        }
    }

    public ChatbotResponse sendMessage(String message, String sessionId, String mode) {
        try {
            String url = baseUrl + "/chat";

            ChatbotRequest request = new ChatbotRequest();
            request.setMessage(message);
            request.setSession_id(sessionId);
            request.setMode(mode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChatbotRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ChatbotResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ChatbotResponse.class
            );

            if (response.getBody() == null) {
                throw new ChatbotServiceException("Réponse vide du chatbot");
            }

            return response.getBody();

        } catch (Exception e) {
            throw new ChatbotServiceException("Erreur appel chatbot: " + e.getMessage());
        }
    }

    public void clearSession(String sessionId) {
        try {
            if (sessionId == null) return;

            String url = baseUrl + "/chat/" + sessionId;
            restTemplate.delete(url);

        } catch (Exception e) {
            // ignore
        }
    }
}
