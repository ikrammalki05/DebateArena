package debatearena.backend.Service;

import debatearena.backend.Entity.*;
import debatearena.backend.Exceptions.*;
import debatearena.backend.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class DebatService {

    private final DebatRepository debatRepository;
    private final SujetRepository sujetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TestRepository testRepository;
    private final ChatbotClient chatbotClient; // Client pour l'API du chatbot

    // Gestion des sessions du chatbot par débat
    private final Map<Long, String> debatSessions = new ConcurrentHashMap<>();

    /**
     * Créer un nouveau débat pour un utilisateur et un sujet donné
     */
    public Debat creerDebat(Long sujetId, Long utilisateurId, String choixUtilisateur) {
        Sujet sujet = sujetRepository.findById(sujetId)
                .orElseThrow(() -> new NotFoundException("Sujet non trouvé"));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        Debat debat = new Debat(sujet, utilisateur, choixUtilisateur);
        return debatRepository.save(debat);
    }

    /**
     * Envoyer un message au chatbot et récupérer la réponse
     */
    public String envoyerMessage(Long debatId, String messageUtilisateur) {
        Debat debat = debatRepository.findById(debatId)
                .orElseThrow(() -> new NotFoundException("Débat non trouvé"));

        try {
            String mode = testRepository.existsByDebat(debat) ? "score" : "train";
            String sessionId = debatSessions.get(debatId);
            boolean nouvelleSession = (sessionId == null);

            ChatbotResponse response = chatbotClient.sendMessage(messageUtilisateur, sessionId, mode);

            if (response == null || response.getResponse() == null) {
                throw new RuntimeException("Réponse chatbot invalide");
            }

            if (response.getSession_id() != null) {
                debatSessions.put(debatId, response.getSession_id());
                if (nouvelleSession) System.out.println("Nouvelle session chatbot: " + response.getSession_id());
            }

            return response.getResponse();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'appel au chatbot: " + e.getMessage());
        }
    }

    /**
     * Terminer un débat et supprimer la session du chatbot
     */
    public void terminerDebat(Long debatId) {
        Debat debat = debatRepository.findById(debatId)
                .orElseThrow(() -> new NotFoundException("Débat non trouvé"));

        nettoyerSessionDebat(debatId);
        debatRepository.delete(debat);
    }

    /**
     * Évaluer un test après un débat
     */
    public Test evaluerTest(Long debatId, Integer note) {
        if (note == null || note < 0 || note > 20) {
            throw new BadRequestException("Note invalide (0-20)");
        }

        Debat debat = debatRepository.findById(debatId)
                .orElseThrow(() -> new NotFoundException("Débat non trouvé"));

        Test test = new Test();
        test.setDebat(debat);
        test.setNote(note);

        return testRepository.save(test);
    }

    /**
     * Obtenir tous les débats d'un utilisateur
     */
    public List<Debat> getDebatsByUtilisateur(Long utilisateurId) {
        return debatRepository.findAllByUtilisateurId(utilisateurId);
    }

    /**
     * Obtenir le dernier débat d'un utilisateur
     */
    public Debat getDernierDebat(Long utilisateurId) {
        return debatRepository.findTopByUtilisateurIdOrderByDateDebutDesc(utilisateurId)
                .orElseThrow(() -> new NotFoundException("Aucun débat trouvé pour cet utilisateur"));
    }

    /**
     * Vérifier si un débat existe
     */
    public boolean debatExiste(Long debatId) {
        return debatRepository.existsById(debatId);
    }

    /**
     * Nettoyer une session du chatbot
     */
    private void nettoyerSessionDebat(Long debatId) {
        String sessionId = debatSessions.remove(debatId);
        if (sessionId != null) {
            try {
                chatbotClient.clearSession(sessionId);
            } catch (Exception e) {
                System.err.println("Erreur nettoyage session chatbot: " + e.getMessage());
            }
        }
    }

}
