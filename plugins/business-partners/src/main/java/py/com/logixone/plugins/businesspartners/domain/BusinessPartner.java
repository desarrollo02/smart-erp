package py.com.logixone.plugins.businesspartners.domain;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import py.com.logixone.kernel.api.company.CompanyId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerId;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerKind;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerReference;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerRole;
import py.com.logixone.plugins.businesspartners.api.BusinessPartnerState;

/** Framework-free aggregate root for one company-scoped commercial participant. */
public final class BusinessPartner {

    private final CompanyId companyId;
    private final BusinessPartnerId id;
    private BusinessPartnerCode code;
    private final BusinessPartnerKind kind;
    private BusinessPartnerName displayName;
    private Optional<BusinessPartnerName> legalName;
    private Optional<BusinessPartnerName> tradeName;
    private BusinessPartnerState state;
    private final EnumMap<BusinessPartnerRole, CommercialRole> roles;
    private final LinkedHashMap<BusinessPartnerDetailId, BusinessPartnerIdentification>
            identifications;
    private final LinkedHashMap<BusinessPartnerDetailId, BusinessPartnerAddress> addresses;
    private final LinkedHashMap<BusinessPartnerDetailId, BusinessPartnerContactChannel> channels;
    private final LinkedHashMap<BusinessPartnerDetailId, BusinessPartnerContact> contacts;
    private long version;

