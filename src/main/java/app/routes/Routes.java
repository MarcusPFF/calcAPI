package app.routes;

import app.controllers.AdminController;
import app.controllers.CalculationController;
import app.controllers.PublicController;
import app.security.controllers.AuthController;
import app.security.enums.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static app.routes.handling.RouteDocs.*;

public class Routes {

    public EndpointGroup api(EntityManagerFactory emf) {
        var auth = new AuthController(emf);
        var calc = new CalculationController(emf);
        var pub = new PublicController(emf);
        var admin = new AdminController(emf);

        return () -> {
            // Auth – (An
            path("/auth", () -> {
                get("/healthcheck", ctx -> ctx.json(java.util.Map.of("msg", "API is up and running")), Role.ANYONE);
                post("/login", auth.login, Role.ANYONE);
                post("/register", auth.register, Role.ANYONE);
            });

            // Public (Public Endpoints)
            path("/public", () -> {
                get("/info", pub.info(), Role.ANYONE);
                get("/stats", pub.stats(), Role.ANYONE);
                get("/examples", pub.examples(), Role.ANYONE);
                get("/calculations", calc.getAll(), Role.ANYONE);
            });

            // Admin (Admin-protected)
            path("/admin", () -> {
                get("/panel", admin.panel(), Role.ADMIN);
                get("/users", admin.users(), Role.ADMIN);
            });

            // Calc (Role-guarded)
            path("/calc", () -> {
                post("/add", calc.add(), Role.GUEST, Role.ADMIN);
                post("/subtract", calc.subtract(), Role.GUEST, Role.ADMIN);
                post("/multiply", calc.multiply(), Role.ADMIN);
                post("/divide", calc.divide(), Role.ADMIN);
                get("/calculations", calc.getMine(), Role.GUEST, Role.ADMIN);
                delete("/calculations/{id}", calc.deleteById(), Role.ADMIN);
            });
        };
    }
}