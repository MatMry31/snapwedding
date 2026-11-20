package fr.limayrac.snapwedding.shared.exception;

/**
 * 500 : erreur technique du stockage objet (RustFS / S3 injoignable, objet introuvable...).
 * Ce n'est pas la faute du client : le détail est logué, jamais renvoyé.
 */
public final class StorageException extends SnapWeddingException {
    public StorageException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
