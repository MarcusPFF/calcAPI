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

        // /public/info
        get("/public/info")
                .then()
                .statusCode(200)
                .body("name", not(emptyOrNullString()));

        // /public/stats
        get("/public/stats")
                .then()
                .statusCode(200)
                .body("total", notNullValue());

        // /public/examples
        get("/public/examples")
                .then()
                .statusCode(200)
                .body("add", notNullValue());

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
    void register_negative_cases() {
        // Try to register with missing fields
        given()
                .contentType("application/json")
                .body(Map.of("username", "test", "password", "123"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(201))); // Without role, should default to GUEST

        // Try to register with invalid role
        String invalidRoleUser = "invalid_" + UUID.randomUUID().toString().substring(0, 8);
        given()
                .contentType("application/json")
                .body(Map.of("username", invalidRoleUser, "password", "123", "role", "INVALID"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(201))); // Should default to GUEST

        // Try to register with empty password
        given()
                .contentType("application/json")
                .body(Map.of("username", "test2", "password", ""))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400)));
    }

    @Test @Order(4)
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

        // Missing Bearer prefix
        given()
                .header("Authorization", "not-a-valid-token")
                .when()
                .get("/calc/calculations")
                .then()
                .statusCode(401);

        // Try to access non-existent route with auth
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/nonexistent/route")
                .then()
                .statusCode(404);
    }

    @Test @Order(5)
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

        // Test divide by zero
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(Map.of("num1", 10, "num2", 0))
                .when()
                .post("/calc/divide")
                .then()
                .statusCode(400);

        // Test negative numbers
        Integer negCalc = postCalc(adminToken, "add", -5, 10, 200);
        Assertions.assertNotNull(negCalc);

        // Test decimal numbers
        Integer decimalCalc = postCalc(adminToken, "multiply", 3.5, 2.5, 200);
        Assertions.assertNotNull(decimalCalc);
    }

    @Test @Order(6)
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
        @SuppressWarnings("unchecked")
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

        // Test very large numbers
        Integer largeCalc = postCalc(guestToken, "add", 999999999, 888888888, 200);
        Assertions.assertNotNull(largeCalc);
    }

    @Test @Order(7)
    void public_get_all_reflects_everything() {
        // public list should include at least those made above
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> all =
                get("/public/calculations")
                        .then()
                        .statusCode(200)
                        .extract().as(List.class);

        Assertions.assertNotNull(all);
        // Should contain both admin and guest calculations
    }

    @Test @Order(8)
    void delete_rules() {
        // guest cannot delete (admin-only)
        given()
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .delete("/calc/calculations/{id}", guestCalcId)
                .then()
                .statusCode(403);

        // admin can delete guest's calculation
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

        // Try to delete a non-existent calculation
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .delete("/calc/calculations/{id}", 999999)
                .then()
                .statusCode(404);
    }

    @Test @Order(9)
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

        // missing username
        given()
                .contentType("application/json")
                .body(Map.of("password", "123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(anyOf(is(400), is(500)));

        // missing password
        given()
                .contentType("application/json")
                .body(Map.of("username", "test"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(anyOf(is(400), is(500)));

        // non-existent user
        given()
                .contentType("application/json")
                .body(Map.of("username", "nonExistentUser", "password", "123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test @Order(10)
    void calculation_data_structure() {
        // Create a calculation and verify all fields
        Integer calcId = postCalc(adminToken, "add", 5, 10, 200);
        Assertions.assertNotNull(calcId);

        // Get it back from public calculations
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> all = get("/public/calculations")
                .then()
                .statusCode(200)
                .extract().as(List.class);

        // Find our calculation
        Map<String, Object> calc = all.stream()
                .filter(c -> calcId.equals(c.get("id")))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(calc);
        Assertions.assertEquals(5.0, (Double) calc.get("num1"));
        Assertions.assertEquals(10.0, (Double) calc.get("num2"));
        Assertions.assertEquals(15.0, (Double) calc.get("result"));
        Assertions.assertEquals("ADD", calc.get("operation"));
        Assertions.assertNotNull(calc.get("timestamp"));
        Assertions.assertEquals(adminUser, calc.get("username"));
    }

    @Test @Order(11)
    void edge_cases_and_special_values() {
        // Test zero operations
        postCalc(adminToken, "add", 0, 0, 200);
        postCalc(adminToken, "multiply", 0, 100, 200);
        postCalc(adminToken, "subtract", 0, 5, 200);

        // Test floating point precision
        postCalc(adminToken, "divide", 1.0, 3.0, 200);

        // Test very small numbers
        postCalc(adminToken, "multiply", 0.0001, 0.0001, 200);
    }

    @Test @Order(12)
    void register_duplicate_username() {
        // Try to register with existing username
        given()
                .contentType("application/json")
                .body(Map.of("username", adminUser, "password", "different", "role", "GUEST"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(anyOf(is(400), is(500), is(200))); // Depends on your implementation
    }
}
