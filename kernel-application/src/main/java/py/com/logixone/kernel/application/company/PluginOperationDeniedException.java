package py.com.logixone.kernel.application.company;

/** Deliberately generic denial that does not reveal company or catalog state. */
public final class PluginOperationDeniedException extends RuntimeException {

    public PluginOperationDeniedException() {
        super("PLUGIN_OPERATION_DENIED");
    }
}
