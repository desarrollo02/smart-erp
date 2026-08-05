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

/** Read-only neutral surface for release provenance and current bootstrap entries. */
public final class ReferenceDataScreenContract {

    public static final String ROUTE = "/reference-data";
    public static final ScreenId CATALOGS =
            new ScreenId(ReferenceDataIdentity.PLUGIN_ID, "catalogs");
    public static final ScreenElementId RESULTS = new ScreenElementId("results");
    public static final ScreenSlotId DIRECTORY_EXTENSIONS =
            new ScreenSlotId("directory_extensions");
    public static final ScreenSlotId DETAIL_EXTENSIONS =
            new ScreenSlotId("detail_extensions");

    private static final Set<ScreenCustomizationOperation> CONTENT_CHANGES = EnumSet.of(
            ScreenCustomizationOperation.CHANGE_LABEL,
            ScreenCustomizationOperation.CHANGE_HELP,
            ScreenCustomizationOperation.HIDE,
            ScreenCustomizationOperation.REORDER);

    private ReferenceDataScreenContract() {
    }

    public static ScreenDefinition definition() {
        return new ScreenDefinition(
                CATALOGS,
                SemanticVersion.parse("1.0.0"),
                List.of(new ScreenElementDefinition(
                        RESULTS,
                        ScreenElementType.DATA_TABLE,
                        new ScreenRegionId("results"),
                        10,
                        new ScreenTextKey("reference_data.catalogs.results.label"),
                        Optional.of(new ScreenTextKey("reference_data.catalogs.results.help")),
                        true,
                        true,
                        false,
                        CONTENT_CHANGES)),
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
}
