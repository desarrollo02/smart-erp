package py.com.logixone.plugins.commercialcatalog.infrastructure.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.time.Clock;
import java.util.List;
import py.com.logixone.kernel.api.audit.TechnicalAudit;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuote;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.CatalogCommandService;
import py.com.logixone.plugins.commercialcatalog.application.CatalogDefinitionService;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationContext;
import py.com.logixone.plugins.commercialcatalog.application.CatalogOperationResult;
import py.com.logixone.plugins.commercialcatalog.application.CatalogQueryService;
import py.com.logixone.plugins.commercialcatalog.application.CommercialCatalogUseCases;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogCodeSequenceRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogDefinitionRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogIdGenerator;
import py.com.logixone.plugins.commercialcatalog.application.port.CatalogItemRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.CurrencyReferencePolicy;
import py.com.logixone.plugins.commercialcatalog.application.port.PriceListRepository;
import py.com.logixone.plugins.commercialcatalog.application.port.VariantFamilyAssignmentRepository;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;

/** JTA boundary; REST and Jakarta Faces exposure belong to later stories. */
@ApplicationScoped
@Transactional(rollbackOn = RuntimeException.class)
public class TransactionalCommercialCatalogUseCases implements CommercialCatalogUseCases {

    @Inject CatalogItemRepository items;
    @Inject PriceListRepository priceLists;
    @Inject CatalogCodeSequenceRepository sequences;
    @Inject CatalogDefinitionRepository definitions;
    @Inject VariantFamilyAssignmentRepository variantFamilies;
    @Inject CatalogIdGenerator ids;
    @Inject CurrencyReferencePolicy currencies;
    @Inject TechnicalAudit audit;

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> registerItem(
            CatalogOperationContext context, CatalogCommands.RegisterItem command) {
        return commands().registerItem(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> reviseItem(
            CatalogOperationContext context, CatalogCommands.ReviseItem command) {
        return commands().reviseItem(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> addIdentifier(
            CatalogOperationContext context, CatalogCommands.AddIdentifier command) {
        return commands().addIdentifier(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> inactivateIdentifier(
            CatalogOperationContext context, CatalogCommands.InactivateIdentifier command) {
        return commands().inactivateIdentifier(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> addUnitConversion(
            CatalogOperationContext context, CatalogCommands.AddUnitConversion command) {
        return commands().addUnitConversion(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> classify(
            CatalogOperationContext context, CatalogCommands.Classify command) {
        return commands().classify(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> assignTaxProfile(
            CatalogOperationContext context, CatalogCommands.AssignTaxProfile command) {
        return commands().assignTaxProfile(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> assignVariant(
            CatalogOperationContext context, CatalogCommands.AssignVariant command) {
        return commands().assignVariant(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogItemSnapshot> changeItemLifecycle(
            CatalogOperationContext context, CatalogCommands.ChangeItemLifecycle command) {
        return commands().changeItemLifecycle(context, command);
    }

    @Override
    public CatalogOperationResult<PriceListSnapshot> registerPriceList(
            CatalogOperationContext context, CatalogCommands.RegisterPriceList command) {
        return commands().registerPriceList(context, command);
    }

    @Override
    public CatalogOperationResult<PriceListSnapshot> renamePriceList(
            CatalogOperationContext context, CatalogCommands.RenamePriceList command) {
        return commands().renamePriceList(context, command);
    }

    @Override
    public CatalogOperationResult<PriceListSnapshot> addPriceEntry(
            CatalogOperationContext context, CatalogCommands.AddPriceEntry command) {
        return commands().addPriceEntry(context, command);
    }

    @Override
    public CatalogOperationResult<PriceListSnapshot> inactivatePriceEntry(
            CatalogOperationContext context, CatalogCommands.InactivatePriceEntry command) {
        return commands().inactivatePriceEntry(context, command);
    }

    @Override
    public CatalogOperationResult<PriceListSnapshot> changePriceListLifecycle(
            CatalogOperationContext context, CatalogCommands.ChangePriceListLifecycle command) {
        return commands().changePriceListLifecycle(context, command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<CatalogSearchPage> search(
            CatalogOperationContext context, CatalogSearchCriteria criteria) {
        return queries().search(context, criteria);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<CatalogItemSnapshot> detail(
            CatalogOperationContext context, CatalogItemId itemId) {
        return queries().detail(context, itemId);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<CatalogUnitConversionResult> convert(
            CatalogOperationContext context, CatalogUnitConversionRequest request) {
        return queries().convert(context, request);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<CatalogPriceQuote> quote(
            CatalogOperationContext context, CatalogPriceQuoteRequest request) {
        return queries().quote(context, request);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<PriceListSearchPage> priceLists(
            CatalogOperationContext context, PriceListSearchCriteria criteria) {
        return queries().priceLists(context, criteria);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<PriceListSnapshot> priceListDetail(
            CatalogOperationContext context, PriceListId priceListId) {
        return queries().priceListDetail(context, priceListId);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<CatalogDefinitions.Snapshot> definitions(
            CatalogOperationContext context) {
        return definitionService().available(context);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<CatalogDefinitions.Snapshot> managedDefinitions(
            CatalogOperationContext context) {
        return definitionService().managed(context);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.Unit> registerUnit(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterUnit command) {
        return definitionService().registerUnit(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.Category> registerCategory(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterCategory command) {
        return definitionService().registerCategory(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.Brand> registerBrand(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterBrand command) {
        return definitionService().registerBrand(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.Tag> registerTag(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterTag command) {
        return definitionService().registerTag(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.Lifecycle> changeSimpleDefinitionState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeSimpleState command) {
        return definitionService().changeSimpleState(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseSimpleDefinition(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseSimpleDefinition command) {
        return definitionService().reviseSimpleDefinition(context, command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<List<CatalogDefinitions.SimpleRevision>>
            simpleDefinitionHistory(
                    CatalogOperationContext context,
                    CatalogDefinitions.SimpleKind kind,
                    String identity) {
        return definitionService().simpleDefinitionHistory(context, kind, identity);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.Replacement> replaceSimpleDefinition(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReplaceSimpleDefinition command) {
        return definitionService().replaceSimpleDefinition(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.TaxProfile> registerTaxProfile(
            CatalogOperationContext context,
            CatalogDefinitionCommands.RegisterTaxProfile command) {
        return definitionService().registerTaxProfile(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.TaxProfile> changeTaxProfileState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeTaxProfileState command) {
        return definitionService().changeTaxProfileState(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.TaxProfile> reviseTaxProfile(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseTaxProfile command) {
        return definitionService().reviseTaxProfile(context, command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<List<CatalogDefinitions.TaxProfileRevision>> taxProfileHistory(
            CatalogOperationContext context, TaxProfileId id) {
        return definitionService().taxProfileHistory(context, id);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.VariantFamily> registerVariantFamily(
            CatalogOperationContext context,
            CatalogDefinitionCommands.RegisterVariantFamily command) {
        return definitionService().registerVariantFamily(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.VariantFamily> changeVariantFamilyState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeVariantFamilyState command) {
        return definitionService().changeVariantFamilyState(context, command);
    }

    @Override
    public CatalogOperationResult<CatalogDefinitions.VariantFamily> reviseVariantFamily(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseVariantFamily command) {
        return definitionService().reviseVariantFamily(context, command);
    }

    @Override
    @Transactional(TxType.SUPPORTS)
    public CatalogOperationResult<List<CatalogDefinitions.VariantFamilyRevision>>
            variantFamilyHistory(CatalogOperationContext context, VariantFamilyId id) {
        return definitionService().variantFamilyHistory(context, id);
    }

    private CatalogCommandService commands() {
        return new CatalogCommandService(
                items, priceLists, variantFamilies, sequences, ids, currencies,
                audit, Clock.systemUTC());
    }

    private CatalogQueryService queries() {
        return new CatalogQueryService(items, priceLists);
    }

    private CatalogDefinitionService definitionService() {
        return new CatalogDefinitionService(definitions, ids, audit, Clock.systemUTC());
    }
}
