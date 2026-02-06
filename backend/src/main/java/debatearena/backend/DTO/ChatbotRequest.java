package debatearena.backend.Client;

import debatearena.backend.DTO.ChatbotRequest;
import debatearena.backend.DTO.ChatbotResponse;
import debatearena.backend.exception.ChatbotServiceException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Client pour communiquer avec le Chatbot
 */
public class ChatbotClient {

    private final String chatbotUrl;
    private final RestTemplate restTemplate;

    public ChatbotClient(String chatbotUrl) {
        this.chatbotUrl = chatbotUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Méthode principale pour envoyer un message au chatbot
     *
     * @param message    le texte du message
     * @param session_id identifiant unique de la session
     * @param mode       mode de réponse ("score", "default", etc.)
     * @return ChatbotResponse contenant la réponse
     */
    public ChatbotResponse sendMessage(String message, String session_id, String mode) {
        try {
            ChatbotRequest request = new ChatbotRequest();
            request.setMessage(message);
            request.setSession_id(session_id);

            // Si mode est nécessaire dans ChatbotRequest
            // Ajoute un champ "mode" dans ChatbotRequest si tu veux stocker le mode
            // request.setMode(mode); // si tu l'ajoutes dans DTO

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChatbotRequest> entity = new HttpEntity<>(request, headers);

            // Envoi de la requête POST
            return restTemplate.postForObject(chatbotUrl, entity, ChatbotResponse.class);

        } catch (Exception e) {
            throw new ChatbotServiceException("Erreur lors de l'envoi du message au chatbot", e);
        }
    }

    /**
     * Surcharge pour compatibilité avec anciens tests (2 arguments)
     */
    public ChatbotResponse sendMessage(String message, String session_id) {
        return sendMessage(message, session_id, "default");
    }
}
