package py.com.logixone.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RootEntryPointArchitectureTest {

    private static final Path REPOSITORY_ROOT = locateProjectRoot();
    private static final Path WEBAPP = REPOSITORY_ROOT.resolve(
            "distribution/logixone-war/src/main/webapp");
    private static final Path ROOT_SERVLET = REPOSITORY_ROOT.resolve(
            "web-shell/src/main/java/py/com/logixone/web/shell/RootEntryServlet.java");

    @Test
    void contextRootUsesAPhysicalWelcomePageBeforeTheProtectedFacesRoute() throws IOException {
        String descriptor = Files.readString(
                WEBAPP.resolve("WEB-INF/web.xml"), StandardCharsets.UTF_8);
        String entryPoint = Files.readString(WEBAPP.resolve("index.xhtml"), StandardCharsets.UTF_8);
        String rootServlet = Files.readString(ROOT_SERVLET, StandardCharsets.UTF_8);

        int physicalWelcome = descriptor.indexOf("<welcome-file>index.xhtml</welcome-file>");
        int facesFallback = descriptor.indexOf(
                "<welcome-file>faces/app/index.xhtml</welcome-file>");

        assertTrue(physicalWelcome >= 0, "the context root must have a physical welcome file");
        assertTrue(facesFallback > physicalWelcome,
                "the virtual Faces route may only remain as a fallback");
        assertTrue(descriptor.contains("<url-pattern>/index.xhtml</url-pattern>"),
                "the physical welcome page must use an exact Faces mapping");
        assertTrue(entryPoint.contains("url=faces/app/index.xhtml"),
                "the welcome page must redirect to the OIDC-protected workspace");
        assertTrue(entryPoint.contains("href=\"faces/app/index.xhtml\""),
                "the welcome page must offer a non-script fallback link");
        assertTrue(rootServlet.contains("urlPatterns = \"\""),
                "the root redirect servlet must map only the empty context path");
        assertTrue(rootServlet.contains("/faces/app/index.xhtml"),
                "the root redirect servlet must target the protected workspace");
    }

    private static Path locateProjectRoot() {
        Path candidate = Path.of("").toAbsolutePath().normalize();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("pom.xml"))
                    && Files.isDirectory(candidate.resolve("distribution/logixone-war"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new AssertionError("Unable to locate the Smart ERP reactor root");
    }
}
