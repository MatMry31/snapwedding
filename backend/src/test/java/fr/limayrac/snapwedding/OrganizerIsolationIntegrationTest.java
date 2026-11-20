package fr.limayrac.snapwedding;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
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
@SpringBootTest(properties = "GOOGLE_CLIENT_ID=test-client-id")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrganizerIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

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
    void deletingAnEventAlsoDeletesItsAlbumsThroughTheEventDeletedEvent() throws Exception {
        String eventId = createEvent("carol", "Anniversaire");
        createAlbum("carol", eventId, "Gâteau");
        createAlbum("carol", eventId, "Danse");

        mvc.perform(delete("/api/events/" + eventId).with(organizer("carol")))
                .andExpect(status().isNoContent());

        // L'écouteur du module album est asynchrone (après le commit) : on attend qu'il ait fini
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        long remaining;
        do {
            remaining = countAlbums(eventId);
            if (remaining > 0) Thread.sleep(100);
        } while (remaining > 0 && Instant.now().isBefore(deadline));

        assertThat(remaining).isZero();
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

    private long countAlbums(String eventId) {
        return jdbc.queryForObject("select count(*) from albums where event_id = ?::uuid", Long.class, eventId);
    }
}
