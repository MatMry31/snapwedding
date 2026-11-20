package fr.limayrac.snapwedding.shared.exception;

/**
 * Racine des erreurs prévues de l'application. "sealed" : la liste des sous-classes est fermée,
 * donc le switch de l'ApiExceptionHandler est vérifié par le compilateur (aucun cas oublié).
 */
public sealed abstract class SnapWeddingException extends RuntimeException
        permits ResourceNotFoundException, InvalidRequestException, ConflictException, StorageException {

    protected SnapWeddingException(String message) {
        super(message);
    }
}
