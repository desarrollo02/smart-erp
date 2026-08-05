package py.com.logixone.tests.jta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;

@Path("/transactions")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class JtaProbeResource {

    @Inject
    JtaProbeService service;

    @DELETE
    @Path("/reset")
    public Response reset() {
        service.reset();
        return Response.noContent().build();
    }

    @POST
    @Path("/commit/{companyId}")
    public Response commit(@PathParam("companyId") String companyId) {
        service.commit(companyId(companyId));
        return Response.ok("{\"outcome\":\"COMMITTED\"}").build();
    }

    @POST
    @Path("/rollback/{companyId}")
    public Response rollback(@PathParam("companyId") String companyId) {
        try {
            service.rollback(companyId(companyId));
            throw new IllegalStateException("rollback probe unexpectedly returned");
        } catch (ExpectedRollbackException expected) {
            return Response.ok("{\"outcome\":\"ROLLED_BACK\"}").build();
        }
    }

    @POST
    @Path("/application/commit-a/{companyId}")
    public Response applicationCommitA(@PathParam("companyId") String companyId) {
        return applicationResponse(service.applicationCommitA(companyId(companyId)));
    }

    @POST
    @Path("/application/commit-b/{companyId}")
    public Response applicationCommitB(@PathParam("companyId") String companyId) {
        return applicationResponse(service.applicationCommitB(companyId(companyId)));
    }

    @POST
    @Path("/application/rollback/{companyId}")
    public Response applicationRollback(@PathParam("companyId") String companyId) {
        try {
            service.applicationRollback(companyId(companyId));
            throw new IllegalStateException("application rollback probe unexpectedly returned");
        } catch (ExpectedRollbackException expected) {
            return Response.ok("{\"outcome\":\"ROLLED_BACK\"}").build();
        }
    }

    @GET
    @Path("/application/effective/{companyId}")
    public Response effective(@PathParam("companyId") String companyId) {
        String plugins = service.effectivePlugins(companyId(companyId)).stream()
                .map(plugin -> "\"" + plugin + "\"")
                .collect(Collectors.joining(","));
        return Response.ok("{\"plugins\":[" + plugins + "]}").build();
    }

    @GET
    @Path("/application/contributions/{companyId}")
    public Response contributions(@PathParam("companyId") String companyId) {
        JtaProbeService.ContributionProbeResult result =
                service.contributions(companyId(companyId));
        return Response.ok("{\"plugins\":" + jsonArray(result.plugins())
                + ",\"capabilities\":" + jsonArray(result.capabilities())
                + ",\"permissions\":" + jsonArray(result.permissions())
                + ",\"menus\":" + jsonArray(result.menus()) + "}").build();
    }

    @GET
    @Path("/application/screens/{companyId}")
    public Response screens(@PathParam("companyId") String companyId) {
        JtaProbeService.ScreenProbeResult result = service.screens(companyId(companyId));
        return Response.ok("{\"screen\":\"" + result.screen()
                + "\",\"summaryLabel\":\"" + result.summaryLabel()
                + "\",\"summaryVisible\":" + result.summaryVisible()
                + ",\"summaryRequired\":" + result.summaryRequired()
                + ",\"refreshEnabled\":" + result.refreshEnabled()
                + ",\"fragmentOwners\":" + jsonArray(result.fragmentOwners()) + "}").build();
    }

    @GET
    @Path("/state/{companyId}")
    public Response state(@PathParam("companyId") String companyId) {
        JtaProbeService.ProbeState state = service.state(companyId(companyId));
        return Response.ok("{\"company\":" + state.company()
                + ",\"activation\":" + state.activation() + "}").build();
    }

    @POST
    @Path("/system-authority/commit/{probeId}")
    public Response systemAuthorityCommit(@PathParam("probeId") String probeId) {
        JtaProbeService.SystemAuthorityProbeResult result =
                service.systemAuthorityCommit(UUID.fromString(probeId));
        return Response.ok("{\"outcome\":\"" + result.outcome()
                + "\",\"userId\":\"" + result.userId()
                + "\",\"roleId\":\"" + result.roleId() + "\"}").build();
    }

    @POST
    @Path("/system-authority/rollback/{probeId}")
    public Response systemAuthorityRollback(@PathParam("probeId") String probeId) {
        try {
            service.systemAuthorityRollback(UUID.fromString(probeId));
            throw new IllegalStateException("system-authority rollback probe unexpectedly returned");
        } catch (ExpectedRollbackException expected) {
            return Response.ok("{\"outcome\":\"ROLLED_BACK\"}").build();
        }
    }

    @GET
    @Path("/system-authority/state/{probeId}")
    public Response systemAuthorityState(@PathParam("probeId") String probeId) {
        JtaProbeService.SystemAuthorityProbeState state =
                service.systemAuthorityState(UUID.fromString(probeId));
        return Response.ok("{\"users\":" + state.users()
                + ",\"roles\":" + state.roles()
                + ",\"assignments\":" + state.assignments()
                + ",\"permissions\":" + state.permissions()
                + ",\"auditEvents\":" + state.auditEvents() + "}").build();
    }

    private CompanyId companyId(String value) {
        return new CompanyId(UUID.fromString(value));
    }

    private Response applicationResponse(JtaProbeService.ApplicationProbeResult result) {
        return Response.ok("{\"registration\":\"" + result.registration()
                + "\",\"activation\":\"" + result.activation()
                + "\",\"companyStatus\":\"" + result.companyStatus() + "\"}").build();
    }

    private String jsonArray(java.util.List<String> values) {
        return values.stream()
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}
