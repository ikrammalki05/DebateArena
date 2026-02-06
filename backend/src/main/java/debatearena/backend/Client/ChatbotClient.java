package com.debatearena.backend.client;

import com.debatearena.backend.dto.ChatbotRequest;
import com.debatearena.backend.dto.ChatbotResponse;
import com.debatearena.backend.exception.ChatbotServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ChatbotClient {

    private final RestTemplate restTemplate;

    @Value("${chatbot.api.base-url}")
    private String baseUrl;

    public ChatbotClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Méthode principale : envoie un message au chatbot avec mode + sessionId
     */
    public ChatbotResponse sendMessage(String message, String sessionId, String mode) {
        try {
            if (message == null || message.trim().isEmpty()) {
                throw new ChatbotServiceException("Message vide ou null envoyé au chatbot");
            }

            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = "default-session";
            }

            if (mode == null || mode.trim().isEmpty()) {
                mode = "train"; // mode par défaut (tu peux mettre "score" ou "chat")
            }

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

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new ChatbotServiceException("Erreur chatbot HTTP: " + response.getStatusCode());
            }

            if (response.getBody() == null) {
                throw new ChatbotServiceException("Réponse vide du chatbot");
            }

            return response.getBody();

        } catch (ChatbotServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ChatbotServiceException("Erreur lors de l'appel au chatbot: " + e.getMessage());
        }
    }

    /**
     * Surcharge : si on appelle seulement avec message + sessionId
     */
    public ChatbotResponse sendMessage(String message, String sessionId) {
        return sendMessage(message, sessionId, "train");
    }

    /**
     * Surcharge : si on appelle seulement avec message
     */
    public ChatbotResponse sendMessage(String message) {
        return sendMessage(message, "default-session", "train");
    }
}
