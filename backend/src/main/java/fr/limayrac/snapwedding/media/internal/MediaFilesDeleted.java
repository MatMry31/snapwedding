package fr.limayrac.snapwedding.media.internal;

import java.util.List;

/**
 * Événement INTERNE au module media : des lignes Media ont été supprimées,
 * leurs fichiers doivent l'être aussi sur S3, une fois la transaction validée.
 */
public record MediaFilesDeleted(List<String> objectKeys) {
}
