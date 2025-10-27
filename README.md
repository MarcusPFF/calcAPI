# Calculator API

A simple REST API built with Java (Javalin + JPA) that allows authenticated users to perform calculations. Results are stored in a PostgreSQL database with user attribution.

## Description

This API provides basic mathematical operations (add, subtract, multiply, divide) with role-based access control. All calculations are saved to the database, and users can view their own calculation history.

## Endpoints & Role Access
- http://localhost:7070/api/routes - routes overview with roles
- http://calcAPI.marcuspff.com/api/routes - for deployed version
### Public Endpoints (No Auth Required)
- `GET /api/auth/healthcheck` - Check API status
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - Register new user
- `GET /api/public/info` - API information
- `GET /api/public/stats` - Calculation statistics
- `GET /api/public/examples` - API usage examples
- `GET /api/public/calculations` - View all calculations (public)

### Guest User Endpoints
- `POST /api/calc/add` - Addition
- `POST /api/calc/subtract` - Subtraction
- `GET /api/calc/calculations` - View my calculations

### Admin Only Endpoints
- `POST /api/calc/multiply` - Multiplication
- `POST /api/calc/divide` - Division
- `DELETE /api/calc/calculations/{id}` - Delete calculation by ID
- `GET /api/admin/panel` - Admin panel info
- `GET /api/admin/users` - List all users

## Security

### Password Hashing
- Passwords are hashed using **BCrypt** before storage
- Only the hash is saved in the database, never plain text passwords
- BCrypt handles salt generation automatically

### JWT Authentication
- All protected endpoints require a Bearer token in the Authorization header
- Format: `Authorization: Bearer <token>`
- Tokens contain: username, role, and expiration time
- Token validation is automatic for protected routes

## Tech Stack

**Framework**
- Javalin 6.3.0 (REST framework)

**Database & ORM**
- PostgreSQL 42.7.4
- Hibernate 6.2.4 (JPA)

**Security**
- TokenSecurity library (JWT)
- jBCrypt 0.4 (password hashing)

**Utilities**
- Jackson (JSON processing)
- Lombok (code generation)
- SLF4J + Logback (logging)
- HikariCP (connection pooling)

**Testing**
- JUnit 5
- REST Assured
- Testcontainers

## Error Handling

| Status Code | Meaning |
|-------------|---------|
| **200** | OK - Request successful |
| **400** | Bad Request - Invalid input or validation error |
| **401** | Unauthorized - Missing or invalid token |
| **403** | Forbidden - Insufficient role permissions |
| **500** | Internal Server Error - Unexpected server error |

Error responses include a JSON object with `error`, `status`, and `message` fields.

## Getting Started

1. Configure database connection in `src/main/resources/config.properties`
2. Set SECRET_KEY for JWT (or use default)
3. Run the application: `mvn clean package && java -jar target/app.jar`
4. Server starts on `http://localhost:7070/api`

## Example Usage

```bash
# Register a user
curl -X POST http://localhost:7070/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"secret","role":"GUEST"}'

# Login
curl -X POST http://localhost:7070/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"secret"}'

# Perform calculation (with token from login)
curl -X POST http://localhost:7070/api/calc/add \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{"num1":10,"num2":5}'
```
