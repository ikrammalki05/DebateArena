package debatearena.backend.Client;

import debatearena.backend.DTO.ChatbotResponse;
import debatearena.backend.Exceptions.ChatbotServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class ChatbotClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ChatbotClient(RestTemplateBuilder restTemplateBuilder,
                         @Value("${app.chatbot.base-url:http://localhost:8000}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isHealthy() {
        try {
            String url = baseUrl + "/";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public ChatbotResponse sendMessage(String message, String sessionId, String mode) {
        try {
            String url = baseUrl + "/chat";

            // Construire le corps de la requête
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            requestBody.put("mode", mode != null ? mode : "train");
            if (sessionId != null) {
                requestBody.put("session_id", sessionId);
            }

            // Configurer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Envoyer la requête
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() == null) {
                throw new ChatbotServiceException("Réponse vide du chatbot");
            }

            Map<String, Object> responseBody = response.getBody();

            // CORRECTION : Le service Python retourne "text", pas "response"
            String responseText = (String) responseBody.get("text");
            String newSessionId = (String) responseBody.get("session_id");

            if (responseText == null) {
                responseText = "Erreur: pas de réponse textuelle";
            }

            // Créer la réponse avec les bons champs
            ChatbotResponse chatbotResponse = new ChatbotResponse();
            chatbotResponse.setResponse(responseText);  // Stocker le "text" dans "response"
            chatbotResponse.setSession_id(newSessionId);

            // Log pour débogage
            System.out.println("Chatbot response received: " + responseText.substring(0, Math.min(100, responseText.length())) + "...");
            System.out.println("Session ID: " + newSessionId);

            return chatbotResponse;

        } catch (Exception e) {
            System.err.println("ChatbotClient error: " + e.getMessage());
            e.printStackTrace();
            throw new ChatbotServiceException("Erreur lors de l'appel au chatbot: " + e.getMessage(), e);
        }
    }

    public void clearSession(String sessionId) {
        try {
            String url = baseUrl + "/session/" + sessionId;
            restTemplate.delete(url);
            System.out.println("Session cleared: " + sessionId);
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage de session: " + e.getMessage());
        }
    }
}
