package py.com.logixone.integration.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;

import org.junit.jupiter.api.Test;

class HealthEndpointsIT {

    private static final String BASE_URI = requiredBaseUri();

    @Test
    void livenessExposesTheMinimalApplicationState() {
        given()
                .baseUri(BASE_URI)
        .when()
                .get("/logixone/health/live")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .header("Cache-Control", "no-store")
                .body("status", equalTo("UP"))
                .body("checks.name", contains("application"))
                .body("checks.status", everyItem(equalTo("UP")));
    }

    @Test
    void readinessExposesAllTechnicalChecksInDeterministicOrder() {
        given()
                .baseUri(BASE_URI)
        .when()
                .get("/logixone/health/ready")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .header("Cache-Control", "no-store")
                .body("status", equalTo("UP"))
                .body("checks.name", contains(
                        "catalog", "configuration", "database", "migrations", "oidc-configuration"))
                .body("checks.status", everyItem(equalTo("UP")));
    }

    private static String requiredBaseUri() {
        String baseUri = System.getProperty("logixone.base-uri");
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalStateException("logixone.base-uri is required for runtime integration tests");
        }
        return baseUri;
    }
}
