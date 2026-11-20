package fr.limayrac.snapwedding.shared.exception;

/** 400 : la requête est mal formée ou ses données sont invalides. */
public final class InvalidRequestException extends SnapWeddingException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
