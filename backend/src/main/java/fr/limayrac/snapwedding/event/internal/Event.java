package fr.limayrac.snapwedding.event.internal;

import fr.limayrac.snapwedding.event.EventStatus;
import fr.limayrac.snapwedding.shared.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité JPA d'un événement. Elle ne sort JAMAIS de l'API : le controller la convertit en DTO.
 * <p>
 * Protection contre le mass assignment, côté entité :
 * <ul>
 *   <li>aucun setter : l'état ne change que par des méthodes métier ({@link #updateDetails}, {@link #close}...) ;</li>
 *   <li>{@code ownerId}, {@code publicCode} et {@code createdAt} ne sont fixés qu'à la création
 *       ({@code updatable = false} : même Hibernate ne les modifiera pas en UPDATE).</li>
 * </ul>
 */
@Entity
@Table(name = "events", indexes = @Index(name = "idx_events_owner", columnList = "owner_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // exigé par JPA, inaccessible au reste du code
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Tenant : le "sub" Google de l'organisateur. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private String ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(length = 200)
    private String location;

    /** Code mis dans le QR code (au moins 128 bits aléatoires) : jamais l'id. */
    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String publicCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Event(String ownerId, String name, LocalDate eventDate, String location, String publicCode, Instant now) {
        this.ownerId = ownerId;
        this.name = name;
        this.eventDate = eventDate;
        this.location = location;
        this.publicCode = publicCode;
        this.status = EventStatus.OPEN;   // un nouvel événement est toujours ouvert, le client ne choisit pas
        this.createdAt = now;
    }

    /** Seuls les champs que l'organisateur a le droit de modifier. */
    public void updateDetails(String name, LocalDate eventDate, String location) {
        requireNotSuspended();
        this.name = name;
        this.eventDate = eventDate;
        this.location = location;
    }

    /** O7 : plus aucun dépôt possible, mais l'album reste consultable. Fermer deux fois ne change rien. */
    public void close() {
        requireNotSuspended();
        this.status = EventStatus.CLOSED;
    }

    /** Les invités ne peuvent déposer des photos que dans un événement ouvert. */
    public boolean acceptsUploads() {
        return status == EventStatus.OPEN;
    }

    private void requireNotSuspended() {
        if (status == EventStatus.SUSPENDED) {
            throw new ConflictException("This event is suspended");
        }
    }
}
