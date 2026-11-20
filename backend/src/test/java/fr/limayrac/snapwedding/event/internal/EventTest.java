package fr.limayrac.snapwedding.event.internal;

import fr.limayrac.snapwedding.event.EventStatus;
import fr.limayrac.snapwedding.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de la logique métier de l'entité : pas de Spring, pas de base.
 */
class EventTest {

    private static Event newEvent() {
        return new Event("alice", "Mariage", LocalDate.of(2027, 6, 12), "Toulouse", "code", Instant.now());
    }

    @Test
    void aNewEventIsAlwaysOpen() {
        Event event = newEvent();

        assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(event.acceptsUploads()).isTrue();
    }

    @Test
    void aClosedEventNoLongerAcceptsUploads() {
        Event event = newEvent();

        event.close();

        assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
        assertThat(event.acceptsUploads()).isFalse();
    }

    @Test
    void updateDetailsDoesNotTouchProtectedFields() {
        Event event = newEvent();

        event.updateDetails("Mariage de Léa et Tom", LocalDate.of(2027, 7, 1), "Albi");

        assertThat(event.getName()).isEqualTo("Mariage de Léa et Tom");
        assertThat(event.getOwnerId()).isEqualTo("alice");
        assertThat(event.getPublicCode()).isEqualTo("code");
        assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
    }

    @Test
    void aSuspendedEventCanNeitherBeClosedNorModifiedByItsOrganizer() {
        Event event = newEvent();
        // La suspension sera faite par le module admin ; ici on force l'état pour tester la règle.
        ReflectionTestUtils.setField(event, "status", EventStatus.SUSPENDED);

        assertThatThrownBy(event::close).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> event.updateDetails("x", LocalDate.now(), null))
                .isInstanceOf(ConflictException.class);
    }
}
