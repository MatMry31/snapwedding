package fr.limayrac.snapwedding.media.internal;

import fr.limayrac.snapwedding.event.EventApi;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage de l'ORGANISATEUR sur les photos de ses événements : O5 (tout voir) et O6 (modérer).
 * Le dépôt par les invités viendra avec le module guest.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private final MediaRepository mediaRepository;
    private final EventApi eventApi;
    private final ApplicationEventPublisher events;

    /** O5 : toutes les photos, masquées comprises, avec le pseudo de l'invité. albumId facultatif. */
    public Page<Media> findForOrganizer(UUID eventId, UUID albumId, Pageable pageable) {
        eventApi.assertOwnedByCurrentOrganizer(eventId);
        return albumId == null
                ? mediaRepository.findAllByEventId(eventId, pageable)
                : mediaRepository.findAllByEventIdAndAlbumId(eventId, albumId, pageable);
    }

    /** O6 : la photo n'apparaît plus aux invités, mais reste visible (et réaffichable) par l'organisateur. */
    @Transactional
    public Media hide(UUID eventId, UUID mediaId) {
        Media media = findOwned(eventId, mediaId);
        media.hide();
        return media;
    }

    @Transactional
    public Media show(UUID eventId, UUID mediaId) {
        Media media = findOwned(eventId, mediaId);
        media.show();
        return media;
    }

    /**
     * O6 : suppression définitive. D'abord la base, puis le fichier S3 APRÈS le commit
     * (via MediaFilesDeleted) : si la transaction échoue, le fichier existe toujours.
     * Le pire cas possible est un fichier orphelin, jamais une photo en base sans fichier.
     */
    @Transactional
    public void delete(UUID eventId, UUID mediaId) {
        Media media = findOwned(eventId, mediaId);
        mediaRepository.delete(media);
        events.publishEvent(new MediaFilesDeleted(List.of(media.getObjectKey())));
    }

    private Media findOwned(UUID eventId, UUID mediaId) {
        eventApi.assertOwnedByCurrentOrganizer(eventId);
        return mediaRepository.findByIdAndEventId(mediaId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Media " + mediaId + " not found"));
    }
}
