package fr.limayrac.snapwedding.event.web;

import fr.limayrac.snapwedding.event.internal.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Routes de l'organisateur sur ses événements.
 * Entrée : un DTO *Request validé. Sortie : un DTO *Response. L'entité ne traverse jamais la frontière HTTP.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Les événements de l'organisateur connecté")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Créer un événement")
    @ApiResponse(responseCode = "201", description = "Événement créé, ouvert aux dépôts")
    @ApiResponse(responseCode = "400", description = "Données invalides ou champ non autorisé")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventCreateRequest request) {
        return EventResponse.from(eventService.create(request.name(), request.eventDate(), request.location()));
    }

    @Operation(summary = "Lister mes événements", description = "Liste paginée (?page=0&size=20).")
    @GetMapping
    public Page<EventResponse> findMine(@ParameterObject Pageable pageable) {
        return eventService.findMine(pageable).map(EventResponse::from);
    }

    @Operation(summary = "Détail d'un de mes événements")
    @ApiResponse(responseCode = "404", description = "Inexistant ou appartenant à un autre organisateur")
    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable UUID id) {
        return EventResponse.from(eventService.findMine(id));
    }

    @Operation(summary = "Modifier un événement (nom, date, lieu)")
    @PutMapping("/{id}")
    public EventResponse update(@PathVariable UUID id, @Valid @RequestBody EventUpdateRequest request) {
        return EventResponse.from(eventService.update(id, request.name(), request.eventDate(), request.location()));
    }

    @Operation(summary = "Fermer un événement", description = "Plus aucun dépôt possible, l'album reste consultable.")
    @ApiResponse(responseCode = "409", description = "Événement suspendu par l'administrateur")
    @PostMapping("/{id}/close")
    public EventResponse close(@PathVariable UUID id) {
        return EventResponse.from(eventService.close(id));
    }

    /**
     * Pas de "produces = image/png" sur le mapping : en cas d'erreur (404), l'ApiExceptionHandler
     * doit pouvoir répondre en JSON. Le type PNG est fixé seulement sur la réponse réussie.
     */
    @Operation(summary = "QR code de l'événement (PNG à imprimer)",
            description = "Contient l'URL invité {front}/e/{publicCode}, jamais l'id de l'événement.")
    @ApiResponse(responseCode = "200", description = "Image PNG",
            content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE))
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> qrCode(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode.png\"")
                .body(eventService.qrCode(id));
    }

    @Operation(summary = "Supprimer un événement")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        eventService.delete(id);
    }
}
