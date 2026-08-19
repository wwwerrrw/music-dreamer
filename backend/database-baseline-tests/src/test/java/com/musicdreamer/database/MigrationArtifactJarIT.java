package com.musicdreamer.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class MigrationArtifactJarIT {

    private static final Set<String> OWNERS = Set.of("identity", "catalog", "interaction", "media");
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("musicDreamer.projectRoot"))
            .toAbsolutePath()
            .normalize();
    private static final String PROJECT_VERSION = System.getProperty("musicDreamer.projectVersion");

    @Test
    void packagedArtifactsContainOnlyTheirOwnedMigration() throws Exception {
        for (String owner : OWNERS) {
            Path artifact = PROJECT_ROOT.resolve("database")
                    .resolve(owner)
                    .resolve("target")
                    .resolve("database-" + owner + "-migrations-" + PROJECT_VERSION + ".jar");

            assertThat(artifact).as(owner + " packaged migration artifact").isRegularFile();
            try (JarFile jar = new JarFile(artifact.toFile())) {
                List<String> sqlEntries = jar.stream()
                        .map(entry -> entry.getName())
                        .filter(name -> name.endsWith(".sql"))
                        .sorted()
                        .toList();

                assertThat(sqlEntries)
                        .as(owner + " packaged Flyway resources")
                        .containsExactly("db/migration/" + owner + "/V1__baseline.sql");
            }
        }
    }
}
