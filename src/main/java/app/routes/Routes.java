package app.routes;

import app.security.controllers.AuthController;
import app.security.enums.Role;
import io.javalin.Javalin;
import io.javalin.http.ForbiddenResponse;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public class Routes {

    public void registerAuthRoutes(Javalin app, EntityManagerFactory emf) {
        var auth = new AuthController(emf);
        app.post("/auth/register", auth.register);
        app.post("/auth/login", auth.login);
    }

    public void registerAppRoutes(Javalin app, EntityManagerFactory emf) {
        app.get("/admin/panel", ctx -> {
            Role role = ctx.attribute("jwt.role");
            if (role != Role.ADMIN) {
                throw new ForbiddenResponse("Admin only");
            }
            ctx.json(Map.of("ok", true, "msg", "Welcome, admin"));
        });
    }
}