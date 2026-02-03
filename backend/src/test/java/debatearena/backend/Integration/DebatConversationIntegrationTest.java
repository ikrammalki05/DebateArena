package debatearena.backend.Integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import debatearena.backend.Client.ChatbotClient;
import debatearena.backend.DTO.ChatbotResponse;
import debatearena.backend.DTO.MessageRequest;
import debatearena.backend.Entity.Debat;
import debatearena.backend.Entity.Sujet;
import debatearena.backend.Entity.Utilisateur;
import debatearena.backend.Entity.categorie_sujet_enum;
import debatearena.backend.Entity.niveau_enum;
import debatearena.backend.Entity.role_enum;
import debatearena.backend.Repository.DebatRepository;
import debatearena.backend.Repository.MessageRepository;
import debatearena.backend.Repository.SujetRepository;
import debatearena.backend.Repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DebatConversationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private SujetRepository sujetRepository;
    @Autowired private DebatRepository debatRepository;
    @Autowired private MessageRepository messageRepository;

    @MockBean private ChatbotClient chatbotClient;

    private Debat activeDebat;

    @BeforeEach
    void setUp() {
        // Nettoyage dans l'ordre inverse des dépendances
        messageRepository.deleteAll();
        debatRepository.deleteAll();
        sujetRepository.deleteAll();
        utilisateurRepository.deleteAll();

        // 1. Création de l'Utilisateur
        Utilisateur user = new Utilisateur();
        user.setEmail("user@test.com");
        user.setNom("User");
        user.setPrenom("Test");
        user.setPassword("pass");
        user.setRole(role_enum.UTILISATEUR);
        user.setScore(100);
        utilisateurRepository.save(user);

        // 2. Création du Chatbot (nécessaire pour enregistrer ses réponses en base)
        Utilisateur bot = new Utilisateur();
        bot.setEmail("chatbot@system");
        bot.setRole(role_enum.CHATBOT);
        bot.setNom("Chatbot");
        bot.setPrenom("IA");
        bot.setPassword("x");
        utilisateurRepository.save(bot);

        // 3. Création du Sujet
        Sujet sujet = new Sujet();
        sujet.setTitre("Sujet Conv");
        sujet.setCategorie(categorie_sujet_enum.INFORMATIQUE); // Catégorie valide
        sujet.setDifficulte(niveau_enum.DEBUTANT);
        sujetRepository.save(sujet);

        // 4. Création d'un débat actif
        activeDebat = new Debat();
        activeDebat.setUtilisateur(user);
        activeDebat.setSujet(sujet);
        activeDebat.setChoixUtilisateur("POUR");
        activeDebat.setDateDebut(LocalDateTime.now());
        activeDebat = debatRepository.save(activeDebat);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "UTILISATEUR")
    void envoyerMessage_ShouldSaveUserMessageAndGetBotResponse() throws Exception {
        // --- ARRANGE ---

        // 1. IMPORTANT : On simule que le chatbot est en ligne (Health Check)
        when(chatbotClient.isHealthy()).thenReturn(true);

        // 2. On prépare la réponse simulée du chatbot
        ChatbotResponse mockResponse = new ChatbotResponse();
        mockResponse.setResponse("Ceci est une réponse automatique de l'IA.");
        mockResponse.setSession_id("session-123");

        // 3. On mocke l'envoi du message (avec any() pour être plus large sur les arguments)
        when(chatbotClient.sendMessage(any(), any(), any())).thenReturn(mockResponse);

        // Requête de l'utilisateur
        MessageRequest userMessage = new MessageRequest();
        userMessage.setContenu("Je pense que c'est une bonne idée.");

        // --- ACT & ASSERT ---
        mockMvc.perform(post("/api/debats/" + activeDebat.getId() + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userMessage)))

                .andExpect(status().isOk())
                // On vérifie que la réponse JSON contient bien le texte du mock
                .andExpect(jsonPath("$.contenu", is("Ceci est une réponse automatique de l'IA.")))
                .andExpect(jsonPath("$.auteur", is("CHATBOT")));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "UTILISATEUR")
    void getHistoriqueMessages_ShouldReturnList() throws Exception {
        // Simple vérification que l'endpoint de lecture fonctionne
        mockMvc.perform(get("/api/debats/" + activeDebat.getId() + "/messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "UTILISATEUR")
    void terminerDebat_ShouldCloseDebat() throws Exception {
        // Vérification de la clôture du débat
        mockMvc.perform(post("/api/debats/" + activeDebat.getId() + "/terminer")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("TERMINE"))); // Assurez-vous que votre DTO renvoie un statut ou une date de fin
    }
}
