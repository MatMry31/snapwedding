package fr.limayrac.snapwedding.album.web;

import fr.limayrac.snapwedding.album.internal.Album;

import java.time.Instant;
import java.util.UUID;

/** DTO de sortie d'un album. */
public record AlbumResponse(
        UUID id,
        String name,
        Instant createdAt
) {
    public static AlbumResponse from(Album album) {
        return new AlbumResponse(album.getId(), album.getName(), album.getCreatedAt());
    }
}
