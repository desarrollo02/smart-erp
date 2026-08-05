package py.com.logixone.web.shell;

import java.util.Map;

/** Safe native form values restored after the server has revalidated the return context. */
public record NativeSelectorReturnRestoration(
        String usageId,
        Map<String, String> inputs) {

    public NativeSelectorReturnRestoration {
        inputs = Map.copyOf(inputs);
    }
}
