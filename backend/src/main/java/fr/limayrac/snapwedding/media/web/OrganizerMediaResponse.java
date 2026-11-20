package fr.limayrac.snapwedding.media.web;

import fr.limayrac.snapwedding.media.internal.Media;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de sortie d'une photo, vue par l'ORGANISATEUR (masquées comprises).
 * Ni la clé S3 ni l'id de l'invité ne sortent : seulement un lien temporaire et le pseudo.
 */
public record OrganizerMediaResponse(
        UUID id,
        UUID albumId,
        String guestNickname,
        String contentType,
        long sizeBytes,
        boolean hidden,
        Instant createdAt,
        /* Lien présigné valable 15 minutes */
        String url
) {
    public static OrganizerMediaResponse from(Media media, String url) {
        return new OrganizerMediaResponse(media.getId(), media.getAlbumId(), media.getGuestNickname(),
                media.getContentType(), media.getSizeBytes(), media.isHidden(), media.getCreatedAt(), url);
    }
}
