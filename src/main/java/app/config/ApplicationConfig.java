package app.config;

import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.exceptions.ValidationException;
import app.routes.Routes;
import app.routes.handling.RouteDocs;
import app.security.utils.JwtUtil;
import app.utils.Utils;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private static int counter = 1;

    public static void configuration(JavalinConfig config) {
        config.showJavalinBanner = false;
        config.router.contextPath = "/api";
        // (We’re using our own /api/routes overview via RouteDocs; not the built-in plugin)
    }

    public static Javalin startServer(int port) {
        // Build EMF first (used by route builder)
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        // Create server and mount the EndpointGroup (Javalin 6 style)
        Javalin server = Javalin.create(cfg -> {
            configuration(cfg);
            cfg.router.apiBuilder(new Routes().api(emf));
        });

        // Our custom routes overview (public)
        server.get("/routes", RouteDocs.overviewHtml);

        // Global JWT guard; allow /api/routes and /api/auth/*
        server.before(ctx -> {
            // If you enable CORS later, consider allowing OPTIONS here.
            String p = ctx.path(); // includes contextPath, e.g. /api/...
            boolean isPublic =
                    p.equals("/routes") || p.equals("/api/routes") ||
                            p.startsWith("/auth/") || p.startsWith("/api/auth/") ||
                            p.startsWith("/public/") || p.startsWith("/api/public/");
            if (isPublic) return;

            String header = ctx.header("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw NotAuthorizedException.unauthorized("Missing or invalid Authorization header");
            }
            String token = header.substring("Bearer ".length()).trim();
            if (!JwtUtil.validateToken(token)) {
                throw NotAuthorizedException.unauthorized("Invalid or expired token");
            }

            ctx.attribute("jwt.user", JwtUtil.getUsername(token));
            ctx.attribute("jwt.role", JwtUtil.getRole(token));
        });

        // Exceptions
        server.exception(ValidationException.class, (e, ctx) ->
                ctx.status(400).json(Utils.convertToJsonMessage(ctx, "error", e.getMessage()))
        );
        server.exception(NotAuthorizedException.class, (e, ctx) ->
                ctx.status(e.getStatus() == 0 ? 401 : e.getStatus())
                        .json(Utils.convertToJsonMessage(ctx, "error", e.getMessage()))
        );
        server.exception(ApiException.class, ApplicationConfig::apiExceptionHandler);
        server.exception(Exception.class, ApplicationConfig::generalExceptionHandler);

        // Logging
        server.after(ApplicationConfig::afterRequest);

        server.start(port);
        logger.info("Server started on http://localhost:{}{}", port, "/api");
        return server;
    }

    public static void stopServer(Javalin server) {
        server.stop();
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