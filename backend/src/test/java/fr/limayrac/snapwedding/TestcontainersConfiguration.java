package fr.limayrac.snapwedding;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Un vrai PostgreSQL dans Docker pour les tests d'intégration.
 * <p>
 * Le conteneur est un BEAN Spring (et non un champ @Container géré par JUnit) : il démarre avec
 * le contexte Spring et s'arrête APRÈS lui. Sinon JUnit l'arrêterait à la fin de la classe de test
 * alors que le contexte (mis en cache) vit jusqu'à la fin de la JVM : à l'arrêt, Spring Modulith
 * interrogerait une base disparue et bloquerait 30 secondes.
 * <p>
 * {@code @ServiceConnection} : Spring Boot lit l'URL, l'utilisateur et le mot de passe du conteneur.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18");
    }
}
