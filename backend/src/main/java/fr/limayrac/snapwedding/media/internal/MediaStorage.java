package fr.limayrac.snapwedding.media.internal;

import fr.limayrac.snapwedding.shared.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * Accès au stockage objet pour les photos. Les fichiers ne sont JAMAIS en base ni sur le disque
 * du serveur : la base ne garde que la clé de l'objet dans le bucket.
 */
@Component
@RequiredArgsConstructor
public class MediaStorage {

    /** Durée de validité d'un lien de photo : assez pour afficher la galerie, trop court pour être partagé. */
    static final Duration URL_VALIDITY = Duration.ofMinutes(15);

    /** Limite de l'API S3 DeleteObjects. */
    private static final int MAX_KEYS_PER_DELETE = 1000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties props;

    public void put(String objectKey, String contentType, InputStream content, long size) {
        try {
            s3Client.putObject(b -> b.bucket(props.bucket()).key(objectKey).contentType(contentType),
                    RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            throw new StorageException("Upload failed for object " + objectKey, e);
        }
    }

    /** Lien temporaire de lecture : le navigateur charge l'image directement depuis RustFS. */
    public String temporaryUrl(String objectKey) {
        return s3Presigner.presignGetObject(p -> p
                        .signatureDuration(URL_VALIDITY)
                        .getObjectRequest(g -> g.bucket(props.bucket()).key(objectKey)))
                .url().toString();
    }

    /**
     * Supprime des objets par paquets de 1000 (une requête S3 par paquet, pas une par photo).
     * Lève une StorageException en cas d'échec : l'appelant (un listener Modulith) sera rejoué.
     */
    public void delete(List<String> objectKeys) {
        for (int from = 0; from < objectKeys.size(); from += MAX_KEYS_PER_DELETE) {
            List<ObjectIdentifier> batch = objectKeys
                    .subList(from, Math.min(from + MAX_KEYS_PER_DELETE, objectKeys.size()))
                    .stream().map(key -> ObjectIdentifier.builder().key(key).build()).toList();
            try {
                DeleteObjectsResponse response = s3Client.deleteObjects(b -> b
                        .bucket(props.bucket())
                        .delete(Delete.builder().objects(batch).quiet(true).build()));
                if (response != null && response.hasErrors() && !response.errors().isEmpty()) {
                    throw new StorageException("Deletion failed for " + response.errors().size() + " object(s)", null);
                }
            } catch (SdkException e) {
                throw new StorageException("Deletion failed", e);
            }
        }
    }
}
