package app.routes.handling;

import app.security.enums.Role;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleGuardTest {

    @Test
    void requireShouldAllowAccessWhenRoleMatches() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("jwt.role")).thenReturn(Role.ADMIN);

        assertDoesNotThrow(() -> RoleGuard.require(ctx, Role.ADMIN));
    }

    @Test
    void requireShouldThrowWhenRoleIsNull() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("jwt.role")).thenReturn(null);

        assertThrows(ForbiddenResponse.class, () -> RoleGuard.require(ctx, Role.ADMIN));
    }

    @Test
    void requireShouldThrowWhenRoleDoesNotMatch() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("jwt.role")).thenReturn(Role.GUEST);

        assertThrows(ForbiddenResponse.class, () -> RoleGuard.require(ctx, Role.ADMIN));
    }

    @Test
    void requireShouldAllowAccessWithMultipleRoles() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("jwt.role")).thenReturn(Role.GUEST);

        assertDoesNotThrow(() -> RoleGuard.require(ctx, Role.ADMIN, Role.GUEST));
    }

    @Test
    void requireAdminShouldThrowWhenNotAdmin() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("jwt.role")).thenReturn(Role.GUEST);

        assertThrows(ForbiddenResponse.class, () -> RoleGuard.requireAdmin(ctx));
    }

    @Test
    void requireAdminShouldAllowWhenAdmin() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("jwt.role")).thenReturn(Role.ADMIN);

        assertDoesNotThrow(() -> RoleGuard.requireAdmin(ctx));
    }
}

