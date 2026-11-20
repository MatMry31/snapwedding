package fr.limayrac.snapwedding.event.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO d'ENTRÉE pour créer un événement : la liste blanche des champs que le client peut envoyer.
 * <p>
 * Pas d'id, pas d'ownerId, pas de status, pas de publicCode : ces champs sont fixés par le serveur.
 * Si le client les envoie quand même, Jackson refuse la requête (400 "Unknown field"),
 * car spring.jackson.deserialization.fail-on-unknown-properties = true.
 */
public record EventCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDate eventDate,
        @Size(max = 200) String location
) {}
