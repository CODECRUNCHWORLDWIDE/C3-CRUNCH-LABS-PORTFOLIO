// Exercise 3 — Validation and ProblemDetail
//
// Goal: Make the goals API reject bad input at the edge with Jakarta Bean
//       Validation, write ONE custom constraint, and turn every error into an
//       RFC-9457 ProblemDetail from a single @RestControllerAdvice. After this
//       exercise, a client mistake NEVER returns a 500 + stack trace — it
//       returns a 400 or 404 with a machine-readable application/problem+json
//       body the mobile app can parse.
//
// Estimated time: 50 minutes.
//
// HOW TO USE THIS FILE
//
// 1. Builds on Exercise 2. spring-boot-starter-validation is already a
//    dependency (you added it in Exercise 1). This file is a single drop-in
//    unit for readability; in the real project split each public type into its
//    own .java file under the package shown.
//
// 2. Fill in every `// TODO`. Then:
//      - Add @Valid to the @RequestBody parameters in GoalController (Exercise 2).
//      - Run ./mvnw spring-boot:run and exercise the EXPECTED BEHAVIOUR block.
//
// ACCEPTANCE CRITERIA
//
//   [ ] CreateGoalRequest is fully annotated (title, description, category, targetPerWeek).
//   [ ] @Valid is on the @RequestBody params in the controller.
//   [ ] A blank title returns 400 with Content-Type: application/problem+json
//       and an "errors" array naming the field.
//   [ ] An unknown category is rejected by the custom @KnownCategory constraint,
//       NOT by an if-statement in the controller.
//   [ ] NotFoundException maps to 404 ProblemDetail via the advice.
//   [ ] No handler method contains a try/catch for HTTP concerns.
//
// Inline hints at the bottom. Don't peek until you've tried for 15 minutes.

package com.crunchcrunch.tracker.web;

import com.crunchcrunch.tracker.domain.NotFoundException;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ----------------------------------------------------------------------------
// 1. The validated request DTO  (com.crunchcrunch.tracker.web.dto)
// ----------------------------------------------------------------------------
//
// Replace the un-annotated CreateGoalRequest from Exercise 2 with this one.

record CreateGoalRequest(
        // TODO: title is required (not null, not blank) and at most 120 chars.
        //       Annotations: @NotBlank @Size(max = 120)
        String title,

        // description is optional but capped at 500 chars when present.
        @Size(max = 500) String description,

        // TODO: category is required AND must be one of the known set.
        //       Annotations: @NotBlank @KnownCategory  (the custom one below)
        String category,

        // TODO: targetPerWeek must be at least 1 and at most 21 (3x/day cap).
        //       Annotations: @Min(1) @Max(21)
        int targetPerWeek) {
}

// ----------------------------------------------------------------------------
// 2. A CUSTOM constraint: @KnownCategory
// ----------------------------------------------------------------------------
//
// Built-in annotations can't express "must be one of {fitness, learning, ...}".
// So we write our own: an annotation + a ConstraintValidator. This is the
// canonical pattern for a domain rule the built-ins can't cover.

@Documented
@Constraint(validatedBy = KnownCategoryValidator.class)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@interface KnownCategory {
    String message() default "category must be one of: fitness, learning, health, finance, mindfulness";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class KnownCategoryValidator implements ConstraintValidator<KnownCategory, String> {

    private static final Set<String> KNOWN =
            Set.of("fitness", "learning", "health", "finance", "mindfulness");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // TODO: return true when value is null (let @NotBlank handle null/blank —
        //       a constraint should validate only its own concern), otherwise
        //       return whether KNOWN contains the lowercased value.
        //   Hint:  if (value == null) return true;
        //          return KNOWN.contains(value.toLowerCase());
        throw new UnsupportedOperationException("TODO: isValid");
    }
}

// ----------------------------------------------------------------------------
// 3. The global error handler  (com.crunchcrunch.tracker.web)
// ----------------------------------------------------------------------------
//
// One class handles errors for EVERY controller. No controller try/catches.

