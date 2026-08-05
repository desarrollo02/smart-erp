package py.com.logixone.plugins.businesspartners.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Lightweight named contact owned by the partner; it is not another partner. */
public record BusinessPartnerContact(
        BusinessPartnerDetailId id,
        BusinessPartnerName name,
        Optional<BusinessPartnerName> position,
        List<BusinessPartnerContactChannel> channels,
        boolean active) {

    public BusinessPartnerContact {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        position = Objects.requireNonNull(position, "position");
        channels = List.copyOf(Objects.requireNonNull(channels, "channels"));
        Set<String> primaryKeys = new HashSet<>();
        for (BusinessPartnerContactChannel channel : channels) {
            if (channel.primary()) {
                String key = channel.kind().value() + ":" + channel.purpose().value();
                if (!primaryKeys.add(key)) {
                    throw new IllegalArgumentException(
                            "A contact can have only one primary channel per kind and purpose");
                }
            }
        }
    }
}
