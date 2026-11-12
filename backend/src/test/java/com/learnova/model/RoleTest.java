package com.learnova.model;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void role_ShouldCreateRoleUsingNoArgsConstructor() {
        Role role = new Role();

        assertNull(role.getId());
        assertNull(role.getName());
        assertNull(role.getDescription());
    }

    @Test
    void role_ShouldCreateRoleUsingAllArgsConstructor() {
        Role role = new Role(1L, "ADMIN", "Administrator role");

        assertEquals(1L, role.getId());
        assertEquals("ADMIN", role.getName());
        assertEquals("Administrator role", role.getDescription());
    }

    @Test
    void role_ShouldCreateRoleUsingBuilder() {
        Role role = Role.builder()
            .id(1L)
            .name("STUDENT")
            .description("Student role")
            .build();

        assertEquals(1L, role.getId());
        assertEquals("STUDENT", role.getName());
        assertEquals("Student role", role.getDescription());
    }

    @Test
    void role_ShouldUpdateRoleFields() {
        Role role = new Role();

        role.setId(2L);
        role.setName("INSTRUCTOR");
        role.setDescription("Instructor role");

        assertEquals(2L, role.getId());
        assertEquals("INSTRUCTOR", role.getName());
        assertEquals("Instructor role", role.getDescription());
    }

    @Test
    void role_ShouldAllowNullDescription() {
        Role role = Role.builder()
            .id(3L)
            .name("ADMIN")
            .build();

        assertNotNull(role);
        assertEquals("ADMIN", role.getName());
        assertNull(role.getDescription());
    }
}
