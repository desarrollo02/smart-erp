package py.com.logixone.kernel.api.security;

/** Trusted request adapter available to plugin UI/inbound adapters. */
@FunctionalInterface
public interface CurrentCompanyAuthorization {

    AuthorizedCompanyOperation require(String pluginId, String permissionId);
}
