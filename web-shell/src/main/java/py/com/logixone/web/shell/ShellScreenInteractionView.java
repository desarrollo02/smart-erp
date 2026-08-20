package py.com.logixone.web.shell;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugin.api.SelectorManagementCapability;
import py.com.logixone.plugin.api.SelectorLoadingStrategy;
import py.com.logixone.plugin.api.SelectorSourceDefinition;

/** JSF-safe copy of neutral interactive data returned by one plugin handler. */
public final class ShellScreenInteractionView {

    private final Map<String, List<OptionView>> options;
    private final Map<String, SelectorSourceView> selectorSources;
    private final Optional<TableView> table;
    private final Optional<DetailView> detail;
    private final List<NoticeView> notices;
    private final Map<String, ElementStateView> elementStates;

    private ShellScreenInteractionView(
            Map<String, List<OptionView>> options,
            Map<String, SelectorSourceView> selectorSources,
            Optional<TableView> table,
            Optional<DetailView> detail,
            List<NoticeView> notices,
            Map<String, ElementStateView> elementStates) {
        this.options = Map.copyOf(options);
        this.selectorSources = Map.copyOf(selectorSources);
        this.table = Objects.requireNonNull(table, "table");
        this.detail = Objects.requireNonNull(detail, "detail");
        this.notices = List.copyOf(notices);
        this.elementStates = Map.copyOf(elementStates);
    }

    static ShellScreenInteractionView empty() {
        return new ShellScreenInteractionView(
                Map.of(), Map.of(), Optional.empty(), Optional.empty(), List.of(), Map.of());
    }

    static ShellScreenInteractionView from(ScreenInteraction.Result result) {
        return from(result, Map.of(), Set.of());
    }

