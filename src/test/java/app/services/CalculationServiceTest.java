package app.services;

import app.config.HibernateConfig;
import app.entities.Calculation;
import app.entities.User;
import app.exceptions.ValidationException;
import app.security.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CalculationServiceTest {

    private PostgreSQLContainer<?> postgres;
    private EntityManagerFactory emf;
    private CalculationService calcService;
    private UserService userService;
    private User testUser;

    @BeforeAll
    void setup() {
        postgres = new PostgreSQLContainer<>("postgres:15.3-alpine3.18")
                .withDatabaseName("test_calc")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();

        HibernateConfig.setTest(true);
        emf = HibernateConfig.createNewEntityManagerFactoryForTest();
        userService = new UserService(emf);
        calcService = new CalculationService(emf);

        testUser = userService.registerUser("MathUser", "password", Role.GUEST);
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) emf.close();
        postgres.stop();
    }

    @Test
    void addShouldPersistAndReturnCorrectResult() {
        Calculation c = calcService.add(testUser, 5, 10);
        assertEquals(15, c.getResult());
        assertEquals("ADD", c.getOperation());
        assertNotNull(c.getId());
    }

    @Test
    void subtractShouldReturnCorrectResult() {
        Calculation c = calcService.subtract(testUser, 10, 4);
        assertEquals(6, c.getResult());
    }

    @Test
    void multiplyShouldReturnCorrectResult() {
        Calculation c = calcService.multiply(testUser, 3, 4);
        assertEquals(12, c.getResult());
    }

    @Test
    void divideShouldReturnCorrectResult() throws ValidationException {
        Calculation c = calcService.divide(testUser, 10, 2);
        assertEquals(5, c.getResult());
    }

    @Test
    void divideShouldThrowExceptionWhenDividingByZero() {
        assertThrows(ArithmeticException.class, () -> calcService.divide(testUser, 10, 0));
    }

    @Test
    void findByIdShouldReturnPersistedCalculation() {
        Calculation c = calcService.add(testUser, 1, 2);
        Calculation found = calcService.findById(c.getId());
        assertNotNull(found);
        assertEquals(3, found.getResult());
    }

    @Test
    void findAllByUserShouldReturnAllUserCalculations() {
        calcService.add(testUser, 2, 2);
        calcService.subtract(testUser, 8, 3);
        List<Calculation> list = calcService.findAllByUser(testUser);
        assertTrue(list.size() >= 2);
    }
}