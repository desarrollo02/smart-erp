package py.com.logixone.plugin.api;

import java.util.Objects;

/** Static semantics of one action; runtime availability is returned separately. */
public record ScreenActionDefinition(
        ScreenElementId elementId,
        ScreenActionIntent intent,
        ScreenActionEmphasis emphasis,
        ScreenConfirmationMode confirmationMode) {

    public ScreenActionDefinition {
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(emphasis, "emphasis");
        Objects.requireNonNull(confirmationMode, "confirmationMode");
        if (emphasis == ScreenActionEmphasis.DESTRUCTIVE
                && confirmationMode == ScreenConfirmationMode.NONE) {
            throw new IllegalArgumentException("Destructive actions require confirmation");
        }
    }
}
