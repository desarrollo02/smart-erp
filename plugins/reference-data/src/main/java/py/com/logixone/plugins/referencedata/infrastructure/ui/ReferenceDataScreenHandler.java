package py.com.logixone.plugins.referencedata.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.api.security.AuthorizedCompanyOperation;
import py.com.logixone.kernel.api.security.CurrentCompanyAuthorization;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenInteraction;
import py.com.logixone.plugins.referencedata.ReferenceDataPluginDefinition;
import py.com.logixone.plugins.referencedata.ReferenceDataScreenContract;
import py.com.logixone.plugins.referencedata.api.CountryReference;
import py.com.logixone.plugins.referencedata.api.CurrencyReference;
import py.com.logixone.plugins.referencedata.api.ReferenceDataCatalog;
import py.com.logixone.plugins.referencedata.api.ReferenceDataDirectory;
import py.com.logixone.plugins.referencedata.api.ReferenceDataQuery;
import py.com.logixone.plugins.referencedata.api.ReferenceDataRelease;
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;
import py.com.logixone.plugins.referencedata.application.policy.ChangeReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicy;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyResult;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyRevision;
import py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicyUseCases;

/** Authorized presentation adapter; normative publications remain deployment controlled. */
@ApplicationScoped
public class ReferenceDataScreenHandler implements ScreenInteraction.Handler {

    private static final int PAGE_SIZE = 50;

    @Inject
    ReferenceDataDirectory directory;

    @Inject
    ReferenceDataPolicyUseCases policies;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return ReferenceDataScreenContract.CATALOGS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        Map<ScreenElementId, String> inputs = new LinkedHashMap<>(request.inputs());
        inputs.putIfAbsent(ReferenceDataScreenContract.SEARCH_TEXT, "");
        inputs.putIfAbsent(
                ReferenceDataScreenContract.SEARCH_CATALOG,
                ReferenceDataCatalog.COUNTRY.name());
        List<ScreenInteraction.Notice> notices = new ArrayList<>();
        Optional<ReferenceKey> selected = request.selectedResourceId().map(ReferenceKey::parse);

