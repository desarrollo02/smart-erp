package py.com.logixone.web.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.application.health.HealthCheckResult;
import py.com.logixone.kernel.application.health.HealthReport;
import py.com.logixone.web.rest.LogixoneRestApplication;

class HealthResourceTest {

    @Test
    void restContractUsesTheExpectedApplicationAndResourcePaths() throws NoSuchMethodException {
        assertEquals("/", LogixoneRestApplication.class.getAnnotation(ApplicationPath.class).value());
        assertEquals("/health", HealthResource.class.getAnnotation(Path.class).value());
        assertNotNull(HealthResource.class.getAnnotation(ApplicationScoped.class));
        assertNotNull(HealthResource.class.getConstructor());
        assertEquals(
                MediaType.APPLICATION_JSON,
                HealthResource.class.getAnnotation(Produces.class).value()[0]);

        assertEndpoint("liveness", "/live");
        assertEndpoint("readiness", "/ready");
    }

    @Test
    void serializesLivenessWithAStableMinimalPayload() {
        assertEquals(
                "{\"status\":\"UP\",\"checks\":[{\"name\":\"application\",\"status\":\"UP\"}]}",
                HealthResource.toJson(HealthReport.liveness()));
    }

    @Test
    void serializesReadinessWithoutInternalDiagnosticDetails() {
        HealthReport report = HealthReport.readiness(List.of(
                HealthCheckResult.up("catalog"),
                HealthCheckResult.down("database")));

        assertEquals(
                "{\"status\":\"DOWN\",\"checks\":[{\"name\":\"catalog\",\"status\":\"UP\"},{\"name\":\"database\",\"status\":\"DOWN\"}]}",
                HealthResource.toJson(report));
    }

    private void assertEndpoint(String methodName, String path) throws NoSuchMethodException {
        Method method = HealthResource.class.getMethod(methodName);
        assertNotNull(method.getAnnotation(GET.class));
        assertEquals(path, method.getAnnotation(Path.class).value());
    }
}
