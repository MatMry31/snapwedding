package fr.limayrac.snapwedding.media.internal;

import fr.limayrac.snapwedding.album.AlbumDeleted;
import fr.limayrac.snapwedding.event.EventDeleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Supprime les photos (lignes en base + fichiers S3) quand leur événement ou leur album disparaît.
 * <p>
 * {@code @ApplicationModuleListener} : après le commit, dans sa propre transaction. Ordre choisi :
 * on supprime les lignes, PUIS les fichiers. Si S3 échoue, l'exception annule la transaction :
 * les lignes reviennent, et l'événement reste "non traité" dans event_publication pour être rejoué.
 * On n'a donc jamais de photo en base dont le fichier aurait disparu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaCleanupListener {

    private final MediaRepository mediaRepository;
    private final MediaStorage mediaStorage;

    @ApplicationModuleListener
    public void on(EventDeleted event) {
        List<String> keys = mediaRepository.findObjectKeysByEventId(event.eventId());
        mediaRepository.deleteAllByEventId(event.eventId());
        mediaStorage.delete(keys);
        log.info("{} media deleted with event {}", keys.size(), event.eventId());
    }

    @ApplicationModuleListener
    public void on(AlbumDeleted event) {
        List<String> keys = mediaRepository.findObjectKeysByAlbumId(event.albumId());
        mediaRepository.deleteAllByAlbumId(event.albumId());
        mediaStorage.delete(keys);
        log.info("{} media deleted with album {}", keys.size(), event.albumId());
    }

    /** Suppression d'une photo par l'organisateur : la ligne est déjà supprimée, reste le fichier. */
    @ApplicationModuleListener
    public void on(MediaFilesDeleted event) {
        mediaStorage.delete(event.objectKeys());
    }
}
