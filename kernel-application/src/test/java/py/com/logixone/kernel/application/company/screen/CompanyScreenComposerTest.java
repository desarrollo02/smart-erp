package py.com.logixone.kernel.application.company.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.kernel.application.company.CompanyPluginQueryService;
import py.com.logixone.kernel.application.company.contribution.CompanyContributionService;
import py.com.logixone.kernel.application.company.port.CompanyRepository;
import py.com.logixone.kernel.application.company.port.PluginActivationRepository;
import py.com.logixone.kernel.application.plugin.PluginRegistry;
import py.com.logixone.kernel.domain.company.Company;
import py.com.logixone.kernel.domain.company.CompanyPluginResolver;
import py.com.logixone.kernel.domain.company.CompanyStatus;
import py.com.logixone.kernel.domain.company.PluginActivationDecision;
import py.com.logixone.kernel.domain.company.PluginActivationState;
import py.com.logixone.plugin.api.ContributionId;
import py.com.logixone.plugin.api.DependencyKind;
import py.com.logixone.plugin.api.PluginDefinition;
import py.com.logixone.plugin.api.PluginDependency;
import py.com.logixone.plugin.api.PluginDescriptor;
import py.com.logixone.plugin.api.PluginId;
import py.com.logixone.plugin.api.PluginKind;
import py.com.logixone.plugin.api.ScreenChange;
import py.com.logixone.plugin.api.ScreenActionDefinition;
import py.com.logixone.plugin.api.ScreenActionEmphasis;
import py.com.logixone.plugin.api.ScreenActionIntent;
import py.com.logixone.plugin.api.ScreenConfirmationMode;
import py.com.logixone.plugin.api.ScreenCustomizationOperation;
import py.com.logixone.plugin.api.ScreenDefinition;
import py.com.logixone.plugin.api.ScreenElementDefinition;
import py.com.logixone.plugin.api.ScreenElementId;
import py.com.logixone.plugin.api.ScreenElementType;
import py.com.logixone.plugin.api.ScreenExperienceDefinition;
import py.com.logixone.plugin.api.ScreenFragmentId;
import py.com.logixone.plugin.api.ScreenId;
import py.com.logixone.plugin.api.ScreenOverlay;
import py.com.logixone.plugin.api.ScreenPurpose;
import py.com.logixone.plugin.api.ScreenRegionDefinition;
import py.com.logixone.plugin.api.ScreenRegionId;
import py.com.logixone.plugin.api.ScreenRegionRole;
import py.com.logixone.plugin.api.ScreenSemanticType;
import py.com.logixone.plugin.api.ScreenSlotDefinition;
import py.com.logixone.plugin.api.ScreenSlotId;
import py.com.logixone.plugin.api.ScreenTextKey;
import py.com.logixone.plugin.api.SemanticVersion;
import py.com.logixone.plugin.api.VersionRange;

class CompanyScreenComposerTest {

    private static final CompanyId COMPANY_A = new CompanyId(new UUID(0, 201));
    private static final CompanyId COMPANY_B = new CompanyId(new UUID(0, 202));
    private static final PluginId FUNCTIONAL = new PluginId("functional");
    private static final PluginId CUSTOM_A = new PluginId("custom_a");
    private static final PluginId CUSTOM_B = new PluginId("custom_b");
    private static final ScreenId DASHBOARD = new ScreenId(FUNCTIONAL, "dashboard");
    private static final ScreenElementId CUSTOMER = new ScreenElementId("customer");
    private static final ScreenElementId AMOUNT = new ScreenElementId("amount");
    private static final ScreenElementId SUBMIT = new ScreenElementId("submit");
    private static final ScreenSlotId SUMMARY = new ScreenSlotId("summary");
    private static final VersionRange API_RANGE = range("0.4.0", "0.5.0");
    private static final VersionRange VERSION_ONE = range("1.0.0", "2.0.0");

