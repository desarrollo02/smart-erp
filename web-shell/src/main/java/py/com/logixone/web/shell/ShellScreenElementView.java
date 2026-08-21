package py.com.logixone.web.shell;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ScreenActionDefinition;
import py.com.logixone.plugin.api.ScreenActionEmphasis;
import py.com.logixone.plugin.api.ScreenActionIntent;
import py.com.logixone.plugin.api.ScreenConfirmationMode;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenSemanticType;

/** JSF-safe presentation of one element accepted by the closed shell renderer. */
public final class ShellScreenElementView {

    private final String id;
    private final ScreenElementType type;
    private final String label;
    private final Optional<String> help;
    private final boolean enabled;
    private final boolean required;
    private final Optional<ScreenSemanticType> semantic;
    private final Optional<ScreenActionDefinition> action;

    ShellScreenElementView(
            String id,
            ScreenElementType type,
            String label,
            Optional<String> help,
            boolean enabled,
            boolean required) {
        this(id, type, label, help, enabled, required, Optional.empty(), Optional.empty());
    }

    ShellScreenElementView(
            String id,
            ScreenElementType type,
            String label,
            Optional<String> help,
            boolean enabled,
            boolean required,
            Optional<ScreenSemanticType> semantic,
            Optional<ScreenActionDefinition> action) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.label = Objects.requireNonNull(label, "label");
        this.help = Objects.requireNonNull(help, "help");
        this.enabled = enabled;
        this.required = required;
        this.semantic = Objects.requireNonNull(semantic, "semantic");
        this.action = Objects.requireNonNull(action, "action");
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

    public boolean isEditableLines() {
        return isDataTable() && semantic
                .filter(value -> value == ScreenSemanticType.EDITABLE_LINES)
                .isPresent();
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

    public String getSemanticClass() {
        return semantic
                .map(value -> "semantic-" + value.name().toLowerCase(java.util.Locale.ROOT)
                        .replace('_', '-'))
                .orElse("semantic-action");
    }

    public String getActionClass() {
        ScreenActionEmphasis emphasis = action
                .map(ScreenActionDefinition::emphasis)
                .orElse(ScreenActionEmphasis.SECONDARY);
        return switch (emphasis) {
            case PRIMARY -> "button button-primary";
            case SECONDARY -> "button button-secondary";
            case DESTRUCTIVE -> "button button-destructive";
        };
    }

    public String getConfirmationScript() {
        return action
                .map(ScreenActionDefinition::confirmationMode)
                .filter(mode -> mode != ScreenConfirmationMode.NONE)
                .map(ignored -> "return confirm('Confirma que deseas continuar con esta acción.');")
                .orElse("");
    }

    public String getConfirmationGuardScript() {
        return action
                .map(ScreenActionDefinition::confirmationMode)
                .filter(mode -> mode != ScreenConfirmationMode.NONE)
                .map(ignored -> "if (!confirm('Confirma que deseas continuar con esta acción.')) { return false; }")
                .orElse("");
    }

    public boolean isCreateIntent() {
        return action.map(ScreenActionDefinition::intent)
                .filter(intent -> intent == ScreenActionIntent.CREATE)
                .isPresent();
    }

    public boolean isSearchIntent() {
        return action.map(ScreenActionDefinition::intent)
                .filter(intent -> intent == ScreenActionIntent.SEARCH)
                .isPresent();
    }

    public boolean isNavigateIntent() {
        return action.map(ScreenActionDefinition::intent)
                .filter(intent -> intent == ScreenActionIntent.NAVIGATE)
                .isPresent();
    }

    public boolean isTechnicalToken() {
        return semantic.filter(value -> value == ScreenSemanticType.TECHNICAL_TOKEN)
                .isPresent();
    }

    public boolean isContextRefreshOnChange() {
        return semantic.filter(value -> value == ScreenSemanticType.SEARCHABLE_REFERENCE)
                .isPresent();
    }
}
