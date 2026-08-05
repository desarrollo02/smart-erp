package py.com.logixone.web.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import py.com.logixone.web.security.TrustedWebAccess;

/** Minimal probe that succeeds only with a currently valid local company context. */
@Path("/api/company-context")
public class TrustedContextResource {

    @Inject
    TrustedWebAccess access;

    @GET
    public Response current() {
        access.requireCompany();
        return Response.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }
}