    @Test
    void appliesEveryAuthorizedOperationAndPreservesStricterBaseRules() {
        ScreenOverlay overlay = overlay(
                CUSTOM_A,
                "custom_a.dashboard",
                VERSION_ONE,
                List.of(
                        new ScreenChange.Label(CUSTOMER, text("custom_a.customer")),
                        new ScreenChange.Help(CUSTOMER, text("custom_a.customer.help")),
                        new ScreenChange.Hide(AMOUNT),
                        new ScreenChange.Disable(SUBMIT),
                        new ScreenChange.Require(CUSTOMER),
                        new ScreenChange.Move(CUSTOMER, 0),
                        new ScreenChange.SlotContent(
                                SUMMARY, new ScreenFragmentId(CUSTOM_A, "tax_notice"), 0)));
        CompanyScreenComposition result = activeService(
                COMPANY_A, CUSTOM_A, List.of(functional(screen("1.0.0")), customization(CUSTOM_A, overlay)))
                .compose(COMPANY_A);

        assertTrue(result.operational());
        assertTrue(result.diagnostics().isEmpty());
        ComposedScreen screen = result.screens().getFirst();
        ComposedScreenElement customer = element(screen, CUSTOMER);
        ComposedScreenElement amount = element(screen, AMOUNT);
        ComposedScreenElement submit = element(screen, SUBMIT);
        assertEquals(0, customer.position());
        assertEquals("custom_a.customer", customer.labelKey().value());
        assertEquals("custom_a.customer.help", customer.helpKey().orElseThrow().value());
        assertTrue(customer.required());
        assertEquals(1, amount.position());
        assertFalse(amount.visible());
        assertTrue(amount.required(), "an existing required rule must remain strict");
        assertFalse(submit.enabled());
        assertEquals(
                List.of(new ScreenFragmentId(CUSTOM_A, "tax_notice")),
                screen.slotContents().stream().map(ComposedSlotContent::fragmentId).toList());
    }

    @Test
    void isolatesTwoCompanyOverlaysOnTheSamePhysicalCatalogDeterministically() {
        ScreenOverlay overlayA = overlay(
                CUSTOM_A,
                "custom_a.dashboard",
                VERSION_ONE,
                List.of(new ScreenChange.Label(CUSTOMER, text("custom_a.customer"))));
        ScreenOverlay overlayB = overlay(
                CUSTOM_B,
                "custom_b.dashboard",
                VERSION_ONE,
                List.of(
                        new ScreenChange.Label(CUSTOMER, text("custom_b.customer")),
                        new ScreenChange.Hide(CUSTOMER)));
        Repositories repositories = new Repositories();
        repositories.addCompany(activeCompany(COMPANY_A, CUSTOM_A));
        repositories.addCompany(activeCompany(COMPANY_B, CUSTOM_B));
        repositories.addActivation(enabled(COMPANY_A, FUNCTIONAL));
        repositories.addActivation(enabled(COMPANY_B, FUNCTIONAL));
        List<PluginDefinition> definitions = List.of(
                definition(customization(CUSTOM_B, overlayB)),
                definition(functional(screen("1.0.0"))),
                definition(customization(CUSTOM_A, overlayA)));
        CompanyScreenService service = service(repositories, definitions);

        CompanyScreenComposition first = service.compose(COMPANY_A);
        CompanyScreenComposition second = service.compose(COMPANY_B);
        CompanyScreenService permuted = service(
                repositories,
                List.of(
                        definition(customization(CUSTOM_A, overlayA)),
                        definition(functional(screen("1.0.0"))),
                        definition(customization(CUSTOM_B, overlayB))));

        assertEquals("custom_a.customer", element(first.screens().getFirst(), CUSTOMER).labelKey().value());
        assertTrue(element(first.screens().getFirst(), CUSTOMER).visible());
        assertEquals("custom_b.customer", element(second.screens().getFirst(), CUSTOMER).labelKey().value());
        assertFalse(element(second.screens().getFirst(), CUSTOMER).visible());
        assertEquals(first, permuted.compose(COMPANY_A));
    }

    @Test
    void rejectsMissingTargetAndIncompatibleVersionWithoutReturningStandardScreens() {
        ScreenOverlay missing = new ScreenOverlay(
                new ContributionId("custom_a.missing"),
                new ScreenId(FUNCTIONAL, "missing"),
                VERSION_ONE,
                List.of(new ScreenChange.Hide(CUSTOMER)));
        ScreenOverlay incompatible = overlay(
                CUSTOM_A,
                "custom_a.incompatible",
                range("2.0.0", "3.0.0"),
                List.of(new ScreenChange.Hide(CUSTOMER)));
        CompanyScreenComposition result = activeService(
                COMPANY_A,
                CUSTOM_A,
                List.of(functional(screen("1.0.0")), customization(CUSTOM_A, missing, incompatible)))
                .compose(COMPANY_A);

        assertRejected(
                result,
                ScreenCompositionDiagnosticCode.SCREEN_TARGET_NOT_FOUND,
                ScreenCompositionDiagnosticCode.SCREEN_VERSION_INCOMPATIBLE);
    }

