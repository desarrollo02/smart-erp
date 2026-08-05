package py.com.logixone.web.shell;

import java.util.List;
import py.com.logixone.plugin.api.ScreenInteraction;

/** JSF-safe bounded page returned by one governed on-demand selector source. */
public final class ShellSelectorOptionPageView {

    private final List<OptionView> options;
    private final long total;
    private final int offset;
    private final int limit;

    ShellSelectorOptionPageView(ScreenInteraction.SelectorOptionPage page) {
        options = page.options().stream().map(OptionView::new).toList();
        total = page.total();
        offset = page.offset();
        limit = page.limit();
    }

    public List<OptionView> getOptions() {
        return options;
    }

    public long getTotal() {
        return total;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isEmpty() {
        return options.isEmpty();
    }

    public boolean isHasPreviousPage() {
        return offset > 0;
    }

    public boolean isHasNextPage() {
        return offset + options.size() < total;
    }

    public long getFirstVisible() {
        return options.isEmpty() ? 0 : offset + 1L;
    }

    public long getLastVisible() {
        return offset + options.size();
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
}
