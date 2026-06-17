// Exercise 2 — Goal controller
//
// Goal: Build a CRUD controller for the Crunch Tracker "goals" resource.
//       Practice DTO mapping (never serialize the domain entity), constructor
//       injection, and returning the RIGHT status code on purpose:
//       200 read, 201+Location on create, 204 on delete, 404 on not-found.
//
// Estimated time: 55 minutes.
//
// HOW TO USE THIS FILE
//
// 1. You already have the crunch-tracker-api project from Exercise 1, with the
//    Week 3 domain under com.crunchcrunch.tracker.domain. This file is a single
//    drop-in compilation unit so the whole exercise reads top to bottom; in the
//    real project you would split each public type into its own .java file
//    matching its package. Put the DTO records under
//    com.crunchcrunch.tracker.web.dto and the controller under
//    com.crunchcrunch.tracker.web.
//
// 2. Fill in every body marked `// TODO`. Do not change the public method
//    signatures or the DTO shapes — the tests at the bottom (translate them
//    into a real @WebMvcTest class) pin the contract.
//
// 3. Run it:
//        ./mvnw spring-boot:run
//    then exercise it with the curl commands in the EXPECTED BEHAVIOUR block.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented; ./mvnw compile is clean.
//   [ ] GET  /api/goals            -> 200, JSON array of GoalResponse
//   [ ] GET  /api/goals/{id}       -> 200 if found, 404 ProblemDetail if not
//   [ ] POST /api/goals            -> 201 with a Location header
//   [ ] PUT  /api/goals/{id}       -> 200 with the updated GoalResponse
//   [ ] DELETE /api/goals/{id}     -> 204 No Content (empty body)
//   [ ] No domain entity is ever returned from a handler — only DTO records.
//   [ ] The controller holds ZERO business logic; it delegates to GoalService.
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

package com.crunchcrunch.tracker.web;

import com.crunchcrunch.tracker.domain.Goal;
import com.crunchcrunch.tracker.domain.GoalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ----------------------------------------------------------------------------
// DTOs  (in the real project: com.crunchcrunch.tracker.web.dto, one file each)
// ----------------------------------------------------------------------------

// What a client may SEND to create a goal. Validation comes in Exercise 3 —
// here we focus on the shape and the mapping. Only fields the client controls.
record CreateGoalRequest(String title, String description, String category, int targetPerWeek) {
}

// What a client may SEND to update a goal. Note: no category — categories are
// fixed at creation time in this domain. The wire contract makes that explicit.
record UpdateGoalRequest(String title, String description, int targetPerWeek) {
}

// What a client RECEIVES. Includes the server-assigned id and createdAt that a
// client never sets. This is the response half of the contract.
record GoalResponse(
        UUID id,
        String title,
        String description,
        String category,
        int targetPerWeek,
        Instant createdAt) {

    // The single place domain -> wire mapping happens. When the domain and the
    // API diverge later, you change THIS method, not every call site.
    static GoalResponse from(Goal goal) {
        // TODO: construct a GoalResponse from the Goal record's accessors.
        //       Assume Goal exposes id(), title(), description(), category(),
        //       targetPerWeek(), createdAt() (the Week 3 record shape).
        throw new UnsupportedOperationException("TODO: map Goal -> GoalResponse");
    }
}

// ----------------------------------------------------------------------------
// Controller
// ----------------------------------------------------------------------------

@RestController
@RequestMapping("/api/goals")
class GoalController {

    private final GoalService service;

    // TODO: constructor injection. Assign the injected GoalService to the field.
    GoalController(GoalService service) {
        throw new UnsupportedOperationException("TODO: assign the service");
    }

    /**
     * List goals, optionally filtered by category.
     * GET /api/goals            -> all goals
     * GET /api/goals?category=x -> goals in category x
     * Must return a List<GoalResponse> (DTOs!), never List<Goal>.
     */
    @GetMapping
    public List<GoalResponse> list(@RequestParam(required = false) String category) {
        // TODO: call service.findAll(category), map each Goal with GoalResponse::from,
        //       collect to a List. (Assume findAll accepts a nullable category.)
        throw new UnsupportedOperationException("TODO: list");
    }