    @Test
    void rejectsUnknownReferencesForeignFragmentsAndForbiddenOperationsAtomically() {
        ScreenOverlay overlay = overlay(
                CUSTOM_A,
                "custom_a.invalid_references",
                VERSION_ONE,
                List.of(
                        new ScreenChange.Label(new ScreenElementId("unknown"), text("custom_a.unknown")),
                        new ScreenChange.Hide(SUBMIT),
                        new ScreenChange.SlotContent(
                                new ScreenSlotId("unknown_slot"),
                                new ScreenFragmentId(CUSTOM_A, "content"),
                                0),
                        new ScreenChange.SlotContent(
                                SUMMARY,
                                new ScreenFragmentId(CUSTOM_B, "foreign_content"),
                                0)));
        CompanyScreenComposition result = activeService(
                COMPANY_A, CUSTOM_A, List.of(functional(screen("1.0.0")), customization(CUSTOM_A, overlay)))
                .compose(COMPANY_A);

        assertRejected(
                result,
                ScreenCompositionDiagnosticCode.SCREEN_ELEMENT_NOT_FOUND,
                ScreenCompositionDiagnosticCode.SCREEN_FRAGMENT_OWNER_MISMATCH,
                ScreenCompositionDiagnosticCode.SCREEN_OPERATION_NOT_ALLOWED,
                ScreenCompositionDiagnosticCode.SCREEN_SLOT_NOT_FOUND);
    }

    @Test
    void rejectsConflictsPositionsAndSlotCapacityAsOneAtomicOverlay() {
        ScreenOverlay overlay = overlay(
                CUSTOM_A,
                "custom_a.conflicts",
                VERSION_ONE,
                List.of(
                        new ScreenChange.Label(CUSTOMER, text("custom_a.first")),
                        new ScreenChange.Label(CUSTOMER, text("custom_a.second")),
                        new ScreenChange.Move(CUSTOMER, 9),
                        new ScreenChange.SlotContent(
                                SUMMARY, new ScreenFragmentId(CUSTOM_A, "first"), 0),
                        new ScreenChange.SlotContent(
                                SUMMARY, new ScreenFragmentId(CUSTOM_A, "second"), 0)));
        CompanyScreenComposition result = activeService(
                COMPANY_A, CUSTOM_A, List.of(functional(screen("1.0.0")), customization(CUSTOM_A, overlay)))
                .compose(COMPANY_A);

        assertRejected(
                result,
                ScreenCompositionDiagnosticCode.SCREEN_CHANGE_CONFLICT,
                ScreenCompositionDiagnosticCode.SCREEN_POSITION_OUT_OF_RANGE,
                ScreenCompositionDiagnosticCode.SCREEN_SLOT_CAPACITY_EXCEEDED);
    }

    @Test
    void inactiveCompanyNeverReceivesTheUncustomizedScreen() {
        Repositories repositories = new Repositories();
        repositories.addCompany(new Company(COMPANY_A, CompanyStatus.INACTIVE, CUSTOM_A, 0));
        CompanyScreenService service = service(
                repositories,
                List.of(
                        definition(functional(screen("1.0.0"))),
                        definition(customization(CUSTOM_A, overlay(
                                CUSTOM_A,
                                "custom_a.dashboard",
                                VERSION_ONE,
                                List.of(new ScreenChange.Hide(CUSTOMER)))))));

        CompanyScreenComposition result = service.compose(COMPANY_A);

        assertRejected(result, ScreenCompositionDiagnosticCode.COMPANY_NOT_OPERATIONAL);
    }

