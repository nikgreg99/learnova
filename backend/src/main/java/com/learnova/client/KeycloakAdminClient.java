package com.learnova.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learnova.config.keycloak.KeycloakAdminProperties;
import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserRequest;
import com.learnova.exception.UserProfileConflictException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class KeycloakAdminClient {

    private static final int USER_PAGE_SIZE = 100;
    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminClient(RestClient.Builder restClientBuilder, KeycloakAdminProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public String createUser(UserRequest request) {

        String accessToken = getAdminToken();

        UserRepresentation user = new UserRepresentation(request.username(), request.email(), request.firstName(),
                request.lastName(), true, List.of(new CredentialRepresentation("password", request.password(), false)));

        var response = restClient.post().uri(adminUsersUri()).headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON).body(user).retrieve().toBodilessEntity();

        return getKeycloakId(response);
    }

    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        String accessToken = getAdminToken();

        restClient.put().uri(adminUserUri(keycloakUserId)).headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON).body(new UserStatusRepresentation(enabled)).retrieve()
                .toBodilessEntity();
    }

    public void updateUserProfile(String keycloakUserId, UserProfileUpdateRequest request) {
        String accessToken = getAdminToken();

        try {
            restClient.put().uri(adminUserUri(keycloakUserId)).headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON).body(new UserProfileRepresentation(request.username(),
                            request.email(), request.firstName(), request.lastName()))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new UserProfileConflictException("email or username");
        }
    }

    public List<TestUser> findTestUsers() {
        String accessToken = getAdminToken();
        List<TestUser> testUsers = new ArrayList<>();

        for (int first = 0;; first += USER_PAGE_SIZE) {
            List<KeycloakUserRepresentation> users = restClient.get()
                    .uri(adminUsersUri() + "?first=" + first + "&max=" + USER_PAGE_SIZE)
                    .headers(headers -> headers.setBearerAuth(accessToken)).retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (users == null || users.isEmpty()) {
                break;
            }

            for (KeycloakUserRepresentation user : users) {
                if (isTestUser(user)) {
                    testUsers.add(new TestUser(user.id(), user.username(), user.email(), user.firstName(),
                            user.lastName(), user.enabled(), getRealmRoles(user.id(), accessToken)));
                }
            }

            if (users.size() < USER_PAGE_SIZE) {
                break;
            }
        }

        return List.copyOf(testUsers);
    }

    private static boolean isTestUser(KeycloakUserRepresentation user) {
        return user.id() != null && user.username() != null && user.email() != null
                && user.username().startsWith("test-");
    }

    private Set<String> getRealmRoles(String keycloakUserId, String accessToken) {
        List<RealmRoleRepresentation> roles = restClient.get()
                .uri(adminUserUri(keycloakUserId) + "/role-mappings/realm")
                .headers(headers -> headers.setBearerAuth(accessToken)).retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (roles == null) {
            return Set.of();
        }

        Set<String> roleNames = new HashSet<>();
        for (RealmRoleRepresentation role : roles) {
            roleNames.add(role.name());
        }
        return Set.copyOf(roleNames);
    }

    private static @NonNull String getKeycloakId(ResponseEntity<Void> response) {
        URI location = response.getHeaders().getLocation();

        if (location == null) {
            throw new IllegalStateException("Keycloak did not return the created user location");
        }

        String path = location.getPath();
        String keycloakId = path.substring(path.lastIndexOf('/') + 1);

        if (keycloakId.isBlank()) {
            throw new IllegalStateException("Unable to retrieve Keycloak user ID");
        }
        return keycloakId;
    }

    private String getAdminToken() {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.admin().clientId());
        form.add("client_secret", properties.admin().clientSecret());

        TokenResponse response = restClient.post().uri(tokenUri()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Unable to obtain Keycloak admin token");
        }

        return response.accessToken();
    }

    private String tokenUri() {
        return baseUrl() + "/realms/" + properties.realm() + "/protocol/openid-connect/token";
    }

    private String adminUsersUri() {
        return baseUrl() + "/admin/realms/" + properties.realm() + "/users";
    }

    private String adminUserUri(String keycloakUserId) {
        return adminUsersUri() + "/" + keycloakUserId;
    }

    private String baseUrl() {
        return properties.baseUrl();
    }

    private record TokenResponse(
            @JsonProperty("access_token")
            String accessToken) {
    }

    private record UserRepresentation(
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            List<CredentialRepresentation> credentials) {
    }

    private record UserStatusRepresentation(
            boolean enabled) {
    }

    private record UserProfileRepresentation(
            String username,
            String email,
            String firstName,
            String lastName) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record KeycloakUserRepresentation(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled) {
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record RealmRoleRepresentation(
            String name) {
    }

    private record CredentialRepresentation(
            String type,
            String value,
            boolean temporary) {
    }

    public record TestUser(
            String keycloakId,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            Set<String> realmRoles) {
    }
}
