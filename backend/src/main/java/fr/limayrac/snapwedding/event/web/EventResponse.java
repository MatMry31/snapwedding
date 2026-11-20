package fr.limayrac.snapwedding.event.web;

import fr.limayrac.snapwedding.event.EventStatus;
import fr.limayrac.snapwedding.event.internal.Event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de SORTIE : ce que l'API renvoie. L'ownerId (le "sub" Google) n'en fait pas partie :
 * c'est une donnée interne qui n'a rien à faire dans le navigateur.
 */
public record EventResponse(
        UUID id,
        String name,
        LocalDate eventDate,
        String location,
        String publicCode,
        EventStatus status,
        Instant createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getPublicCode(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }
}
