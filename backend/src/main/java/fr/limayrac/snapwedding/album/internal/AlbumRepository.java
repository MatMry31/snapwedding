package fr.limayrac.snapwedding.album.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository extends JpaRepository<Album, UUID> {

    /** Peu d'albums par événement (3 à 10) : pas besoin de pagination ici, contrairement aux médias. */
    List<Album> findAllByEventIdOrderByCreatedAtAsc(UUID eventId);

    /** L'album doit appartenir à l'événement de l'URL : sinon /events/A/albums/{album de B} passerait. */
    Optional<Album> findByIdAndEventId(UUID id, UUID eventId);

    /** Une seule requête DELETE (et non un SELECT puis un DELETE par album). */
    @Modifying
    @Query("delete from Album a where a.eventId = :eventId")
    int deleteAllByEventId(@Param("eventId") UUID eventId);
}
