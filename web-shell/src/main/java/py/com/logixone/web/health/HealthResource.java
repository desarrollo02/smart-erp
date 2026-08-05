package py.com.logixone.web.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.com.logixone.kernel.application.health.ApplicationHealthService;
import py.com.logixone.kernel.application.health.HealthCheckResult;
import py.com.logixone.kernel.application.health.HealthReport;
import py.com.logixone.kernel.application.health.HealthStatus;

@ApplicationScoped
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final String CACHE_CONTROL = "no-store";

    @Inject
    ApplicationHealthService healthService;

    public HealthResource() {
    }

    @GET
    @Path("/live")
    public Response liveness() {
        return response(healthService.liveness());
    }

    @GET
    @Path("/ready")
    public Response readiness() {
        return response(healthService.readiness());
    }

    private Response response(HealthReport report) {
        int status = report.status() == HealthStatus.UP
                ? Response.Status.OK.getStatusCode()
                : Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .header("Cache-Control", CACHE_CONTROL)
                .entity(toJson(report))
                .build();
    }

    static String toJson(HealthReport report) {
        StringBuilder json = new StringBuilder(96);
        json.append("{\"status\":\"").append(report.status()).append("\",\"checks\":[");
        for (int index = 0; index < report.checks().size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            HealthCheckResult check = report.checks().get(index);
            json.append("{\"name\":\"")
                    .append(check.name())
                    .append("\",\"status\":\"")
                    .append(check.status())
                    .append("\"}");
        }
        return json.append("]}").toString();
    }
}
