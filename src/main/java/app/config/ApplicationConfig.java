package app.config;

import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.exceptions.ValidationException;
import app.routes.Routes;
import app.security.controllers.AuthController;
import app.security.utils.JwtUtil;
import app.security.controllers.RoleGuard;
import app.utils.Utils;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final Routes routes = new Routes();
    private static int counter = 1;

    public static void configuration(JavalinConfig config) {
        config.showJavalinBanner = false;
        config.router.contextPath = "/api";
        // Route list is available at /api/routes
        config.bundledPlugins.enableRouteOverview("/routes");
    }

    public static Javalin startServer(int port) {
        Javalin app = Javalin.create(ApplicationConfig::configuration);

        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        // Public auth routes (no token needed)
        AuthController auth = new AuthController(emf);
        app.post("/auth/register", auth.register); // -> /api/auth/register
        app.post("/auth/login", auth.login);       // -> /api/auth/login

        // JWT check for all other requests
        app.before(ctx -> {
            String p = ctx.path(); // includes the /api prefix
            boolean isPublic =
                    p.equals("/routes") || p.equals("/api/routes") ||
                            p.startsWith("/auth/") || p.startsWith("/api/auth/");
            if (isPublic) return;

            String header = ctx.header("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw NotAuthorizedException.unauthorized("Missing or invalid Authorization header");
            }
            String token = header.substring("Bearer ".length()).trim();
            if (!JwtUtil.validateToken(token)) {
                throw NotAuthorizedException.unauthorized("Invalid or expired token");
            }

            // Store claims for later use in handlers
            ctx.attribute("jwt.user", JwtUtil.getUsername(token));
            ctx.attribute("jwt.role", JwtUtil.getRole(token));
        });

        // Admin-only paths
        app.before("/admin/*", RoleGuard::requireAdmin);

        // App routes (protected by the before filters above)
        routes.registerAppRoutes(app, emf);

        // Error handling
        app.exception(ValidationException.class, (e, ctx) ->
                ctx.status(400).json(Utils.convertToJsonMessage(ctx, "error", e.getMessage()))
        );
        app.exception(NotAuthorizedException.class, (e, ctx) ->
                ctx.status(e.getStatus() == 0 ? 401 : e.getStatus())
                        .json(Utils.convertToJsonMessage(ctx, "error", e.getMessage()))
        );
        app.exception(ApiException.class, ApplicationConfig::apiExceptionHandler);
        app.exception(Exception.class, ApplicationConfig::generalExceptionHandler);

        // Simple request log
        app.after(ApplicationConfig::afterRequest);

        app.start(port);
        logger.info("Server started on http://localhost:{}{}", port, "/api");
        return app;
    }

    public static void stopServer(Javalin app) {
        app.stop();
        logger.info("Server stopped.");
    }

    private static void afterRequest(Context ctx) {
        String info = ctx.method() + " " + ctx.path();
        logger.info("Request {} - {} -> {}", counter++, info, ctx.status());
    }

    private static void generalExceptionHandler(Exception e, Context ctx) {
        logger.error("Unhandled exception", e);
        ctx.status(500).json(Utils.convertToJsonMessage(ctx, "error", e.getMessage()));
    }

    public static void apiExceptionHandler(ApiException e, Context ctx) {
        ctx.status(e.getStatusCode());
        logger.warn("API exception {}: {}", e.getStatusCode(), e.getMessage());
        ctx.json(Utils.convertToJsonMessage(ctx, "warning", e.getMessage()));
    }
}