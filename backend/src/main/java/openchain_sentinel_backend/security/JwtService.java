package openchain_sentinel_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {

        this.signingKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(StandardCharsets.UTF_8)
                );

        this.expirationMs = expirationMs;
    }

    /*
     * Generate JWT.
     *
     * Subject = Mongo user ID.
     *
     * Additional claims:
     * username
     * email
     * role
     */
    public String generateToken(
            String userId,
            String username,
            String email,
            String role
    ) {

        Date now = new Date();

        Date expiration =
                new Date(
                        now.getTime() + expirationMs
                );

        return Jwts.builder()
                .subject(userId)

                .claim("username", username)
                .claim("email", email)
                .claim("role", role)

                .issuedAt(now)
                .expiration(expiration)

                .signWith(signingKey)
                .compact();
    }

    /*
     * Extract the JWT subject.
     *
     * In our application this is the Mongo user ID.
     */
    public String extractSubject(String token) {

        return parseClaims(token)
                .getSubject();
    }

    /*
     * Extract explicit username claim.
     */
    public String extractUsernameClaim(String token) {

        return parseClaims(token)
                .get("username", String.class);
    }

    /*
     * Extract email claim.
     */
    public String extractEmailClaim(String token) {

        return parseClaims(token)
                .get("email", String.class);
    }

    /*
     * Extract role claim.
     */
    public String extractRoleClaim(String token) {

        return parseClaims(token)
                .get("role", String.class);
    }

    /*
     * Parse and validate JWT.
     *
     * This automatically checks:
     * - signature
     * - expiration
     */
    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}