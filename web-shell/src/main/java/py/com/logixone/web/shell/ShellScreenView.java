package py.com.logixone.web.shell;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.plugin.api.ScreenElementId;

/** Request-only view model produced from one authorized ComposedScreen. */
public final class ShellScreenView {

    private final String id;
    private final String contractVersion;
    private final String eyebrow;
    private final String title;
    private final String description;
    private final String variantLabel;
    private final String variantClass;
    private final List<ShellScreenElementView> mainElements;
    private final List<ShellScreenElementView> actions;
    private final List<ShellScreenFragmentView> fragments;
    private final int hiddenElementCount;
    private final boolean interactive;
    private final List<ShellScreenSectionView> directorySections;
    private final List<ShellScreenSectionView> detailSections;
    private final List<ShellDetailTabView> detailTabs;
    private final ShellScreenElementView tableElement;
    private final ShellScreenElementView rowAction;
    private final ShellEntityPresentation entityPresentation;
    private final ShellFloorplan floorplan;
    private final List<ShellScreenRegionView> floorplanRegions;

    ShellScreenView(
            String id,
            String contractVersion,
            String eyebrow,
            String title,
            String description,
            String variantLabel,
            String variantClass,
            List<ShellScreenElementView> mainElements,
            List<ShellScreenElementView> actions,
            List<ShellScreenFragmentView> fragments,
            int hiddenElementCount) {
        this(
                id,
                contractVersion,
                eyebrow,
                title,
                description,
                variantLabel,
                variantClass,
                mainElements,
                actions,
                fragments,
                hiddenElementCount,
                false,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                List.of());
    }

    ShellScreenView(
            String id,
            String contractVersion,
            String eyebrow,
            String title,
            String description,
            String variantLabel,
            String variantClass,
            List<ShellScreenElementView> mainElements,
            List<ShellScreenElementView> actions,
            List<ShellScreenFragmentView> fragments,
            int hiddenElementCount,
            boolean interactive,
            List<ShellScreenSectionView> directorySections,
            List<ShellScreenSectionView> detailSections,
            List<ShellDetailTabView> detailTabs,
            ShellScreenElementView tableElement,
            ShellScreenElementView rowAction,
            ShellEntityPresentation entityPresentation) {
        this(
                id,
                contractVersion,
                eyebrow,
                title,
                description,
                variantLabel,
                variantClass,
                mainElements,
                actions,
                fragments,
                hiddenElementCount,
                interactive,
                directorySections,
                detailSections,
                detailTabs,
                tableElement,
                rowAction,
                entityPresentation,
                null,
                List.of());
    }

    ShellScreenView(
            String id,
            String contractVersion,
            String eyebrow,
            String title,
            String description,
            String variantLabel,
            String variantClass,
            int hiddenElementCount,
            ShellFloorplan floorplan,
            List<ShellScreenRegionView> floorplanRegions) {
        this(
                id,
                contractVersion,
                eyebrow,
                title,
                description,
                variantLabel,
                variantClass,
                List.of(),
                List.of(),
                List.of(),
                hiddenElementCount,
                true,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                Objects.requireNonNull(floorplan, "floorplan"),
                floorplanRegions);
    }

    private ShellScreenView(
            String id,
            String contractVersion,
            String eyebrow,
            String title,
            String description,
            String variantLabel,
            String variantClass,
            List<ShellScreenElementView> mainElements,
            List<ShellScreenElementView> actions,
            List<ShellScreenFragmentView> fragments,
            int hiddenElementCount,
            boolean interactive,
            List<ShellScreenSectionView> directorySections,
            List<ShellScreenSectionView> detailSections,
            List<ShellDetailTabView> detailTabs,
            ShellScreenElementView tableElement,
            ShellScreenElementView rowAction,
            ShellEntityPresentation entityPresentation,
            ShellFloorplan floorplan,
            List<ShellScreenRegionView> floorplanRegions) {
        this.id = Objects.requireNonNull(id, "id");
        this.contractVersion = Objects.requireNonNull(contractVersion, "contractVersion");
        this.eyebrow = Objects.requireNonNull(eyebrow, "eyebrow");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.variantLabel = Objects.requireNonNull(variantLabel, "variantLabel");
        this.variantClass = Objects.requireNonNull(variantClass, "variantClass");
        this.mainElements = List.copyOf(mainElements);
        this.actions = List.copyOf(actions);
        this.fragments = List.copyOf(fragments);
        this.hiddenElementCount = hiddenElementCount;
        this.interactive = interactive;
        this.directorySections = List.copyOf(directorySections);
        this.detailSections = List.copyOf(detailSections);
        this.detailTabs = List.copyOf(detailTabs);
        this.tableElement = tableElement;
        this.rowAction = rowAction;
        this.entityPresentation = entityPresentation;
        this.floorplan = floorplan;
        this.floorplanRegions = List.copyOf(floorplanRegions);
    }

    public String getId() {
        return id;
    }

