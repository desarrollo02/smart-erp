package py.com.logixone.plugin.api;

/**
 * Framework-neutral visual role rendered by a closed adapter in the application shell.
 * Plugins cannot use this contract to supply classes, markup or executable code.
 */
public enum ScreenElementType {
    DISPLAY_TEXT,
    TEXT_INPUT,
    SELECT,
    DATA_TABLE,
    ACTION
}
