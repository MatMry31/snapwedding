package fr.limayrac.snapwedding.media.web;

import fr.limayrac.snapwedding.media.internal.Media;
import fr.limayrac.snapwedding.media.internal.MediaService;
import fr.limayrac.snapwedding.media.internal.MediaStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Galerie et modération de l'organisateur (O5, O6) : /api/events/{eventId}/media.
 */
@RestController
@RequestMapping("/api/events/{eventId}/media")
@RequiredArgsConstructor
@Tag(name = "Media (organisateur)", description = "Voir et modérer les photos d'un de ses événements")
public class OrganizerMediaController {

    private final MediaService mediaService;
    private final MediaStorage mediaStorage;

    @Operation(summary = "Toutes les photos de l'événement (masquées comprises)",
            description = "Paginé, 50 par page par défaut, les plus récentes d'abord. Filtre facultatif par album.")
    @GetMapping
    public Page<OrganizerMediaResponse> findAll(
            @PathVariable UUID eventId,
            @Parameter(description = "Limiter à un album") @RequestParam(required = false) UUID albumId,
            @ParameterObject @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return mediaService.findForOrganizer(eventId, albumId, pageable).map(this::toResponse);
    }

    @Operation(summary = "Masquer une photo", description = "Elle n'apparaît plus aux invités.")
    @PostMapping("/{mediaId}/hide")
    public OrganizerMediaResponse hide(@PathVariable UUID eventId, @PathVariable UUID mediaId) {
        return toResponse(mediaService.hide(eventId, mediaId));
    }

    @Operation(summary = "Réafficher une photo masquée")
    @PostMapping("/{mediaId}/show")
    public OrganizerMediaResponse show(@PathVariable UUID eventId, @PathVariable UUID mediaId) {
        return toResponse(mediaService.show(eventId, mediaId));
    }

    @Operation(summary = "Supprimer définitivement une photo")
    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId, @PathVariable UUID mediaId) {
        mediaService.delete(eventId, mediaId);
    }

    private OrganizerMediaResponse toResponse(Media media) {
        return OrganizerMediaResponse.from(media, mediaStorage.temporaryUrl(media.getObjectKey()));
    }
}
