package py.com.logixone.integration.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.path.json.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfSystemProperty(named = "logixone.oidc-probe", matches = "true")
class OidcRuntimeIT {

    private static final String APPLICATION_REALM = "logixone";
    private static final String PROTECTED_PATH = "/logixone/api/protected-probe";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final String suffix = UUID.randomUUID().toString().replace("-", "");
    private final String validClientId = "logixone-oidc-valid-" + suffix;
    private final String wrongAudienceClientId = "logixone-oidc-wrong-aud-" + suffix;
    private final String invalidIssuerRealm = "logixone-invalid-issuer-" + suffix;
    private final String invalidIssuerClientId = "logixone-invalid-issuer-client-" + suffix;
    private final String invalidIssuerUser = "logixone-invalid-issuer-user-" + suffix;

    private String applicationBaseUri;
    private String keycloakBaseUri;
    private String applicationAudience;
    private String demoUsername;
    private String demoPassword;
    private String adminToken;
    private String validClientUuid;
    private String wrongAudienceClientUuid;
    private int originalAccessTokenLifespan;
    private boolean lifespanChanged;
    private boolean invalidIssuerRealmCreated;

    @BeforeAll
    void provisionEphemeralOidcFixtures() {
        applicationBaseUri = requiredProperty("logixone.base-uri");
        keycloakBaseUri = requiredProperty("logixone.keycloak-base-uri");
        applicationAudience = System.getProperty("logixone.oidc-audience", "logixone-web");
        demoUsername = System.getProperty("logixone.demo-username", "demo.empresas.ab");
        demoPassword = readSecret("logixone.demo-user-password-file");
        String adminUsername = System.getProperty("logixone.keycloak-admin-user", "logixone-admin");
        String adminPassword = readSecret("logixone.keycloak-admin-password-file");

        adminToken = requestToken("master", "admin-cli", adminUsername, adminPassword);
        originalAccessTokenLifespan = realmAccessTokenLifespan(APPLICATION_REALM);

        validClientUuid = createPublicDirectGrantClient(APPLICATION_REALM, validClientId, applicationAudience);
        wrongAudienceClientUuid = createPublicDirectGrantClient(
                APPLICATION_REALM, wrongAudienceClientId, null);

        createRealm(invalidIssuerRealm);
        invalidIssuerRealmCreated = true;
        createPublicDirectGrantClient(invalidIssuerRealm, invalidIssuerClientId, applicationAudience);
        createUserWithPassword(invalidIssuerRealm, invalidIssuerUser, demoPassword);
    }

    @AfterAll
    void removeEphemeralOidcFixtures() {
        if (adminToken == null) {
            return;
        }
        if (lifespanChanged) {
            updateRealmAccessTokenLifespan(APPLICATION_REALM, originalAccessTokenLifespan);
            lifespanChanged = false;
        }
        if (validClientUuid != null) {
            deleteAdmin("/admin/realms/" + APPLICATION_REALM + "/clients/" + validClientUuid);
        }
        if (wrongAudienceClientUuid != null) {
            deleteAdmin("/admin/realms/" + APPLICATION_REALM + "/clients/" + wrongAudienceClientUuid);
        }
        if (invalidIssuerRealmCreated) {
            deleteAdmin("/admin/realms/" + invalidIssuerRealm);
        }
    }

    @Test
    void validIssuerSignatureAudienceAndExpirationAreAccepted() {
        String token = requestToken(APPLICATION_REALM, validClientId, demoUsername, demoPassword);

        assertEquals(204, protectedStatus(token));
    }

    @Test
    void tokenWithWrongAudienceIsDenied() {
        String token = requestToken(
                APPLICATION_REALM, wrongAudienceClientId, demoUsername, demoPassword);

        assertEquals(401, protectedStatus(token));
    }

    @Test
    void tokenFromAnotherIssuerIsDeniedEvenWithTheExpectedAudience() {
        String token = requestToken(
                invalidIssuerRealm, invalidIssuerClientId, invalidIssuerUser, demoPassword);

        assertEquals(401, protectedStatus(token));
    }

    @Test
    void expiredTokenIsDeniedAfterRealmConfigurationIsRestored() throws InterruptedException {
        try {
            updateRealmAccessTokenLifespan(APPLICATION_REALM, 1);
            lifespanChanged = true;
            String token = requestToken(APPLICATION_REALM, validClientId, demoUsername, demoPassword);
            updateRealmAccessTokenLifespan(APPLICATION_REALM, originalAccessTokenLifespan);
            lifespanChanged = false;

            Thread.sleep(2_200);

            assertEquals(401, protectedStatus(token));
        } finally {
            if (lifespanChanged) {
                updateRealmAccessTokenLifespan(APPLICATION_REALM, originalAccessTokenLifespan);
                lifespanChanged = false;
            }
        }
    }

