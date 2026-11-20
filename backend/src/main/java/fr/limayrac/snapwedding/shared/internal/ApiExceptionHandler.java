package fr.limayrac.snapwedding.shared.internal;

import fr.limayrac.snapwedding.shared.exception.ConflictException;
import fr.limayrac.snapwedding.shared.exception.InvalidRequestException;
import fr.limayrac.snapwedding.shared.exception.ResourceNotFoundException;
import fr.limayrac.snapwedding.shared.exception.SnapWeddingException;
import fr.limayrac.snapwedding.shared.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforme toutes les erreurs en réponses JSON au même format : { "error": "..." }.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Erreurs métier prévues : pattern matching sur la hiérarchie sealed. */
    @ExceptionHandler(SnapWeddingException.class)
    public ResponseEntity<Map<String, String>> handleApplicationError(SnapWeddingException ex) {
        return switch (ex) {
            case ResourceNotFoundException e -> error(HttpStatus.NOT_FOUND, "Resource not found");
            case InvalidRequestException e -> error(HttpStatus.BAD_REQUEST, e.getMessage());
            case ConflictException e -> error(HttpStatus.CONFLICT, e.getMessage());
            case StorageException e -> {
                log.error("Object storage error", e);
                yield error(HttpStatus.INTERNAL_SERVER_ERROR, "File storage error");
            }
        };
    }

    /**
     * 400 - Tri sur une propriété inconnue (ex : ?sort=nimportequoi). Sans ce handler,
     * Spring Data lèverait une exception qui finirait en 500.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSort(PropertyReferenceException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid sort property: " + ex.getPropertyName());
    }

    /**
     * 400 - JSON illisible. Cas particulier : un champ qui n'existe pas dans le DTO
     * (ex : "status" ou "ownerId" envoyé pour tenter un mass assignment) est refusé explicitement.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        if (ex.getCause() instanceof UnrecognizedPropertyException unknown) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unknown field: " + unknown.getPropertyName()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Malformed JSON request"));
    }

    /** 400 - Erreurs de validation des DTO annotés @Valid. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "details", errors
        ));
    }

    /** Autres erreurs standard de Spring MVC (404, 405, 413, 415...) : même code HTTP, notre format JSON. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response == null) {
            return null; // réponse déjà envoyée au client
        }
        String message = response.getBody() instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : "Request failed";

        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(Map.of("error", message));
    }

    /** 500 - Erreur inattendue : on logue le détail, mais on ne l'envoie jamais au client. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(Exception ex) {
        log.error("Unexpected server error", ex);
        return Map.of("error", "Internal server error");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