    @Test
    void composedCollectionsAreImmutable() {
        CompanyScreenComposition result = activeService(
                COMPANY_A,
                CUSTOM_A,
                List.of(
                        functional(screen("1.0.0")),
                        customization(CUSTOM_A, overlay(
                                CUSTOM_A,
                                "custom_a.dashboard",
                                VERSION_ONE,
                                List.of(new ScreenChange.Hide(CUSTOMER))))))
                .compose(COMPANY_A);

        assertThrows(UnsupportedOperationException.class, () -> result.screens().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.screens().getFirst().elements().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.screens().getFirst().slots().clear());
    }

    @Test
    void preservesTheV2ExperienceForTheClosedShellRenderer() {
        ScreenExperienceDefinition experience = new ScreenExperienceDefinition(
                ScreenPurpose.TRANSACTION_EDITOR,
                List.of(
                        new ScreenRegionDefinition(
                                new ScreenRegionId("header"), ScreenRegionRole.HEADER, 0),
                        new ScreenRegionDefinition(
                                new ScreenRegionId("actions"), ScreenRegionRole.ACTIONS, 1)),
                Map.of(
                        CUSTOMER, ScreenSemanticType.SEARCHABLE_REFERENCE,
                        AMOUNT, ScreenSemanticType.MONEY),
                List.of(new ScreenActionDefinition(
                        SUBMIT,
                        ScreenActionIntent.SUBMIT,
                        ScreenActionEmphasis.PRIMARY,
                        ScreenConfirmationMode.NONE)));
        ScreenDefinition definition = new ScreenDefinition(
                DASHBOARD,
                SemanticVersion.parse("2.0.0"),
                List.of(
                        element(AMOUNT, ScreenElementType.TEXT_INPUT, "header", 0),
                        element(CUSTOMER, ScreenElementType.SELECT, "header", 1),
                        element(SUBMIT, ScreenElementType.ACTION, "actions", 0)),
                List.of(),
                Optional.of(experience));

        CompanyScreenComposition result = activeService(
                COMPANY_A,
                CUSTOM_A,
                List.of(functional(definition), customization(CUSTOM_A)))
                .compose(COMPANY_A);

        assertTrue(result.operational());
        assertEquals(Optional.of(experience), result.screens().getFirst().experience());
    }

    private static CompanyScreenService activeService(
            CompanyId companyId,
            PluginId customizationId,
            List<PluginDescriptor> descriptors) {
        Repositories repositories = new Repositories();
        repositories.addCompany(activeCompany(companyId, customizationId));
        repositories.addActivation(enabled(companyId, FUNCTIONAL));
        return service(repositories, descriptors.stream().map(CompanyScreenComposerTest::definition).toList());
    }

    private static CompanyScreenService service(
            Repositories repositories,
            List<PluginDefinition> definitions) {
        CompanyPluginQueryService query = new CompanyPluginQueryService(
                repositories,
                repositories,
                PluginRegistry.create(definitions),
                new CompanyPluginResolver());
        return new CompanyScreenService(
                new CompanyContributionService(query),
                new CompanyScreenComposer());
    }

    private static Company activeCompany(CompanyId companyId, PluginId customizationId) {
        return new Company(companyId, CompanyStatus.ACTIVE, customizationId, 0);
    }

    private static PluginActivationDecision enabled(CompanyId companyId, PluginId pluginId) {
        return new PluginActivationDecision(
                companyId, pluginId, PluginActivationState.ENABLED, 0);
    }

    private static PluginDescriptor functional(ScreenDefinition screen) {
        return descriptor(FUNCTIONAL, PluginKind.FUNCTIONAL, List.of(), List.of(screen), List.of());
    }

    private static PluginDescriptor customization(PluginId id, ScreenOverlay... overlays) {
        return descriptor(
                id,
                PluginKind.CUSTOMIZATION,
                List.of(new PluginDependency(FUNCTIONAL, VERSION_ONE, DependencyKind.REQUIRED)),
                List.of(),
                List.of(overlays));
    }

    private static PluginDescriptor descriptor(
            PluginId id,
            PluginKind kind,
            List<PluginDependency> dependencies,
            List<ScreenDefinition> screens,
            List<ScreenOverlay> overlays) {
        return new PluginDescriptor(
                id,
                kind,
                SemanticVersion.parse("1.0.0"),
                API_RANGE,
                id.value(),
                dependencies,
                List.of(), List.of(), List.of(), List.of(),
                screens,
                overlays);
    }

