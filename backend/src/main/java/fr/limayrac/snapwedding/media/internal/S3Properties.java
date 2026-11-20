package fr.limayrac.snapwedding.media.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés du stockage objet, lues depuis storage.s3.* dans application.yaml.
 *
 * @param endpoint       adresse utilisée par le BACKEND pour parler à S3 / RustFS
 * @param publicEndpoint adresse utilisée par le NAVIGATEUR pour afficher les photos (URL présignées).
 *                       Identique à endpoint en local ; différente si le backend tourne dans Docker
 *                       (il voit "rustfs:9000", le navigateur voit "localhost:9000").
 */
@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
        String endpoint,
        String publicEndpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket
) {}
