package fr.limayrac.snapwedding.album.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Un album d'un événement (« Cérémonie », « Vin d'honneur », « Soirée »...).
 * <p>
 * L'événement est référencé par son ID ({@code eventId}), pas par un {@code @ManyToOne Event} :
 * l'entité Event est interne au module event, le module album n'a pas le droit de la connaître.
 */
@Entity
@Table(name = "albums", indexes = @Index(name = "idx_albums_event", columnList = "event_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Un album ne change jamais d'événement. */
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Album(UUID eventId, String name, Instant now) {
        this.eventId = eventId;
        this.name = name;
        this.createdAt = now;
    }

    public void rename(String newName) {
        this.name = newName;
    }
}
