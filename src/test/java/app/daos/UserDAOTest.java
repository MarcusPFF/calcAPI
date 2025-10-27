package app.daos;

import app.config.HibernateConfig;
import app.entities.User;
import app.security.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserDAOTest {

    private PostgreSQLContainer<?> postgres;
    private EntityManagerFactory emf;
    private UserDAO userDAO;
    private User testUser;

    @BeforeAll
    void setup() {
        postgres = new PostgreSQLContainer<>("postgres:15.3-alpine3.18")
                .withDatabaseName("test_user_dao")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();

        HibernateConfig.setTest(true);
        emf = HibernateConfig.createNewEntityManagerFactoryForTest();
        userDAO = new UserDAO(emf);

        testUser = new User();
        testUser.setUsername("DaoUser");
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
    void createShouldPersistUser() {
        User user = new User();
        user.setUsername("CreateTestUser");
        user.setPassword("pass123");
        user.setRole(Role.ADMIN);

        User saved = userDAO.create(user);
        assertNotNull(saved.getId());
        assertEquals("CreateTestUser", saved.getUsername());
        assertEquals(Role.ADMIN, saved.getRole());
    }

    @Test
    void findByIdShouldReturnUser() {
        User found = userDAO.findById(testUser.getId());
        assertNotNull(found);
        assertEquals("DaoUser", found.getUsername());
    }

    @Test
    void getAllShouldReturnAllUsers() {
        User newUser = new User();
        newUser.setUsername("GetAllUser");
        newUser.setPassword("pass");
        newUser.setRole(Role.GUEST);
        userDAO.create(newUser);

        List<User> all = userDAO.getAll();
        assertNotNull(all);
        assertTrue(all.size() >= 2);
    }

    @Test
    void updateShouldModifyUser() {
        testUser.setRole(Role.ADMIN);
        User updated = userDAO.update(testUser);
        assertEquals(Role.ADMIN, updated.getRole());
    }

    @Test
    void deleteShouldRemoveUser() {
        User user = new User();
        user.setUsername("DeleteTestUser");
        user.setPassword("pass");
        user.setRole(Role.GUEST);
        user = userDAO.create(user);
        int id = user.getId();

        userDAO.delete(id);

        User found = userDAO.findById(id);
        assertNull(found);
    }

    @Test
    void findByUsernameShouldReturnUser() {
        User found = userDAO.findByUsername("DaoUser");
        assertNotNull(found);
        assertEquals(testUser.getId(), found.getId());
    }

    @Test
    void findByUsernameShouldReturnNullForNonExistent() {
        User found = userDAO.findByUsername("NonExistentUser12345");
        assertNull(found);
    }

    @Test
    void findByIdShouldReturnNullForNonExistent() {
        User found = userDAO.findById(999999);
        assertNull(found);
    }
}