    public String getContractVersion() {
        return contractVersion;
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVariantLabel() {
        return variantLabel;
    }

    public String getVariantClass() {
        return "screen-variant " + variantClass;
    }

    public List<ShellScreenElementView> getMainElements() {
        return mainElements;
    }

    public List<ShellScreenElementView> getActions() {
        return actions;
    }

    public List<ShellScreenFragmentView> getFragments() {
        return fragments;
    }

    public int getHiddenElementCount() {
        return hiddenElementCount;
    }

    public boolean isHasHiddenElements() {
        return hiddenElementCount > 0;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean isFloorplanV2() {
        return floorplan != null;
    }

    public String getFloorplanClass() {
        return floorplan == null ? "" : "floorplan floorplan-" + floorplan.code();
    }

    public String getFloorplanLabel() {
        return floorplan == null ? "" : floorplan.label();
    }

    public List<ShellScreenRegionView> getFloorplanRegions() {
        return floorplanRegions;
    }

    public List<ShellScreenSectionView> getDirectorySections() {
        return directorySections;
    }

    public List<ShellScreenSectionView> getDetailSections() {
        return detailSections;
    }

    public List<ShellDetailTabView> getDetailTabs() {
        return detailTabs;
    }

    public String getEntityContext() {
        return entityPresentation.contextLabel();
    }

    public String getCreateTitle() {
        return entityPresentation.createTitle();
    }

    public String getCreateDescription() {
        return entityPresentation.createDescription();
    }

    public String getNewActionLabel() {
        return entityPresentation.newActionLabel();
    }

    public String getBackActionLabel() {
        return entityPresentation.backActionLabel();
    }

    public String getDetailDescription() {
        return entityPresentation.detailDescription();
    }

    public String getSummaryDescription() {
        return entityPresentation.summaryDescription();
    }

    public ShellScreenElementView getTableElement() {
        return tableElement;
    }

    public ShellScreenElementView getRowAction() {
        return rowAction;
    }

    public boolean isHasTableElement() {
        return tableElement != null;
    }

    public boolean isHasRowAction() {
        return rowAction != null;
    }

    public ShellScreenElementView getFloorplanRowAction() {
        boolean hasFloorplanTable = floorplanRegions.stream()
                .anyMatch(region -> !region.getTables().isEmpty());
        if (!hasFloorplanTable) {
            return null;
        }
        return floorplanRegions.stream()
                .flatMap(region -> region.getActions().stream())
                .filter(ShellScreenElementView::isNavigateIntent)
                .findFirst()
                .orElse(null);
    }

    public boolean isHasFloorplanRowAction() {
        return getFloorplanRowAction() != null;
    }

    boolean acceptsAction(String actionId) {
        if (rowAction != null && rowAction.getId().equals(actionId) && rowAction.isEnabled()) {
            return true;
        }
        boolean legacyAction = java.util.stream.Stream.concat(
                        directorySections.stream(), detailSections.stream())
                .flatMap(section -> section.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
        return legacyAction || floorplanRegions.stream()
                .flatMap(region -> region.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
    }

    boolean acceptsSelector(String elementId) {
        boolean legacySelector = java.util.stream.Stream.concat(
                        directorySections.stream(), detailSections.stream())
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(elementId) && field.isSelect());
        return legacySelector || floorplanRegions.stream()
                .flatMap(region -> region.getFields().stream())
                .anyMatch(field -> field.getId().equals(elementId) && field.isSelect());
    }

    Set<String> safeDraftInputIds() {
        java.util.stream.Stream<ShellScreenElementView> legacyFields = java.util.stream.Stream.concat(
                        directorySections.stream(), detailSections.stream())
                .flatMap(section -> section.getFields().stream());
        return java.util.stream.Stream.concat(
                        legacyFields,
                        floorplanRegions.stream().flatMap(region -> region.getFields().stream()))
                .filter(ShellScreenElementView::isEnabled)
                .filter(field -> !field.isTechnicalToken())
                .filter(field -> field.isTextInput() || field.isSelect())
                .map(ShellScreenElementView::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    boolean acceptsDetailTab(String tabId) {
        return "summary".equals(tabId)
                || detailTabs.stream().anyMatch(tab -> tab.getId().equals(tabId));
    }

    boolean isCreateAction(String actionId) {
        boolean legacyCreate = directorySections.stream()
                .filter(section -> section.getId().equals("create"))
                .flatMap(section -> section.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
        return legacyCreate || floorplanRegions.stream()
                .flatMap(region -> region.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId)
                        && action.isEnabled()
                        && action.isCreateIntent());
    }

    boolean isSearchAction(String actionId) {
        boolean legacySearch = directorySections.stream()
                .filter(section -> section.getId().equals("search"))
                .flatMap(section -> section.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
        return legacySearch || floorplanRegions.stream()
                .flatMap(region -> region.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId)
                        && action.isEnabled()
                        && action.isSearchIntent());
    }

    boolean acceptsDynamicStateIds(Set<ScreenElementId> elementIds) {
        Set<String> declaredIds = java.util.stream.Stream.of(
                        mainElements.stream(),
                        actions.stream(),
                        directorySections.stream().flatMap(section -> java.util.stream.Stream.concat(
                                section.getFields().stream(), section.getActions().stream())),
                        detailSections.stream().flatMap(section -> java.util.stream.Stream.concat(
                                section.getFields().stream(), section.getActions().stream())),
                        floorplanRegions.stream().flatMap(region -> java.util.stream.Stream.of(
                                        region.getFields().stream(),
                                        region.getTables().stream(),
                                        region.getActions().stream())
                                .flatMap(java.util.function.Function.identity())))
                .flatMap(java.util.function.Function.identity())
                .map(ShellScreenElementView::getId)
                .collect(Collectors.toUnmodifiableSet());
        return elementIds.stream().map(ScreenElementId::value).allMatch(declaredIds::contains);
    }
}
