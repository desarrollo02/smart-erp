package py.com.logixone.plugins.businesspartners.application;

import java.util.List;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.application.command.BusinessPartnerCommands;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchCriteria;
import py.com.logixone.plugins.businesspartners.application.query.BusinessPartnerSearchPage;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentificationKey;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerSnapshot;

/** Internal application facade consumed by inbound adapters such as the neutral screen handler. */
public interface BusinessPartnerUseCases {

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> register(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.Register command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> rename(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.Rename command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeCode(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.ChangeCode command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> addIdentification(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.AddIdentification command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> addAddress(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.AddAddress command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> addChannel(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.AddChannel command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> addContact(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.AddContact command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> assignRole(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.AssignRole command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeRoleState(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.ChangeRoleState command);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> changeLifecycle(
            BusinessPartnerOperationContext context, BusinessPartnerCommands.ChangeLifecycle command);

    BusinessPartnerOperationResult<BusinessPartnerSearchPage> search(
            BusinessPartnerOperationContext context, BusinessPartnerSearchCriteria criteria);

    BusinessPartnerOperationResult<BusinessPartnerSnapshot> detail(
            BusinessPartnerOperationContext context, BusinessPartnerId id);

    BusinessPartnerOperationResult<List<BusinessPartnerId>> duplicateCandidates(
            BusinessPartnerOperationContext context, BusinessPartnerIdentificationKey candidate);
}
