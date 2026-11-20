package fr.limayrac.snapwedding.event.internal;

import fr.limayrac.snapwedding.event.EventApi;
import fr.limayrac.snapwedding.event.EventDeleted;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import fr.limayrac.snapwedding.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cas d'usage de l'organisateur sur SES événements.
 * <p>
 * Le service reçoit des valeurs simples (pas le DTO) : c'est le controller qui décide
 * quels champs du JSON arrivent jusqu'ici. Le tenant vient toujours de {@link CurrentUser},
 * jamais du client.
 * <p>
 * Il implémente aussi {@link EventApi}, l'API publique du module : les autres modules
 * ne voient que cette interface, pas cette classe.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService implements EventApi {

    private final EventRepository eventRepository;
    private final PublicCodeGenerator publicCodeGenerator;
    private final QrCodeGenerator qrCodeGenerator;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    @Transactional
    public Event create(String name, LocalDate eventDate, String location) {
        Event event = new Event(currentUser.id(), name, eventDate, location,
                publicCodeGenerator.generate(), Instant.now());
        return eventRepository.save(event);
    }

    public Page<Event> findMine(Pageable pageable) {
        return eventRepository.findAllByOwnerId(currentUser.id(), pageable);
    }

    public Event findMine(UUID eventId) {
        return eventRepository.findByIdAndOwnerId(eventId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " not found"));
    }

    @Transactional
    public Event update(UUID eventId, String name, LocalDate eventDate, String location) {
        Event event = findMine(eventId);
        event.updateDetails(name, eventDate, location);
        return event;   // entité "managed" : Hibernate fait l'UPDATE au commit, pas besoin de save()
    }

    @Transactional
    public Event close(UUID eventId) {
        Event event = findMine(eventId);
        event.close();
        return event;
    }

    @Transactional
    public void delete(UUID eventId) {
        Event event = findMine(eventId);
        eventRepository.delete(event);
        // Les modules album, media... font leur ménage en écoutant cet événement.
        // Il est enregistré dans la même transaction que la suppression (table event_publication) :
        // s'il y a un rollback, l'événement n'est pas envoyé.
        events.publishEvent(new EventDeleted(event.getId()));
    }

    /** O4 : le QR code PNG, uniquement pour l'organisateur propriétaire. */
    public byte[] qrCode(UUID eventId) {
        return qrCodeGenerator.generatePng(findMine(eventId).getPublicCode());
    }

    // --- API publique du module (EventApi) ---

    @Override
    public void assertOwnedByCurrentOrganizer(UUID eventId) {
        findMine(eventId);   // lève ResourceNotFoundException si absent ou pas à lui
    }
}
