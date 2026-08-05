package py.com.logixone.kernel.application.security.port;

import py.com.logixone.kernel.application.security.SecurityBootstrapState;
import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.command.BootstrapSecurityCommand;

/** Internal infrastructure port; no HTTP or public API adapter is provided. */
@FunctionalInterface
public interface SecurityBootstrapPort {

    SecurityOperationResult<SecurityBootstrapState> bootstrap(BootstrapSecurityCommand command);
}
