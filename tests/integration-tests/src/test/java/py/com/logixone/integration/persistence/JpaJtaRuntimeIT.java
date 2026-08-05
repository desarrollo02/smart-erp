package py.com.logixone.integration.persistence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.contains;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "logixone.jta-probe", matches = "true")
class JpaJtaRuntimeIT {

    private static final String BASE_URI = System.getProperty("logixone.base-uri");
    private static final String PROBE_PATH = "/logixone-jta-harness/jta-probe/transactions";

    @BeforeEach
    void resetBeforeTest() {
        resetProbeData();
    }

    @AfterEach
    void resetAfterTest() {
        resetProbeData();
    }

    @Test
    void commitsCompanyAndActivationInOneJtaTransaction() {
        String companyId = UUID.randomUUID().toString();

        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/commit/" + companyId)
        .then()
                .statusCode(200)
                .body("outcome", equalTo("COMMITTED"));

        assertState(companyId, true, true);
    }

    @Test
    void runtimeExceptionRollsBackCompanyAndActivationAtomically() {
        String companyId = UUID.randomUUID().toString();

        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/rollback/" + companyId)
        .then()
                .statusCode(200)
                .body("outcome", equalTo("ROLLED_BACK"));

        assertState(companyId, false, false);
    }

    @Test
    void applicationUseCasesCommitAndIsolateTwoCompanies() {
        String firstCompanyId = UUID.randomUUID().toString();
        String secondCompanyId = UUID.randomUUID().toString();

        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/application/commit-a/" + firstCompanyId)
        .then()
                .statusCode(200)
                .body("registration", equalTo("CHANGED"))
                .body("activation", equalTo("CHANGED"))
                .body("companyStatus", equalTo("CHANGED"));
        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/application/commit-b/" + secondCompanyId)
        .then()
                .statusCode(200)
                .body("registration", equalTo("CHANGED"))
                .body("activation", equalTo("CHANGED"))
                .body("companyStatus", equalTo("CHANGED"));

        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/application/effective/" + firstCompanyId)
        .then()
                .statusCode(200)
                .body("plugins", contains("jta_functional", "jta_custom_a"));
        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/application/effective/" + secondCompanyId)
        .then()
                .statusCode(200)
                .body("plugins", contains("jta_functional", "jta_custom_b"));

        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/application/contributions/" + firstCompanyId)
        .then()
                .statusCode(200)
                .body("plugins", contains("jta_functional", "jta_custom_a"))
                .body("capabilities", contains(
                        "jta_functional.capability", "jta_custom_a.capability"))
                .body("permissions", contains(
                        "jta_functional.permission", "jta_custom_a.permission"))
                .body("menus", contains("jta_functional.menu", "jta_custom_a.menu"));
        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/application/contributions/" + secondCompanyId)
        .then()
                .statusCode(200)
                .body("plugins", contains("jta_functional", "jta_custom_b"))
                .body("capabilities", contains(
                        "jta_functional.capability", "jta_custom_b.capability"))
                .body("permissions", contains(
                        "jta_functional.permission", "jta_custom_b.permission"))
                .body("menus", contains("jta_functional.menu", "jta_custom_b.menu"));

        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/application/screens/" + firstCompanyId)
        .then()
                .statusCode(200)
                .body("screen", equalTo("jta_functional:dashboard"))
                .body("summaryLabel", equalTo("jta_custom_a.dashboard.summary"))
                .body("summaryVisible", equalTo(true))
                .body("summaryRequired", equalTo(true))
                .body("refreshEnabled", equalTo(true))
                .body("fragmentOwners", contains("jta_custom_a"));
        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/application/screens/" + secondCompanyId)
        .then()
                .statusCode(200)
                .body("screen", equalTo("jta_functional:dashboard"))
                .body("summaryLabel", equalTo("jta_custom_b.dashboard.summary"))
                .body("summaryVisible", equalTo(false))
                .body("summaryRequired", equalTo(false))
                .body("refreshEnabled", equalTo(false))
                .body("fragmentOwners", contains("jta_custom_b"));

        assertState(firstCompanyId, true, true);
        assertState(secondCompanyId, true, true);
    }

    @Test
    void mandatoryAuditFailureRollsBackTheApplicationUseCase() {
        String companyId = UUID.randomUUID().toString();

        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/application/rollback/" + companyId)
        .then()
                .statusCode(200)
                .body("outcome", equalTo("ROLLED_BACK"));

        assertState(companyId, false, false);
    }

    @Test
    void commitsSystemAuthorityAndItsAuditInOneJtaTransaction() {
        String probeId = UUID.randomUUID().toString();

        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/system-authority/commit/" + probeId)
        .then()
                .statusCode(200)
                .body("outcome", equalTo("CHANGED"));

        assertSystemAuthorityState(probeId, 1, 1, 1, 2, 1);

        resetProbeData();
        assertSystemAuthorityState(probeId, 0, 0, 0, 0, 1);
    }

    @Test
    void rollsBackSystemAuthorityAndItsAuditAtomically() {
        String probeId = UUID.randomUUID().toString();

        given()
                .baseUri(BASE_URI)
        .when()
                .post(PROBE_PATH + "/system-authority/rollback/" + probeId)
        .then()
                .statusCode(200)
                .body("outcome", equalTo("ROLLED_BACK"));

        assertSystemAuthorityState(probeId, 0, 0, 0, 0, 0);
    }

    private void assertState(String companyId, boolean company, boolean activation) {
        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/state/" + companyId)
        .then()
                .statusCode(200)
                .body("company", equalTo(company))
                .body("activation", equalTo(activation));
    }

    private void assertSystemAuthorityState(
            String probeId,
            int users,
            int roles,
            int assignments,
            int permissions,
            int auditEvents) {
        given()
                .baseUri(BASE_URI)
        .when()
                .get(PROBE_PATH + "/system-authority/state/" + probeId)
        .then()
                .statusCode(200)
                .body("users", equalTo(users))
                .body("roles", equalTo(roles))
                .body("assignments", equalTo(assignments))
                .body("permissions", equalTo(permissions))
                .body("auditEvents", equalTo(auditEvents));
    }

    private void resetProbeData() {
        given()
                .baseUri(BASE_URI)
        .when()
                .delete(PROBE_PATH + "/reset")
        .then()
                .statusCode(204);
    }
}
