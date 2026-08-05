package py.com.logixone.web.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SelectorReturnDraftTest {

    @Test
    void retainsOnlyFieldsAcceptedByTheRenderedScreen() {
        Map<String, String> retained = SelectorReturnDraft.retain(
                Map.of(
                        "name", "Borrador válido",
                        "category", "category-1",
                        "server_only_version", "9"),
                Set.of("name", "category"));

        assertEquals(Map.of(
                "name", "Borrador válido",
                "category", "category-1"), retained);
        assertFalse(retained.containsKey("server_only_version"));
    }

    @Test
    void rejectsControlCharactersAndOversizedDrafts() {
        assertThrows(IllegalArgumentException.class, () -> SelectorReturnDraft.retain(
                Map.of("name", "no\nseguro"), Set.of("name")));
        assertThrows(IllegalArgumentException.class, () -> SelectorReturnDraft.retain(
                Map.of("name", "x".repeat(2049)), Set.of("name")));
    }

    @Test
    void decodesAFormBodyAndRejectsDuplicateOrMalformedKeys() {
        assertEquals(
                Map.of("name", "Borrador seguro", "category", "CAT-1"),
                SelectorReturnDraft.decode(
                        "name=Borrador+seguro&category=CAT-1"));
        assertThrows(IllegalArgumentException.class, () ->
                SelectorReturnDraft.decode("name=uno&name=dos"));
        assertThrows(IllegalArgumentException.class, () ->
                SelectorReturnDraft.decode("bad-key=uno"));
        assertThrows(IllegalArgumentException.class, () ->
                SelectorReturnDraft.decode("name=%ZZ"));
    }
}
