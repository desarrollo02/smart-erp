package py.com.logixone.plugins.businesspartners.application.command;

import java.util.Objects;
import java.util.Optional;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerAddress;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerCode;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContact;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerContactChannel;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerIdentification;
import py.com.logixone.plugins.businesspartners.domain.BusinessPartnerName;

/** Validated command payloads; no transport or Jakarta types are exposed. */
public final class BusinessPartnerCommands {

    private BusinessPartnerCommands() {
    }

    public record Register(
            Optional<BusinessPartnerCode> code,
            BusinessPartnerKind kind,
            BusinessPartnerName displayName,
            Optional<BusinessPartnerName> legalName,
            Optional<BusinessPartnerName> tradeName) {
        public Register {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(legalName, "legalName");
            Objects.requireNonNull(tradeName, "tradeName");
        }
    }

    public record Rename(
            BusinessPartnerId id,
            long expectedVersion,
            BusinessPartnerName displayName,
            Optional<BusinessPartnerName> legalName,
            Optional<BusinessPartnerName> tradeName) {
        public Rename {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(legalName, "legalName");
            Objects.requireNonNull(tradeName, "tradeName");
        }
    }

    public record ChangeCode(
            BusinessPartnerId id, long expectedVersion, BusinessPartnerCode code) {
        public ChangeCode {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(code, "code");
        }
    }

    public record AddIdentification(
            BusinessPartnerId id,
            long expectedVersion,
            BusinessPartnerIdentification identification) {
        public AddIdentification {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(identification, "identification");
        }
    }

    public record AddAddress(
            BusinessPartnerId id, long expectedVersion, BusinessPartnerAddress address) {
        public AddAddress {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(address, "address");
        }
    }

    public record AddChannel(
            BusinessPartnerId id,
            long expectedVersion,
            BusinessPartnerContactChannel channel) {
        public AddChannel {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(channel, "channel");
        }
    }

    public record AddContact(
            BusinessPartnerId id, long expectedVersion, BusinessPartnerContact contact) {
        public AddContact {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(contact, "contact");
        }
    }

    public record AssignRole(
            BusinessPartnerId id,
            long expectedVersion,
            BusinessPartnerRole role,
            Optional<BusinessPartnerCode> roleCode) {
        public AssignRole {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(roleCode, "roleCode");
        }
    }

    public record ChangeRoleState(
            BusinessPartnerId id,
            long expectedVersion,
            BusinessPartnerRole role,
            BusinessPartnerState state) {
        public ChangeRoleState {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(state, "state");
        }
    }

    public record ChangeLifecycle(
            BusinessPartnerId id,
            long expectedVersion,
            BusinessPartnerState state) {
        public ChangeLifecycle {
            requireMutation(id, expectedVersion);
            Objects.requireNonNull(state, "state");
        }
    }

    private static void requireMutation(BusinessPartnerId id, long expectedVersion) {
        Objects.requireNonNull(id, "id");
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
    }
}
