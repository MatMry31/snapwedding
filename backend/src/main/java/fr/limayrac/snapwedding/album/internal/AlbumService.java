package fr.limayrac.snapwedding.album.internal;

import fr.limayrac.snapwedding.event.EventApi;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O3 : l'organisateur gère les albums de SES événements.
 * <p>
 * Dépendance entre modules : pour savoir si l'événement appartient à l'organisateur,
 * on passe par {@link EventApi} (API publique du module event), jamais par EventRepository.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final EventApi eventApi;

    @Transactional
    public Album create(UUID eventId, String name) {
        eventApi.assertOwnedByCurrentOrganizer(eventId);
        return albumRepository.save(new Album(eventId, name, Instant.now()));
    }

    public List<Album> findAll(UUID eventId) {
        eventApi.assertOwnedByCurrentOrganizer(eventId);
        return albumRepository.findAllByEventIdOrderByCreatedAtAsc(eventId);
    }

    @Transactional
    public Album rename(UUID eventId, UUID albumId, String newName) {
        Album album = findOwned(eventId, albumId);
        album.rename(newName);
        return album;
    }

    @Transactional
    public void delete(UUID eventId, UUID albumId) {
        albumRepository.delete(findOwned(eventId, albumId));
    }

    /** Double vérification : l'événement est à moi, ET l'album est bien dans cet événement. */
    private Album findOwned(UUID eventId, UUID albumId) {
        eventApi.assertOwnedByCurrentOrganizer(eventId);
        return albumRepository.findByIdAndEventId(albumId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Album " + albumId + " not found"));
    }
}
