package py.com.logixone.web.shell;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/** Server-owned native selector origin; only its opaque token is exposed to the browser. */
record NativeSelectorReturnContext(
        String userId,
        long sessionRevision,
        String usageId,
        String originRoute,
        String originTitle,
        String targetRoute,
        Map<String, String> inputs) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    NativeSelectorReturnContext {
        if (userId == null || userId.isBlank() || userId.length() > 64
                || sessionRevision < 0) {
            throw new IllegalArgumentException("Invalid native selector identity binding");
        }
        NativeSelectorReturnPlan plan = NativeSelectorReturnPlan.find(usageId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown native selector usage"));
        if (!plan.originRoute().equals(originRoute)
                || !plan.originTitle().equals(originTitle)
                || !plan.targetRoute().equals(targetRoute)) {
            throw new IllegalArgumentException("Native selector context does not match its plan");
        }
        inputs = SelectorReturnDraft.retain(inputs, plan.draftInputIds());
    }

    boolean belongsTo(String expectedUserId, long expectedRevision) {
        return userId.equals(expectedUserId) && sessionRevision == expectedRevision;
    }
}
