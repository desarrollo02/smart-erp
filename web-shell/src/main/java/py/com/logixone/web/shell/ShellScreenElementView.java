package py.com.logixone.web.shell;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ScreenElementType;

/** JSF-safe presentation of one element accepted by the closed shell renderer. */
public final class ShellScreenElementView {

    private final String id;
    private final ScreenElementType type;
    private final String label;
    private final Optional<String> help;
    private final boolean enabled;
    private final boolean required;

    ShellScreenElementView(
            String id,
            ScreenElementType type,
            String label,
            Optional<String> help,
            boolean enabled,
            boolean required) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.label = Objects.requireNonNull(label, "label");
        this.help = Objects.requireNonNull(help, "help");
        this.enabled = enabled;
        this.required = required;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getHelp() {
        return help.orElse("");
    }

    public boolean isHasHelp() {
        return help.isPresent();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isDisplayText() {
        return type == ScreenElementType.DISPLAY_TEXT;
    }

    public boolean isTextInput() {
        return type == ScreenElementType.TEXT_INPUT;
    }

    public boolean isSelect() {
        return type == ScreenElementType.SELECT;
    }

    public boolean isDataTable() {
        return type == ScreenElementType.DATA_TABLE;
    }

    public boolean isAction() {
        return type == ScreenElementType.ACTION;
    }

    public String getStateClass() {
        StringBuilder classes = new StringBuilder("composed-element");
        if (!enabled) {
            classes.append(" composed-element-disabled");
        }
        if (required) {
            classes.append(" composed-element-required");
        }
        return classes.toString();
    }
}
