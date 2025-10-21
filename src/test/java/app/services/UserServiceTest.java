package app.services;

import app.config.HibernateConfig;
import app.entities.User;
import app.security.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mindrot.jbcrypt.BCrypt;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3-alpine3.18").withDatabaseName("test_db").withUsername("postgres").withPassword("postgres");

    private EntityManagerFactory emf;
    private UserService userService;
    private User testUser;

    @BeforeAll
    void setup() {
        postgres.start();
        HibernateConfig.setTest(true);
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        userService = new UserService(emf);

        testUser = userService.registerUser("Marcus", "secret123", Role.ADMIN);
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) emf.close();
        postgres.stop();
    }

    @Test
    void registerUserShouldHashPasswordAndAssignRole() {
        assertNotNull(testUser.getId());
        assertNotEquals("secret123", testUser.getPassword());
        assertTrue(BCrypt.checkpw("secret123", testUser.getPassword()));
        assertEquals(Role.ADMIN, testUser.getRole());
    }

    @Test
    void registerUserShouldDefaultToGuestWhenRoleIsNull() {
        User u = userService.registerUser("GuestUser", "1234", null);
        assertEquals(Role.GUEST, u.getRole());
    }

    @Test
    void validateUserShouldReturnTrueWhenPasswordMatches() {
        boolean valid = userService.validateUser("Marcus", "secret123");
        assertTrue(valid);
    }

    @Test
    void validateUserShouldReturnFalseWhenPasswordIsWrong() {
        boolean valid = userService.validateUser("Marcus", "wrongPass");
        assertFalse(valid);
    }

    @Test
    void findByIdShouldReturnUser() {
        User found = userService.findById(testUser.getId());
        assertNotNull(found);
        assertEquals("Marcus", found.getUsername());
    }

    @Test
    void findByUsernameShouldReturnUser() {
        User found = userService.findByUsername("Marcus");
        assertNotNull(found);
        assertEquals(testUser.getId(), found.getId());
    }
}