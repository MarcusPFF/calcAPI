package app.security.utils;

import app.entities.User;
import app.security.enums.Role;
import app.utils.Utils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public class JwtUtil {

    private static volatile boolean loaded = false;
    private static byte[] SECRET_BYTES;
    private static String ISSUER;
    private static long EXPIRE_MS;

    private static void loadOnce() {
        if (loaded) return;
        synchronized (JwtUtil.class) {
            if (loaded) return;

            String secret = null;
            try {
                secret = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            } catch (Exception ignored) {}
            if (secret == null) {
                secret = System.getenv("SECRET_KEY");
                if (secret == null) secret = System.getenv("SECRET_KEY");
            }
            if (secret == null) secret = "change-me-please-32-bytes-minimum!!";
            if (secret.length() < 32) {
                secret = String.format("%-32s", secret).replace(' ', '0');
            }
            SECRET_BYTES = secret.getBytes(StandardCharsets.UTF_8);

            String issuer = null;
            try {
                issuer = Utils.getPropertyValue("ISSUER", "config.properties");
            } catch (Exception ignored) {}
            if (issuer == null) issuer = System.getenv("JWT_ISSUER");
            ISSUER = issuer != null ? issuer : "app";

            long ttlMs = 3600000L; // default 1h
            try {
                String ttl = Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties"); // ms
                if (ttl != null) ttlMs = Long.parseLong(ttl.trim());
            } catch (Exception ignored) {
                String envTtl = System.getenv("JWT_TTL_MS");
                if (envTtl != null) {
                    try { ttlMs = Long.parseLong(envTtl.trim()); } catch (Exception ignored2) {}
                }
            }
            if (ttlMs < 1000L) ttlMs = 3600000L; // sanity
            EXPIRE_MS = ttlMs;

            loaded = true;
        }
    }

    public static String generateToken(User user) throws JOSEException {
        loadOnce();
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(EXPIRE_MS)))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET_BYTES));
        return jwt.serialize();
    }

    public static boolean validateToken(String token) {
        try {
            loadOnce();
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(SECRET_BYTES))) return false;

            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            if (exp == null || exp.before(new Date())) return false;

            String iss = jwt.getJWTClaimsSet().getIssuer();
            return iss == null || iss.equals(ISSUER);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getUsername(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public static Role getRole(String token) {
        try {
            String role = (String) SignedJWT.parse(token).getJWTClaimsSet().getClaim("role");
            return Role.valueOf(role);
        } catch (Exception e) {
            return Role.ANYONE;
        }
    }
}