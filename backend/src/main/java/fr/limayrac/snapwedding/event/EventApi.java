package fr.limayrac.snapwedding.event;

import java.util.UUID;

/**
 * API publique du module event : ce que les AUTRES modules ont le droit de demander.
 * <p>
 * Les autres modules dépendent de cette interface, jamais de son implémentation
 * (EventService, dans event.internal, qui leur est invisible). Spring injecte l'implémentation.
 */
public interface EventApi {

    /**
     * Vérifie que l'événement existe ET appartient à l'organisateur connecté.
     *
     * @throws fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException sinon
     *         (404 : on ne révèle pas l'existence de l'événement d'un autre organisateur)
     */
    void assertOwnedByCurrentOrganizer(UUID eventId);
}
