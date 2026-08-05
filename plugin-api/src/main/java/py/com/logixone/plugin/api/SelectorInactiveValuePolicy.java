package py.com.logixone.plugin.api;

/** Policy for values that existed historically but are no longer active. */
public enum SelectorInactiveValuePolicy {
    NOT_APPLICABLE,
    EXCLUDE_FOR_NEW_KEEP_SELECTED,
    INCLUDE_MARKED
}
