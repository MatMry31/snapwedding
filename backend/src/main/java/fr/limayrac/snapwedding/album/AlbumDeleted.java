package fr.limayrac.snapwedding.album;

import java.util.UUID;

/**
 * Événement applicatif publié quand un organisateur supprime un album.
 * Le module media l'écoute pour supprimer les photos de l'album (en base et sur S3).
 */
public record AlbumDeleted(UUID eventId, UUID albumId) {
}
