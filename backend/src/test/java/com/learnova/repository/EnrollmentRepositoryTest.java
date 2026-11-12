package com.learnova.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.learnova.model.Course;
import com.learnova.model.Enrollment;
import com.learnova.model.EnrollmentStatus;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class EnrollmentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User student1;
    private User student2;
    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        student1 = User.builder()
                .keycloakId("kc-student-1")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .username("alicesmith")
                .status(UserStatus.ACTIVE)
                .build();
        student1 = entityManager.persistAndFlush(student1);

        student2 = User.builder()
                .keycloakId("kc-student-2")
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@example.com")
                .username("bobjones")
                .status(UserStatus.ACTIVE)
                .build();
        student2 = entityManager.persistAndFlush(student2);

        course1 = new Course();
        course1.setName("Docker Mastery");
        course1.setDescription("Learn Docker from scratch");
        course1.setPrice(new BigDecimal("49.99"));
        course1 = entityManager.persistAndFlush(course1);

        course2 = new Course();
        course2.setName("Kubernetes Deep Dive");
        course2.setDescription("Orchestration at scale");
        course2.setPrice(new BigDecimal("89.99"));
        course2 = entityManager.persistAndFlush(course2);
    }

    @Test
    void shouldSaveEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .student(student1)
                .course(course1)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);

        assertNotNull(saved.getId());
        assertEquals(student1.getId(), saved.getStudent().getId());
        assertEquals(course1.getId(), saved.getCourse().getId());
        assertEquals(EnrollmentStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getEnrolledAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldFindById() {
        Enrollment enrollment = Enrollment.builder()
                .student(student1)
                .course(course1)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        Enrollment persisted = entityManager.persistAndFlush(enrollment);
        entityManager.clear();

        Optional<Enrollment> found = enrollmentRepository.findById(persisted.getId());

        assertTrue(found.isPresent());
        assertEquals(persisted.getId(), found.get().getId());
        assertEquals(student1.getId(), found.get().getStudent().getId());
        assertEquals(course1.getId(), found.get().getCourse().getId());
    }

    @Test
    void shouldCheckExistsByStudentIdAndCourseId() {
        Enrollment enrollment = Enrollment.builder()
                .student(student1)
                .course(course1)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(enrollment);
        entityManager.clear();

        assertTrue(enrollmentRepository.existsByStudentIdAndCourseId(student1.getId(), course1.getId()));
        assertFalse(enrollmentRepository.existsByStudentIdAndCourseId(student1.getId(), course2.getId()));
        assertFalse(enrollmentRepository.existsByStudentIdAndCourseId(student2.getId(), course1.getId()));
    }

    @Test
    void shouldFindByStudentIdAndCourseId() {
        Enrollment enrollment = Enrollment.builder()
                .student(student1)
                .course(course1)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(enrollment);
        entityManager.clear();

        Optional<Enrollment> found = enrollmentRepository.findByStudentIdAndCourseId(student1.getId(), course1.getId());
        assertTrue(found.isPresent());
        assertEquals(course1.getId(), found.get().getCourse().getId());

        Optional<Enrollment> notFound = enrollmentRepository.findByStudentIdAndCourseId(student1.getId(), course2.getId());
        assertTrue(notFound.isEmpty());
    }

    @Test
    void shouldFindByIdAndStudentId() {
        Enrollment enrollment = Enrollment.builder()
                .student(student1)
                .course(course1)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        Enrollment persisted = entityManager.persistAndFlush(enrollment);
        entityManager.clear();

        Optional<Enrollment> found = enrollmentRepository.findByIdAndStudentId(persisted.getId(), student1.getId());
        assertTrue(found.isPresent());

        Optional<Enrollment> wrongStudent = enrollmentRepository.findByIdAndStudentId(persisted.getId(), student2.getId());
        assertTrue(wrongStudent.isEmpty());
    }

    @Test
    void shouldFindByStudentIdWithPagination() {
        Enrollment e1 = Enrollment.builder().student(student1).course(course1).status(EnrollmentStatus.ACTIVE).build();
        Enrollment e2 = Enrollment.builder().student(student1).course(course2).status(EnrollmentStatus.CANCELLED).build();
        entityManager.persist(e1);
        entityManager.persist(e2);
        entityManager.flush();
        entityManager.clear();

        Page<Enrollment> page = enrollmentRepository.findByStudentId(student1.getId(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void shouldFindByStudentIdAndStatusWithPagination() {
        Enrollment e1 = Enrollment.builder().student(student1).course(course1).status(EnrollmentStatus.ACTIVE).build();
        Enrollment e2 = Enrollment.builder().student(student1).course(course2).status(EnrollmentStatus.CANCELLED).build();
        entityManager.persist(e1);
        entityManager.persist(e2);
        entityManager.flush();
        entityManager.clear();

        Page<Enrollment> activePage = enrollmentRepository.findByStudentIdAndStatus(
                student1.getId(), EnrollmentStatus.ACTIVE, PageRequest.of(0, 10));
        assertEquals(1, activePage.getTotalElements());
        assertEquals(course1.getId(), activePage.getContent().get(0).getCourse().getId());

        Page<Enrollment> cancelledPage = enrollmentRepository.findByStudentIdAndStatus(
                student1.getId(), EnrollmentStatus.CANCELLED, PageRequest.of(0, 10));
        assertEquals(1, cancelledPage.getTotalElements());
        assertEquals(course2.getId(), cancelledPage.getContent().get(0).getCourse().getId());
    }

    @Test
    void shouldFindByCourseIdWithPagination() {
        Enrollment e1 = Enrollment.builder().student(student1).course(course1).status(EnrollmentStatus.ACTIVE).build();
        Enrollment e2 = Enrollment.builder().student(student2).course(course1).status(EnrollmentStatus.ACTIVE).build();
        entityManager.persist(e1);
        entityManager.persist(e2);
        entityManager.flush();
        entityManager.clear();

        Page<Enrollment> page = enrollmentRepository.findByCourseId(course1.getId(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void shouldFindByCourseIdAndStatusWithPagination() {
        Enrollment e1 = Enrollment.builder().student(student1).course(course1).status(EnrollmentStatus.ACTIVE).build();
        Enrollment e2 = Enrollment.builder().student(student2).course(course1).status(EnrollmentStatus.COMPLETED).build();
        entityManager.persist(e1);
        entityManager.persist(e2);
        entityManager.flush();
        entityManager.clear();

        Page<Enrollment> activeRoster = enrollmentRepository.findByCourseIdAndStatus(
                course1.getId(), EnrollmentStatus.ACTIVE, PageRequest.of(0, 10));
        assertEquals(1, activeRoster.getTotalElements());
        assertEquals(student1.getId(), activeRoster.getContent().get(0).getStudent().getId());
    }

    @Test
    void shouldFailWhenDuplicateEnrollment() {
        Enrollment e1 = Enrollment.builder().student(student1).course(course1).status(EnrollmentStatus.ACTIVE).build();
        entityManager.persistAndFlush(e1);

        Enrollment duplicate = Enrollment.builder().student(student1).course(course1).status(EnrollmentStatus.ACTIVE).build();

        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }

    @Test
    void shouldFailWhenInvalidStatusConstraintViolated() {
        assertThrows(Exception.class, () -> {
            entityManager.getEntityManager()
                    .createNativeQuery("INSERT INTO enrollment (student_id, course_id, status, enrolled_at, updated_at) "
                            + "VALUES (:studentId, :courseId, 'INVALID_STATUS', NOW(), NOW())")
                    .setParameter("studentId", student1.getId())
                    .setParameter("courseId", course1.getId())
                    .executeUpdate();
            entityManager.flush();
        });
    }
}
