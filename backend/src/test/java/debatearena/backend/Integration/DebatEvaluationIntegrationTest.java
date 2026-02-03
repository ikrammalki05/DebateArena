package debatearena.backend.Integration;

import debatearena.backend.Client.ChatbotClient;
import debatearena.backend.DTO.ChatbotResponse;
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
// Importez votre Repository de Test (adaptez le nom si nécessaire)
import debatearena.backend.Repository.TestRepository;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DebatEvaluationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private SujetRepository sujetRepository;
    @Autowired private DebatRepository debatRepository;
    @Autowired private MessageRepository messageRepository;

    // AJOUT : Injection du repository Test
    @Autowired private TestRepository testRepository;

    @MockBean private ChatbotClient chatbotClient;

    private Debat debatTest;

    @BeforeEach
    void setUp() {
        // NETTOYAGE (Ordre important : Test dépend de Debat)
        testRepository.deleteAll();
        messageRepository.deleteAll();
        debatRepository.deleteAll();
        sujetRepository.deleteAll();
        utilisateurRepository.deleteAll();

        // 1. Utilisateur
        Utilisateur user = new Utilisateur();
        user.setEmail("user@test.com");
        user.setNom("User");
        user.setPrenom("Test");
        user.setPassword("pass");
        user.setRole(role_enum.UTILISATEUR);
        user.setScore(100);
        utilisateurRepository.save(user);

        // 2. Chatbot
        Utilisateur bot = new Utilisateur();
        bot.setEmail("chatbot@system");
        bot.setRole(role_enum.CHATBOT);
        bot.setNom("Chatbot");
        bot.setPrenom("IA");
        bot.setPassword("x");
        utilisateurRepository.save(bot);

        // 3. Sujet
        Sujet sujet = new Sujet();
        sujet.setTitre("Sujet Eval");
        sujet.setCategorie(categorie_sujet_enum.INFORMATIQUE);
        sujet.setDifficulte(niveau_enum.DEBUTANT);
        sujetRepository.save(sujet);

        // 4. Débat
        debatTest = new Debat();
        debatTest.setUtilisateur(user);
        debatTest.setSujet(sujet);
        debatTest.setChoixUtilisateur("POUR");
        debatTest.setDateDebut(LocalDateTime.now());
        debatTest = debatRepository.save(debatTest);

        // 5. AJOUT CRUCIAL : Création de l'entité "Test" liée au débat
        // On utilise le chemin complet pour éviter le conflit avec @Test de JUnit
        debatearena.backend.Entity.Test testEntity = new debatearena.backend.Entity.Test();
        testEntity.setDebat(debatTest);
        // Initialisez d'autres champs obligatoires de l'entité Test ici si nécessaire (ex: score à null, statut...)
        testRepository.save(testEntity);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "UTILISATEUR")
    void evaluerTest_ShouldWork() throws Exception {
        // ARRANGE
        when(chatbotClient.isHealthy()).thenReturn(true);

        ChatbotResponse mockResponse = new ChatbotResponse();
        mockResponse.setResponse("Note: 15/20. Excellent argumentation.");
        mockResponse.setSession_id("session-eval-1");

        when(chatbotClient.sendMessage(any(), any(), anyString())).thenReturn(mockResponse);

        // ACT & ASSERT
        mockMvc.perform(post("/api/debats/" + debatTest.getId() + "/evaluation")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
