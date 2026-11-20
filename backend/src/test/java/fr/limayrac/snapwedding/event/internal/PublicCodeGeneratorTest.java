package fr.limayrac.snapwedding.event.internal;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicCodeGeneratorTest {

    private final PublicCodeGenerator generator = new PublicCodeGenerator();

    @Test
    void codeCarriesAtLeast128RandomBitsAndIsUrlSafe() {
        String code = generator.generate();

        assertThat(Base64.getUrlDecoder().decode(code)).hasSize(16);   // 16 octets = 128 bits
        assertThat(code).matches("[A-Za-z0-9_-]+");                    // utilisable tel quel dans une URL
    }

    @Test
    void codesAreNotRepeated() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            codes.add(generator.generate());
        }
        assertThat(codes).hasSize(10_000);
    }
}
