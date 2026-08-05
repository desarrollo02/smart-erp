package py.com.logixone.web.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/** Authentication-only probe used by the deferred OIDC integration matrix. */
@Path("/api/protected-probe")
public class ProtectedProbeResource {

    @GET
    public Response probe() {
        return Response.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }
}
