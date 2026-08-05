package py.com.logixone.plugins.inventory.infrastructure.ui;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.inventory.InventoryPluginDefinition;
import py.com.logixone.plugins.inventory.application.InventoryOperationContext;
import py.com.logixone.plugins.inventory.application.InventoryOperationResult;
import py.com.logixone.plugins.inventory.application.InventoryResultCode;

/** Shared, module-local parsing and presentation rules for inventory screens. */
final class InventoryScreenSupport {
    static final int PAGE_SIZE = 20;
    static final String ALL = "ALL";

    private InventoryScreenSupport() {
    }

    static InventoryOperationContext context(
            CurrentCompanyAuthorization authorization, ContributionId permission) {
        return InventoryOperationContext.from(authorization.require(
                InventoryPluginDefinition.ID.value(), permission.value()));
    }

    static Map<ScreenElementId, String> copy(Map<ScreenElementId, String> submitted) {
        return new HashMap<>(submitted);
    }

    static String required(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).orElseThrow(
                () -> new IllegalArgumentException("Missing required screen value"));
    }

    static Optional<String> optional(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Optional.ofNullable(inputs.get(field))
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    static Optional<String> filter(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).filter(value -> !ALL.equals(value));
    }

    static <E extends Enum<E>> E enumValue(
            Map<ScreenElementId, String> inputs, ScreenElementId field, Class<E> type) {
        return Enum.valueOf(type, required(inputs, field));
    }

    static <E extends Enum<E>> Optional<E> filterEnum(
            Map<ScreenElementId, String> inputs, ScreenElementId field, Class<E> type) {
        return filter(inputs, field).map(value -> Enum.valueOf(type, value));
    }

    static BigDecimal decimal(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return new BigDecimal(required(inputs, field));
    }

    static long longValue(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Long.parseLong(required(inputs, field));
    }

    static Optional<LocalDate> date(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return optional(inputs, field).map(LocalDate::parse);
    }

    static Instant instant(Map<ScreenElementId, String> inputs, ScreenElementId field) {
        return Instant.parse(required(inputs, field));
    }

    static ScreenInteraction.Option option(String value, String label) {
        return new ScreenInteraction.Option(value, label);
    }

    static Optional<String> first(
            Map<ScreenElementId, List<ScreenInteraction.Option>> options, ScreenElementId field) {
        return options.getOrDefault(field, List.of()).stream()
                .findFirst()
                .map(ScreenInteraction.Option::value);
    }

    static void clear(Map<ScreenElementId, String> inputs, ScreenElementId... fields) {
        for (ScreenElementId field : fields) {
            inputs.remove(field);
        }
    }

    static ScreenInteraction.Notice error(String summary, String detail) {
        return new ScreenInteraction.Notice(ScreenInteraction.NoticeLevel.ERROR, summary, detail);
    }

    static ScreenInteraction.Notice success(String summary) {
        return new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                summary,
                "El cambio fue confirmado y auditado por el servidor.");
    }

    static <T> Mutation mutation(
            InventoryOperationResult<T> result,
            String summary,
            Function<T, String> resourceId,
            Optional<String> fallbackId) {
        if (!result.successful()) {
            return new Mutation(
                    fallbackId,
                    List.of(error("No se pudo completar la operación", failureMessage(result.code()))),
                    false);
        }
        return new Mutation(
                Optional.of(resourceId.apply(result.value().orElseThrow())),
                List.of(success(summary)),
                true);
    }

    static String failureMessage(InventoryResultCode code) {
        return switch (code) {
            case ACCESS_DENIED -> "No tienes el permiso requerido para esta operación.";
            case NOT_FOUND -> "El registro ya no existe o no pertenece a la empresa activa.";
            case VERSION_CONFLICT -> "Otra operación modificó el registro; vuelve a cargarlo.";
            case DUPLICATE -> "Ya existe un registro con la misma identidad o código.";
            case REFERENCE_CONFLICT -> "La referencia externa ya no está disponible o no es válida.";
            case SCOPE_LOCKED -> "Existe un conteo físico que bloquea temporalmente este alcance.";
            case INSUFFICIENT_STOCK -> "La cantidad disponible no alcanza para completar la operación.";
            case IDEMPOTENCY_CONFLICT -> "La clave de operación ya fue utilizada con otros datos.";
            case INVALID_OPERATION -> "El estado actual o los datos no permiten esta operación.";
            case STORAGE_FAILURE -> "No fue posible confirmar el cambio en el almacenamiento.";
            case SUCCESS -> throw new IllegalArgumentException("SUCCESS is not a failure code");
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
