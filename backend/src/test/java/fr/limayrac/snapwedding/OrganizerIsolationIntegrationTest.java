package fr.limayrac.snapwedding;

import com.jayway.jsonpath.JsonPath;
import fr.limayrac.snapwedding.media.internal.Media;
import fr.limayrac.snapwedding.media.internal.MediaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'INTÉGRATION : toute l'application (sécurité, services, JPA, événements Modulith)
 * contre un vrai PostgreSQL lancé dans Docker par Testcontainers.
 * <p>
 * Il prouve la promesse du multi-tenant (O8) : un organisateur ne voit et ne modifie jamais
 * les événements ni les albums d'un autre.
 * <p>
 * disabledWithoutDocker : sans Docker, le test est ignoré au lieu d'échouer.
 */
@SpringBootTest(properties = {
        "GOOGLE_CLIENT_ID=test-client-id",
        // Compte les requêtes SQL : sert à prouver l'absence de N+1 sur la galerie
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)   // sans Docker, le test est ignoré au lieu d'échouer
class OrganizerIsolationIntegrationTest {

    /** Pas de RustFS dans ce test : il porte sur l'isolation et le SQL, pas sur le stockage. */
    @MockitoBean
    private S3Client s3Client;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private static RequestPostProcessor organizer(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    @Test
    void anOrganizerCanNeitherSeeNorTouchTheEventsAndAlbumsOfAnother() throws Exception {
        String eventId = createEvent("alice", "Mariage d'Alice");
        String albumId = createAlbum("alice", eventId, "Soirée");

        // Bob ne voit pas l'événement dans sa liste...
        mvc.perform(get("/api/events").with(organizer("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        // ... ni en accès direct, ni son QR code : 404, comme s'il n'existait pas
        mvc.perform(get("/api/events/" + eventId).with(organizer("bob"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/events/" + eventId + "/qrcode").with(organizer("bob"))).andExpect(status().isNotFound());

        // ... ne peut ni le modifier, ni le fermer, ni le supprimer
        mvc.perform(put("/api/events/" + eventId).with(organizer("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Piraté", "eventDate": "2027-06-12"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/events/" + eventId + "/close").with(organizer("bob"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/events/" + eventId).with(organizer("bob"))).andExpect(status().isNotFound());

        // ... ne voit pas ses albums, ne peut ni en ajouter, ni en supprimer
        mvc.perform(get("/api/events/" + eventId + "/albums").with(organizer("bob"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/events/" + eventId + "/albums").with(organizer("bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Intrusion"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/events/" + eventId + "/albums/" + albumId).with(organizer("bob")))
                .andExpect(status().isNotFound());

        // Rien n'a bougé pour Alice
        mvc.perform(get("/api/events/" + eventId).with(organizer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mariage d'Alice"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/api/events/" + eventId + "/albums").with(organizer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void anOrganizerCanNeitherSeeNorModerateThePhotosOfAnotherOrganizer() throws Exception {
        String eventId = createEvent("alice", "Mariage d'Alice");
        String albumId = createAlbum("alice", eventId, "Cérémonie");
        Media photo = mediaRepository.save(aPhoto(eventId, albumId, 0));

        mvc.perform(get("/api/events/" + eventId + "/media").with(organizer("bob")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/events/" + eventId + "/media/" + photo.getId() + "/hide").with(organizer("bob")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/events/" + eventId + "/media/" + photo.getId()).with(organizer("bob")))
                .andExpect(status().isNotFound());

        // Alice voit sa photo, avec le pseudo de l'invité et un lien temporaire, mais sans la clé S3
        mvc.perform(get("/api/events/" + eventId + "/media").with(organizer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].guestNickname").value("Invité 0"))
                .andExpect(jsonPath("$.content[0].hidden").value(false))
                .andExpect(jsonPath("$.content[0].url").isNotEmpty())
                .andExpect(jsonPath("$.content[0].objectKey").doesNotExist());

        // Alice peut la masquer
        mvc.perform(post("/api/events/" + eventId + "/media/" + photo.getId() + "/hide").with(organizer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(true));
    }

    @Test
    void theGalleryOf50PhotosCosts3SqlQueriesNotOnePerPhoto() throws Exception {
        String eventId = createEvent("dave", "Séminaire");
        String albumId = createAlbum("dave", eventId, "Plénière");
        List<Media> photos = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            photos.add(aPhoto(eventId, albumId, i));
        }
        mediaRepository.saveAll(photos);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        mvc.perform(get("/api/events/" + eventId + "/media?size=50").with(organizer("dave")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(50))
                .andExpect(jsonPath("$.page.totalElements").value(60));

        // 1 : l'événement appartient-il à dave ?  2 : la page de 50 photos.  3 : le total (pagination).
        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void deletingAnEventAlsoDeletesItsAlbumsAndPhotosThroughTheEventDeletedEvent() throws Exception {
        String eventId = createEvent("carol", "Anniversaire");
        String albumId = createAlbum("carol", eventId, "Gâteau");
        createAlbum("carol", eventId, "Danse");
        mediaRepository.save(aPhoto(eventId, albumId, 1));
        mediaRepository.save(aPhoto(eventId, albumId, 2));

        mvc.perform(delete("/api/events/" + eventId).with(organizer("carol")))
                .andExpect(status().isNoContent());

        // Les écouteurs des modules album et media sont asynchrones (après le commit) : on attend
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        long remaining;
        do {
            remaining = count("albums", eventId) + count("media", eventId);
            if (remaining > 0) Thread.sleep(100);
        } while (remaining > 0 && Instant.now().isBefore(deadline));

        assertThat(remaining).isZero();
        verify(s3Client).deleteObjects(any(Consumer.class));   // les fichiers ont été supprimés de S3
    }

    @Test
    void sortingOnAnUnknownPropertyIsA400NotA500() throws Exception {
        mvc.perform(get("/api/events?sort=doesNotExist").with(organizer("alice")))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

    private String createEvent(String owner, String name) throws Exception {
        String json = mvc.perform(post("/api/events").with(organizer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "eventDate": "2027-06-12"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String createAlbum(String owner, String eventId, String name) throws Exception {
        String json = mvc.perform(post("/api/events/" + eventId + "/albums").with(organizer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private long count(String table, String eventId) {
        return jdbc.queryForObject("select count(*) from " + table + " where event_id = ?::uuid", Long.class, eventId);
    }

    private static Media aPhoto(String eventId, String albumId, int i) {
        return new Media(UUID.fromString(eventId), UUID.fromString(albumId), UUID.randomUUID(), "Invité " + i,
                "events/" + eventId + "/" + UUID.randomUUID() + ".jpg", "image/jpeg", 2048,
                Instant.now().minusSeconds(i));
    }
}