@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 400 — Bean Validation failures. Build a ProblemDetail with a per-field
     * "errors" array so the client can show field-level messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // TODO:
        //   1. ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        //          HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        //   2. pd.setTitle("Validation failed");
        //   3. pd.setType(URI.create("https://crunchtracker.dev/problems/validation"));
        //   4. Build a List<Map<String,String>> from ex.getBindingResult()
        //          .getFieldErrors(), each entry {"field": fe.getField(),
        //          "message": fe.getDefaultMessage()}.
        //   5. pd.setProperty("errors", thatList);
        //   6. return pd;
        throw new UnsupportedOperationException("TODO: handleValidation");
    }

    /**
     * 404 — domain "not found". Maps your NotFoundException to a clean body.
     */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        // TODO:
        //   ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        //   pd.setTitle("Resource not found");
        //   pd.setType(URI.create("https://crunchtracker.dev/problems/not-found"));
        //   return pd;
        throw new UnsupportedOperationException("TODO: handleNotFound");
    }
}

// ----------------------------------------------------------------------------
// DON'T FORGET: add @Valid in GoalController (from Exercise 2)
// ----------------------------------------------------------------------------
//
//   @PostMapping
//   public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest req) { ... }
//
//   @PutMapping("/{id}")
//   public GoalResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateGoalRequest req) { ... }
//
// Without @Valid the annotations do nothing.
//
// ----------------------------------------------------------------------------
// EXPECTED BEHAVIOUR (curl against a running app)
// ----------------------------------------------------------------------------
//
// $ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' \
//     -d '{"title":"","category":"fitness","targetPerWeek":3}'
// HTTP/1.1 400
// Content-Type: application/problem+json
// {"type":"https://crunchtracker.dev/problems/validation","title":"Validation failed",
//  "status":400,"detail":"One or more fields are invalid.","instance":"/api/goals",
//  "errors":[{"field":"title","message":"must not be blank"}]}
//
// $ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' \
//     -d '{"title":"Read more","category":"telepathy","targetPerWeek":3}'
// HTTP/1.1 400
// Content-Type: application/problem+json
// {... "errors":[{"field":"category","message":"category must be one of: fitness, learning, health, finance, mindfulness"}]}
//
// $ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' \
//     -d '{"title":"Train","category":"fitness","targetPerWeek":99}'
// HTTP/1.1 400   (targetPerWeek fails @Max(21))
//
// ----------------------------------------------------------------------------
// A SLICE TEST TO PORT INTO src/test/.../web/GoalValidationTest.java
// ----------------------------------------------------------------------------
//
// @WebMvcTest(GoalController.class)
// class GoalValidationTest {
//     @Autowired MockMvc mvc;
//     @MockBean GoalService service;
//
//     @Test
//     void blank_title_returns_400_problemdetail() throws Exception {
//         mvc.perform(post("/api/goals").contentType("application/json").content("""
//                 {"title":"","category":"fitness","targetPerWeek":3}
//             """))
//            .andExpect(status().isBadRequest())
//            .andExpect(content().contentType("application/problem+json"))
//            .andExpect(jsonPath("$.title").value("Validation failed"))
//            .andExpect(jsonPath("$.errors[0].field").value("title"));
//     }
//
//     @Test
//     void unknown_category_returns_400() throws Exception {
//         mvc.perform(post("/api/goals").contentType("application/json").content("""
//                 {"title":"Read","category":"telepathy","targetPerWeek":3}
//             """))
//            .andExpect(status().isBadRequest())
//            .andExpect(jsonPath("$.errors[0].field").value("category"));
//     }
// }
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// CreateGoalRequest:
//   record CreateGoalRequest(
//       @NotBlank @Size(max = 120) String title,
//       @Size(max = 500) String description,
//       @NotBlank @KnownCategory String category,
//       @Min(1) @Max(21) int targetPerWeek) {}
//
// KnownCategoryValidator.isValid:
//   if (value == null) return true;
//   return KNOWN.contains(value.toLowerCase());
//
// handleValidation:
//   ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
//   pd.setTitle("Validation failed");
//   pd.setType(URI.create("https://crunchtracker.dev/problems/validation"));
//   List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
//       .map(fe -> Map.of("field", fe.getField(),
//                         "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
//       .toList();
//   pd.setProperty("errors", errors);
//   return pd;
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// A REST API that returns 500 for a client mistake is lying about whose fault
// it is, and it leaks internals (the stack trace) to the world. Validating at
// the edge means your service code can ASSUME valid input; centralizing errors
// in one @RestControllerAdvice means the contract for "what an error looks
// like" is defined in exactly one place. The mobile app in Week 9 parses
// application/problem+json — so this body shape IS part of your public API.
// ----------------------------------------------------------------------------
