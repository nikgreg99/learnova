package com.learnova.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnova.model.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RoleRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldFindAllByNameIn() {
        List<Role> roles = roleRepository.findAllByNameIn(List.of("STUDENT", "INSTRUCTOR"));

        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> "STUDENT".equals(r.getName())));
        assertTrue(roles.stream().anyMatch(r -> "INSTRUCTOR".equals(r.getName())));
    }
}
