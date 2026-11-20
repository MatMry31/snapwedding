package fr.limayrac.snapwedding.shared.exception;

/** 409 : l'action est impossible dans l'état actuel de la ressource (ex : modifier un événement suspendu). */
public final class ConflictException extends SnapWeddingException {
    public ConflictException(String message) {
        super(message);
    }
}
