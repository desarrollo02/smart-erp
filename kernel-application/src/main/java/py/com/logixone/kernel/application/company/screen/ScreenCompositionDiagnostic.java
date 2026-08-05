package py.com.logixone.kernel.application.company.screen;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.ScreenId;

/** Deterministic, framework-neutral diagnostic for one rejected screen overlay. */
public record ScreenCompositionDiagnostic(
        ScreenCompositionDiagnosticCode code,
        Optional<PluginId> customizationPluginId,
        Optional<ContributionId> overlayId,
        Optional<ScreenId> screenId,
        String subject) implements Comparable<ScreenCompositionDiagnostic> {

    public ScreenCompositionDiagnostic {
        Objects.requireNonNull(code, "code");
        customizationPluginId = Objects.requireNonNull(customizationPluginId, "customizationPluginId");
        overlayId = Objects.requireNonNull(overlayId, "overlayId");
        screenId = Objects.requireNonNull(screenId, "screenId");
        Objects.requireNonNull(subject, "subject");
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
    }

    public static ScreenCompositionDiagnostic company(String subject) {
        return new ScreenCompositionDiagnostic(
                ScreenCompositionDiagnosticCode.COMPANY_NOT_OPERATIONAL,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                subject);
    }

    public static ScreenCompositionDiagnostic overlay(
            ScreenCompositionDiagnosticCode code,
            PluginId customizationPluginId,
            ContributionId overlayId,
            ScreenId screenId,
            String subject) {
        return new ScreenCompositionDiagnostic(
                code,
                Optional.of(customizationPluginId),
                Optional.of(overlayId),
                Optional.of(screenId),
                subject);
    }

    @Override
    public int compareTo(ScreenCompositionDiagnostic other) {
        Objects.requireNonNull(other, "other");
        int result = code.compareTo(other.code);
        if (result == 0) {
            result = customizationPluginId.map(PluginId::value).orElse("")
                    .compareTo(other.customizationPluginId.map(PluginId::value).orElse(""));
        }
        if (result == 0) {
            result = overlayId.map(ContributionId::value).orElse("")
                    .compareTo(other.overlayId.map(ContributionId::value).orElse(""));
        }
        if (result == 0) {
            result = screenId.map(ScreenId::toString).orElse("")
                    .compareTo(other.screenId.map(ScreenId::toString).orElse(""));
        }
        return result != 0 ? result : subject.compareTo(other.subject);
    }
}
