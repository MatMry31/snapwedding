package fr.limayrac.snapwedding.event;

/**
 * État d'un événement. Public (package racine du module) : les autres modules
 * pourront le connaître, par exemple pour refuser un dépôt de photo.
 */
public enum EventStatus {
    /** Les invités peuvent déposer des photos. */
    OPEN,
    /** Fermé par l'organisateur : plus de dépôt, l'album reste consultable. */
    CLOSED,
    /** Suspendu par le super-admin (abus, contenu signalé) : plus aucun accès invité. */
    SUSPENDED
}
