package fr.limayrac.snapwedding.event;

import java.util.UUID;

/**
 * Événement applicatif publié quand un organisateur supprime un événement.
 * <p>
 * Le module event ne sait pas qui l'écoute : les modules qui ont des données rattachées
 * à l'événement (albums, médias...) réagissent de leur côté pour faire le ménage.
 */
public record EventDeleted(UUID eventId) {
}
