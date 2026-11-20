package fr.limayrac.snapwedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * Point d'entrée. Chaque package de premier niveau sous {@code fr.limayrac.snapwedding}
 * est un module applicatif Spring Modulith (event, album, media, guest, admin, shared).
 */
@Modulithic(sharedModules = "shared")
@SpringBootApplication
public class SnapWeddingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapWeddingApplication.class, args);
    }
}
