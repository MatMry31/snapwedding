package fr.limayrac.snapwedding.event.internal;

import fr.limayrac.snapwedding.event.EventDeleted;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import fr.limayrac.snapwedding.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock EventRepository eventRepository;
    @Mock PublicCodeGenerator publicCodeGenerator;
    @Mock QrCodeGenerator qrCodeGenerator;
    @Mock CurrentUser currentUser;
    @Mock ApplicationEventPublisher events;

    @InjectMocks EventService eventService;

    @BeforeEach
    void connectedAsAlice() {
        when(currentUser.id()).thenReturn("alice");
    }

    @Test
    void deletingAnEventPublishesEventDeletedForTheOtherModules() {
        Event event = new Event("alice", "Mariage", LocalDate.of(2027, 6, 12), null, "code", Instant.now());
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        when(eventRepository.findByIdAndOwnerId(EVENT_ID, "alice")).thenReturn(Optional.of(event));

        eventService.delete(EVENT_ID);

        verify(eventRepository).delete(event);
        verify(events).publishEvent(new EventDeleted(EVENT_ID));
    }

    @Test
    void anOrganizerCannotDeleteTheEventOfAnotherOrganizer() {
        // Le repository filtre par ownerId : pour "alice", l'événement de Bob n'existe pas
        when(eventRepository.findByIdAndOwnerId(EVENT_ID, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(EVENT_ID)).isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }
}
