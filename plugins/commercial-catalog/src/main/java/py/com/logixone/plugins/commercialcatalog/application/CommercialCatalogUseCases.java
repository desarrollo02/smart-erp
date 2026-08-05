package py.com.logixone.plugins.commercialcatalog.application;

import java.util.List;
import py.com.logixone.plugins.commercialcatalog.api.CatalogItemId;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuote;
import py.com.logixone.plugins.commercialcatalog.api.CatalogPriceQuoteRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.api.CatalogSearchPage;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionRequest;
import py.com.logixone.plugins.commercialcatalog.api.CatalogUnitConversionResult;
import py.com.logixone.plugins.commercialcatalog.api.PriceListId;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogCommands;
import py.com.logixone.plugins.commercialcatalog.application.command.CatalogDefinitionCommands;
import py.com.logixone.plugins.commercialcatalog.application.definition.CatalogDefinitions;
import py.com.logixone.plugins.commercialcatalog.domain.CatalogItemSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.PriceListSnapshot;
import py.com.logixone.plugins.commercialcatalog.domain.TaxProfileId;
import py.com.logixone.plugins.commercialcatalog.domain.VariantFamilyId;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchCriteria;
import py.com.logixone.plugins.commercialcatalog.application.query.PriceListSearchPage;

/** Internal application facade consumed by future neutral inbound adapters. */
public interface CommercialCatalogUseCases {

    CatalogOperationResult<CatalogItemSnapshot> registerItem(
            CatalogOperationContext context, CatalogCommands.RegisterItem command);
    CatalogOperationResult<CatalogItemSnapshot> reviseItem(
            CatalogOperationContext context, CatalogCommands.ReviseItem command);
    CatalogOperationResult<CatalogItemSnapshot> addIdentifier(
            CatalogOperationContext context, CatalogCommands.AddIdentifier command);
    CatalogOperationResult<CatalogItemSnapshot> inactivateIdentifier(
            CatalogOperationContext context, CatalogCommands.InactivateIdentifier command);
    CatalogOperationResult<CatalogItemSnapshot> addUnitConversion(
            CatalogOperationContext context, CatalogCommands.AddUnitConversion command);
    CatalogOperationResult<CatalogItemSnapshot> classify(
            CatalogOperationContext context, CatalogCommands.Classify command);
    CatalogOperationResult<CatalogItemSnapshot> assignTaxProfile(
            CatalogOperationContext context, CatalogCommands.AssignTaxProfile command);
    CatalogOperationResult<CatalogItemSnapshot> assignVariant(
            CatalogOperationContext context, CatalogCommands.AssignVariant command);
    CatalogOperationResult<CatalogItemSnapshot> changeItemLifecycle(
            CatalogOperationContext context, CatalogCommands.ChangeItemLifecycle command);

    CatalogOperationResult<PriceListSnapshot> registerPriceList(
            CatalogOperationContext context, CatalogCommands.RegisterPriceList command);
    CatalogOperationResult<PriceListSnapshot> renamePriceList(
            CatalogOperationContext context, CatalogCommands.RenamePriceList command);
    CatalogOperationResult<PriceListSnapshot> addPriceEntry(
            CatalogOperationContext context, CatalogCommands.AddPriceEntry command);
    CatalogOperationResult<PriceListSnapshot> inactivatePriceEntry(
            CatalogOperationContext context, CatalogCommands.InactivatePriceEntry command);
    CatalogOperationResult<PriceListSnapshot> changePriceListLifecycle(
            CatalogOperationContext context, CatalogCommands.ChangePriceListLifecycle command);

    CatalogOperationResult<CatalogSearchPage> search(
            CatalogOperationContext context, CatalogSearchCriteria criteria);
    CatalogOperationResult<CatalogItemSnapshot> detail(
            CatalogOperationContext context, CatalogItemId itemId);
    CatalogOperationResult<CatalogUnitConversionResult> convert(
            CatalogOperationContext context, CatalogUnitConversionRequest request);
    CatalogOperationResult<CatalogPriceQuote> quote(
            CatalogOperationContext context, CatalogPriceQuoteRequest request);
    CatalogOperationResult<PriceListSearchPage> priceLists(
            CatalogOperationContext context, PriceListSearchCriteria criteria);
    CatalogOperationResult<PriceListSnapshot> priceListDetail(
            CatalogOperationContext context, PriceListId priceListId);

    CatalogOperationResult<CatalogDefinitions.Snapshot> definitions(
            CatalogOperationContext context);
    CatalogOperationResult<CatalogDefinitions.Snapshot> managedDefinitions(
            CatalogOperationContext context);
    CatalogOperationResult<CatalogDefinitions.Unit> registerUnit(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterUnit command);
    CatalogOperationResult<CatalogDefinitions.Category> registerCategory(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterCategory command);
    CatalogOperationResult<CatalogDefinitions.Brand> registerBrand(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterBrand command);
    CatalogOperationResult<CatalogDefinitions.Tag> registerTag(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterTag command);
    CatalogOperationResult<CatalogDefinitions.Lifecycle> changeSimpleDefinitionState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeSimpleState command);
    CatalogOperationResult<CatalogDefinitions.SimpleRevision> reviseSimpleDefinition(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseSimpleDefinition command);
    CatalogOperationResult<List<CatalogDefinitions.SimpleRevision>> simpleDefinitionHistory(
            CatalogOperationContext context,
            CatalogDefinitions.SimpleKind kind,
            String identity);
    CatalogOperationResult<CatalogDefinitions.Replacement> replaceSimpleDefinition(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReplaceSimpleDefinition command);
    CatalogOperationResult<CatalogDefinitions.TaxProfile> registerTaxProfile(
            CatalogOperationContext context, CatalogDefinitionCommands.RegisterTaxProfile command);
    CatalogOperationResult<CatalogDefinitions.TaxProfile> changeTaxProfileState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeTaxProfileState command);
    CatalogOperationResult<CatalogDefinitions.TaxProfile> reviseTaxProfile(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseTaxProfile command);
    CatalogOperationResult<List<CatalogDefinitions.TaxProfileRevision>> taxProfileHistory(
            CatalogOperationContext context, TaxProfileId id);
    CatalogOperationResult<CatalogDefinitions.VariantFamily> registerVariantFamily(
            CatalogOperationContext context,
            CatalogDefinitionCommands.RegisterVariantFamily command);
    CatalogOperationResult<CatalogDefinitions.VariantFamily> changeVariantFamilyState(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ChangeVariantFamilyState command);
    CatalogOperationResult<CatalogDefinitions.VariantFamily> reviseVariantFamily(
            CatalogOperationContext context,
            CatalogDefinitionCommands.ReviseVariantFamily command);
    CatalogOperationResult<List<CatalogDefinitions.VariantFamilyRevision>> variantFamilyHistory(
            CatalogOperationContext context, VariantFamilyId id);
}