    /**
     * Fetch one goal.
     * GET /api/goals/{id} -> 200 GoalResponse, or 404 if it doesn't exist.
     * The 404 is produced by GoalService.findById throwing NotFoundException,
     * which the global advice (Exercise 3) turns into a ProblemDetail. Here you
     * just map the found Goal — do NOT catch anything.
     */
    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable UUID id) {
        // TODO: map service.findById(id) with GoalResponse.from
        throw new UnsupportedOperationException("TODO: get");
    }

    /**
     * Create a goal.
     * POST /api/goals -> 201 Created, Location: /api/goals/{newId}, body = GoalResponse.
     * You MUST return 201 (not 200) and include the Location header.
     */
    @PostMapping
    public ResponseEntity<GoalResponse> create(@RequestBody CreateGoalRequest req) {
        // TODO:
        //   1. Goal created = service.create(req.title(), req.description(),
        //                                    req.category(), req.targetPerWeek());
        //   2. Build the Location URI: "/api/goals/" + created.id()
        //   3. return ResponseEntity.created(location).body(GoalResponse.from(created));
        throw new UnsupportedOperationException("TODO: create");
    }

    /**
     * Update a goal.
     * PUT /api/goals/{id} -> 200 GoalResponse, or 404 if it doesn't exist.
     */
    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable UUID id, @RequestBody UpdateGoalRequest req) {
        // TODO: map service.update(id, req.title(), req.description(), req.targetPerWeek())
        throw new UnsupportedOperationException("TODO: update");
    }

    /**
     * Delete a goal.
     * DELETE /api/goals/{id} -> 204 No Content (empty body), or 404 if absent.
     * Use @ResponseStatus(NO_CONTENT) and a void method.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        // TODO: service.delete(id);  (it throws NotFoundException if absent -> 404)
        throw new UnsupportedOperationException("TODO: delete");
    }
}

// ----------------------------------------------------------------------------
// EXPECTED BEHAVIOUR (curl against a running app — yours should match)
// ----------------------------------------------------------------------------
//
// $ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' \
//     -d '{"title":"Run 5k","description":"Couch to 5k","category":"fitness","targetPerWeek":3}'
// HTTP/1.1 201
// Location: /api/goals/3fa85f64-5717-4562-b3fc-2c963f66afa6
// Content-Type: application/json
// {"id":"3fa85f64-...","title":"Run 5k","description":"Couch to 5k","category":"fitness","targetPerWeek":3,"createdAt":"2026-06-12T14:03:11.882Z"}
//
// $ curl -s localhost:8080/api/goals | jq length
// 1
//
// $ curl -i localhost:8080/api/goals/00000000-0000-0000-0000-000000000000
// HTTP/1.1 404
// Content-Type: application/problem+json
// {"type":"...","title":"Resource not found","status":404,"detail":"Goal ... not found"}
//
// $ curl -i -X DELETE localhost:8080/api/goals/3fa85f64-5717-4562-b3fc-2c963f66afa6
// HTTP/1.1 204
// (empty body)
//
// ----------------------------------------------------------------------------
// A SLICE TEST TO PORT INTO src/test/.../web/GoalControllerTest.java
// ----------------------------------------------------------------------------
//
// @WebMvcTest(GoalController.class)
// class GoalControllerTest {
//     @Autowired MockMvc mvc;
//     @MockBean GoalService service;
//
//     @Test
//     void create_returns_201_with_location() throws Exception {
//         var id = UUID.randomUUID();
//         var goal = new Goal(id, "Run 5k", "Couch to 5k", "fitness", 3, Instant.now());
//         when(service.create("Run 5k", "Couch to 5k", "fitness", 3)).thenReturn(goal);
//
//         mvc.perform(post("/api/goals").contentType("application/json").content("""
//                 {"title":"Run 5k","description":"Couch to 5k","category":"fitness","targetPerWeek":3}
//             """))
//            .andExpect(status().isCreated())
//            .andExpect(header().string("Location", "/api/goals/" + id))
//            .andExpect(jsonPath("$.id").value(id.toString()))
//            .andExpect(jsonPath("$.title").value("Run 5k"));
//     }
//
//     @Test
//     void delete_returns_204() throws Exception {
//         var id = UUID.randomUUID();
//         mvc.perform(delete("/api/goals/" + id)).andExpect(status().isNoContent());
//         verify(service).delete(id);
//     }
// }
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// GoalResponse.from:
//   static GoalResponse from(Goal g) {
//       return new GoalResponse(g.id(), g.title(), g.description(),
//                               g.category(), g.targetPerWeek(), g.createdAt());
//   }
//
// constructor:
//   GoalController(GoalService service) { this.service = service; }
//
// list:
//   return service.findAll(category).stream().map(GoalResponse::from).toList();
//
// get:
//   return GoalResponse.from(service.findById(id));
//
// create:
//   Goal created = service.create(req.title(), req.description(), req.category(), req.targetPerWeek());
//   return ResponseEntity.created(URI.create("/api/goals/" + created.id()))
//                        .body(GoalResponse.from(created));
//
// update:
//   return GoalResponse.from(service.update(id, req.title(), req.description(), req.targetPerWeek()));
//
// delete:
//   service.delete(id);
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// The DTO boundary you build here is what makes Week 5 painless: when the in-
// memory repository becomes JPA + Postgres, the Goal ENTITY changes (it gains
// @Entity, @Id, maybe a lazy relationship), but GoalResponse does not. The
// mobile client in Week 9 consumes GoalResponse, so as long as that record's
// JSON is stable, the front end never knows the database arrived. Returning the
// entity directly would have leaked that change straight onto the wire.
// ----------------------------------------------------------------------------
