package py.com.logixone.plugins.referencedata.infrastructure.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import py.com.logixone.kernel.api.company.CompanyId;
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
import py.com.logixone.plugins.referencedata.api.ReferenceDataRelease;
import py.com.logixone.plugins.referencedata.application.ReferenceDataPermissions;

/** Read-only presentation adapter; publication changes never come from browser input. */
@ApplicationScoped
public class ReferenceDataScreenHandler implements ScreenInteraction.Handler {

    @Inject
    ReferenceDataDirectory directory;

    @Inject
    CurrentCompanyAuthorization authorization;

    @Override
    public ScreenId screenId() {
        return ReferenceDataScreenContract.CATALOGS;
    }

    @Override
    public ScreenInteraction.Result interact(ScreenInteraction.Request request) {
        if (request.actionId().isPresent()) {
            throw new IllegalArgumentException("Reference data screen is read-only");
        }
        CompanyId companyId = authorization.require(
                        ReferenceDataPluginDefinition.ID.value(),
                        ReferenceDataPermissions.VIEW.value())
                .context().companyId();
        ReferenceDataRelease countryRelease =
                directory.currentRelease(companyId, ReferenceDataCatalog.COUNTRY);
        ReferenceDataRelease currencyRelease =
                directory.currentRelease(companyId, ReferenceDataCatalog.CURRENCY);

        List<ScreenInteraction.Row> rows = new ArrayList<>();
        directory.countries(companyId).forEach(country -> rows.add(countryRow(country)));
        directory.currencies(companyId).forEach(currency -> rows.add(currencyRow(currency)));

        String detail = "Países: " + releaseSummary(countryRelease)
                + ". Monedas: " + releaseSummary(currencyRelease) + ".";
        return new ScreenInteraction.Result(
                request.inputs(),
                Map.of(),
                Optional.of(new ScreenInteraction.Table(
                        ReferenceDataScreenContract.RESULTS,
                        List.of(
                                new ScreenInteraction.Column("catalog", "Catálogo"),
                                new ScreenInteraction.Column("code", "Código"),
                                new ScreenInteraction.Column("name", "Nombre de referencia"),
                                new ScreenInteraction.Column("numeric", "Numérico"),
                                new ScreenInteraction.Column("release", "Publicación"),
                                new ScreenInteraction.Column("state", "Empresa")),
                        rows,
                        rows.size(),
                        "No hay datos de referencia",
                        "Verifica la migración y la publicación corriente del plugin.")),
                Optional.empty(),
                List.of(new ScreenInteraction.Notice(
                        ScreenInteraction.NoticeLevel.INFO,
                        "Procedencia normativa verificada",
                        detail)),
                Optional.empty(),
                Optional.empty());
    }

    private static ScreenInteraction.Row countryRow(CountryReference value) {
        return new ScreenInteraction.Row("COUNTRY:" + value.code(), List.of(
                "País ISO 3166-1 / UN M49",
                value.code().value() + " / " + value.alpha3Code(),
                value.displayName(),
                value.numericCode(),
                value.releaseId(),
                value.enabled() ? "Habilitado" : "Inhabilitado"));
    }

    private static ScreenInteraction.Row currencyRow(CurrencyReference value) {
        return new ScreenInteraction.Row("CURRENCY:" + value.code(), List.of(
                "Moneda ISO 4217",
                value.code().value(),
                value.displayName(),
                value.numericCode() + " · " + value.minorUnit() + " dec.",
                value.releaseId(),
                value.enabled() ? "Habilitada" : "Inhabilitada"));
    }

    private static String releaseSummary(ReferenceDataRelease release) {
        return release.releaseId() + " · " + release.completeness()
                + " · SHA-256 " + release.sourceSha256();
    }
}
