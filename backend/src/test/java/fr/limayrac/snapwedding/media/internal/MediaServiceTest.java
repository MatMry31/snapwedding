package fr.limayrac.snapwedding.media.internal;

import fr.limayrac.snapwedding.event.EventApi;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    private static final UUID EVENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_EVENT = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MEDIA = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock MediaRepository mediaRepository;
    @Mock EventApi eventApi;
    @Mock ApplicationEventPublisher events;

    @InjectMocks MediaService mediaService;

    private static Media aPhoto() {
        return new Media(EVENT, UUID.randomUUID(), UUID.randomUUID(), "Tonton Jean",
                "photos/abc.jpg", "image/jpeg", 1024, Instant.now());
    }

    @Test
    void hidingAPhotoKeepsItButMarksItHidden() {
        Media photo = aPhoto();
        when(mediaRepository.findByIdAndEventId(MEDIA, EVENT)).thenReturn(Optional.of(photo));

        mediaService.hide(EVENT, MEDIA);
        assertThat(photo.isHidden()).isTrue();

        mediaService.show(EVENT, MEDIA);
        assertThat(photo.isHidden()).isFalse();
    }

    @Test
    void anOrganizerCannotModerateThePhotosOfAnotherOrganizersEvent() {
        doThrow(new ResourceNotFoundException("Event not found"))
                .when(eventApi).assertOwnedByCurrentOrganizer(OTHER_EVENT);

        assertThatThrownBy(() -> mediaService.hide(OTHER_EVENT, MEDIA)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> mediaService.delete(OTHER_EVENT, MEDIA)).isInstanceOf(ResourceNotFoundException.class);

        verify(mediaRepository, never()).findByIdAndEventId(any(), any());
        verify(mediaRepository, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void deletingAPhotoRemovesTheRowAndAsksForTheFileDeletionAfterCommit() {
        Media photo = aPhoto();
        when(mediaRepository.findByIdAndEventId(MEDIA, EVENT)).thenReturn(Optional.of(photo));

        mediaService.delete(EVENT, MEDIA);

        verify(mediaRepository).delete(photo);
        // Le fichier S3 n'est PAS supprimé ici : un événement est publié, traité après le commit
        verify(events).publishEvent(new MediaFilesDeleted(List.of("photos/abc.jpg")));
    }
}
