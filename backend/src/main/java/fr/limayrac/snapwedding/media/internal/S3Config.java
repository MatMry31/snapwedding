package fr.limayrac.snapwedding.media.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Client S3 générique (AWS S3, RustFS, Garage...). Changer de fournisseur = changer la config, pas le code.
 * Seul le module media parle à S3 : la config vit donc dans ce module.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))      // sans ça, le SDK appelle AWS
                .region(Region.of(props.region()))                   // exigé par la signature, ignoré par RustFS
                .credentialsProvider(credentials(props))
                .forcePathStyle(true)                                // http://host:9000/<bucket>/<clé>
                // Le SDK ajoute des checksums CRC que tous les serveurs S3 ne gèrent pas
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    /**
     * Fabrique des URL présignées : des liens temporaires, signés avec la clé secrète, qui permettent
     * au navigateur de lire UNE photo pendant quelques minutes, sans rendre le bucket public.
     * La signature est un simple calcul local : aucun appel réseau.
     */
    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(props.publicEndpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /** Crée le bucket au démarrage s'il n'existe pas. */
    @Bean
    public ApplicationRunner createBucketIfMissing(S3Client s3Client, S3Properties props) {
        return args -> {
            try {
                s3Client.headBucket(b -> b.bucket(props.bucket()));
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(b -> b.bucket(props.bucket()));
                log.info("Bucket '{}' created", props.bucket());
            }
        };
    }

    private static StaticCredentialsProvider credentials(S3Properties props) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
    }
}
