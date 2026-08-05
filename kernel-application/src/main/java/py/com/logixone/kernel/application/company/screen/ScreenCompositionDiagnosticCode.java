package py.com.logixone.kernel.application.company.screen;

/** Stable reasons why a company screen overlay cannot be applied atomically. */
public enum ScreenCompositionDiagnosticCode {
    COMPANY_NOT_OPERATIONAL,
    SCREEN_TARGET_NOT_FOUND,
    SCREEN_VERSION_INCOMPATIBLE,
    SCREEN_TARGET_DEPENDENCY_INVALID,
    SCREEN_ELEMENT_NOT_FOUND,
    SCREEN_SLOT_NOT_FOUND,
    SCREEN_OPERATION_NOT_ALLOWED,
    SCREEN_CHANGE_CONFLICT,
    SCREEN_POSITION_OUT_OF_RANGE,
    SCREEN_SLOT_CAPACITY_EXCEEDED,
    SCREEN_FRAGMENT_OWNER_MISMATCH
}
