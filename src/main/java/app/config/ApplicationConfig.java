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
    }

    public static Javalin startServer(int port) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        Javalin server = Javalin.create(cfg -> {
            configuration(cfg);

//            cfg.bundledPlugins.enableCors(cors -> {
//
//                cors.addRule(rule -> {
//
//                    rule.allowHost("https://marcuspff.com", "https://www.marcuspff.com", "http://localhost:7070");
//
//                });
//
            cfg.router.apiBuilder(new Routes().api(emf));
        });

        server.before(ctx -> corsHeaders(ctx));
        server.options("/*", ctx -> corsHeadersOptions(ctx));

        server.get("/routes", ctx -> {
            String accept = String.valueOf(ctx.header("Accept")).toLowerCase();
            if (accept.contains("application/json") || "json".equals(ctx.queryParam("format"))) {
                ctx.contentType("application/json");
                ctx.json(RouteDocs.overviewJson());
            } else {
                RouteDocs.overviewHtml.handle(ctx);
            }
        });
        server.get("/", ctx -> ctx.redirect(ctx.contextPath() + "/routes"));

        // Global JWT GUARD
        // Global JWT GUARD
        server.before(ctx -> {
            if ("OPTIONS".equals(ctx.method())) return;

            String base = ctx.contextPath();
            String p = ctx.path();

            // 1. Define Public Routes
            boolean isPublic = p.equals(base + "/")
                    || p.equals(base + "/routes")
                    || p.startsWith(base + "/auth/")
                    || p.startsWith(base + "/public/")
                    || p.equals(base + "/calc/add")
                    || p.equals(base + "/calc/subtract");

            String header = ctx.header("Authorization");

            // 2. Scenario A: User HAS a Token (Admin/User)
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring("Bearer ".length()).trim();
                if (!JwtUtil.validateToken(token)) throw NotAuthorizedException.unauthorized("Invalid or expired token");

                // Assign their REAL role from the token
                ctx.attribute("jwt.user", JwtUtil.getUsername(token));
                ctx.attribute("jwt.role", JwtUtil.getRole(token));
            }
            // 3. Scenario B: User is Public (Guest) with NO Token
            else if (isPublic) {
                // GIVE THEM A GUEST BADGE!
                // This prevents "Missing role" errors in the controller
                ctx.attribute("jwt.user", "guest");
                ctx.attribute("jwt.role", "GUEST"); // or "any", depending on your Enum
            }
            // 4. Scenario C: Private Route + No Token = BLOCK
            else {
                throw NotAuthorizedException.unauthorized("Missing or invalid Authorization header");
            }
        });

        server.exception(ValidationException.class, (e, ctx) -> ctx.status(400).json(Utils.convertToJsonMessage(ctx, "error", e.getMessage())));
        server.exception(NotAuthorizedException.class, (e, ctx) -> ctx.status(e.getStatus() == 0 ? 401 : e.getStatus()).json(Utils.convertToJsonMessage(ctx, "error", e.getMessage())));
        server.exception(ApiException.class, ApplicationConfig::apiExceptionHandler);
        server.exception(Exception.class, ApplicationConfig::generalExceptionHandler);

        server.after(ApplicationConfig::afterRequest);

        server.start(port);
        // logger.info("Server started on http://localhost:{}{}", port, "/api"); // valid if logger exists
        return server;
    }

    public static void stopServer(Javalin server) {
        if (server != null) {
            server.stop();
            logger.info("Server stopped.");
        }
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

    private static void corsHeaders(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void corsHeadersOptions(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ctx.status(204);
    }
}

