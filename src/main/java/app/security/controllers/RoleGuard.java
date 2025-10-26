package app.security.controllers;

import app.security.enums.Role;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;

public final class RoleGuard {
    private RoleGuard() {}

    public static void require(Context ctx, Role... allowed) {
        Role role = ctx.attribute("jwt.role");
        if (role == null) throw new ForbiddenResponse("Missing role");
        for (Role r : allowed) {
            if (r == role) return; // allowed
        }
        throw new ForbiddenResponse("Forbidden");
    }

    public static void requireAdmin(Context ctx) {
        require(ctx, Role.ADMIN);
    }
}