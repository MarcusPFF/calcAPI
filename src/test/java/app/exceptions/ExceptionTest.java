package app.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void apiExceptionShouldStoreStatusCode() {
        ApiException ex = new ApiException(404, "Not found");
        assertEquals(404, ex.getStatusCode());
        assertEquals("Not found", ex.getMessage());
    }

    @Test
    void notAuthorizedExceptionShouldHaveStatus() {
        NotAuthorizedException ex = new NotAuthorizedException("Unauthorized", 401);
        assertEquals(401, ex.getStatus());
        assertEquals("Unauthorized", ex.getMessage());
    }

    @Test
    void notAuthorizedExceptionUnauthorizedFactoryShouldCreateWith401() {
        NotAuthorizedException ex = NotAuthorizedException.unauthorized("Access denied");
        assertEquals(401, ex.getStatus());
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void validationExceptionShouldStoreMessage() {
        ValidationException ex = new ValidationException("Invalid input");
        assertEquals("Invalid input", ex.getMessage());
    }
}