    private String createPublicDirectGrantClient(
            String realm,
            String clientId,
            String includedAudience) {
        String body = """
                {
                  "clientId": "%s",
                  "enabled": true,
                  "protocol": "openid-connect",
                  "publicClient": true,
                  "standardFlowEnabled": false,
                  "directAccessGrantsEnabled": true,
                  "serviceAccountsEnabled": false
                }
                """.formatted(clientId);
        HttpResponse<String> created = adminRequest(
                "POST", "/admin/realms/" + realm + "/clients", body);
        requireStatus(created, 201, "create ephemeral OIDC client");

        HttpResponse<String> found = adminRequest(
                "GET",
                "/admin/realms/" + realm + "/clients?clientId=" + urlEncode(clientId),
                null);
        requireStatus(found, 200, "resolve ephemeral OIDC client");
        String clientUuid = JsonPath.from(found.body()).getString("[0].id");
        assertNotNull(clientUuid, "Keycloak did not return the ephemeral client id");

        if (includedAudience != null) {
            String mapper = """
                    {
                      "name": "logixone-expected-audience",
                      "protocol": "openid-connect",
                      "protocolMapper": "oidc-audience-mapper",
                      "consentRequired": false,
                      "config": {
                        "included.client.audience": "%s",
                        "id.token.claim": "false",
                        "access.token.claim": "true",
                        "userinfo.token.claim": "false",
                        "introspection.token.claim": "true"
                      }
                    }
                    """.formatted(includedAudience);
            HttpResponse<String> mapperCreated = adminRequest(
                    "POST",
                    "/admin/realms/" + realm + "/clients/" + clientUuid
                            + "/protocol-mappers/models",
                    mapper);
            requireStatus(mapperCreated, 201, "create ephemeral audience mapper");
        }
        return clientUuid;
    }

    private void createRealm(String realm) {
        String body = """
                {
                  "realm": "%s",
                  "enabled": true,
                  "sslRequired": "external",
                  "accessTokenLifespan": 300
                }
                """.formatted(realm);
        HttpResponse<String> response = adminRequest("POST", "/admin/realms", body);
        requireStatus(response, 201, "create ephemeral issuer realm");
    }

    private void createUserWithPassword(String realm, String username, String password) {
        HttpResponse<String> created = adminRequest(
                "POST",
                "/admin/realms/" + realm + "/users",
                "{\"username\":\"" + username
                        + "\",\"enabled\":true,\"firstName\":\"Issuer\",\"lastName\":\"Probe\""
                        + ",\"email\":\"" + username + "@demo.invalid\",\"emailVerified\":true}");
        requireStatus(created, 201, "create ephemeral issuer user");

        HttpResponse<String> found = adminRequest(
                "GET",
                "/admin/realms/" + realm + "/users?username=" + urlEncode(username) + "&exact=true",
                null);
        requireStatus(found, 200, "resolve ephemeral issuer user");
        String userUuid = JsonPath.from(found.body()).getString("[0].id");
        assertNotNull(userUuid, "Keycloak did not return the ephemeral user id");

        String credential = "{\"type\":\"password\",\"temporary\":false,\"value\":\""
                + jsonEscape(password) + "\"}";
        HttpResponse<String> reset = adminRequest(
                "PUT",
                "/admin/realms/" + realm + "/users/" + userUuid + "/reset-password",
                credential);
        requireStatus(reset, 204, "set ephemeral issuer credential");
    }

    private int realmAccessTokenLifespan(String realm) {
        HttpResponse<String> response = adminRequest("GET", "/admin/realms/" + realm, null);
        requireStatus(response, 200, "read realm token lifespan");
        Integer lifespan = JsonPath.from(response.body()).getInt("accessTokenLifespan");
        assertNotNull(lifespan, "Keycloak did not return accessTokenLifespan");
        assertTrue(lifespan > 1, "The baseline token lifespan must be greater than one second");
        return lifespan;
    }

    private void updateRealmAccessTokenLifespan(String realm, int lifespan) {
        HttpResponse<String> response = adminRequest(
                "PUT",
                "/admin/realms/" + realm,
                "{\"accessTokenLifespan\":" + lifespan + "}");
        requireStatus(response, 204, "update realm token lifespan");
    }

    private void deleteAdmin(String path) {
        HttpResponse<String> response = adminRequest("DELETE", path, null);
        if (response.statusCode() != 204 && response.statusCode() != 404) {
            throw new IllegalStateException("Keycloak cleanup failed with HTTP " + response.statusCode());
        }
    }

    private HttpResponse<String> adminRequest(String method, String path, String body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(keycloakBaseUri + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + adminToken)
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        return send(request.build());
    }

    private String requestToken(String realm, String clientId, String username, String password) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", clientId);
        form.put("username", username);
        form.put("password", password);
        String encoded = form.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElseThrow();
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        keycloakBaseUri + "/realms/" + realm + "/protocol/openid-connect/token"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encoded, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        requireStatus(response, 200, "obtain ephemeral OIDC token");
        String token = JsonPath.from(response.body()).getString("access_token");
        assertNotNull(token, "Keycloak did not return an access token");
        assertTrue(!token.isBlank(), "Keycloak returned an empty access token");
        return token;
    }

    private int protectedStatus(String token) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(applicationBaseUri + PROTECTED_PATH))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return send(request).statusCode();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", failure);
        } catch (IOException failure) {
            throw new IllegalStateException("HTTP request failed", failure);
        }
    }

    private static void requireStatus(
            HttpResponse<String> response,
            int expected,
            String operation) {
        if (response.statusCode() != expected) {
            throw new IllegalStateException(
                    operation + " failed with HTTP " + response.statusCode());
        }
    }

    private static String readSecret(String propertyName) {
        Path path = Path.of(requiredProperty(propertyName));
        try {
            String secret = Files.readString(path, StandardCharsets.UTF_8).strip();
            if (secret.isEmpty() || secret.length() > 4096 || secret.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalStateException(propertyName + " contains an invalid secret");
            }
            return secret;
        } catch (IOException failure) {
            throw new IllegalStateException(propertyName + " could not be read", failure);
        }
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for OIDC runtime tests");
        }
        return value;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
