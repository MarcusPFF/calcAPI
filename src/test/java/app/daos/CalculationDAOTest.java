package app.daos;

import app.config.HibernateConfig;
import app.entities.Calculation;
import app.entities.User;
import app.security.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CalculationDAOTest {

    private PostgreSQLContainer<?> postgres;
    private EntityManagerFactory emf;
    private CalculationDAO calcDAO;
    private UserDAO userDAO;
    private User testUser;

    @BeforeAll
    void setup() {
        postgres = new PostgreSQLContainer<>("postgres:15.3-alpine3.18")
                .withDatabaseName("test_calc_dao")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();

        HibernateConfig.setTest(true);
        emf = HibernateConfig.createNewEntityManagerFactoryForTest();
        calcDAO = new CalculationDAO(emf);
        userDAO = new UserDAO(emf);

        // Create a test user
        testUser = new User();
        testUser.setUsername("DaoTestUser");
        testUser.setPassword("test123");
        testUser.setRole(Role.GUEST);
        testUser = userDAO.create(testUser);
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) emf.close();
        postgres.stop();
    }

    @Test
    void createShouldPersistCalculation() {
        Calculation calc = new Calculation();
        calc.setNum1(5);
        calc.setNum2(10);
        calc.setResult(15);
        calc.setOperation("ADD");
        calc.setTimestamp(LocalDateTime.now());
        calc.setUser(testUser);

        Calculation saved = calcDAO.create(calc);
        assertNotNull(saved.getId());
        assertEquals(15, saved.getResult());
    }

    @Test
    void findByIdShouldReturnCalculation() {
        Calculation calc = new Calculation();
        calc.setNum1(3);
        calc.setNum2(4);
        calc.setResult(7);
        calc.setOperation("ADD");
        calc.setTimestamp(LocalDateTime.now());
        calc.setUser(testUser);
        calc = calcDAO.create(calc);

        Calculation found = calcDAO.findById(calc.getId());
        assertNotNull(found);
        assertEquals(7, found.getResult());
    }

    @Test
    void getAllShouldReturnAllCalculations() {
        calcDAO.create(createTestCalc(1, 2, 3, "ADD"));
        calcDAO.create(createTestCalc(4, 5, 9, "ADD"));

        List<Calculation> all = calcDAO.getAll();
        assertNotNull(all);
        assertTrue(all.size() >= 2);
    }

    @Test
    void updateShouldModifyCalculation() {
        Calculation calc = calcDAO.create(createTestCalc(10, 20, 30, "ADD"));
        calc.setResult(100);

        Calculation updated = calcDAO.update(calc);
        assertEquals(100, updated.getResult());
    }

    @Test
    void deleteShouldRemoveCalculation() {
        Calculation calc = calcDAO.create(createTestCalc(1, 1, 2, "ADD"));
        int id = calc.getId();

        calcDAO.delete(id);

        Calculation found = calcDAO.findById(id);
        assertNull(found);
    }

    @Test
    void findAllByUserShouldReturnUserCalculations() {
        calcDAO.create(createTestCalc(1, 1, 2, "ADD"));
        calcDAO.create(createTestCalc(2, 2, 4, "ADD"));

        List<Calculation> userCalcs = calcDAO.findAllByUser(testUser);
        assertTrue(userCalcs.size() >= 2);
    }

    @Test
    void findByIdShouldReturnNullForNonExistent() {
        Calculation found = calcDAO.findById(999999);
        assertNull(found);
    }

    private Calculation createTestCalc(double n1, double n2, double res, String op) {
        Calculation calc = new Calculation();
        calc.setNum1(n1);
        calc.setNum2(n2);
        calc.setResult(res);
        calc.setOperation(op);
        calc.setTimestamp(LocalDateTime.now());
        calc.setUser(testUser);
        return calc;
    }
}

