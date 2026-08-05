package py.com.logixone.kernel.api.company;

/** Read-only company scope established by a trusted runtime adapter. */
@FunctionalInterface
public interface CompanyContext {

    CompanyId requiredCompanyId();
}
