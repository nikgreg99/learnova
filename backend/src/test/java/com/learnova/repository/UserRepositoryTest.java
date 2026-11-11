package com.learnova.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnova.model.User;
import com.learnova.model.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().keycloakId("kc-test-user-1").firstName("John").lastName("Doe")
                .email("john.doe@example.com").username("johndoe").status(UserStatus.ACTIVE).build();
    }

    @Test
    void shouldSaveAndFindUser() {
        User saved = userRepository.save(sampleUser);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals("john.doe@example.com", saved.getEmail());
        assertEquals("johndoe", saved.getUsername());
    }

    @Test
    void shouldFindByKeycloakId() {
        User persisted = entityManager.persistAndFlush(sampleUser);
        entityManager.clear();

        Optional<User> found = userRepository.findByKeycloakId(persisted.getKeycloakId());

        assertTrue(found.isPresent());
        assertEquals("john.doe@example.com", found.get().getEmail());
    }

    @Test
    void shouldFindByEmail() {
        User persisted = entityManager.persistAndFlush(sampleUser);
        entityManager.clear();

        Optional<User> found = userRepository.findByEmail(persisted.getEmail());

        assertTrue(found.isPresent());
        assertEquals("johndoe", found.get().getUsername());
    }

    @Test
    void shouldFindByUsername() {
        User persisted = entityManager.persistAndFlush(sampleUser);
        entityManager.clear();

        Optional<User> found = userRepository.findByUsername(persisted.getUsername());

        assertTrue(found.isPresent());
        assertEquals("john.doe@example.com", found.get().getEmail());
    }

    @Test
    void shouldCheckExistsByEmailAndIdNot() {
        User user1 = entityManager.persistAndFlush(sampleUser);

        User user2 = User.builder().keycloakId("kc-test-user-2").firstName("Jane").lastName("Doe")
                .email("jane.doe@example.com").username("janedoe").status(UserStatus.ACTIVE).build();
        User persistedUser2 = entityManager.persistAndFlush(user2);
        entityManager.clear();

        assertTrue(userRepository.existsByEmailAndIdNot("john.doe@example.com", persistedUser2.getId()));
        assertFalse(userRepository.existsByEmailAndIdNot("john.doe@example.com", user1.getId()));
        assertFalse(userRepository.existsByEmailAndIdNot("other@example.com", user1.getId()));
    }

    @Test
    void shouldCheckExistsByUsernameAndIdNot() {
        User user1 = entityManager.persistAndFlush(sampleUser);

        User user2 = User.builder().keycloakId("kc-test-user-2").firstName("Jane").lastName("Doe")
                .email("jane.doe@example.com").username("janedoe").status(UserStatus.ACTIVE).build();
        User persistedUser2 = entityManager.persistAndFlush(user2);
        entityManager.clear();

        assertTrue(userRepository.existsByUsernameAndIdNot("johndoe", persistedUser2.getId()));
        assertFalse(userRepository.existsByUsernameAndIdNot("johndoe", user1.getId()));
        assertFalse(userRepository.existsByUsernameAndIdNot("nonexistent", user1.getId()));
    }
}