    private static ScreenDefinition screen(String version) {
        Set<ScreenCustomizationOperation> all = EnumSet.allOf(ScreenCustomizationOperation.class);
        return new ScreenDefinition(
                DASHBOARD,
                SemanticVersion.parse(version),
                List.of(
                        new ScreenElementDefinition(
                                AMOUNT,
                                new ScreenRegionId("main"),
                                0,
                                text("functional.amount"),
                                Optional.empty(),
                                true,
                                true,
                                true,
                                all),
                        new ScreenElementDefinition(
                                CUSTOMER,
                                new ScreenRegionId("main"),
                                1,
                                text("functional.customer"),
                                Optional.of(text("functional.customer.help")),
                                true,
                                true,
                                false,
                                all),
                        new ScreenElementDefinition(
                                SUBMIT,
                                new ScreenRegionId("actions"),
                                0,
                                text("functional.submit"),
                                Optional.empty(),
                                true,
                                true,
                                false,
                                Set.of(ScreenCustomizationOperation.DISABLE))),
                List.of(new ScreenSlotDefinition(
                        SUMMARY, new ScreenRegionId("main"), 2, 1)));
    }

    private static ScreenElementDefinition element(
            ScreenElementId id,
            ScreenElementType type,
            String region,
            int order) {
        return new ScreenElementDefinition(
                id,
                type,
                new ScreenRegionId(region),
                order,
                text("functional." + id.value()),
                Optional.empty(),
                true,
                true,
                false,
                Set.of());
    }

    private static ScreenOverlay overlay(
            PluginId owner,
            String id,
            VersionRange screenVersions,
            List<ScreenChange> changes) {
        return new ScreenOverlay(
                new ContributionId(id),
                DASHBOARD,
                screenVersions,
                changes);
    }

    private static PluginDefinition definition(PluginDescriptor descriptor) {
        return () -> descriptor;
    }

    private static ScreenTextKey text(String value) {
        return new ScreenTextKey(value);
    }

    private static VersionRange range(String minimum, String maximum) {
        return new VersionRange(
                SemanticVersion.parse(minimum), SemanticVersion.parse(maximum));
    }

    private static ComposedScreenElement element(ComposedScreen screen, ScreenElementId id) {
        return screen.elements().stream()
                .filter(element -> element.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void assertRejected(
            CompanyScreenComposition result,
            ScreenCompositionDiagnosticCode... expected) {
        assertFalse(result.operational());
        assertTrue(result.screens().isEmpty());
        assertEquals(
                Set.of(expected),
                result.diagnostics().stream()
                        .map(ScreenCompositionDiagnostic::code)
                        .collect(Collectors.toSet()));
    }

    private static final class Repositories implements CompanyRepository, PluginActivationRepository {
        private final Map<CompanyId, Company> companies = new HashMap<>();
        private final List<PluginActivationDecision> activations = new ArrayList<>();

        void addCompany(Company company) {
            companies.put(company.id(), company);
        }

        void addActivation(PluginActivationDecision decision) {
            activations.add(decision);
        }

        @Override
        public List<Company> findAll() {
            return companies.values().stream().toList();
        }

        @Override
        public Optional<Company> findById(CompanyId companyId) {
            return Optional.ofNullable(companies.get(companyId));
        }

        @Override
        public Company save(Company company) {
            companies.put(company.id(), company);
            return company;
        }

        @Override
        public boolean isCustomizationAssignedToAnotherCompany(
                PluginId customizationPluginId,
                CompanyId companyId) {
            return companies.values().stream()
                    .anyMatch(company -> !company.id().equals(companyId)
                            && company.customizationPluginId().equals(customizationPluginId));
        }

        @Override
        public List<PluginActivationDecision> findByCompanyId(CompanyId companyId) {
            return activations.stream()
                    .filter(decision -> decision.companyId().equals(companyId))
                    .toList();
        }

        @Override
        public Optional<PluginActivationDecision> findByCompanyAndPlugin(
                CompanyId companyId,
                PluginId pluginId) {
            return activations.stream()
                    .filter(decision -> decision.companyId().equals(companyId)
                            && decision.pluginId().equals(pluginId))
                    .findFirst();
        }

        @Override
        public PluginActivationDecision save(PluginActivationDecision decision) {
            activations.add(decision);
            return decision;
        }
    }
}