    private BusinessPartner(
            CompanyId companyId,
            BusinessPartnerId id,
            BusinessPartnerCode code,
            BusinessPartnerKind kind,
            BusinessPartnerName displayName,
            Optional<BusinessPartnerName> legalName,
            Optional<BusinessPartnerName> tradeName) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.legalName = Objects.requireNonNull(legalName, "legalName");
        this.tradeName = Objects.requireNonNull(tradeName, "tradeName");
        this.state = BusinessPartnerState.ACTIVE;
        this.roles = new EnumMap<>(BusinessPartnerRole.class);
        this.identifications = new LinkedHashMap<>();
        this.addresses = new LinkedHashMap<>();
        this.channels = new LinkedHashMap<>();
        this.contacts = new LinkedHashMap<>();
        this.version = 0;
    }

    public static BusinessPartner create(
            CompanyId companyId,
            BusinessPartnerId id,
            BusinessPartnerCode code,
            BusinessPartnerKind kind,
            BusinessPartnerName displayName,
            Optional<BusinessPartnerName> legalName,
            Optional<BusinessPartnerName> tradeName) {
        return new BusinessPartner(
                companyId, id, code, kind, displayName, legalName, tradeName);
    }

    public static BusinessPartner restore(BusinessPartnerSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        BusinessPartner restored = new BusinessPartner(
                snapshot.companyId(),
                snapshot.id(),
                snapshot.code(),
                snapshot.kind(),
                snapshot.displayName(),
                snapshot.legalName(),
                snapshot.tradeName());
        restored.state = snapshot.state();
        snapshot.roles().forEach(role -> {
            if (restored.roles.putIfAbsent(role.type(), role) != null) {
                throw new IllegalArgumentException("Duplicate business partner role: " + role.type());
            }
        });
        snapshot.identifications().forEach(value -> putRestored(
                restored.identifications, value.id(), value, "identification"));
        snapshot.addresses().forEach(value -> putRestored(
                restored.addresses, value.id(), value, "address"));
        snapshot.channels().forEach(value -> putRestored(
                restored.channels, value.id(), value, "channel"));
        snapshot.contacts().forEach(value -> putRestored(
                restored.contacts, value.id(), value, "contact"));
        validatePrimaryAddresses(restored.addresses.values());
        validatePrimaryChannels(restored.channels.values());
        restored.version = snapshot.version();
        return restored;
    }

    public void rename(
            long expectedVersion,
            BusinessPartnerName newDisplayName,
            Optional<BusinessPartnerName> newLegalName,
            Optional<BusinessPartnerName> newTradeName) {
        verifyVersion(expectedVersion);
        displayName = Objects.requireNonNull(newDisplayName, "newDisplayName");
        legalName = Objects.requireNonNull(newLegalName, "newLegalName");
        tradeName = Objects.requireNonNull(newTradeName, "newTradeName");
        version++;
    }

    public void changeCode(long expectedVersion, BusinessPartnerCode newCode) {
        verifyActiveAndVersion(expectedVersion);
        code = Objects.requireNonNull(newCode, "newCode");
        version++;
    }

    public void assignRole(
            long expectedVersion,
            BusinessPartnerRole role,
            Optional<BusinessPartnerCode> roleCode) {
        verifyActiveAndVersion(expectedVersion);
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(roleCode, "roleCode");
        if (roles.containsKey(role)) {
            throw new IllegalStateException("Business partner role is already assigned: " + role);
        }
        roles.put(role, new CommercialRole(role, BusinessPartnerState.ACTIVE, roleCode));
        version++;
    }

    public void changeRoleState(
            long expectedVersion,
            BusinessPartnerRole role,
            BusinessPartnerState newState) {
        verifyActiveAndVersion(expectedVersion);
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(newState, "newState");
        CommercialRole existing = roles.get(role);
        if (existing == null) {
            throw new IllegalStateException("Business partner role is not assigned: " + role);
        }
        if (existing.state() == newState) {
            return;
        }
        roles.put(role, existing.withState(newState));
        version++;
    }

    public void inactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state == BusinessPartnerState.INACTIVE) {
            return;
        }
        state = BusinessPartnerState.INACTIVE;
        version++;
    }

    public void addIdentification(
            long expectedVersion, BusinessPartnerIdentification identification) {
        verifyActiveAndVersion(expectedVersion);
        Objects.requireNonNull(identification, "identification");
        putNew(identifications, identification.id(), identification, "identification");
        version++;
    }

    public boolean hasPotentialDuplicate(BusinessPartnerIdentification identification) {
        Objects.requireNonNull(identification, "identification");
        return identifications.values().stream()
                .map(BusinessPartnerIdentification::duplicateCandidateKey)
                .anyMatch(identification.duplicateCandidateKey()::equals);
    }

    public void addAddress(long expectedVersion, BusinessPartnerAddress address) {
        verifyActiveAndVersion(expectedVersion);
        Objects.requireNonNull(address, "address");
        if (addresses.containsKey(address.id())) {
            throw new IllegalStateException("Business partner address id is already assigned");
        }
        if (address.primary()) {
            addresses.replaceAll((id, existing) -> sameAddressCategory(existing, address)
                    ? existing.withoutPrimary()
                    : existing);
        }
        addresses.put(address.id(), address);
        version++;
    }

    public void addContactChannel(
            long expectedVersion, BusinessPartnerContactChannel channel) {
        verifyActiveAndVersion(expectedVersion);
        Objects.requireNonNull(channel, "channel");
        if (channels.containsKey(channel.id())) {
            throw new IllegalStateException("Business partner channel id is already assigned");
        }
        if (channel.primary()) {
            channels.replaceAll((id, existing) -> sameChannelCategory(existing, channel)
                    ? existing.withoutPrimary()
                    : existing);
        }
        channels.put(channel.id(), channel);
        version++;
    }

    public void addContact(long expectedVersion, BusinessPartnerContact contact) {
        verifyActiveAndVersion(expectedVersion);
        Objects.requireNonNull(contact, "contact");
        putNew(contacts, contact.id(), contact, "contact");
        version++;
    }

    public void reactivate(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state == BusinessPartnerState.ACTIVE) {
            return;
        }
        state = BusinessPartnerState.ACTIVE;
        version++;
    }

    public BusinessPartnerReference toReference() {
        Set<BusinessPartnerRole> activeRoles = roles.values().stream()
                .filter(role -> role.state() == BusinessPartnerState.ACTIVE)
                .map(CommercialRole::type)
                .collect(Collectors.toUnmodifiableSet());
        return new BusinessPartnerReference(
                id,
                code.value(),
                displayName.value(),
                kind,
                state,
                activeRoles,
                version);
    }

    public CompanyId companyId() {
        return companyId;
    }

    public BusinessPartnerId id() {
        return id;
    }

    public BusinessPartnerCode code() {
        return code;
    }

    public BusinessPartnerKind kind() {
        return kind;
    }

    public BusinessPartnerName displayName() {
        return displayName;
    }

    public Optional<BusinessPartnerName> legalName() {
        return legalName;
    }

    public Optional<BusinessPartnerName> tradeName() {
        return tradeName;
    }

    public BusinessPartnerState state() {
        return state;
    }

    public Map<BusinessPartnerRole, CommercialRole> roles() {
        return Map.copyOf(roles);
    }

    public List<BusinessPartnerIdentification> identifications() {
        return List.copyOf(identifications.values());
    }

    public List<BusinessPartnerAddress> addresses() {
        return List.copyOf(addresses.values());
    }

    public List<BusinessPartnerContactChannel> channels() {
        return List.copyOf(channels.values());
    }

    public List<BusinessPartnerContact> contacts() {
        return List.copyOf(contacts.values());
    }

    public long version() {
        return version;
    }

    public BusinessPartnerSnapshot snapshot() {
        return new BusinessPartnerSnapshot(
                companyId,
                id,
                code,
                kind,
                displayName,
                legalName,
                tradeName,
                state,
                List.copyOf(roles.values()),
                identifications(),
                addresses(),
                channels(),
                contacts(),
                version);
    }

    private void verifyActiveAndVersion(long expectedVersion) {
        verifyVersion(expectedVersion);
        if (state != BusinessPartnerState.ACTIVE) {
            throw new IllegalStateException("Business partner must be active");
        }
    }

    private void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new ConcurrentBusinessPartnerChangeException(expectedVersion, version);
        }
    }

    private static boolean sameAddressCategory(
            BusinessPartnerAddress first, BusinessPartnerAddress second) {
        return first.active()
                && first.type().equals(second.type())
                && first.purpose().equals(second.purpose());
    }

    private static boolean sameChannelCategory(
            BusinessPartnerContactChannel first, BusinessPartnerContactChannel second) {
        return first.active()
                && first.kind().equals(second.kind())
                && first.purpose().equals(second.purpose());
    }

    private static <T> void putNew(
            Map<BusinessPartnerDetailId, T> target,
            BusinessPartnerDetailId id,
            T value,
            String label) {
        if (target.putIfAbsent(id, value) != null) {
            throw new IllegalStateException("Business partner " + label + " id is already assigned");
        }
    }

    private static <T> void putRestored(
            Map<BusinessPartnerDetailId, T> target,
            BusinessPartnerDetailId id,
            T value,
            String label) {
        if (target.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("Duplicate business partner " + label + " id");
        }
    }

    private static void validatePrimaryAddresses(
            java.util.Collection<BusinessPartnerAddress> values) {
        long distinct = values.stream()
                .filter(value -> value.active() && value.primary())
                .map(value -> value.type().value() + ':' + value.purpose().value())
                .distinct()
                .count();
        long primary = values.stream().filter(value -> value.active() && value.primary()).count();
        if (distinct != primary) {
            throw new IllegalArgumentException(
                    "Only one active primary address is allowed per type and purpose");
        }
    }

    private static void validatePrimaryChannels(
            java.util.Collection<BusinessPartnerContactChannel> values) {
        long distinct = values.stream()
                .filter(value -> value.active() && value.primary())
                .map(value -> value.kind().value() + ':' + value.purpose().value())
                .distinct()
                .count();
        long primary = values.stream().filter(value -> value.active() && value.primary()).count();
        if (distinct != primary) {
            throw new IllegalArgumentException(
                    "Only one active primary channel is allowed per kind and purpose");
        }
    }
}
