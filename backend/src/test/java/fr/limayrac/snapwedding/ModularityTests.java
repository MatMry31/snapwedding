package fr.limayrac.snapwedding;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Vérifie l'architecture du monolithe modulaire, sans démarrer Spring.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(SnapWeddingApplication.class);

    /**
     * Échoue si un module accède aux classes internes d'un autre module,
     * ou s'il existe un cycle de dépendances entre modules.
     */
    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    /**
     * Génère la documentation des modules (diagrammes C4 / PlantUML + fiches des modules)
     * dans docs/modulith à la racine du dépôt. Ce dossier est commité.
     */
    @Test
    void writesDocumentation() {
        new Documenter(modules, Documenter.Options.defaults().withOutputFolder("../docs/modulith"))
                .writeDocumentation();
    }
}
