package app.services;

import app.daos.UserDAO;
import app.entities.User;
import app.security.enums.Role;
import jakarta.persistence.EntityManagerFactory;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    private final UserDAO userDAO;

    public UserService(EntityManagerFactory emf) {
        this.userDAO = new UserDAO(emf);
    }

    // Register new user and hashes password after that give user GUEST role.
    public User registerUser(String username, String password, Role role) {
        if (userDAO.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword);
        user.setRole(role != null ? role : Role.GUEST);

        return userDAO.create(user);
    }

    // Validate User
    public boolean validateUser(String username, String password) {
        User found = userDAO.findByUsername(username);
        if (found == null) return false;
        return BCrypt.checkpw(password, found.getPassword());
    }


    //Find user by username
    public User findByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    //Get by ID
    public User findById(int id) {
        return userDAO.findById(id);
    }
}
