package py.com.logixone.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Framework-neutral request and presentation data for an interactive screen.
 * Implementations provide values only; markup and visual behavior remain owned by
 * the application shell.
 */
public final class ScreenInteraction {

    private static final int MAX_INPUTS = 96;
    private static final int MAX_INPUT_LENGTH = 2048;

    private ScreenInteraction() {
    }

    public interface Handler {

        ScreenId screenId();

        /**
         * Declares the governed option source of each SELECT field handled by this
         * screen. Existing handlers remain compatible and may migrate incrementally.
         */
        default Map<ScreenElementId, SelectorSourceDefinition> selectorSources() {
            return Map.of();
        }

        /**
         * Resolves one bounded page for a selector declared as SEARCH_ON_DEMAND.
         * The shell validates ownership and strategy before invoking this method.
         */
        default SelectorOptionPage searchOptions(SelectorOptionRequest request) {
            throw new UnsupportedOperationException("On-demand selector search is not supported");
        }

        Result interact(Request request);
    }

    public record Request(
            Optional<ScreenElementId> actionId,
            Map<ScreenElementId, String> inputs,
            Optional<String> selectedResourceId,
            Optional<Long> selectedResourceVersion,
            Optional<TablePageRequest> tablePage) {

        public Request(
                Optional<ScreenElementId> actionId,
                Map<ScreenElementId, String> inputs,
                Optional<String> selectedResourceId,
                Optional<Long> selectedResourceVersion) {
            this(actionId, inputs, selectedResourceId, selectedResourceVersion, Optional.empty());
        }

        public Request {
            actionId = Objects.requireNonNull(actionId, "actionId");
            inputs = Map.copyOf(Objects.requireNonNull(inputs, "inputs"));
            selectedResourceId = text(selectedResourceId, "selectedResourceId", 160);
            selectedResourceVersion = Objects.requireNonNull(
                    selectedResourceVersion, "selectedResourceVersion");
            tablePage = Objects.requireNonNull(tablePage, "tablePage");
            if (inputs.size() > MAX_INPUTS) {
                throw new IllegalArgumentException("Too many screen inputs");
            }
            inputs.forEach((key, value) -> {
                Objects.requireNonNull(key, "input key");
                requireTextValue(value, "input value", MAX_INPUT_LENGTH, true);
            });
            selectedResourceVersion.ifPresent(version -> {
                if (version < 0) {
                    throw new IllegalArgumentException(
                            "selectedResourceVersion must not be negative");
                }
            });
        }

        public static Request load(Map<ScreenElementId, String> inputs) {
            return new Request(
                    Optional.empty(), inputs, Optional.empty(), Optional.empty(), Optional.empty());
        }
    }

    public record TablePageRequest(int offset, int limit) {

        public TablePageRequest {
            if (offset < 0 || limit < 1 || limit > 50) {
                throw new IllegalArgumentException("Table pages must contain between 1 and 50 rows");
            }
        }
    }

    public record SelectorOptionRequest(
            ScreenElementId elementId,
            String query,
            int offset,
            int limit) {

        public SelectorOptionRequest {
            Objects.requireNonNull(elementId, "elementId");
            query = requireTextValue(query, "selector query", 100, true).strip();
            if (offset < 0 || limit < 1 || limit > 50) {
                throw new IllegalArgumentException(
                        "Selector pages must contain between 1 and 50 options");
            }
        }
    }

    public record SelectorOptionPage(
            List<Option> options,
            long total,
            int offset,
            int limit) {

        public SelectorOptionPage {
            options = List.copyOf(Objects.requireNonNull(options, "options"));
            if (offset < 0
                    || limit < 1
                    || limit > 50
                    || options.size() > limit
                    || total < options.size()
                    || offset > total) {
                throw new IllegalArgumentException("Invalid selector option page");
            }
        }
    }

