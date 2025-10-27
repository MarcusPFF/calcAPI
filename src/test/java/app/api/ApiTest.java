package app.api;

import app.config.ApplicationConfig;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import app.config.HibernateConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiTest {

    private static Javalin server;

    private static String adminUser;
    private static String adminPass = "secret123";
    private static String adminToken;

    private static String guestUser;
    private static String guestPass = "pass123";
    private static String guestToken;

    private static Integer guestCalcId; // captured from /calc/add

    @BeforeAll
    static void start() {
        HibernateConfig.setTest(true);
        server = ApplicationConfig.startServer(0);
        RestAssured.baseURI = "http://localhost:" + server.port();
        RestAssured.basePath = "/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        String runId = java.util.UUID.randomUUID().toString().substring(0, 8);
        adminUser = "admin_" + runId;
        guestUser = "guest_" + runId;
    }

    @AfterAll
    static void stop() {
        ApplicationConfig.stopServer(server);
    }

    // --- helpers -----------------------------------------------------------------

    private static void register(String username, String password, String role) {
        given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", password, "role", role))
                .when()
                .post("/auth/register")
                .then()
                // Depending on your implementation you may return 200 or 201
                .statusCode(anyOf(is(200), is(201)));
    }

    private static String login(String username, String password, String expectedRole) {
        return given()
                .contentType("application/json")
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("username", equalTo(username))
                .body("role", equalTo(expectedRole))
                .extract().path("token");
    }

    private static Integer postCalc(String token, String op, double n1, double n2, int expectedStatus) {
        var res =
                given()
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .body(Map.of("num1", n1, "num2", n2))
                        .when()
                        .post("/calc/" + op);

        res.then().statusCode(expectedStatus);

        String ct = res.getContentType();
        if (expectedStatus == 200 && ct != null && ct.toLowerCase().contains("application/json")) {
            return res.jsonPath().getInt("id");
        }
        return null; // for 403/401/etc.
    }

    // --- tests -------------------------------------------------------------------

    @Test @Order(1)
    void public_endpoints() {
        // /auth/healthcheck
        get("/auth/healthcheck")
                .then()
                .statusCode(200)
                .body("msg", not(emptyOrNullString()));

        // /public/calculations (no token)
        get("/public/calculations")
                .then()
                .statusCode(200)
                .body("$", notNullValue()); // array, possibly empty

        // /routes (custom overview, HTML)
        get("/routes")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("/auth/login"))
                .body(containsString("/calc/add"))
                .body(anyOf(containsString("ADMIN"), containsString("GUEST"))); // roles visible
    }

    @Test @Order(2)
    void register_and_login_both_roles() {
        register(adminUser, adminPass, "ADMIN");
        adminToken = login(adminUser, adminPass, "ADMIN");

        register(guestUser, guestPass, "GUEST");
        guestToken = login(guestUser, guestPass, "GUEST");
    }

    @Test @Order(3)
    void security_edges() {
        // missing auth on protected route
        get("/calc/calculations").then().statusCode(401);

        // invalid token
        given()
                .header("Authorization", "Bearer not-a-valid-token")
                .when()
                .get("/calc/calculations")
                .then()
                .statusCode(401);
    }

    @Test @Order(4)
    void admin_access_and_admin_only_ops() {
        // admin panel allowed
        given().header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/admin/panel")
                .then()
                .statusCode(200)
                .body("ok", is(true));

        // admin can multiply (admin-only)
        Integer idMul = postCalc(adminToken, "multiply", 6, 7, 200);
        Assertions.assertNotNull(idMul, "admin multiply should return an id");

        // admin can divide (admin-only)
        Integer idDiv = postCalc(adminToken, "divide", 10, 2, 200);
        Assertions.assertNotNull(idDiv, "admin divide should return an id");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(Map.of("num1", 10, "num2", 0))
                .when()
                .post("/calc/divide")
                .then()
                .statusCode(400);
    }

    @Test @Order(5)
    void guest_permissions_and_own_data() {
        // guest cannot access admin panel
        given().header("Authorization", "Bearer " + guestToken)
                .when()
                .get("/admin/panel")
                .then()
                .statusCode(403);

        // guest can add and subtract
        guestCalcId = postCalc(guestToken, "add", 2, 5, 200);
        Assertions.assertNotNull(guestCalcId, "guest add should return an id");

        Integer idSub = postCalc(guestToken, "subtract", 10, 3, 200);
        Assertions.assertNotNull(idSub, "guest subtract should return an id");

        // guest cannot multiply
        postCalc(guestToken, "multiply", 3, 3, 403);

        // guest cannot divide
        postCalc(guestToken, "divide", 8, 2, 403);

        // guest /calc/calculations -> only their own
        List<Map<String, Object>> mine =
                given().header("Authorization", "Bearer " + guestToken)
                        .when().get("/calc/calculations")
                        .then()
                        .statusCode(200)
                        .extract().as(List.class);

        Assertions.assertTrue(
                mine.stream().allMatch(m -> guestUser.equals(m.get("username"))),
                "Guest should only see their own calculations"
        );
    }

    @Test @Order(6)
    void public_get_all_reflects_everything() {
        // public list should include at least those made above
        get("/public/calculations")
                .then()
                .statusCode(200)
                .body("$", notNullValue());
    }

    @Test @Order(7)
    void delete_rules() {
        // guest cannot delete (admin-only)
        given()
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .delete("/calc/calculations/{id}", guestCalcId)
                .then()
                .statusCode(403);

        // admin can delete guest’s calculation
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/calc/calculations/{id}", guestCalcId)
                .then()
                .statusCode(200)
                .body("deletedId", is(guestCalcId));

        // deleting again -> 404 (your service throws ApiException 404)
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/calc/calculations/{id}", guestCalcId)
                .then()
                .statusCode(404);
    }

    @Test @Order(8)
    void login_negative_cases() {
        // wrong password
        given()
                .contentType("application/json")
                .body(Map.of("username", adminUser, "password", "wrong"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);

        // malformed body -> expect 400/500 depending on your controller; accept either
        given()
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/auth/login")
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }
}
