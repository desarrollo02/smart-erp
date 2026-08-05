package py.com.logixone.web.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import py.com.logixone.web.security.TrustedWebAccessException;

/** Emits stable generic 401/403 responses and keeps internal denial codes in logs. */
@Provider
public class TrustedWebAccessExceptionMapper
        implements ExceptionMapper<TrustedWebAccessException> {

    @Override
    public Response toResponse(TrustedWebAccessException failure) {
        Response.ResponseBuilder response = Response.status(failure.status())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .header("Cache-Control", "no-store")
                .entity("{\"error\":\"" + failure.getMessage() + "\"}");
        if (failure.status() == 401) {
            response.header("WWW-Authenticate", "OIDC");
        }
        return response.build();
    }
}
