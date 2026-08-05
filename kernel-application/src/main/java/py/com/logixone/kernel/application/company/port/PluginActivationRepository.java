package py.com.logixone.kernel.application.company.port;

import java.util.List;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.plugin.api.PluginId;

public interface PluginActivationRepository {

    List<PluginActivationDecision> findByCompanyId(CompanyId companyId);

    Optional<PluginActivationDecision> findByCompanyAndPlugin(
            CompanyId companyId,
            PluginId pluginId);

    /** Persists a new decision or an idempotent/versioned replacement and returns stored state. */
    PluginActivationDecision save(PluginActivationDecision decision);
}
