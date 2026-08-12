package py.com.logixone.plugins.purchasing.infrastructure.ui;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.purchasing.PurchasingPluginDefinition;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationContext;
import py.com.logixone.plugins.purchasing.application.PurchasingOperationResult;
import py.com.logixone.plugins.purchasing.application.PurchasingResultCode;

final class PurchasingScreenSupport {
    static final String ALL = "ALL";
    static final int PAGE_SIZE = 20;

    private PurchasingScreenSupport() {
    }

    static PurchasingOperationContext context(
            CurrentCompanyAuthorization authorization, ContributionId permission) {
        return PurchasingOperationContext.from(authorization.require(
                PurchasingPluginDefinition.ID.value(), permission.value()));
    }

    static Map<ScreenElementId, String> copy(Map<ScreenElementId, String> values) {
        return new HashMap<>(values);
    }

    static String required(Map<ScreenElementId, String> values, ScreenElementId field) {
        return optional(values, field).orElseThrow(
                () -> new IllegalArgumentException("Missing required screen value"));
    }

    static Optional<String> optional(
            Map<ScreenElementId, String> values, ScreenElementId field) {
        return Optional.ofNullable(values.get(field)).map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    static Optional<String> filter(
            Map<ScreenElementId, String> values, ScreenElementId field) {
        return optional(values, field).filter(value -> !ALL.equals(value));
    }

    static BigDecimal decimal(Map<ScreenElementId, String> values, ScreenElementId field) {
        return new BigDecimal(required(values, field));
    }

    static Optional<BigDecimal> optionalDecimal(
            Map<ScreenElementId, String> values, ScreenElementId field) {
        return optional(values, field).map(BigDecimal::new);
    }

    static LocalDate date(Map<ScreenElementId, String> values, ScreenElementId field) {
        return LocalDate.parse(required(values, field));
    }

    static Optional<LocalDate> optionalDate(
            Map<ScreenElementId, String> values, ScreenElementId field) {
        return optional(values, field).map(LocalDate::parse);
    }

    static <E extends Enum<E>> E enumValue(
            Map<ScreenElementId, String> values, ScreenElementId field, Class<E> type) {
        return Enum.valueOf(type, required(values, field));
    }

    static <E extends Enum<E>> Optional<E> filterEnum(
            Map<ScreenElementId, String> values, ScreenElementId field, Class<E> type) {
        return filter(values, field).map(value -> Enum.valueOf(type, value));
    }

    static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    static String operationKey(
            PurchasingOperationContext context, ScreenElementId action, String... values) {
        String canonical = context.companyContext().companyId() + "|"
                + context.companyContext().actor().userId() + "|" + action.value() + "|"
                + String.join("|", values);
        return "ui-" + UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    static UUID stableId(String operationKey, String role) {
        return UUID.nameUUIDFromBytes((operationKey + "|" + role)
                .getBytes(StandardCharsets.UTF_8));
    }

    static <T> Mutation mutation(
            PurchasingOperationResult<T> result, String success,
            Function<T, String> resourceId, Optional<String> fallback) {
        if (!result.successful()) {
            return new Mutation(fallback,
                    List.of(error("No se pudo completar la operación",
                            failureMessage(result.code()))), false);
        }
        T value = result.value().orElseThrow();
        return new Mutation(Optional.of(resourceId.apply(value)),
                List.of(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.SUCCESS, success,
                        "El cambio fue confirmado y auditado por el servidor.")), true);
    }

    static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.ERROR, summary, detail);
    }

    static String failureMessage(PurchasingResultCode code) {
        return switch (code) {
            case ACCESS_DENIED -> "No tienes el permiso requerido para esta operación.";
            case NOT_FOUND -> "El documento ya no existe o no pertenece a la empresa activa.";
            case VERSION_CONFLICT -> "Otra operación modificó el documento; vuelve a cargarlo.";
            case DUPLICATE -> "Ya existe un documento con el mismo número o identidad.";
            case REFERENCE_CONFLICT -> "El proveedor, artículo, moneda o referencia dejó de ser válido.";
            case IDEMPOTENCY_CONFLICT -> "La operación ya fue utilizada con otros datos.";
            case IMMUTABLE_DOCUMENT -> "El documento confirmado no admite esa modificación.";
            case INVENTORY_FAILURE -> "Inventario rechazó el movimiento; no se confirmó ningún cambio.";
            case INVALID_OPERATION -> "El estado actual o los datos no permiten esta operación.";
            case STORAGE_FAILURE -> "No fue posible confirmar el cambio en el almacenamiento.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure");
        };
    }

    record Mutation(
            Optional<String> selectedResourceId,
            List<ScreenInteraction.Notice> notices,
            boolean successful) {
        Mutation {
            selectedResourceId = Optional.ofNullable(selectedResourceId).orElse(Optional.empty());
            notices = List.copyOf(notices);
        }
    }
}
