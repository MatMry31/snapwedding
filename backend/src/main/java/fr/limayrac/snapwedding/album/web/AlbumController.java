package fr.limayrac.snapwedding.album.web;

import fr.limayrac.snapwedding.album.internal.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Routes de l'organisateur sur les albums d'un de ses événements.
 * Les albums sont rangés sous leur événement dans l'URL : /api/events/{eventId}/albums.
 */
@RestController
@RequestMapping("/api/events/{eventId}/albums")
@RequiredArgsConstructor
@Tag(name = "Albums", description = "Les albums d'un événement de l'organisateur connecté")
public class AlbumController {

    private final AlbumService albumService;

    @Operation(summary = "Créer un album")
    @ApiResponse(responseCode = "201", description = "Album créé")
    @ApiResponse(responseCode = "404", description = "Événement inexistant ou appartenant à un autre organisateur")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlbumResponse create(@PathVariable UUID eventId, @Valid @RequestBody AlbumNameRequest request) {
        return AlbumResponse.from(albumService.create(eventId, request.name()));
    }

    @Operation(summary = "Lister les albums de l'événement")
    @GetMapping
    public List<AlbumResponse> findAll(@PathVariable UUID eventId) {
        return albumService.findAll(eventId).stream().map(AlbumResponse::from).toList();
    }

    @Operation(summary = "Renommer un album")
    @PutMapping("/{albumId}")
    public AlbumResponse rename(@PathVariable UUID eventId, @PathVariable UUID albumId,
                                @Valid @RequestBody AlbumNameRequest request) {
        return AlbumResponse.from(albumService.rename(eventId, albumId, request.name()));
    }

    @Operation(summary = "Supprimer un album")
    @DeleteMapping("/{albumId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId, @PathVariable UUID albumId) {
        albumService.delete(eventId, albumId);
    }
}
