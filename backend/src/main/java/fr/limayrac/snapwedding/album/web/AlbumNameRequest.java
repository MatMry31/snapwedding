package fr.limayrac.snapwedding.album.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO d'entrée pour créer ou renommer un album : seul le nom est modifiable.
 * L'eventId vient de l'URL (et il est vérifié), jamais du corps de la requête.
 */
public record AlbumNameRequest(
        @NotBlank @Size(max = 60) String name
) {}
