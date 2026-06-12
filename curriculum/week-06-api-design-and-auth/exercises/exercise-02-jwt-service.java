// Exercise 2 — The JWT service
//
// Goal: Implement a JwtService that issues HS256-signed tokens and validates
//       them with jjwt. Fill in the TODOs. The service must mint a token whose
//       subject is the user id, with iat + exp claims, and validate an incoming
//       token by VERIFYING THE SIGNATURE AND EXPIRY — returning the user id for
//       a good token and Optional.empty() for a tampered, expired, or malformed
//       one.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
// 1. You are extending the week-5 Crunch Tracker Spring Boot 3 project (Java 21).
//    Add the jjwt dependency to your pom.xml:
//
//      <dependency>
//        <groupId>io.jsonwebtoken</groupId>
//        <artifactId>jjwt-api</artifactId>
//        <version>0.12.6</version>
//      </dependency>
//      <dependency>
//        <groupId>io.jsonwebtoken</groupId>
//        <artifactId>jjwt-impl</artifactId>
//        <version>0.12.6</version>
//        <scope>runtime</scope>
//      </dependency>
//      <dependency>
//        <groupId>io.jsonwebtoken</groupId>
//        <artifactId>jjwt-jackson</artifactId>
//        <version>0.12.6</version>
//        <scope>runtime</scope>
//      </dependency>
//
// 2. Add the config to application.yml (generate a real secret:
//    `openssl rand -base64 64`):
//
//      crunch:
//        jwt:
//          secret: "REPLACE_WITH_openssl_rand_base64_64_OUTPUT_AT_LEAST_256_BITS"
//          ttl: PT15M        # ISO-8601 duration: 15 minutes
//
// 3. Drop this file in src/main/java/dev/crunch/tracker/security/JwtService.java
//    and fill in every `// TODO`. Do NOT change the public method signatures —
//    the test at the bottom (copy it into src/test/java/...) drives exactly
//    these methods.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `./mvnw test` passes the JwtServiceTest below.
//   [ ] issue(user) produces a 3-part token; sub == user id; exp is in the future.
//   [ ] validateAndGetUserId returns the id for a fresh token.
//   [ ] validateAndGetUserId returns EMPTY for: a tampered token, an expired
//       token, a malformed string, and a token signed with a DIFFERENT secret.
//   [ ] The secret is injected from config — there is no hard-coded secret.
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

package dev.crunch.tracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${crunch.jwt.secret}") String secret,
            @Value("${crunch.jwt.ttl:PT15M}") Duration ttl) {
        // TODO: build an HS256-capable SecretKey from the secret bytes.
        //       Hint: Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
        //       This throws if the secret is < 256 bits — that's intentional.
        this.key = null;   // TODO
        this.ttl = ttl;
    }

    /**
     * Mint a signed token whose subject is the given user id.
     * Sets iat (now), exp (now + ttl), and a random jti.
     */
    public String issue(long userId) {
        // TODO: return Jwts.builder()
        //          .subject(String.valueOf(userId))
        //          .issuedAt(...).expiration(...).id(UUID...)
        //          .signWith(key).compact();
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Issue a token with an explicit ttl. Used by tests to mint an already-expired
     * token (pass a negative duration). Production code calls issue(userId).
     */
    public String issueWithTtl(long userId, Duration explicitTtl) {
        Instant now = Instant.now();
        // TODO: same as issue() but use explicitTtl instead of the field.
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Validate the signature AND expiry, then return the subject as a user id.
     * Returns Optional.empty() for any invalid token (bad signature, expired,
     * malformed, wrong secret). NEVER throws to the caller.
     */
    public Optional<Long> validateAndGetUserId(String token) {
        // TODO:
        //   try {
        //       Claims claims = Jwts.parser()
        //               .verifyWith(key)
        //               .clockSkewSeconds(30)
        //               .build()
        //               .parseSignedClaims(token)
        //               .getPayload();
        //       return Optional.of(Long.valueOf(claims.getSubject()));
        //   } catch (JwtException | IllegalArgumentException ex) {
        //       return Optional.empty();
        //   }
        return Optional.empty();   // TODO
    }

    /** Token lifetime in seconds — handy for the TokenResponse.expiresIn field. */
    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}

// ============================================================================
// COPY THE TEST BELOW INTO:
//   src/test/java/dev/crunch/tracker/security/JwtServiceTest.java
// (delete it from this file — a .java file can hold only one public top-level
//  type per file when compiled in the main source set).
// ============================================================================
//
// package dev.crunch.tracker.security;
//
// import org.junit.jupiter.api.Test;
// import java.time.Duration;
// import java.util.Optional;
// import static org.assertj.core.api.Assertions.assertThat;
//
// class JwtServiceTest {
//
//     // 64 random base64 chars -> well over 256 bits, valid for HS256.
//     private static final String SECRET =
//             "3rT9pQ2sV8wX1zB4nM6kJ0hG5fD7aS2qW9eR3tY6uI8oP1lK4jH7gF0dS3aZ6xC9v";
//     private final JwtService jwt = new JwtService(SECRET, Duration.ofMinutes(15));
//
//     @Test
//     void round_trips_the_user_id() {
//         String token = jwt.issue(42L);
//         assertThat(token.split("\\.")).hasSize(3);          // header.payload.signature
//         assertThat(jwt.validateAndGetUserId(token)).contains(42L);
//     }
//
//     @Test
//     void rejects_a_tampered_token() {
//         String good = jwt.issue(42L);
//         String tampered = good.substring(0, good.length() - 3) + "xxx";
//         assertThat(jwt.validateAndGetUserId(tampered)).isEmpty();
//     }
//
//     @Test
//     void rejects_an_expired_token() {
//         String expired = jwt.issueWithTtl(42L, Duration.ofSeconds(-60));
//         assertThat(jwt.validateAndGetUserId(expired)).isEmpty();
//     }
//
//     @Test
//     void rejects_garbage() {
//         assertThat(jwt.validateAndGetUserId("not.a.jwt")).isEmpty();
//         assertThat(jwt.validateAndGetUserId("")).isEmpty();
//     }
//
//     @Test
//     void rejects_a_token_signed_with_a_different_secret() {
//         String otherSecret =
//                 "9zZ8yY7xX6wW5vV4uU3tT2sS1rR0qQ9pP8oO7nN6mM5lL4kK3jJ2iI1hH0gG9fF8e";
//         JwtService attacker = new JwtService(otherSecret, Duration.ofMinutes(15));
//         String forged = attacker.issue(42L);
//         assertThat(jwt.validateAndGetUserId(forged)).isEmpty();   // our key rejects it
//     }
// }
//
// ============================================================================
// HINTS (read only if stuck >15 min)
// ============================================================================
//
// Constructor:
//   this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//
// issue:
//   Instant now = Instant.now();
//   return Jwts.builder()
//           .subject(String.valueOf(userId))
//           .issuedAt(Date.from(now))
//           .expiration(Date.from(now.plus(ttl)))
//           .id(UUID.randomUUID().toString())
//           .signWith(key)
//           .compact();
//
// issueWithTtl: identical but `.expiration(Date.from(now.plus(explicitTtl)))`.
//   A negative explicitTtl puts exp in the past -> the token is born expired.
//
// validateAndGetUserId: see the commented body in the method above. The KEY
//   insight is that parseSignedClaims THROWS on a bad signature or expiry; you
//   catch it and return empty. You never re-implement HMAC by hand.
//
// ============================================================================
