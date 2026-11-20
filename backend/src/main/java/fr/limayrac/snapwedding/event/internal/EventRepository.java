package fr.limayrac.snapwedding.event.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Toutes les méthodes utilisées par l'organisateur prennent le tenant (ownerId) en paramètre :
 * impossible de lire l'événement d'un autre organisateur, même avec son id.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByOwnerId(String ownerId, Pageable pageable);

    Optional<Event> findByIdAndOwnerId(UUID id, String ownerId);
}
