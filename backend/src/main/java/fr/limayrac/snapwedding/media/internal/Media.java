package fr.limayrac.snapwedding.media.internal;

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
 * Une photo déposée par un invité. Le fichier est sur S3 ; la base ne garde que sa clé ({@code objectKey}).
 * <p>
 * Les autres modules sont référencés par ID (eventId, albumId, guestId), sans relation JPA.
 * Le pseudo de l'invité est COPIÉ au moment du dépôt ({@code guestNickname}) : la galerie l'affiche
 * sans jointure ni appel au module guest, donc sans requête SQL en plus par photo (pas de N+1).
 */
@Entity
@Table(name = "media", indexes = {
        // La galerie trie par date dans un événement ou un album : ces index servent ces requêtes
        @Index(name = "idx_media_event_created", columnList = "event_id, created_at"),
        @Index(name = "idx_media_album_created", columnList = "album_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "album_id", nullable = false, updatable = false)
    private UUID albumId;

    @Column(name = "guest_id", nullable = false, updatable = false)
    private UUID guestId;

    @Column(nullable = false, updatable = false, length = 40)
    private String guestNickname;

    /** Clé de l'objet dans le bucket S3. */
    @Column(nullable = false, unique = true, updatable = false)
    private String objectKey;

    /** Type réel, détecté par Tika à l'upload (pas celui annoncé par le client). */
    @Column(nullable = false, updatable = false, length = 100)
    private String contentType;

    @Column(nullable = false, updatable = false)
    private long sizeBytes;

    /** O6 : une photo masquée par l'organisateur n'apparaît plus dans la galerie des invités. */
    @Column(nullable = false)
    private boolean hidden;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Media(UUID eventId, UUID albumId, UUID guestId, String guestNickname,
                 String objectKey, String contentType, long sizeBytes, Instant now) {
        this.eventId = eventId;
        this.albumId = albumId;
        this.guestId = guestId;
        this.guestNickname = guestNickname;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.hidden = false;
        this.createdAt = now;
    }

    public void hide() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }
}
