package py.com.logixone.plugins.businesspartners.application;

import java.util.List;
import py.com.logixone.plugins.businesspartners.application.command.ChangeBusinessPartnerDefinitionState;
import py.com.logixone.plugins.businesspartners.application.command.RegisterBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.application.command.ReviseBusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinition;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionKind;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerDefinitionRevision;

/** Internal application facade for selector definitions owned by business_partners. */
public interface BusinessPartnerDefinitionUseCases {

    BusinessPartnerOperationResult<List<BusinessPartnerDefinition>> definitions(
            BusinessPartnerOperationContext context, BusinessPartnerDefinitionKind kind);

    BusinessPartnerOperationResult<BusinessPartnerDefinition> registerDefinition(
            BusinessPartnerOperationContext context, RegisterBusinessPartnerDefinition command);

    BusinessPartnerOperationResult<BusinessPartnerDefinition> reviseDefinition(
            BusinessPartnerOperationContext context, ReviseBusinessPartnerDefinition command);

    BusinessPartnerOperationResult<List<BusinessPartnerDefinitionRevision>> definitionHistory(
            BusinessPartnerOperationContext context,
            BusinessPartnerDefinitionKind kind,
            py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAttributeCode code);

    BusinessPartnerOperationResult<BusinessPartnerDefinition> changeDefinitionState(
            BusinessPartnerOperationContext context,
            ChangeBusinessPartnerDefinitionState command);
}
