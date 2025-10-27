package app.security.controllers;

import app.entities.User;
import app.security.enums.Role;
import app.security.utils.JwtUtil;
import app.services.UserService;
import io.javalin.http.Handler;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public class AuthController {

    private final UserService userService;

    public AuthController(EntityManagerFactory emf) {
        this.userService = new UserService(emf);
    }

    // /auth/healthcheck
    public Handler health() {
        return ctx -> ctx.json(Map.of("msg", "API is up and running"));
    }

    // /auth/register
    public Handler register() {
        return ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String roleString = (String) body.getOrDefault("role", "GUEST");

            if (username == null || password == null) {
                ctx.status(400).json(Map.of("error", "username and password are required"));
                return;
            }

            Role role;
            try {
                role = Role.valueOf(roleString.toUpperCase());
            } catch (Exception ignored) {
                role = Role.GUEST;
            }

            User user = userService.registerUser(username, password, role);
            ctx.json(Map.of(
                    "message", "User registered",
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            ));
        };
    }

    // /auth/login
    public Handler login() {
        return ctx -> {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");

            if (username == null || password == null) {
                ctx.status(400).json(Map.of("error", "username and password are required"));
                return;
            }

            boolean valid = userService.validateUser(username, password);
            if (!valid) {
                ctx.status(401).json(Map.of("error", "Invalid username or password"));
                return;
            }

            User user = userService.findByUsername(username);
            String token = JwtUtil.generateToken(user);

            ctx.json(Map.of(
                    "token", token,
                    "username", username,
                    "role", user.getRole().name()
            ));
        };
    }
}