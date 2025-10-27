package app.routes;

import app.controllers.CalculationController;
import app.security.controllers.AuthController;
import app.security.enums.Role;

import app.dtos.DTOMapper;
import app.entities.Calculation;
import app.services.CalculationService;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Handler;
import jakarta.persistence.EntityManagerFactory;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static app.routes.handling.RouteDocs.*; // wrappers (get/post/path with roles)

public class Routes {

    public EndpointGroup api(EntityManagerFactory emf) {
        AuthController auth = new AuthController(emf);
        CalculationController calc = new CalculationController(emf);
        CalculationService calcService = new CalculationService(emf);

        Handler health = ctx -> ctx.json(Map.of("msg", "API is up and running"));

        // small public helpers
        Handler info = ctx -> ctx.json(Map.of(
                "name", "CalcAPI",
                "version", "1.0",
                "time", Instant.now().toString()
        ));

        Handler stats = ctx -> {
            var all = calcService.getAll();
            var byOp = all.stream()
                    .collect(Collectors.groupingBy(Calculation::getOperation, Collectors.counting()));
            var latest = all.stream()
                    .max(Comparator.comparing(Calculation::getTimestamp))
                    .orElse(null);

            ctx.json(Map.of(
                    "total", all.size(),
                    "byOperation", byOp,
                    "latest", latest != null ? DTOMapper.toCalculationDTO(latest) : null
            ));
        };

        Handler examples = ctx -> ctx.json(Map.of(
                "add",      Map.of("method", "POST", "path", "/api/calc/add",      "body", Map.of("num1", 2,  "num2", 5)),
                "subtract", Map.of("method", "POST", "path", "/api/calc/subtract", "body", Map.of("num1", 10, "num2", 3)),
                "multiply", Map.of("method", "POST", "path", "/api/calc/multiply", "body", Map.of("num1", 6,  "num2", 7)),
                "divide",   Map.of("method", "POST", "path", "/api/calc/divide",   "body", Map.of("num1", 42, "num2", 6))
        ));

        return () -> {
            // Public group
            path("/auth", () -> {
                get("/healthcheck", health, Role.ANYONE);
                post("/login", auth.login, Role.ANYONE);
                post("/register", auth.register, Role.ANYONE);
            });

            // Public showcase endpoints
            path("/public", () -> {
                get("/info", info, Role.ANYONE);
                get("/stats", stats, Role.ANYONE);
                get("/examples", examples, Role.ANYONE);
                get("/calculations", calc.getAll, Role.ANYONE);
            });

            // Admin sample
            path("/admin", () -> {
                get("/panel", ctx -> ctx.json(Map.of("ok", true, "msg", "Welcome, admin")), Role.ADMIN);
            });

            // Calculator (role-guarded)
            path("/calc", () -> {
                post("/add",        calc.add,        Role.GUEST, Role.ADMIN);
                post("/subtract",   calc.subtract,   Role.GUEST, Role.ADMIN);
                post("/multiply",   calc.multiply,   Role.ADMIN);
                post("/divide",     calc.divide,     Role.ADMIN);
                get("/calculations",                 calc.getMine,     Role.GUEST, Role.ADMIN);
                delete("/calculations/{id}",         calc.deleteById,  Role.ADMIN);
            });
        };
    }
}