package py.com.logixone.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class PhysicalPluginSetBuildContractTest {

    private static final Map<String, Set<String>> DEMO_PROFILES = Map.of(
            "with-business-partners-demo", Set.of(
                    "business-partners",
                    "reference-data",
                    "reference-plugin",
                    "reference-customization-a",
                    "reference-customization-b"),
            "with-commercial-catalog-demo", Set.of(
                    "business-partners",
                    "commercial-catalog",
                    "reference-data",
                    "reference-plugin",
                    "reference-customization-a",
                    "reference-customization-b"),
            "with-inventory-demo", Set.of(
                    "business-partners",
                    "commercial-catalog",
                    "inventory",
                    "reference-data",
                    "reference-plugin",
                    "reference-customization-a",
                    "reference-customization-b"));

    private final Path projectRoot = locateProjectRoot();

    @Test
    void declaresProductiveDemoCompositionsOnlyInTheSharedPluginSet() throws Exception {
        Element pluginSet = parsePom("distribution/logixone-plugin-set/pom.xml");
        for (Map.Entry<String, Set<String>> expected : DEMO_PROFILES.entrySet()) {
            Element profile = findProfile(pluginSet, expected.getKey());
            assertEquals(expected.getValue(), dependencyArtifactIds(profile), expected.getKey());
        }

        assertUsesOnlySharedPluginSet("distribution/logixone-war/pom.xml");
        assertUsesOnlySharedPluginSet("migrator/pom.xml");
    }

    @Test
    void bothDockerBuildsAcceptTheSameVerifiedAndVisualComposition() throws IOException {
        for (String dockerfile : Set.of("infra/docker/Dockerfile", "infra/docker/Dockerfile.migrator")) {
            String content = Files.readString(projectRoot.resolve(dockerfile));

            for (String profile : DEMO_PROFILES.keySet()) {
                assertTrue(content.contains("verified:" + profile + ")"), dockerfile + " " + profile);
                assertTrue(content.contains("visual-candidate:" + profile + ")"), dockerfile + " " + profile);
                assertTrue(content.contains("-P" + profile), dockerfile + " " + profile);
            }
        }
    }

    private void assertUsesOnlySharedPluginSet(String relativePom) throws Exception {
        Element project = parsePom(relativePom);
        Set<String> dependencies = dependencyArtifactIds(project);

        assertTrue(dependencies.contains("logixone-plugin-set"), relativePom);
        Set<String> physicalArtifacts = DEMO_PROFILES.values().stream()
                .flatMap(Set::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertFalse(dependencies.stream().anyMatch(physicalArtifacts::contains), relativePom);
    }

    private Element parsePom(String relativePath) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(projectRoot.resolve(relativePath).toFile()).getDocumentElement();
    }

    private static Element findProfile(Element project, String profileId) {
        NodeList profiles = project.getElementsByTagName("profile");
        for (int index = 0; index < profiles.getLength(); index++) {
            Element profile = (Element) profiles.item(index);
            if (profileId.equals(directChildText(profile, "id"))) {
                return profile;
            }
        }
        throw new AssertionError("Missing Maven profile " + profileId);
    }

    private static Set<String> dependencyArtifactIds(Element parent) {
        Set<String> result = new LinkedHashSet<>();
        NodeList dependencies = parent.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            result.add(directChildText((Element) dependencies.item(index), "artifactId"));
        }
        return result;
    }

    private static String directChildText(Element element, String name) {
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement && name.equals(childElement.getTagName())) {
                return childElement.getTextContent().trim();
            }
        }
        throw new AssertionError("Missing " + name + " under " + element.getTagName());
    }

    private static Path locateProjectRoot() {
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("distribution/logixone-plugin-set/pom.xml"))
                    && Files.isRegularFile(candidate.resolve("infra/docker/Dockerfile"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("Unable to locate the Logixone reactor root");
    }
}
