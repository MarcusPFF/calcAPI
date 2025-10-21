package app.config;

import app.entities.Calculation;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that Hibernate can bootstrap correctly,
 * create tables via JPA, and persist simple entities in a temporary
 * PostgreSQL database managed by Testcontainers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateConfigTest {

    private EntityManagerFactory emf;

    @BeforeAll
    void setup() {
        // Use Hibernate's built-in Testcontainers setup
        HibernateConfig.setTest(true);
        emf = HibernateConfig.getEntityManagerFactoryForTest();
    }

    @AfterAll
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    @DisplayName("Hibernate should create tables and persist User + Calculation entities")
    void shouldCreateTablesAndPersistEntities() {
        EntityManager em = emf.createEntityManager();
        assertNotNull(em, "EntityManager should be initialized");

        em.getTransaction().begin();

        // Arrange: create a test user
        User user = new User();
        user.setUsername("Marcus");
        user.setPassword("test123");
        user.setRole(app.security.enums.Role.GUEST);
        em.persist(user);

        // Arrange: create a linked calculation
        Calculation calc = new Calculation();
        calc.setNum1(5);
        calc.setNum2(10);
        calc.setResult(15);
        calc.setOperation("ADD");
        calc.setTimestamp(java.time.LocalDateTime.now());
        calc.setUser(user);
        em.persist(calc);

        // Act: commit transaction
        em.getTransaction().commit();

        // Assert: IDs generated and relationship persisted
        assertNotNull(user.getId(), "User ID should be generated");
        assertNotNull(calc.getId(), "Calculation ID should be generated");

        Calculation found = em.find(Calculation.class, calc.getId());
        assertEquals(15, found.getResult(), "Calculation result should match");

        em.close();
    }
}
