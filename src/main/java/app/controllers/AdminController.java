package app.controllers;

import app.services.UserService;
import io.javalin.http.Handler;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public class AdminController {
    private final UserService userService;

    public AdminController(EntityManagerFactory emf) {
        this.userService = new UserService(emf);
    }

    public Handler panel() {
        return ctx -> ctx.json(Map.of("ok", true, "msg", "Welcome, admin"));
    }

    public Handler users() {
        return ctx -> {
            var out = userService.getAllUsers().stream()
                    .map(u -> Map.<String, Object>of(
                            "id", u.getId(),
                            "username", u.getUsername(),
                            "role", u.getRole().name()
                    ))
                    .toList();
            ctx.json(out);
        };
    }
}