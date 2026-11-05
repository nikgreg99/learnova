package com.learnova.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.learnova.config.keycloak.KeycloakAdminProperties;
import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserRequest;
import com.learnova.exception.UserProfileConflictException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class KeycloakAdminClientTest {

    private static final String BASE_URL = "http://localhost:8081";
    private static final String REALM = "learnova";
    private static final String ACCESS_TOKEN = "test-access-token";
    private static final String KEYCLOAK_USER_ID = "user-123";

    private KeycloakAdminClient keycloakAdminClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(builder).build();

        KeycloakAdminProperties properties = new KeycloakAdminProperties("http", "localhost", 8081, REALM,
                new KeycloakAdminProperties.Admin("learnova-admin-client", "test-client-secret"));

        keycloakAdminClient = new KeycloakAdminClient(builder, properties);
    }

    @Test
    void createUser_shouldReturnKeycloakId() {
        // Arrange
        UserRequest request = createUserRequest();

        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
                        {
                            "username": "john.doe",
                            "email": "john.doe@example.com",
                            "firstName": "John",
                            "lastName": "Doe",
                            "enabled": true,
                            "credentials": [
                                {
                                    "type": "password",
                                    "value": "Password123!",
                                    "temporary": false
                                }
                            ]
                        }
                        """)).andRespond(withCreatedEntity(
                        URI.create(BASE_URL + "/admin/realms/" + REALM + "/users/" + KEYCLOAK_USER_ID)));

        // Act
        String result = keycloakAdminClient.createUser(request);

        // Assert
        assertThat(result).isEqualTo(KEYCLOAK_USER_ID);
        mockServer.verify();
    }

    @Test
    void setUserEnabled_shouldUpdateKeycloakAccountStatus() {
        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users/" + KEYCLOAK_USER_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
                        {"enabled": false}
                        """)).andRespond(withSuccess());

        keycloakAdminClient.setUserEnabled(KEYCLOAK_USER_ID, false);

        mockServer.verify();
    }

    @Test
    void setUserEnabled_shouldEnableKeycloakAccount() {
        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users/" + KEYCLOAK_USER_ID))
                .andExpect(method(HttpMethod.PUT)).andExpect(content().json("""
                        {"enabled": true}
                        """)).andRespond(withSuccess());

        keycloakAdminClient.setUserEnabled(KEYCLOAK_USER_ID, true);

        mockServer.verify();
    }

    @Test
    void updateUserProfile_shouldUpdateKeycloakUserDetails() {
        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users/" + KEYCLOAK_USER_ID))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(content().json("""
                        {
                          "username": "updated",
                          "email": "updated@example.com",
                          "firstName": "Updated",
                          "lastName": "User"
                        }
                        """)).andRespond(withSuccess());

        keycloakAdminClient.updateUserProfile(KEYCLOAK_USER_ID,
                new UserProfileUpdateRequest("Updated", "User", "updated@example.com", "updated"));

        mockServer.verify();
    }

    @Test
    void updateUserProfile_shouldTranslateKeycloakConflict() {
        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users/" + KEYCLOAK_USER_ID))
                .andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> keycloakAdminClient.updateUserProfile(KEYCLOAK_USER_ID,
                new UserProfileUpdateRequest("Updated", "User", "taken@example.com", "updated")))
                .isInstanceOfSatisfying(UserProfileConflictException.class,
                        exception -> assertThat(exception.getConflictingField()).isEqualTo("email or username"));

        mockServer.verify();
    }

    @Test
    void findTestUsers_shouldReturnOnlyTestAccountsWithRealmRoles() {
        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users?first=0&max=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)).andRespond(withSuccess("""
                        [
                          {
                            "id": "test-user-1",
                            "username": "test-student",
                            "email": "test-student@learnova.local",
                            "firstName": "Test",
                            "lastName": "Student",
                            "enabled": true
                          },
                          {
                            "id": "regular-user",
                            "username": "regular-user",
                            "email": "regular@learnova.local",
                            "enabled": true
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users/test-user-1/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)).andRespond(withSuccess("""
                        [
                          {"id": "role-student", "name": "STUDENT"},
                          {"id": "default-role", "name": "default-roles-learnova"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<KeycloakAdminClient.TestUser> users = keycloakAdminClient.findTestUsers();

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().keycloakId()).isEqualTo("test-user-1");
        assertThat(users.getFirst().username()).isEqualTo("test-student");
        assertThat(users.getFirst().realmRoles()).containsExactlyInAnyOrder("STUDENT", "default-roles-learnova");
        mockServer.verify();
    }

    @Test
    void createUser_shouldThrowExceptionWhenLocationHeaderIsMissing() {
        // Arrange
        UserRequest request = createUserRequest();

        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users"))
                .andRespond(withStatus(HttpStatus.CREATED));

        // Act & Assert
        assertThatThrownBy(() -> keycloakAdminClient.createUser(request)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Keycloak did not return the created user location");

        mockServer.verify();
    }

    @Test
    void createUser_shouldThrowExceptionWhenLocationDoesNotContainUserId() {
        // Arrange
        UserRequest request = createUserRequest();

        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users"))
                .andRespond(withCreatedEntity(URI.create(BASE_URL + "/admin/realms/" + REALM + "/users/")));

        // Act & Assert
        assertThatThrownBy(() -> keycloakAdminClient.createUser(request)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to retrieve Keycloak user ID");

        mockServer.verify();
    }

    @Test
    void createUser_shouldThrowExceptionWhenAdminTokenIsMissing() {
        // Arrange
        UserRequest request = createUserRequest();

        mockServer.expect(requestTo(BASE_URL + "/realms/" + REALM + "/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> keycloakAdminClient.createUser(request)).isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to obtain Keycloak admin token");

        mockServer.verify();
    }

    @Test
    void createUser_shouldPropagateConflictWhenUserAlreadyExists() {
        // Arrange
        UserRequest request = createUserRequest();

        mockTokenResponse();

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users")).andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        // Act & Assert
        assertThatThrownBy(() -> keycloakAdminClient.createUser(request))
                .isInstanceOf(HttpClientErrorException.Conflict.class);

        mockServer.verify();
    }

    private UserRequest createUserRequest() {
        return new UserRequest("John", "Doe", "john.doe@example.com", "john.doe", Set.of("STUDENT", "INSTRUCTOR"),
                "Password123!");
    }

    private void mockTokenResponse() {
        mockServer.expect(requestTo(BASE_URL + "/realms/" + REALM + "/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED)).andRespond(withSuccess("""
                        {
                            "access_token": "%s"
                        }
                        """.formatted(ACCESS_TOKEN), MediaType.APPLICATION_JSON));
    }
}
