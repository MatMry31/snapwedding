package fr.limayrac.snapwedding.event.internal;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Génère le code public d'un événement, celui qui est mis dans le QR code : /e/{code}.
 * <p>
 * 16 octets = 128 bits aléatoires, tirés par SecureRandom (générateur cryptographique,
 * contrairement à Random qui est prévisible). Encodés en Base64 "URL safe" sans padding :
 * 22 caractères utilisables tels quels dans une URL.
 */
@Component
public class PublicCodeGenerator {

    private static final int CODE_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[CODE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
