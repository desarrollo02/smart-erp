package py.com.logixone.web.shell;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import py.com.logixone.plugin.api.ScreenPurpose;
import py.com.logixone.plugin.api.ScreenRegionRole;

/** Closed shell-owned renderer family selected from a neutral v2 purpose. */
enum ShellFloorplan {
    MASTER_DATA(
            ScreenPurpose.MASTER_DATA,
            "Maestro",
            EnumSet.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS)),
    WORKLIST(
            ScreenPurpose.WORKLIST,
            "Bandeja de trabajo",
            EnumSet.of(ScreenRegionRole.WORK_ITEMS, ScreenRegionRole.ACTIONS)),
    TRANSACTION_EDITOR(
            ScreenPurpose.TRANSACTION_EDITOR,
            "Editor transaccional",
            EnumSet.of(
                    ScreenRegionRole.HEADER,
                    ScreenRegionRole.LINES,
                    ScreenRegionRole.SUMMARY,
                    ScreenRegionRole.ACTIONS)),
    GUIDED_OPERATION(
            ScreenPurpose.GUIDED_OPERATION,
            "Operación guiada",
            EnumSet.of(ScreenRegionRole.CONTENT, ScreenRegionRole.ACTIONS)),
    INQUIRY(
            ScreenPurpose.INQUIRY,
            "Consulta",
            EnumSet.of(ScreenRegionRole.CONTENT));

    private final ScreenPurpose purpose;
    private final String label;
    private final Set<ScreenRegionRole> requiredRoles;

    ShellFloorplan(
            ScreenPurpose purpose,
            String label,
            Set<ScreenRegionRole> requiredRoles) {
        this.purpose = purpose;
        this.label = label;
        this.requiredRoles = Set.copyOf(requiredRoles);
    }

    static ShellFloorplan from(ScreenPurpose purpose) {
        return switch (purpose) {
            case MASTER_DATA -> MASTER_DATA;
            case WORKLIST -> WORKLIST;
            case TRANSACTION_EDITOR -> TRANSACTION_EDITOR;
            case GUIDED_OPERATION -> GUIDED_OPERATION;
            case INQUIRY -> INQUIRY;
        };
    }

    boolean accepts(Set<ScreenRegionRole> roles) {
        return roles.containsAll(requiredRoles);
    }

    String code() {
        return purpose.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    String label() {
        return label;
    }
}
