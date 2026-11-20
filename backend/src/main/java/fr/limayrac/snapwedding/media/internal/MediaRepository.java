package fr.limayrac.snapwedding.media.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    /**
     * Galerie de l'organisateur, paginée : 1 requête pour la page + 1 pour le total.
     * Media n'a aucune relation JPA à charger : pas de N+1 possible.
     */
    Page<Media> findAllByEventId(UUID eventId, Pageable pageable);

    Page<Media> findAllByEventIdAndAlbumId(UUID eventId, UUID albumId, Pageable pageable);

    /** La photo doit appartenir à l'événement de l'URL. */
    Optional<Media> findByIdAndEventId(UUID id, UUID eventId);

    // --- Nettoyage : on ne charge que les clés S3, pas les entités entières ---

    @Query("select m.objectKey from Media m where m.eventId = :eventId")
    List<String> findObjectKeysByEventId(@Param("eventId") UUID eventId);

    @Query("select m.objectKey from Media m where m.albumId = :albumId")
    List<String> findObjectKeysByAlbumId(@Param("albumId") UUID albumId);

    @Modifying
    @Query("delete from Media m where m.eventId = :eventId")
    int deleteAllByEventId(@Param("eventId") UUID eventId);

    @Modifying
    @Query("delete from Media m where m.albumId = :albumId")
    int deleteAllByAlbumId(@Param("albumId") UUID albumId);
}
