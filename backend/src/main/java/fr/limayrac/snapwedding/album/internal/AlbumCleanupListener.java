package fr.limayrac.snapwedding.album.internal;

import fr.limayrac.snapwedding.event.EventDeleted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Quand un événement est supprimé, ses albums le sont aussi.
 * <p>
 * Communication par ÉVÉNEMENT plutôt que par appel : le module event n'a pas à connaître
 * tous les modules qui ont des données rattachées à un événement (albums, médias, invités...).
 * Il publie EventDeleted, et chaque module fait son ménage.
 * <p>
 * {@code @ApplicationModuleListener} = après le commit de la suppression, dans un autre thread,
 * dans une nouvelle transaction. En cas d'échec, l'événement reste dans la table event_publication
 * et pourra être rejoué.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumCleanupListener {

    private final AlbumRepository albumRepository;

    @ApplicationModuleListener
    public void on(EventDeleted event) {
        int deleted = albumRepository.deleteAllByEventId(event.eventId());
        log.info("{} album(s) deleted with event {}", deleted, event.eventId());
    }
}
