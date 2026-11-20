package fr.limayrac.snapwedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.modulith.Modulithic;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Point d'entrée. Chaque package de premier niveau sous {@code fr.limayrac.snapwedding}
 * est un module applicatif Spring Modulith (event, album, media, guest, admin, shared).
 */
@Modulithic(sharedModules = "shared")
@SpringBootApplication
// Les Page<...> sont sérialisées en un JSON stable { content: [...], page: {...} },
// et pas en exposant tous les détails internes de PageImpl
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class SnapWeddingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapWeddingApplication.class, args);
    }
}
