package com.musicdreamer.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class DatabaseArtifactStructureTest {

    private static final Set<String> OWNERS = Set.of("identity", "catalog", "interaction", "media");
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("musicDreamer.projectRoot"))
            .toAbsolutePath()
            .normalize();

    @Test
    void reactorBuildsFourIndependentlyOwnedMigrationArtifacts() throws Exception {
        Element backendProject = readProject(PROJECT_ROOT.resolve("backend/pom.xml"));

        assertThat(directChildren(directChild(backendProject, "modules"), "module"))
                .contains("database-baseline-tests")
                .containsExactlyInAnyOrderElementsOf(Stream.concat(
                                Stream.of(
                                        "platform-contracts",
                                        "platform-security",
                                        "gateway-service",
                                        "identity-service",
                                        "catalog-service",
                                        "interaction-service",
                                        "media-service",
                                        "database-baseline-tests"),
                                OWNERS.stream().map(owner -> "../database/" + owner))
                        .toList());

        for (String owner : OWNERS) {
            Path moduleRoot = PROJECT_ROOT.resolve("database").resolve(owner);
            Element moduleProject = readProject(moduleRoot.resolve("pom.xml"));

            assertThat(directChildText(moduleProject, "artifactId"))
                    .isEqualTo("database-" + owner + "-migrations");
            assertThat(directChildText(moduleProject, "packaging")).isEqualTo("jar");
            assertThat(parentRelativePath(moduleProject)).isEqualTo("../../backend/pom.xml");

            Path migrationRoot = moduleRoot.resolve("src/main/resources/db/migration").resolve(owner);
            assertThat(sqlFileNames(migrationRoot)).containsExactly("V1__baseline.sql");
        }
    }

    private static Element readProject(Path pom) throws Exception {
        assertThat(pom).as("Maven module descriptor").isRegularFile();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
    }

    private static String parentRelativePath(Element project) {
        return directChildText(directChild(project, "parent"), "relativePath");
    }

    private static Element directChild(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && element.getTagName().equals(name)) {
                return element;
            }
        }
        throw new AssertionError("Missing <" + name + "> under <" + parent.getTagName() + ">");
    }

    private static String directChildText(Element parent, String name) {
        return directChild(parent, name).getTextContent().trim();
    }

    private static List<String> directChildren(Element parent, String name) {
        return Stream.iterate(parent.getFirstChild(), node -> node != null, Node::getNextSibling)
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(element -> element.getTagName().equals(name))
                .map(element -> element.getTextContent().trim())
                .toList();
    }

    private static List<String> sqlFileNames(Path migrationRoot) throws IOException {
        assertThat(migrationRoot).as("owned Flyway migration directory").isDirectory();
        try (Stream<Path> paths = Files.list(migrationRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }
}
