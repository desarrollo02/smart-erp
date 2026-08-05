package py.com.logixone.web.shell;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
                null);
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

    boolean acceptsAction(String actionId) {
        if (rowAction != null && rowAction.getId().equals(actionId) && rowAction.isEnabled()) {
            return true;
        }
        return java.util.stream.Stream.concat(
                        directorySections.stream(), detailSections.stream())
                .flatMap(section -> section.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
    }

    boolean acceptsSelector(String elementId) {
        return java.util.stream.Stream.concat(
                        directorySections.stream(), detailSections.stream())
                .flatMap(section -> section.getFields().stream())
                .anyMatch(field -> field.getId().equals(elementId) && field.isSelect());
    }

    Set<String> safeDraftInputIds() {
        return java.util.stream.Stream.concat(
                        directorySections.stream(), detailSections.stream())
                .flatMap(section -> section.getFields().stream())
                .filter(ShellScreenElementView::isEnabled)
                .filter(field -> field.isTextInput() || field.isSelect())
                .map(ShellScreenElementView::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    boolean acceptsDetailTab(String tabId) {
        return "summary".equals(tabId)
                || detailTabs.stream().anyMatch(tab -> tab.getId().equals(tabId));
    }

    boolean isCreateAction(String actionId) {
        return directorySections.stream()
                .filter(section -> section.getId().equals("create"))
                .flatMap(section -> section.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
    }

    boolean isSearchAction(String actionId) {
        return directorySections.stream()
                .filter(section -> section.getId().equals("search"))
                .flatMap(section -> section.getActions().stream())
                .anyMatch(action -> action.getId().equals(actionId) && action.isEnabled());
    }
}
