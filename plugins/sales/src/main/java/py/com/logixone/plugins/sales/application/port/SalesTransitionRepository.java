package py.com.logixone.plugins.sales.application.port;

public interface SalesTransitionRepository {
    void append(SalesTransitionRecord transition);
}