    public record Result(
            Map<ScreenElementId, String> inputs,
            Map<ScreenElementId, List<Option>> options,
            Optional<Table> table,
            Optional<Detail> detail,
            List<Notice> notices,
            Optional<String> selectedResourceId,
            Optional<Long> selectedResourceVersion,
            Map<ScreenElementId, ElementState> elementStates) {

        /** Compatibility constructor for v1 handlers without dynamic states. */
        public Result(
                Map<ScreenElementId, String> inputs,
                Map<ScreenElementId, List<Option>> options,
                Optional<Table> table,
                Optional<Detail> detail,
                List<Notice> notices,
                Optional<String> selectedResourceId,
                Optional<Long> selectedResourceVersion) {
            this(inputs, options, table, detail, notices, selectedResourceId,
                    selectedResourceVersion, Map.of());
        }

        public Result {
            inputs = Map.copyOf(Objects.requireNonNull(inputs, "inputs"));
            options = Objects.requireNonNull(options, "options").entrySet().stream()
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> List.copyOf(entry.getValue())));
            table = Objects.requireNonNull(table, "table");
            detail = Objects.requireNonNull(detail, "detail");
            notices = List.copyOf(Objects.requireNonNull(notices, "notices"));
            selectedResourceId = text(selectedResourceId, "selectedResourceId", 160);
            selectedResourceVersion = Objects.requireNonNull(
                    selectedResourceVersion, "selectedResourceVersion");
            elementStates = Map.copyOf(Objects.requireNonNull(elementStates, "elementStates"));
            if (selectedResourceId.isPresent() != selectedResourceVersion.isPresent()) {
                throw new IllegalArgumentException(
                        "Selected resource identity and version must appear together");
            }
            if (detail.isPresent()
                    && !detail.orElseThrow().resourceId().equals(selectedResourceId.orElse(""))) {
                throw new IllegalArgumentException("Detail must describe the selected resource");
            }
        }
    }

    /**
     * Runtime state of a declared element. Missing entries retain the static
     * definition, which keeps v1 handlers compatible.
     */
    public record ElementState(
            boolean visible,
            boolean enabled,
            boolean required,
            Optional<String> unavailableReason) {

        public ElementState {
            unavailableReason = text(
                    Objects.requireNonNull(unavailableReason, "unavailableReason"),
                    "unavailableReason",
                    320);
            if (!visible && (enabled || required || unavailableReason.isPresent())) {
                throw new IllegalArgumentException("A hidden element cannot be interactive");
            }
            if (required && !enabled) {
                throw new IllegalArgumentException("A disabled element cannot be required");
            }
            if (enabled && unavailableReason.isPresent()) {
                throw new IllegalArgumentException("An enabled element cannot have a blocking reason");
            }
        }

        public static ElementState shown() {
            return new ElementState(true, true, false, Optional.empty());
        }

        public static ElementState requiredInput() {
            return new ElementState(true, true, true, Optional.empty());
        }

        public static ElementState blocked(String reason) {
            return new ElementState(true, false, false, Optional.of(reason));
        }

        public static ElementState hidden() {
            return new ElementState(false, false, false, Optional.empty());
        }
    }

    public record Option(String value, String label) {

        public Option {
            value = requireTextValue(value, "option value", 96, false);
            label = requireTextValue(label, "option label", 160, false);
        }
    }

    public record Table(
            ScreenElementId elementId,
            List<Column> columns,
            List<Row> rows,
            long total,
            String emptyTitle,
            String emptyDescription,
            Optional<TablePage> page) {

        public Table(
                ScreenElementId elementId,
                List<Column> columns,
                List<Row> rows,
                long total,
                String emptyTitle,
                String emptyDescription) {
            this(elementId, columns, rows, total, emptyTitle, emptyDescription, Optional.empty());
        }

        public Table {
            Objects.requireNonNull(elementId, "elementId");
            columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            if (columns.isEmpty() || columns.size() > 12 || rows.size() > 100 || total < rows.size()) {
                throw new IllegalArgumentException("Invalid screen table dimensions");
            }
            for (Row row : rows) {
                if (row.cells().size() != columns.size()) {
                    throw new IllegalArgumentException("Every table row must match its columns");
                }
            }
            emptyTitle = requireTextValue(emptyTitle, "emptyTitle", 160, false);
            emptyDescription = requireTextValue(
                    emptyDescription, "emptyDescription", 320, false);
            page = Objects.requireNonNull(page, "page");
            if (page.isPresent()) {
                TablePage value = page.orElseThrow();
                if (rows.size() > value.limit() || value.offset() > total) {
                    throw new IllegalArgumentException("Invalid table page");
                }
            }
        }
    }

    public record TablePage(int offset, int limit) {

        public TablePage {
            if (offset < 0 || limit < 1 || limit > 50) {
                throw new IllegalArgumentException("Table pages must contain between 1 and 50 rows");
            }
        }
    }

    public record Column(String key, String label) {

        public Column {
            key = requireIdentifier(key, "column key");
            label = requireTextValue(label, "column label", 160, false);
        }
    }

    public record Row(String resourceId, List<String> cells) {

        public Row {
            resourceId = requireTextValue(resourceId, "resourceId", 160, false);
            cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
            cells.forEach(cell -> requireTextValue(cell, "cell", 320, true));
        }
    }

    public record Detail(String resourceId, String title, List<DetailItem> items) {

        public Detail {
            resourceId = requireTextValue(resourceId, "resourceId", 160, false);
            title = requireTextValue(title, "detail title", 200, false);
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            if (items.isEmpty() || items.size() > 32) {
                throw new IllegalArgumentException("Detail must contain between 1 and 32 items");
            }
        }
    }

    public record DetailItem(String label, String value) {

        public DetailItem {
            label = requireTextValue(label, "detail label", 160, false);
            value = requireTextValue(value, "detail value", 512, true);
        }
    }

    public record Notice(NoticeLevel level, String summary, String detail) {

        public Notice {
            Objects.requireNonNull(level, "level");
            summary = requireTextValue(summary, "notice summary", 160, false);
            detail = requireTextValue(detail, "notice detail", 512, false);
        }
    }

    public enum NoticeLevel {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private static Optional<String> text(
            Optional<String> value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        return value.map(text -> requireTextValue(text, name, maxLength, false));
    }

    private static String requireIdentifier(String value, String name) {
        value = requireTextValue(value, name, 64, false);
        if (!value.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException(name + " must be a stable identifier");
        }
        return value;
    }

    private static String requireTextValue(
            String value, String name, int maxLength, boolean emptyAllowed) {
        Objects.requireNonNull(value, name);
        if ((!emptyAllowed && value.isBlank()) || value.length() > maxLength) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        if (value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must not contain control characters");
        }
        return value;
    }
}
