package py.com.logixone.plugins.referencedata;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugins.referencedata.application.ReferenceDataIdentity;

/** Neutral surface for provenance and enterprise enablement policies. */
public final class ReferenceDataScreenContract {

    public static final String ROUTE = "/reference-data";
    public static final ScreenId CATALOGS =
            new ScreenId(ReferenceDataIdentity.PLUGIN_ID, "catalogs");
    public static final ScreenElementId SEARCH_TEXT = new ScreenElementId("search_text");
    public static final ScreenElementId SEARCH_CATALOG = new ScreenElementId("search_catalog");
    public static final ScreenElementId SEARCH = new ScreenElementId("search");
    public static final ScreenElementId RESULTS = new ScreenElementId("results");
    public static final ScreenElementId HISTORY = new ScreenElementId("history");
    public static final ScreenElementId SELECT_REFERENCE = new ScreenElementId("select_reference");
    public static final ScreenElementId ENABLE_REFERENCE = new ScreenElementId("enable_reference");
    public static final ScreenElementId DISABLE_REFERENCE = new ScreenElementId("disable_reference");
    public static final ScreenSlotId DIRECTORY_EXTENSIONS =
            new ScreenSlotId("directory_extensions");
    public static final ScreenSlotId DETAIL_EXTENSIONS =
            new ScreenSlotId("detail_extensions");

    private static final Set<ScreenCustomizationOperation> CONTENT_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL,
            ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE,
            ScreenCustomizationOperation.REORDER);
    private static final Set<ScreenCustomizationOperation> ACTION_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL,
            ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE,
            ScreenCustomizationOperation.DISABLE,
            ScreenCustomizationOperation.REORDER);

    private ReferenceDataScreenContract() {
    }

    public static ScreenDefinition definition() {
        return new ScreenDefinition(
                CATALOGS,
                SemanticVersion.parse("1.2.0"),
                List.of(
                        field(SEARCH_TEXT, ScreenElementType.TEXT_INPUT, "search", 10, false),
                        field(SEARCH_CATALOG, ScreenElementType.SELECT, "search", 20, true),
                        action(SEARCH, "search_actions", 10),
                        content(RESULTS, "results", 10),
                        content(HISTORY, "history", 10),
                        action(SELECT_REFERENCE, "row_actions", 10),
                        action(ENABLE_REFERENCE, "policy_actions", 10),
                        action(DISABLE_REFERENCE, "policy_actions", 20)),
                List.of(
                        new ScreenSlotDefinition(
                                DIRECTORY_EXTENSIONS,
                                new ScreenRegionId("directory_extensions"),
                                10,
                                2),
                        new ScreenSlotDefinition(
                                DETAIL_EXTENSIONS,
                                new ScreenRegionId("detail_extensions"),
                                10,
                                2)));
    }

    private static ScreenElementDefinition content(
            ScreenElementId id, String region, int order) {
        return element(id, ScreenElementType.DATA_TABLE, region, order, CONTENT_CHANGES);
    }

    private static ScreenElementDefinition field(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order,
            boolean required) {
        String key = "reference_data.catalogs." + id.value();
        return new ScreenElementDefinition(
                id,
                type,
                new ScreenRegionId(region),
                order,
                new ScreenTextKey(key + ".label"),
                Optional.of(new ScreenTextKey(key + ".help")),
                true,
                true,
                required,
                EnumSet.of(
                        ScreenCustomizationOperation.CHANGE_LABEL,
                        ScreenCustomizationOperation.CHANGE_HELP,
                        ScreenCustomizationOperation.HIDE,
                        ScreenCustomizationOperation.DISABLE,
                        ScreenCustomizationOperation.REQUIRE,
                        ScreenCustomizationOperation.REORDER));
    }

    private static ScreenElementDefinition action(
            ScreenElementId id, String region, int order) {
        return element(id, ScreenElementType.ACTION, region, order, ACTION_CHANGES);
    }

    private static ScreenElementDefinition element(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order,
            Set<ScreenCustomizationOperation> changes) {
        String key = "reference_data.catalogs." + id.value();
        return new ScreenElementDefinition(
                id,
                type,
                new ScreenRegionId(region),
                order,
                new ScreenTextKey(key + ".label"),
                Optional.of(new ScreenTextKey(key + ".help")),
                true,
                true,
                false,
                changes);
    }
}
