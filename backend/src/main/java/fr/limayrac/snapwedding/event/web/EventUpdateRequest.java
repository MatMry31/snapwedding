package fr.limayrac.snapwedding.event.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO d'ENTRÉE pour modifier un événement. Séparé de {@link EventCreateRequest} :
 * les deux pourront évoluer différemment sans risquer d'ouvrir un champ par erreur.
 * Le statut ne se modifie pas ici : il a ses propres routes (/close), avec leurs règles.
 */
public record EventUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDate eventDate,
        @Size(max = 200) String location
) {}