        if (request.actionId().filter(ReferenceDataScreenContract.SEARCH::equals).isPresent()) {
            selected = Optional.empty();
        } else if (request.actionId().filter(ReferenceDataScreenContract.SELECT_REFERENCE::equals)
                .isPresent()) {
            selected = Optional.of(selected.orElseThrow(
                    () -> new IllegalArgumentException("A reference selection is required")));
        } else if (request.actionId().filter(ReferenceDataScreenContract.ENABLE_REFERENCE::equals)
                .isPresent()) {
            selected = change(request, selected, true, notices);
        } else if (request.actionId().filter(ReferenceDataScreenContract.DISABLE_REFERENCE::equals)
                .isPresent()) {
            selected = change(request, selected, false, notices);
        } else if (request.actionId().isPresent()) {
            throw new IllegalArgumentException("Unsupported reference-data action");
        }
        return load(inputs, selected, notices, request.tablePage());
    }

    private Optional<ReferenceKey> change(
            ScreenInteraction.Request request,
            Optional<ReferenceKey> selected,
            boolean enabled,
            List<ScreenInteraction.Notice> notices) {
        ReferenceKey key = selected.orElseThrow(
                () -> new IllegalArgumentException("A reference selection is required"));
        long version = request.selectedResourceVersion().orElseThrow(
                () -> new IllegalArgumentException("A reference policy version is required"));
        AuthorizedCompanyOperation manage = authorization.require(
                ReferenceDataPluginDefinition.ID.value(),
                ReferenceDataPermissions.POLICY_MANAGE.value());
        var result = policies.change(
                manage,
                new ChangeReferenceDataPolicy(key.catalog(), key.code(), enabled, version));
        if (!result.successful()) {
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.ERROR,
                    "No se pudo cambiar la política",
                    failureMessage(result.code())));
            return Optional.of(key);
        }
        ReferenceDataPolicy changed = result.value().orElseThrow();
        notices.add(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.SUCCESS,
                enabled ? "Referencia habilitada" : "Referencia inhabilitada",
                changed.version() == version
                        ? "La política ya se encontraba en ese estado."
                        : "El nuevo estado y su versión quedaron auditados para la empresa activa."));
        return Optional.of(key);
    }

    private ScreenInteraction.Result load(
            Map<ScreenElementId, String> inputs,
            Optional<ReferenceKey> requestedSelection,
            List<ScreenInteraction.Notice> notices,
            Optional<ScreenInteraction.TablePageRequest> requestedPage) {
        AuthorizedCompanyOperation access = authorization.require(
                ReferenceDataPluginDefinition.ID.value(),
                ReferenceDataPermissions.POLICY_MANAGE.value());
        CompanyId companyId = access.context().companyId();
        ReferenceDataRelease countryRelease =
                directory.currentRelease(companyId, ReferenceDataCatalog.COUNTRY);
        ReferenceDataRelease currencyRelease =
                directory.currentRelease(companyId, ReferenceDataCatalog.CURRENCY);
        Optional<ReferenceEntry> selected = requestedSelection.flatMap(
                key -> find(companyId, key));
        if (requestedSelection.isPresent() && selected.isEmpty()) {
            notices.add(new ScreenInteraction.Notice(
                    ScreenInteraction.NoticeLevel.ERROR,
                    "Referencia no disponible",
                    "El código no pertenece a la publicación corriente."));
        }

        Optional<ReferenceDataPolicy> policy = selected.map(entry -> required(
                policies.current(access, entry.key().catalog(), entry.key().code()),
                "current reference-data policy"));
        ScreenInteraction.Table table;
        if (selected.isPresent()) {
            ReferenceEntry entry = selected.orElseThrow();
            List<ReferenceDataPolicyRevision> history = required(
                    policies.history(access, entry.key().catalog(), entry.key().code()),
                    "reference-data policy history");
            table = historyTable(history, policy.orElseThrow().version());
        } else {
            ReferenceDataCatalog catalog = ReferenceDataCatalog.valueOf(
                    inputs.get(ReferenceDataScreenContract.SEARCH_CATALOG));
            ScreenInteraction.TablePageRequest page = requestedPage.orElse(
                    new ScreenInteraction.TablePageRequest(0, PAGE_SIZE));
            ReferenceDataQuery query = new ReferenceDataQuery(
                    inputs.getOrDefault(ReferenceDataScreenContract.SEARCH_TEXT, ""),
                    page.offset(),
                    page.limit(),
                    false);
            List<ReferenceEntry> entries = new ArrayList<>();
            long total;
            int offset;
            if (catalog == ReferenceDataCatalog.COUNTRY) {
                var countries = directory.searchCountries(companyId, query);
                countries.entries().forEach(value -> entries.add(ReferenceEntry.country(value)));
                total = countries.total();
                offset = countries.offset();
            } else {
                var currencies = directory.searchCurrencies(companyId, query);
                currencies.entries().forEach(value -> entries.add(ReferenceEntry.currency(value)));
                total = currencies.total();
                offset = currencies.offset();
            }
            table = directoryTable(entries, total, offset, page.limit());
        }

        String detail = "Países: " + releaseSummary(countryRelease)
                + ". Monedas: " + releaseSummary(currencyRelease) + ".";
        notices.addFirst(new ScreenInteraction.Notice(
                ScreenInteraction.NoticeLevel.INFO,
                "Procedencia normativa verificada",
                detail));
        return new ScreenInteraction.Result(
                inputs,
                Map.of(ReferenceDataScreenContract.SEARCH_CATALOG, List.of(
                        new ScreenInteraction.Option(ReferenceDataCatalog.COUNTRY.name(), "Países"),
                        new ScreenInteraction.Option(ReferenceDataCatalog.CURRENCY.name(), "Monedas"))),
                Optional.of(table),
                selected.map(entry -> detail(entry, policy.orElseThrow())),
                notices,
                selected.map(entry -> entry.key().resourceId()),
                policy.map(ReferenceDataPolicy::version));
    }

    private Optional<ReferenceEntry> find(CompanyId companyId, ReferenceKey key) {
        if (key.catalog() == ReferenceDataCatalog.COUNTRY) {
            return directory.findCountry(companyId, new py.com.logixone.plugins.referencedata.api.CountryCode(
                            key.code()))
                    .map(ReferenceEntry::country);
        }
        return directory.findCurrency(companyId, new py.com.logixone.plugins.referencedata.api.CurrencyCode(
                        key.code()))
                .map(ReferenceEntry::currency);
    }

    private static ScreenInteraction.Table directoryTable(
            List<ReferenceEntry> entries,
            long total,
            int offset,
            int limit) {
        return new ScreenInteraction.Table(
                ReferenceDataScreenContract.RESULTS,
                List.of(
                        new ScreenInteraction.Column("catalog", "Catálogo"),
                        new ScreenInteraction.Column("code", "Código"),
                        new ScreenInteraction.Column("name", "Nombre de referencia"),
                        new ScreenInteraction.Column("numeric", "Numérico"),
                        new ScreenInteraction.Column("release", "Publicación"),
                        new ScreenInteraction.Column("state", "Empresa")),
                entries.stream().map(ReferenceDataScreenHandler::row).toList(),
                total,
                "No hay datos de referencia",
                "Ajusta el filtro o verifica la publicación corriente del plugin.",
                Optional.of(new ScreenInteraction.TablePage(offset, limit)));
    }

    private static ScreenInteraction.Table historyTable(
            List<ReferenceDataPolicyRevision> revisions, long currentVersion) {
        return new ScreenInteraction.Table(
                ReferenceDataScreenContract.HISTORY,
                List.of(
                        new ScreenInteraction.Column("version", "Versión"),
                        new ScreenInteraction.Column("condition", "Condición"),
                        new ScreenInteraction.Column("state", "Estado"),
                        new ScreenInteraction.Column("changed_at", "Registrada")),
                revisions.stream().map(revision -> new ScreenInteraction.Row(
                        revision.catalog().name() + ":" + revision.code() + ":" + revision.version(),
                        List.of(
                                Long.toString(revision.version()),
                                revision.version() == currentVersion ? "Actual" : "Histórica",
                                revision.enabled() ? "Habilitada" : "Inhabilitada",
                                revision.changedAt().toString()))).toList(),
                revisions.size(),
                "Sin cambios empresariales",
                "La referencia conserva el estado habilitado de la publicación normativa.");
    }

    private static ScreenInteraction.Detail detail(
            ReferenceEntry entry, ReferenceDataPolicy policy) {
        return new ScreenInteraction.Detail(
                entry.key().resourceId(),
                entry.displayName(),
                List.of(
                        new ScreenInteraction.DetailItem("Catálogo", entry.catalogLabel()),
                        new ScreenInteraction.DetailItem("Código", entry.codeLabel()),
                        new ScreenInteraction.DetailItem("Código numérico", entry.numericCode()),
                        new ScreenInteraction.DetailItem("Publicación", entry.releaseId()),
                        new ScreenInteraction.DetailItem(
                                "Estado empresarial", policy.enabled() ? "Habilitada" : "Inhabilitada"),
                        new ScreenInteraction.DetailItem("Versión", Long.toString(policy.version()))));
    }

    private static ScreenInteraction.Row row(ReferenceEntry entry) {
        return new ScreenInteraction.Row(entry.key().resourceId(), List.of(
                entry.catalogLabel(),
                entry.codeLabel(),
                entry.displayName(),
                entry.numericLabel(),
                entry.releaseId(),
                entry.enabled() ? "Habilitada" : "Inhabilitada"));
    }

    private static <T> T required(ReferenceDataPolicyResult<T> result, String operation) {
        if (!result.successful()) {
            throw new IllegalStateException("Authorized " + operation + " failed: " + result.code());
        }
        return result.value().orElseThrow();
    }

    private static String failureMessage(ReferenceDataPolicyResult.Code code) {
        return switch (code) {
            case ACCESS_DENIED -> "No tienes permiso para administrar esta política.";
            case NOT_FOUND -> "El código no pertenece a la publicación corriente.";
            case VERSION_CONFLICT -> "La política cambió en otra operación; vuelve a abrirla.";
            case SUCCESS -> "La operación no informó un resultado válido.";
        };
    }

    private static String releaseSummary(ReferenceDataRelease release) {
        return release.releaseId() + " · " + release.completeness()
                + " · SHA-256 " + release.sourceSha256();
    }

    private record ReferenceKey(ReferenceDataCatalog catalog, String code) {

        private ReferenceKey {
            code = py.com.logixone.plugins.referencedata.application.policy.ReferenceDataPolicy
                    .canonicalCode(catalog, code);
        }

        private static ReferenceKey parse(String value) {
            String[] parts = value.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid reference resource id");
            }
            return new ReferenceKey(ReferenceDataCatalog.valueOf(parts[0]), parts[1]);
        }

        private String resourceId() {
            return catalog.name() + ":" + code;
        }
    }

    private record ReferenceEntry(
            ReferenceKey key,
            String catalogLabel,
            String codeLabel,
            String displayName,
            String numericCode,
            String numericLabel,
            String releaseId,
            boolean enabled) {

        private static ReferenceEntry country(CountryReference value) {
            return new ReferenceEntry(
                    new ReferenceKey(ReferenceDataCatalog.COUNTRY, value.code().value()),
                    "País ISO 3166-1 / UN M49",
                    value.code().value() + " / " + value.alpha3Code(),
                    value.displayName(),
                    value.numericCode(),
                    value.numericCode(),
                    value.releaseId(),
                    value.enabled());
        }

        private static ReferenceEntry currency(CurrencyReference value) {
            String minorUnit = value.minorUnitIfDefined().isPresent()
                    ? value.minorUnitIfDefined().getAsInt() + " dec."
                    : "unidad menor N.A.";
            return new ReferenceEntry(
                    new ReferenceKey(ReferenceDataCatalog.CURRENCY, value.code().value()),
                    "Moneda ISO 4217",
                    value.code().value(),
                    value.displayName(),
                    value.numericCode(),
                    value.numericCode() + " · " + minorUnit,
                    value.releaseId(),
                    value.enabled());
        }
    }
}
