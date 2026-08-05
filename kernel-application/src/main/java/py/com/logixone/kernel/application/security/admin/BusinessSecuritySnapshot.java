package py.com.logixone.kernel.application.security.admin;

import java.util.List;
import java.util.Objects;

public record BusinessSecuritySnapshot(
        List<SecurityUserView> users,
        List<SecurityCompanyView> companies) {

    public BusinessSecuritySnapshot {
        users = List.copyOf(Objects.requireNonNull(users, "users"));
        companies = List.copyOf(Objects.requireNonNull(companies, "companies"));
    }
}
