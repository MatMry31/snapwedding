package fr.limayrac.snapwedding.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Donne l'identifiant de l'organisateur connecté : le claim "sub" de son JWT Google.
 * C'est le TENANT : toutes les requêtes des services filtrent dessus.
 * <p>
 * Le "sub" vient du jeton validé par Spring Security (signature Google vérifiée) :
 * il ne vient jamais du corps de la requête, donc le client ne peut pas le choisir.
 */
@Component
public class CurrentUser {

    public String id() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        // Ne doit pas arriver : les routes qui appellent cette méthode exigent un JWT (SecurityConfig).
        throw new IllegalStateException("No authenticated organizer in the security context");
    }
}