    static ShellScreenInteractionView from(
            ScreenInteraction.Result result,
            Map<ScreenElementId, SelectorSourceDefinition> sources,
            Set<ScreenElementId> authorizedManagement) {
        Map<String, List<OptionView>> options = result.options().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().value(),
                        entry -> entry.getValue().stream().map(OptionView::new).toList()));
        Map<String, SelectorSourceView> selectorSources = Objects.requireNonNull(
                        sources, "sources").entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().value(),
                        entry -> new SelectorSourceView(
                                entry.getValue(),
                                Objects.requireNonNull(authorizedManagement,
                                                "authorizedManagement")
                                        .contains(entry.getKey()))));
        Map<String, ElementStateView> elementStates = result.elementStates().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().value(),
                        entry -> new ElementStateView(entry.getValue())));
        return new ShellScreenInteractionView(
                options,
                selectorSources,
                result.table().map(TableView::new),
                result.detail().map(DetailView::new),
                result.notices().stream().map(NoticeView::new).toList(),
                elementStates);
    }

    public Map<String, List<OptionView>> getOptions() {
        return options;
    }

    String selectedOptionLabel(String fieldId, String selectedValue) {
        if (selectedValue == null || selectedValue.isBlank()) {
            return "Ninguna";
        }
        return options.getOrDefault(fieldId, List.of()).stream()
                .filter(option -> option.getValue().equals(selectedValue))
                .map(OptionView::getLabel)
                .findFirst()
                .orElse("Selección no disponible");
    }

    public Map<String, SelectorSourceView> getSelectorSources() {
        return selectorSources;
    }

    public TableView getTable() {
        return table.orElse(null);
    }

    public boolean isHasTable() {
        return table.isPresent();
    }

    public DetailView getDetail() {
        return detail.orElse(null);
    }

    public boolean isHasDetail() {
        return detail.isPresent();
    }

    public List<NoticeView> getNotices() {
        return notices;
    }

    public Map<String, ElementStateView> getElementStates() {
        return elementStates;
    }

    boolean acceptsAction(String actionId) {
        ElementStateView dynamic = elementStates.get(actionId);
        return dynamic == null || (dynamic.isVisible() && dynamic.isEnabled());
    }

    public static final class ElementStateView {
        private final boolean visible;
        private final boolean enabled;
        private final boolean required;
        private final String unavailableReason;

        private ElementStateView(ScreenInteraction.ElementState state) {
            visible = state.visible();
            enabled = state.enabled();
            required = state.required();
            unavailableReason = state.unavailableReason().orElse("");
        }

        public boolean isVisible() {
            return visible;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isRequired() {
            return required;
        }

        public String getUnavailableReason() {
            return unavailableReason;
        }

        public boolean isHasUnavailableReason() {
            return !unavailableReason.isEmpty();
        }
    }

    public static final class OptionView {
        private final String value;
        private final String label;

        private OptionView(ScreenInteraction.Option option) {
            value = option.value();
            label = option.label();
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class SelectorSourceView {
        private final boolean managementAvailable;
        private final String managementRoute;
        private final String managementLabel;
        private final boolean searchOnDemand;

        private SelectorSourceView(SelectorSourceDefinition source, boolean authorized) {
            Objects.requireNonNull(source, "source");
            managementAvailable = authorized && source.manageable();
            managementRoute = managementAvailable
                    ? source.managementRoute().orElseThrow()
                    : "";
            managementLabel = source.managementCapabilities()
                    .contains(SelectorManagementCapability.CREATE)
                    ? "Agregar o administrar"
                    : "Administrar";
            searchOnDemand = source.loadingStrategy() == SelectorLoadingStrategy.SEARCH_ON_DEMAND;
        }

        public boolean isManagementAvailable() {
            return managementAvailable;
        }

        public String getManagementRoute() {
            return managementRoute;
        }

        public String getManagementLabel() {
            return managementLabel;
        }

        public boolean isSearchOnDemand() {
            return searchOnDemand;
        }
    }

    public static final class TableView {
        private final String elementId;
        private final List<ColumnView> columns;
        private final List<RowView> rows;
        private final long total;
        private final String emptyTitle;
        private final String emptyDescription;
        private final boolean paged;
        private final int offset;
        private final int pageSize;

        private TableView(ScreenInteraction.Table table) {
            elementId = table.elementId().value();
            columns = table.columns().stream().map(ColumnView::new).toList();
            rows = table.rows().stream().map(RowView::new).toList();
            total = table.total();
            emptyTitle = table.emptyTitle();
            emptyDescription = table.emptyDescription();
            paged = table.page().isPresent();
            offset = table.page().map(ScreenInteraction.TablePage::offset).orElse(0);
            pageSize = table.page().map(ScreenInteraction.TablePage::limit).orElse(0);
        }

        public String getElementId() {
            return elementId;
        }

        public List<ColumnView> getColumns() {
            return columns;
        }

        public List<RowView> getRows() {
            return rows;
        }

        public long getTotal() {
            return total;
        }

        public String getEmptyTitle() {
            return emptyTitle;
        }

        public String getEmptyDescription() {
            return emptyDescription;
        }

        public boolean isRowsEmpty() {
            return rows.isEmpty();
        }

        public boolean isPaged() {
            return paged;
        }

        public int getOffset() {
            return offset;
        }

        public int getPageSize() {
            return pageSize;
        }

        public boolean isHasPreviousPage() {
            return paged && offset > 0;
        }

        public boolean isHasNextPage() {
            return paged && offset + rows.size() < total;
        }

        public long getFirstVisible() {
            return rows.isEmpty() ? 0 : offset + 1L;
        }

        public long getLastVisible() {
            return offset + rows.size();
        }
    }

    public static final class ColumnView {
        private final String key;
        private final String label;

        private ColumnView(ScreenInteraction.Column column) {
            key = column.key();
            label = column.label();
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class RowView {
        private final String resourceId;
        private final List<String> cells;

        private RowView(ScreenInteraction.Row row) {
            resourceId = row.resourceId();
            cells = row.cells();
        }

        public String getResourceId() {
            return resourceId;
        }

        public List<String> getCells() {
            return cells;
        }
    }

    public static final class DetailView {
        private final String resourceId;
        private final String title;
        private final List<DetailItemView> items;

        private DetailView(ScreenInteraction.Detail detail) {
            resourceId = detail.resourceId();
            title = detail.title();
            items = detail.items().stream().map(DetailItemView::new).toList();
        }

        public String getResourceId() {
            return resourceId;
        }

        public String getTitle() {
            return title;
        }

        public List<DetailItemView> getItems() {
            return items;
        }
    }

    public static final class DetailItemView {
        private final String label;
        private final String value;

        private DetailItemView(ScreenInteraction.DetailItem item) {
            label = item.label();
            value = item.value();
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public static final class NoticeView {
        private final String level;
        private final String summary;
        private final String detail;

        private NoticeView(ScreenInteraction.Notice notice) {
            level = notice.level().name().toLowerCase(java.util.Locale.ROOT);
            summary = notice.summary();
            detail = notice.detail();
        }

        public String getLevel() {
            return level;
        }

        public String getSummary() {
            return summary;
        }

        public String getDetail() {
            return detail;
        }

        public String getStyleClass() {
            return "screen-notice screen-notice-" + level;
        }

        public String getRole() {
            return level.equals("error") ? "alert" : "status";
        }
    }
}
