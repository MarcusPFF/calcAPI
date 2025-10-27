package app.security.utils;

import app.config.HibernateConfig;
import app.entities.User;
import app.security.enums.Role;
import app.services.UserService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtUtilTest {

    private PostgreSQLContainer<?> postgres;
    private EntityManagerFactory emf;
    private UserService userService;
    private User testUser;

    @BeforeAll
    void setup() {
        postgres = new PostgreSQLContainer<>("postgres:15.3-alpine3.18")
                .withDatabaseName("test_jwt")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();

        HibernateConfig.setTest(true);
        emf = HibernateConfig.createNewEntityManagerFactoryForTest();
        userService = new UserService(emf);
        testUser = userService.registerUser("jwtTestUser", "password123", Role.ADMIN);
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) emf.close();
        postgres.stop();
    }

    @Test
    void generateTokenShouldCreateValidToken() throws Exception {
        String token = JwtUtil.generateToken(testUser);
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void validateTokenShouldReturnTrueForValidToken() throws Exception {
        String token = JwtUtil.generateToken(testUser);
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    void validateTokenShouldReturnFalseForInvalidToken() {
        assertFalse(JwtUtil.validateToken("invalid-token"));
        assertFalse(JwtUtil.validateToken(""));
        assertFalse(JwtUtil.validateToken(null));
    }

    @Test
    void getUsernameShouldExtractUsernameFromToken() throws Exception {
        String token = JwtUtil.generateToken(testUser);
        String username = JwtUtil.getUsername(token);
        assertEquals("jwtTestUser", username);
    }

    @Test
    void getUsernameShouldReturnNullForInvalidToken() {
        assertNull(JwtUtil.getUsername("invalid"));
        assertNull(JwtUtil.getUsername(null));
    }

    @Test
    void getRoleShouldExtractRoleFromToken() throws Exception {
        String token = JwtUtil.generateToken(testUser);
        Role role = JwtUtil.getRole(token);
        assertEquals(Role.ADMIN, role);
    }

    @Test
    void getRoleShouldReturnAnyoneForInvalidToken() {
        Role role = JwtUtil.getRole("invalid");
        assertEquals(Role.ANYONE, role);
    }

    @Test
    void getRoleShouldHandleNullToken() {
        Role role = JwtUtil.getRole(null);
        assertEquals(Role.ANYONE, role);
    }

    @Test
    void generatedTokenShouldContainValidClaims() throws Exception {
        String token = JwtUtil.generateToken(testUser);
        assertNotNull(token);
        
        // Verify the token contains expected claims
        String username = JwtUtil.getUsername(token);
        Role role = JwtUtil.getRole(token);
        
        assertEquals("jwtTestUser", username);
        assertEquals(Role.ADMIN, role);
        assertTrue(JwtUtil.validateToken(token));
    }
}

