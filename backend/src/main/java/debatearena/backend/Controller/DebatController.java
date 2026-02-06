package debatearena.backend.Controller;

import debatearena.backend.DTO.*;
import debatearena.backend.Service.DebatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debats")
@Tag(name = "Débats", description = "API de gestion des débats avec le chatbot")
@SecurityRequirement(name = "bearerAuth")
public class DebatController {

    private final DebatService debatService;

    public DebatController(DebatService debatService) {
        this.debatService = debatService;
    }

    @Operation(
            summary = "Créer un nouveau débat",
            description = "Crée un débat d'entraînement ou de test sur un sujet spécifique"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Débat créé avec succès",
                    content = @Content(schema = @Schema(implementation = DebatResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou débat déjà en cours sur ce sujet"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Niveau insuffisant pour accéder à ce sujet"
            )
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DebatResponse> creerDebat(
            @Parameter(
                    description = "Données pour créer un débat",
                    required = true
            )
            @RequestBody CreerDebatRequest request
    ) {
        DebatResponse response = debatService.creerDebat(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Envoyer un message dans un débat",
            description = "Envoie un message au chatbot dans le contexte d'un débat spécifique"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Message envoyé et réponse reçue",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Message vide ou débat terminé"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Débat non trouvé"
            )
    })
    @PostMapping("/{debatId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> envoyerMessage(
            @Parameter(description = "ID du débat", example = "123")
            @PathVariable Long debatId,
            @Parameter(description = "Contenu du message", required = true)
            @RequestBody MessageRequest request
    ) {
        MessageResponse response = debatService.envoyerMessage(debatId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Terminer un débat",
            description = "Marque un débat comme terminé et calcule sa durée"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Débat terminé avec succès",
                    content = @Content(schema = @Schema(implementation = DebatResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Débat déjà terminé"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    @PostMapping("/{debatId}/terminer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DebatResponse> terminerDebat(
            @Parameter(description = "ID du débat", example = "123")
            @PathVariable Long debatId
    ) {
        DebatResponse response = debatService.terminerDebat(debatId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer un débat spécifique",
            description = "Retourne les détails d'un débat par son ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Débat récupéré",
                    content = @Content(schema = @Schema(implementation = DebatResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Débat non trouvé"
            )
    })
    @GetMapping("/{debatId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DebatResponse> getDebat(
            @Parameter(description = "ID du débat", example = "123")
            @PathVariable Long debatId
    ) {
        DebatResponse response = debatService.getDebat(debatId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Récupérer les messages d'un débat",
            description = "Retourne l'historique complet des messages d'un débat"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des messages",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MessageResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Débat non trouvé"
            )
    })
    @GetMapping("/{debatId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageResponse>> getMessagesDebat(
            @Parameter(description = "ID du débat", example = "123")
            @PathVariable Long debatId
    ) {
        List<MessageResponse> responses = debatService.getMessagesDebat(debatId);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Récupérer tous mes débats",
            description = "Retourne la liste complète des débats de l'utilisateur"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des débats",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DebatResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    @GetMapping("/mes-debats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DebatResponse>> getMesDebats() {
        List<DebatResponse> responses = debatService.getMesDebats();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Récupérer les débats en cours",
            description = "Retourne la liste des débats actuellement en cours"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des débats en cours",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DebatResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    @GetMapping("/en-cours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DebatResponse>> getDebatsEnCours() {
        List<DebatResponse> responses = debatService.getDebatsEnCours();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Récupérer les débats terminés",
            description = "Retourne la liste des débats déjà terminés"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des débats terminés",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DebatResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    @GetMapping("/termines")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DebatResponse>> getDebatsTermines() {
        List<DebatResponse> responses = debatService.getDebatsTermines();
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Annuler un débat",
            description = "Supprime un débat en cours (non terminé)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Débat annulé avec succès"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Débat déjà terminé"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Débat non trouvé"
            )
    })
    @DeleteMapping("/{debatId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> annulerDebat(
            @Parameter(description = "ID du débat", example = "123")
            @PathVariable Long debatId
    ) {
        debatService.annulerDebat(debatId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Évaluer un test",
            description = "Déclenche l'évaluation d'un débat de type TEST (note et feedback)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Évaluation en cours",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ce débat n'est pas un TEST"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Débat non trouvé"
            )
    })
    @PostMapping("/{debatId}/evaluation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> evaluerTest(
            @Parameter(description = "ID du débat", example = "123")
            @PathVariable Long debatId
    ) {
        MessageResponse response = debatService.evaluerTest(debatId);
        return ResponseEntity.ok(response);
    }
}
