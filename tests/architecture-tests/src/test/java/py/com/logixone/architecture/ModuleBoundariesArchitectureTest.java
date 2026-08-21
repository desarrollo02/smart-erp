package py.com.logixone.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "py.com.logixone", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesArchitectureTest {

    @ArchTest
    static final ArchRule pluginApiUsesOnlyJavaAndItsOwnContracts = noClasses()
            .that().resideInAPackage("py.com.logixone.plugin.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..", "py.com.logixone.plugin.api..");

    @ArchTest
    static final ArchRule pluginScaffoldUsesOnlyJavaAndPublicPluginContracts = noClasses()
            .that().resideInAPackage("py.com.logixone.tools.scaffold..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.tools.scaffold..",
                    "py.com.logixone.plugin.api..");

    @ArchTest
    static final ArchRule kernelApiUsesOnlyJavaAndItsOwnContracts = noClasses()
            .that().resideInAPackage("py.com.logixone.kernel.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..", "py.com.logixone.kernel.api..");

    @ArchTest
    static final ArchRule businessPartnersApiUsesOnlyJavaAndCompanyIdentity = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.businesspartners.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.businesspartners.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule referenceDataApiUsesOnlyJavaAndCompanyIdentity = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.referencedata.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.referencedata.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule commercialCatalogApiUsesOnlyJavaAndCompanyIdentity = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.commercialcatalog.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.commercialcatalog.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule inventoryApiUsesOnlyJavaAndCompanyIdentity = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.inventory.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.inventory.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule inventoryDomainUsesOnlyJavaAndPublicContracts = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.inventory.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.inventory.domain..",
                    "py.com.logixone.plugins.inventory.api..",
                    "py.com.logixone.plugins.commercialcatalog.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule purchasingApiUsesOnlyJavaAndCompanyIdentity = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.purchasing.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.purchasing.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule purchasingDomainUsesOnlyJavaAndPublicContracts = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.purchasing.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..",
                    "py.com.logixone.plugins.purchasing.domain..",
                    "py.com.logixone.plugins.purchasing.api..",
                    "py.com.logixone.plugins.businesspartners.api..",
                    "py.com.logixone.plugins.commercialcatalog.api..",
                    "py.com.logixone.plugins.referencedata.api..",
                    "py.com.logixone.plugins.inventory.api..",
                    "py.com.logixone.kernel.api.company..",
                    "py.com.logixone.kernel.api.security..");

    @ArchTest
    static final ArchRule salesApiUsesOnlyJavaAndCompanyIdentity = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.sales.api..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..", "py.com.logixone.plugins.sales.api..",
                    "py.com.logixone.kernel.api.company..");

    @ArchTest
    static final ArchRule salesDomainUsesOnlyJavaAndPublicContracts = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.sales.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "java..", "py.com.logixone.plugins.sales.domain..",
                    "py.com.logixone.plugins.sales.api..",
                    "py.com.logixone.plugins.businesspartners.api..",
                    "py.com.logixone.plugins.commercialcatalog.api..",
                    "py.com.logixone.plugins.referencedata.api..",
                    "py.com.logixone.plugins.inventory.api..",
                    "py.com.logixone.kernel.api.company..",
                    "py.com.logixone.kernel.api.security..");

    @ArchTest
    static final ArchRule neutralContractsAndDomainDoNotUseFrameworks = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.plugin.api..",
                    "py.com.logixone.kernel.api..",
                    "py.com.logixone.kernel.domain..",
                    "py.com.logixone.kernel.application..",
                    "py.com.logixone.plugins.referencedata.api..",
                    "py.com.logixone.plugins.commercialcatalog.api..",
                    "py.com.logixone.plugins.commercialcatalog.domain..",
                    "py.com.logixone.plugins.commercialcatalog.application..",
                    "py.com.logixone.plugins.inventory.api..",
                    "py.com.logixone.plugins.inventory.domain..",
                    "py.com.logixone.plugins.inventory.application..",
                    "py.com.logixone.plugins.purchasing.api..",
                    "py.com.logixone.plugins.purchasing.domain..",
                    "py.com.logixone.plugins.sales.api..",
                    "py.com.logixone.plugins.sales.domain..",
                    "py.com.logixone.plugins.sales.application..",
                    "py.com.logixone.plugins.businesspartners.api..",
                    "py.com.logixone.plugins.businesspartners.domain..",
                    "py.com.logixone.plugins.businesspartners.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta..",
                    "javax..",
                    "org.hibernate..",
                    "org.jboss..",
                    "org.primefaces..",
                    "java.sql..",
                    "javax.sql..",
                    "org.postgresql..");

    @ArchTest
    static final ArchRule commercialCatalogDoesNotReachBusinessPartnersImplementation = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.plugins.commercialcatalog.api..",
                    "py.com.logixone.plugins.commercialcatalog.domain..",
                    "py.com.logixone.plugins.commercialcatalog.application..")
            .should().dependOnClassesThat().resideInAPackage(
                    "py.com.logixone.plugins.businesspartners..");

    @ArchTest
    static final ArchRule referenceDataConsumersDoNotReachItsImplementation = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.plugins.businesspartners..",
                    "py.com.logixone.plugins.commercialcatalog..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "py.com.logixone.plugins.referencedata.application..",
                    "py.com.logixone.plugins.referencedata.infrastructure..");

    @ArchTest
    static final ArchRule inventoryDoesNotReachCatalogImplementation = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.plugins.inventory.api..",
                    "py.com.logixone.plugins.inventory.domain..",
                    "py.com.logixone.plugins.inventory.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "py.com.logixone.plugins.commercialcatalog.domain..",
                    "py.com.logixone.plugins.commercialcatalog.application..",
                    "py.com.logixone.plugins.commercialcatalog.infrastructure..");

    @ArchTest
    static final ArchRule purchasingDoesNotReachDependencyImplementations = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.plugins.purchasing.api..",
                    "py.com.logixone.plugins.purchasing.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "py.com.logixone.plugins.businesspartners.domain..",
                    "py.com.logixone.plugins.businesspartners.application..",
                    "py.com.logixone.plugins.businesspartners.infrastructure..",
                    "py.com.logixone.plugins.commercialcatalog.domain..",
                    "py.com.logixone.plugins.commercialcatalog.application..",
                    "py.com.logixone.plugins.commercialcatalog.infrastructure..",
                    "py.com.logixone.plugins.referencedata.application..",
                    "py.com.logixone.plugins.referencedata.infrastructure..",
                    "py.com.logixone.plugins.inventory.domain..",
                    "py.com.logixone.plugins.inventory.application..",
                    "py.com.logixone.plugins.inventory.infrastructure..");

    @ArchTest
    static final ArchRule businessPartnersApplicationDoesNotReachInfrastructure = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.businesspartners.application..")
            .should().dependOnClassesThat().resideInAPackage(
                    "py.com.logixone.plugins.businesspartners.infrastructure..");

    @ArchTest
    static final ArchRule commercialCatalogApplicationDoesNotReachInfrastructure = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.commercialcatalog.application..")
            .should().dependOnClassesThat().resideInAPackage(
                    "py.com.logixone.plugins.commercialcatalog.infrastructure..");

    @ArchTest
    static final ArchRule inventoryApplicationDoesNotReachInfrastructure = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.inventory.application..")
            .should().dependOnClassesThat().resideInAPackage(
                    "py.com.logixone.plugins.inventory.infrastructure..");

    @ArchTest
    static final ArchRule purchasingApplicationDoesNotReachInfrastructure = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.purchasing.application..")
            .should().dependOnClassesThat().resideInAPackage(
                    "py.com.logixone.plugins.purchasing.infrastructure..");

    @ArchTest
    static final ArchRule salesApplicationDoesNotReachInfrastructure = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.sales.application..")
            .should().dependOnClassesThat().resideInAPackage(
                    "py.com.logixone.plugins.sales.infrastructure..");

    @ArchTest
    static final ArchRule jpaEntitiesStayInsideTheirOwnerPersistenceInfrastructure = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAnyPackage(
                    "py.com.logixone.kernel.infrastructure.jakarta.persistence..",
                    "py.com.logixone.plugins.businesspartners.infrastructure.persistence..",
                    "py.com.logixone.plugins.commercialcatalog.infrastructure.persistence..",
                    "py.com.logixone.plugins.inventory.infrastructure.persistence..",
                    "py.com.logixone.plugins.purchasing.infrastructure.persistence..",
                    "py.com.logixone.plugins.sales.infrastructure.persistence..");

    @ArchTest
    static final ArchRule kernelDoesNotDependOnPluginImplementations = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.kernel..",
                    "py.com.logixone.web..")
            .should().dependOnClassesThat().resideInAnyPackage("py.com.logixone.plugins..");

    @ArchTest
    static final ArchRule migratorDoesNotCompileAgainstPluginImplementationsOrJakarta = noClasses()
            .that().resideInAPackage("py.com.logixone.migrator..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "py.com.logixone.plugins..",
                    "jakarta..",
                    "javax..");

    @ArchTest
    static final ArchRule pluginsDoNotDependOnKernelImplementations = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "py.com.logixone.kernel.domain..",
                    "py.com.logixone.kernel.application..",
                    "py.com.logixone.kernel.infrastructure..");

    @ArchTest
    static final ArchRule customizationPluginsDoNotDependOnReferencePluginImplementation = noClasses()
            .that().resideInAPackage("py.com.logixone.plugins.customization..")
            .should().dependOnClassesThat().resideInAPackage("py.com.logixone.plugins.reference..");

    @ArchTest
    static final ArchRule globalAuthorityCannotAcquireACompanyScope = noClasses()
            .that().resideInAPackage("py.com.logixone.kernel.domain.security.system..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "py.com.logixone.kernel.api.company..",
                    "py.com.logixone.kernel.domain.company..");

    @ArchTest
    static final ArchRule administrativeWebCannotReachJpaOrInfrastructure = noClasses()
            .that().resideInAnyPackage(
                    "py.com.logixone.web.admin..",
                    "py.com.logixone.web.security..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "py.com.logixone.kernel.infrastructure..");
}
