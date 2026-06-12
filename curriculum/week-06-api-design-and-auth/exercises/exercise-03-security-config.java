// Exercise 3 — The security config
//
// Goal: Wire the Spring Security 6 filter chain for a STATELESS JWT API.
//       Fill in the TODOs so that:
//         - /api/v1/auth/** is open (you can't require a token to GET a token),
//         - the actuator health + OpenAPI docs are open,
//         - every other request requires authentication,
//         - sessions are STATELESS and CSRF is disabled (token API, not cookies),
//         - your JwtAuthFilter runs BEFORE UsernamePasswordAuthenticationFilter,
//         - auth failures return an RFC 9457 ProblemDetail (401) and access
//           denials return one (403) — not an HTML error page.
//
// Estimated time: 50 minutes.
//
// HOW TO USE THIS FILE
//
// 1. You already built JwtService (exercise 2). You also need a JwtAuthFilter
//    (a OncePerRequestFilter) and an AppUser entity + AppUserRepository. The
//    filter skeleton is included at the bottom of this file — wire it up too.
//
// 2. Drop this into:
//    src/main/java/dev/crunch/tracker/security/SecurityConfig.java
//    Fill in every `// TODO`. Keep the bean names — other code depends on them.
//
// 3. Add Spring Security to pom.xml if week-5 didn't already:
//      <dependency>
//        <groupId>org.springframework.boot</groupId>
//        <artifactId>spring-boot-starter-security</artifactId>
//      </dependency>
//
// ACCEPTANCE CRITERIA
//
//   [ ] `./mvnw spring-boot:run` starts with no bean-wiring errors.
//   [ ] GET /api/v1/habits with NO token  -> 401, body is a ProblemDetail JSON.
//   [ ] POST /api/v1/auth/login           -> reachable WITHOUT a token.
//   [ ] GET /api/v1/habits WITH a valid token (from /auth/login) -> 200.
//   [ ] A request with a tampered token   -> 401 ProblemDetail.
//   [ ] The filter chain is STATELESS (no JSESSIONID cookie is ever set).
//   [ ] No use of the removed WebSecurityConfigurerAdapter anywhere.
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

package dev.crunch.tracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity                 // turns on @PreAuthorize for the ownership rule
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsSource;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CorsConfigurationSource corsSource,
                          ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsSource = corsSource;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource))
            // TODO: disable CSRF — we're a stateless token API, not cookie/session.
            //       .csrf(csrf -> csrf.disable())
            // TODO: set session creation policy to STATELESS.
            //       .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // TODO: authorizeHttpRequests:
            //         permitAll for "/api/v1/auth/**"
            //         permitAll for "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**"
            //         anyRequest().authenticated()  <-- must be LAST
            // TODO: disable httpBasic and formLogin (no browser login popup/form).
            // TODO: exceptionHandling: set authenticationEntryPoint(...) for 401
            //       and accessDeniedHandler(...) for 403, using the helpers below.
            // TODO: addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            ;
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // DelegatingPasswordEncoder => {bcrypt}$2a$... so you can migrate later.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
            throws Exception {
        return cfg.getAuthenticationManager();
    }

    // ---- RFC 9457 handlers so auth errors match the rest of the API ---------

    private AuthenticationEntryPoint problemEntryPoint() {
        return (request, response, authException) ->
                writeProblem(response, HttpStatus.UNAUTHORIZED,
                        "Authentication required",
                        "A valid Bearer token is required to access this resource.");
    }

    private AccessDeniedHandler problemAccessDenied() {
        return (request, response, accessDeniedException) ->
                writeProblem(response, HttpStatus.FORBIDDEN,
                        "Access denied",
                        "You are authenticated but not allowed to perform this action.");
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status,
                              String title, String detail) throws java.io.IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), pd);
    }
}

// ============================================================================
// SUPPORTING FILTER — put this in its own file:
//   src/main/java/dev/crunch/tracker/security/JwtAuthFilter.java
// Fill in its TODOs too. (Shown here so the exercise is self-contained.)
// ============================================================================
//
// package dev.crunch.tracker.security;
//
// import dev.crunch.tracker.user.AppUserRepository;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import org.springframework.http.HttpHeaders;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;
// import java.io.IOException;
//
// @Component
// public class JwtAuthFilter extends OncePerRequestFilter {
//
//     private final JwtService jwt;
//     private final AppUserRepository users;
//
//     public JwtAuthFilter(JwtService jwt, AppUserRepository users) {
//         this.jwt = jwt;
//         this.users = users;
//     }
//
//     @Override
//     protected void doFilterInternal(HttpServletRequest request,
//                                     HttpServletResponse response,
//                                     FilterChain chain)
//             throws ServletException, IOException {
//
//         String header = request.getHeader(HttpHeaders.AUTHORIZATION);
//         if (header != null && header.startsWith("Bearer ")) {
//             String token = header.substring(7);
//             // TODO: validateAndGetUserId(token) -> findById -> set authentication
//             jwt.validateAndGetUserId(token)
//                .flatMap(users::findById)
//                .ifPresent(user -> {
//                    var auth = new UsernamePasswordAuthenticationToken(
//                            user, null, user.authorities());
//                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(auth);
//                });
//         }
//         // ALWAYS continue. If unauthenticated, the authorize rules reject later.
//         chain.doFilter(request, response);
//     }
// }
//
// ============================================================================
// HINTS (read only if stuck >15 min)
// ============================================================================
//
// The full filterChain body:
//
//   http
//     .cors(cors -> cors.configurationSource(corsSource))
//     .csrf(csrf -> csrf.disable())
//     .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//     .authorizeHttpRequests(auth -> auth
//         .requestMatchers("/api/v1/auth/**").permitAll()
//         .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
//         .anyRequest().authenticated())
//     .httpBasic(basic -> basic.disable())
//     .formLogin(form -> form.disable())
//     .exceptionHandling(ex -> ex
//         .authenticationEntryPoint(problemEntryPoint())
//         .accessDeniedHandler(problemAccessDenied()))
//     .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
//   return http.build();
//
// Common mistakes:
//   - Putting anyRequest().authenticated() BEFORE the permitAll matchers. Order
//     matters: the first matching rule wins. permitAll lines come first.
//   - Forgetting STATELESS — then Spring sets a JSESSIONID and you have hidden
//     server-side state you didn't want.
//   - Forgetting @EnableMethodSecurity — then @PreAuthorize is silently ignored
//     and your ownership rule never runs (everything "works", nothing is secured).
//
// VERIFY BY HAND:
//   ./mvnw spring-boot:run
//   http POST :8080/api/v1/auth/register email=a@b.co password=correcthorsebattery displayName=Ada
//   TOKEN=$(http POST :8080/api/v1/auth/login email=a@b.co password=correcthorsebattery -b | jq -r .accessToken)
//   http :8080/api/v1/habits                              # 401 ProblemDetail
//   http :8080/api/v1/habits "Authorization: Bearer $TOKEN"   # 200
//
// ============================================================================
