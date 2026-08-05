package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import py.com.logixone.web.selector.NativeSelectorSourceCatalog;

class NativeSelectorReturnContextStoreTest {

    private static final String USER = "00000000-0000-0000-0000-000000000001";

    @Test
    void bindsTheContextToActorRevisionRoutesAndOneUseConsumption() {
        var store = new TestStore();
        String token = store.remember(context());

        assertFalse(store.findForTarget(
                token, USER, 8, "/admin/companies.xhtml").isPresent());
        assertFalse(store.findForTarget(
                token, USER, 7, "/admin/security.xhtml").isPresent());
        assertTrue(store.findForTarget(
                token, USER, 7, "/admin/companies.xhtml").isPresent());
        assertFalse(store.consumeForOrigin(
                token, USER, 7, "/admin/security.xhtml").isPresent());
        assertTrue(store.consumeForOrigin(
                token, USER, 7, "/admin/plugins.xhtml").isPresent());
        assertFalse(store.consumeForOrigin(
                token, USER, 7, "/admin/plugins.xhtml").isPresent());
    }

    @Test
    void expiresContextsKeepsFourAndRemainsPassivationCapable() throws Exception {
        var store = new TestStore();
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            tokens.add(store.remember(context()));
            store.now++;
        }

        assertFalse(store.findForTarget(
                tokens.getFirst(), USER, 7, "/admin/companies.xhtml").isPresent());
        assertTrue(store.findForTarget(
                tokens.getLast(), USER, 7, "/admin/companies.xhtml").isPresent());

        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(store);
        }
        assertTrue(bytes.size() > 0);

        store.now += 10 * 60 * 1000L + 1;
        assertFalse(store.findForTarget(
                tokens.getLast(), USER, 7, "/admin/companies.xhtml").isPresent());
    }

    private static NativeSelectorReturnContext context() {
        NativeSelectorReturnPlan plan = NativeSelectorReturnPlan.find(
                NativeSelectorSourceCatalog.PLUGINS_COMPANY).orElseThrow();
        return new NativeSelectorReturnContext(
                USER, 7, plan.usageId(), plan.originRoute(), plan.originTitle(),
                plan.targetRoute(), Map.of("company_id", "company-1"));
    }

    private static final class TestStore extends NativeSelectorReturnContextStore {
        private long now = 1_000_000L;

        @Override
        long nowMillis() {
            return now;
        }
    }
}
