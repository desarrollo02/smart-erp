package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SelectorReturnContextStoreTest {

    private static final String USER = "00000000-0000-0000-0000-000000000001";
    private static final String COMPANY = "00000000-0000-0000-0000-000000000002";

    @Test
    void contextIsBoundToTargetIdentityAndConsumedOnceAtTheOrigin() {
        var store = new TestStore();
        String token = store.remember(context("/catalog/items", "/catalog/definitions"));

        assertFalse(store.findForTarget(
                token, USER, COMPANY, 7, "/inventory/warehouses").isPresent());
        assertFalse(store.findForTarget(
                token, USER, "00000000-0000-0000-0000-000000000003", 7,
                "/catalog/definitions").isPresent());

        SelectorReturnContext target = store.findForTarget(
                        token, USER, COMPANY, 7, "/catalog/definitions")
                .orElseThrow();
        assertEquals("Borrador", target.inputs().get("name"));
        assertFalse(store.consumeForOrigin(
                token, USER, COMPANY, 7, "/wrong-origin").isPresent());
        assertTrue(store.consumeForOrigin(
                token, USER, COMPANY, 7, "/catalog/items").isPresent());
        assertFalse(store.consumeForOrigin(
                token, USER, COMPANY, 7, "/catalog/items").isPresent());
    }

    @Test
    void contextsExpireAndTheStoreKeepsOnlyTheFourNewest() {
        var store = new TestStore();
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            tokens.add(store.remember(context(
                    "/catalog/items", "/catalog/definitions")));
            store.now++;
        }

        assertFalse(store.findForTarget(
                tokens.getFirst(), USER, COMPANY, 7, "/catalog/definitions").isPresent());
        assertTrue(store.findForTarget(
                tokens.getLast(), USER, COMPANY, 7, "/catalog/definitions").isPresent());

        store.now += 10 * 60 * 1000L + 1;
        assertFalse(store.findForTarget(
                tokens.getLast(), USER, COMPANY, 7, "/catalog/definitions").isPresent());
    }

    @Test
    void sessionStoreRemainsPassivationCapableWithAnActiveDraft() throws Exception {
        var store = new TestStore();
        store.remember(context("/catalog/items", "/catalog/definitions"));

        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(store);
        }

        assertTrue(bytes.size() > 0);
    }

    private static SelectorReturnContext context(String origin, String target) {
        return new SelectorReturnContext(
                USER,
                COMPANY,
                7,
                origin,
                "Artículos",
                target,
                "create",
                "summary",
                null,
                null,
                Map.of("name", "Borrador"));
    }

    private static final class TestStore extends SelectorReturnContextStore {
        private long now = 1_000_000L;

        @Override
        long nowMillis() {
            return now;
        }
    }
}
