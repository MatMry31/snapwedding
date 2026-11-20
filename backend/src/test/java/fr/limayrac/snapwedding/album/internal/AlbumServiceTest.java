package fr.limayrac.snapwedding.album.internal;

import fr.limayrac.snapwedding.event.EventApi;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le module event est remplacé par un mock de son API publique : on teste le module album seul.
 */
@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    private static final UUID MY_EVENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_EVENT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ALBUM_OF_OTHER_EVENT = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock AlbumRepository albumRepository;
    @Mock EventApi eventApi;

    @InjectMocks AlbumService albumService;

    @Test
    void cannotCreateAnAlbumInTheEventOfAnotherOrganizer() {
        doThrow(new ResourceNotFoundException("Event not found"))
                .when(eventApi).assertOwnedByCurrentOrganizer(OTHER_EVENT);

        assertThatThrownBy(() -> albumService.create(OTHER_EVENT, "Soirée"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(albumRepository, never()).save(any());
    }

    @Test
    void cannotReachTheAlbumOfAnotherEventThroughMyEventUrl() {
        // /api/events/{MON événement}/albums/{album d'un AUTRE événement} : l'album n'est pas trouvé
        when(albumRepository.findByIdAndEventId(ALBUM_OF_OTHER_EVENT, MY_EVENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> albumService.delete(MY_EVENT, ALBUM_OF_OTHER_EVENT))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventApi).assertOwnedByCurrentOrganizer(MY_EVENT);
        verify(albumRepository, never()).delete(any());
    }
}
