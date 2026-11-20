package fr.limayrac.snapwedding.shared.internal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation Swagger : http://localhost:8080/swagger-ui.html
 * Déclare l'authentification "Bearer JWT" : Swagger UI affiche un bouton Authorize
 * dans lequel on colle le jeton Google de l'organisateur.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "SnapWedding API", version = "1.0",
                description = "Album photo d'événement partagé par QR code"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
