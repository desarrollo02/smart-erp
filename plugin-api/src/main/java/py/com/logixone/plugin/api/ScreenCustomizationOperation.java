package py.com.logixone.plugin.api;

/** Closed set of presentation changes a screen owner may explicitly authorize per element. */
public enum ScreenCustomizationOperation {
    CHANGE_LABEL,
    CHANGE_HELP,
    HIDE,
    DISABLE,
    REQUIRE,
    REORDER
}
