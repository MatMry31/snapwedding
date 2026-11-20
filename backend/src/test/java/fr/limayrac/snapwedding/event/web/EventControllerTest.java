package fr.limayrac.snapwedding.event.web;

import fr.limayrac.snapwedding.event.internal.Event;
import fr.limayrac.snapwedding.event.internal.EventService;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import fr.limayrac.snapwedding.shared.internal.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de la couche web seule (controller + JSON + validation + sécurité). Le service est un mock.
 * <p>
 * GOOGLE_CLIENT_ID : exigé par application.yaml. @Import(SecurityConfig) : @WebMvcTest ne charge pas
 * les @Configuration, sans cet import on testerait la sécurité par défaut de Spring et pas la nôtre.
 */
@WebMvcTest(controllers = EventController.class, properties = "GOOGLE_CLIENT_ID=test-client-id")
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventService eventService;

    private static RequestPostProcessor alice() {
        return jwt().jwt(token -> token.subject("alice"));
    }

    private static Event savedEvent() {
        Event event = new Event("alice", "Mariage", LocalDate.of(2027, 6, 12), "Toulouse",
                "Xk9fQz", Instant.parse("2026-11-20T10:00:00Z"));
        ReflectionTestUtils.setField(event, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
        return event;
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(eventService);
    }

    @Test
    void createReturns201AndNeverExposesTheOwner() throws Exception {
        when(eventService.create("Mariage", LocalDate.of(2027, 6, 12), "Toulouse")).thenReturn(savedEvent());

        mvc.perform(post("/api/events").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariage", "eventDate": "2027-06-12", "location": "Toulouse"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mariage"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.publicCode").value("Xk9fQz"))
                .andExpect(jsonPath("$.ownerId").doesNotExist());   // donnée interne : ne doit pas fuiter
    }

    // --- Mass assignment : un champ qui n'est pas dans le DTO est refusé, le service n'est jamais appelé ---

    @Test
    void createRejectsAnAttemptToChooseTheStatus() throws Exception {
        mvc.perform(post("/api/events").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariage", "eventDate": "2027-06-12", "status": "SUSPENDED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown field: status"));

        verifyNoInteractions(eventService);
    }

    @Test
    void createRejectsAnAttemptToChooseTheOwnerOrThePublicCode() throws Exception {
        mvc.perform(post("/api/events").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariage", "eventDate": "2027-06-12", "ownerId": "bob"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown field: ownerId"));

        mvc.perform(post("/api/events").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariage", "eventDate": "2027-06-12", "publicCode": "1234"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown field: publicCode"));

        verifyNoInteractions(eventService);
    }

    @Test
    void updateRejectsAnAttemptToChangeTheIdOrTheCreationDate() throws Exception {
        mvc.perform(put("/api/events/11111111-1111-1111-1111-111111111111").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariage", "eventDate": "2027-06-12", "id": "22222222-2222-2222-2222-222222222222"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown field: id"));

        mvc.perform(put("/api/events/11111111-1111-1111-1111-111111111111").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mariage", "eventDate": "2027-06-12", "createdAt": "2020-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown field: createdAt"));

        verifyNoInteractions(eventService);
    }

    // --- Validation ---

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        mvc.perform(post("/api/events").with(alice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.eventDate").exists());

        verifyNoInteractions(eventService);
    }

    @Test
    void qrCodeIsServedAsPng() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        when(eventService.qrCode(any(UUID.class))).thenReturn(png);

        mvc.perform(get("/api/events/11111111-1111-1111-1111-111111111111/qrcode").with(alice()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
    }

    @Test
    void qrCodeOfAnotherOrganizersEventReturns404AsJson() throws Exception {
        when(eventService.qrCode(any(UUID.class))).thenThrow(new ResourceNotFoundException("Event not found"));

        mvc.perform(get("/api/events/11111111-1111-1111-1111-111111111111/qrcode").with(alice()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }

    @Test
    void unknownEventReturns404() throws Exception {
        when(eventService.findMine(any(UUID.class))).thenThrow(new ResourceNotFoundException("Event not found"));

        mvc.perform(get("/api/events/99999999-9999-9999-9999-999999999999").with(alice()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }
}
