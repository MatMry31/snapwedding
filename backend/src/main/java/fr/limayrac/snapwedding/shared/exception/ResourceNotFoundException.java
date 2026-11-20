package fr.limayrac.snapwedding.shared.exception;

/** 404 : la ressource n'existe pas, OU elle appartient à un autre tenant (on ne révèle pas son existence). */
public final class ResourceNotFoundException extends SnapWeddingException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
