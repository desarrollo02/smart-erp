package py.com.logixone.kernel.application.security.system.port;

import py.com.logixone.kernel.application.security.SecurityOperationResult;
import py.com.logixone.kernel.application.security.system.SystemAuthorityBootstrapState;
import py.com.logixone.kernel.application.security.system.command.BootstrapSystemAuthorityCommand;

/** Internal bootstrap boundary; intentionally has no HTTP or Faces adapter. */
@FunctionalInterface
public interface SystemAuthorityBootstrapPort {

    SecurityOperationResult<SystemAuthorityBootstrapState> bootstrap(
            BootstrapSystemAuthorityCommand command);
}
