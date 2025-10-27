package app.services;

import app.config.HibernateConfig;
import app.entities.User;
import app.security.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceAdditionalTest {

    private PostgreSQLContainer<?> postgres;
    private EntityManagerFactory emf;
    private UserService userService;

    @BeforeAll
    void setup() {
        postgres = new PostgreSQLContainer<>("postgres:15.3-alpine3.18")
                .withDatabaseName("test_user_additional")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();

        HibernateConfig.setTest(true);
        emf = HibernateConfig.createNewEntityManagerFactoryForTest();
        userService = new UserService(emf);
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) emf.close();
        postgres.stop();
    }

    @Test
    void registerUserShouldThrowExceptionWhenUsernameExists() {
        userService.registerUser("ExistingUser", "pass123", Role.GUEST);
        
        assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser("ExistingUser", "differentPass", Role.ADMIN);
        });
    }

    @Test
    void registerUserWithNullRoleShouldDefaultToGuest() {
        User user = userService.registerUser("NullRoleUser", "pass123", null);
        assertEquals(Role.GUEST, user.getRole());
    }

    @Test
    void validateUserShouldReturnFalseForNonExistentUser() {
        boolean valid = userService.validateUser("NonExistentUser", "password");
        assertFalse(valid);
    }

    @Test
    void findByUsernameShouldReturnUserWithDifferentCases() {
        userService.registerUser("CaseTest", "pass123", Role.GUEST);
        User found = userService.findByUsername("CaseTest");
        assertNotNull(found);
    }

    @Test
    void registerUserShouldHashPassword() {
        User user = userService.registerUser("HashTest", "plainPassword", Role.GUEST);
        assertNotEquals("plainPassword", user.getPassword());
        assertNotNull(user.getPassword());
        assertTrue(user.getPassword().length() > 20); // BCrypt hashed passwords are long
    }
}

