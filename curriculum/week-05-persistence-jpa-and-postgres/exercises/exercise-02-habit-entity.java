// Exercise 2 — The Habit entity and its Spring Data repository
//
// Goal: Map the Habit aggregate to a table with JPA, add a LAZY @OneToMany to
//       CheckIn, and write a Spring Data repository with a derived query and a
//       JOIN FETCH that avoids the N+1 problem.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
//   1. This is real, idiomatic Spring Boot 3 / Java 21 code. Split it into the
//      files indicated by the `// ==== file: ... ====` banners and drop them
//      into your Crunch Tracker project under:
//
//        src/main/java/tech/crunch/tracker/habit/
//
//   2. Fill in every body marked `// TODO`. Do NOT change the public surface
//      (class names, fields, method signatures). The integration test at the
//      bottom (HabitRepositoryIT) exercises what you fill in; if you wire it
//      correctly, `./mvnw test` is green.
//
//   3. You must have Postgres running (docker compose up -d) and the V2 migration
//      from exercise 3 applied, OR rely on the Testcontainers config which builds
//      the schema from your db/migration folder automatically.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `./mvnw test`: HabitRepositoryIT passes against a real Postgres container.
//   [ ] The @ManyToOne on CheckIn is explicitly FetchType.LAZY.
//   [ ] @Enumerated(EnumType.STRING) is used for the Cadence column.
//   [ ] findAllWithCheckIns() fires ONE query, not 1 + N (assert the count).
//   [ ] Hibernate `validate` passes — entity and migration agree.
//
// Inline hints are at the bottom of this file. Don't peek until you've tried for
// at least 15 minutes.

// ============================================================================
// ==== file: src/main/java/tech/crunch/tracker/habit/Cadence.java ====
// ============================================================================
package tech.crunch.tracker.habit;

public enum Cadence { DAILY, WEEKLY, WEEKDAYS }

// ============================================================================
// ==== file: src/main/java/tech/crunch/tracker/habit/Habit.java ====
// ============================================================================
package tech.crunch.tracker.habit;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "target_per_week", nullable = false)
    private int targetPerWeek;

    // TODO 1: store the Cadence enum as its NAME (e.g. "DAILY"), not its ordinal.
    //         Add the two annotations and a column named "cadence", length 20,
    //         NOT NULL. (Hint: @Enumerated + @Column)
    private Cadence cadence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    // TODO 2: map the inverse side of the Habit -> CheckIn one-to-many.
    //         The FK lives on CheckIn.habit. Cascade ALL, orphanRemoval true.
    //         (Hint: @OneToMany(mappedBy = "habit", cascade = ..., orphanRemoval = true))
    private List<CheckIn> checkIns = new ArrayList<>();

    protected Habit() { }

    public Habit(String name, int targetPerWeek, Cadence cadence) {
        this.name = name;
        this.targetPerWeek = targetPerWeek;
        this.cadence = cadence;
    }

    /**
     * Add a check-in for the given day, keeping BOTH sides of the relationship
     * in sync (the owning side's habit field AND this list). This is the
     * standard JPA "helper method" pattern.
     */
    public CheckIn checkIn(LocalDate day, String note) {
        // TODO 3: create a CheckIn(this, day, note), add it to checkIns, return it.
        throw new UnsupportedOperationException("TODO 3");
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTargetPerWeek() { return targetPerWeek; }
    public void setTargetPerWeek(int targetPerWeek) { this.targetPerWeek = targetPerWeek; }
    public Cadence getCadence() { return cadence; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
    public List<CheckIn> getCheckIns() { return checkIns; }
}

// ============================================================================
// ==== file: src/main/java/tech/crunch/tracker/habit/CheckIn.java ====
// ============================================================================
package tech.crunch.tracker.habit;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // TODO 4: map the OWNING side of the many-to-one to Habit.
    //         It MUST be LAZY (an eager many-to-one is the classic N+1 cause).
    //         The FK column is "habit_id", NOT NULL.
    //         (Hint: @ManyToOne(fetch = FetchType.LAZY, optional = false)
    //                @JoinColumn(name = "habit_id", nullable = false))
    private Habit habit;

    @Column(name = "checked_on", nullable = false)
    private LocalDate checkedOn;

    @Column(length = 280)
    private String note;

    protected CheckIn() { }

    public CheckIn(Habit habit, LocalDate checkedOn, String note) {
        this.habit = habit;
        this.checkedOn = checkedOn;
        this.note = note;
    }

    public UUID getId() { return id; }
    public Habit getHabit() { return habit; }
    public LocalDate getCheckedOn() { return checkedOn; }
    public String getNote() { return note; }
}

// ============================================================================
// ==== file: src/main/java/tech/crunch/tracker/habit/HabitRepository.java ====
// ============================================================================
package tech.crunch.tracker.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HabitRepository extends JpaRepository<Habit, UUID> {

    // TODO 5: a DERIVED query that returns all habits with the given cadence.
    //         Spring parses the method name; you write zero implementation.
    //         (Hint: name it findByCadence(Cadence cadence))
    List<Habit> findByCadence(Cadence cadence);

    // TODO 6: a @Query with JOIN FETCH that loads every habit AND its check-ins
    //         in ONE query (the N+1 fix). Use `select distinct h ... left join
    //         fetch h.checkIns`.
    //         (Hint: @Query("select distinct h from Habit h left join fetch h.checkIns"))
    @Query(/* TODO 6 */ "")
    List<Habit> findAllWithCheckIns();
}

// ============================================================================
// ==== file: src/test/java/tech/crunch/tracker/habit/HabitRepositoryIT.java ====
// ============================================================================
package tech.crunch.tracker.habit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class HabitRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired HabitRepository habits;
    @Autowired EntityManagerFactory emf;

    @Test
    void derived_query_filters_by_cadence() {
        habits.save(new Habit("Read", 5, Cadence.DAILY));
        habits.save(new Habit("Long run", 1, Cadence.WEEKLY));

        List<Habit> daily = habits.findByCadence(Cadence.DAILY);

        assertThat(daily).extracting(Habit::getName).containsExactly("Read");
    }

    @Test
    void join_fetch_loads_check_ins_in_one_query() {
        Habit h = new Habit("Read", 5, Cadence.DAILY);
        h.checkIn(LocalDate.parse("2026-06-08"), "10 pages");
        h.checkIn(LocalDate.parse("2026-06-09"), "12 pages");
        habits.save(h);

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        List<Habit> loaded = habits.findAllWithCheckIns();
        // Touch the lazy collection — with JOIN FETCH this triggers NO extra query.
        long total = loaded.stream().mapToLong(x -> x.getCheckIns().size()).sum();

        assertThat(total).isEqualTo(2);
        assertThat(stats.getPrepareStatementCount())
            .as("findAllWithCheckIns must be a single query, not 1 + N")
            .isEqualTo(1);
    }
}

// ============================================================================
// HINTS (read only if stuck > 15 min)
// ============================================================================
//
// TODO 1 (cadence column):
//   @Enumerated(EnumType.STRING)
//   @Column(nullable = false, length = 20)
//   private Cadence cadence;
//
// TODO 2 (inverse one-to-many):
//   @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
//   private List<CheckIn> checkIns = new ArrayList<>();
//
// TODO 3 (helper method):
//   public CheckIn checkIn(LocalDate day, String note) {
//       CheckIn c = new CheckIn(this, day, note);
//       checkIns.add(c);
//       return c;
//   }
//
// TODO 4 (owning many-to-one):
//   @ManyToOne(fetch = FetchType.LAZY, optional = false)
//   @JoinColumn(name = "habit_id", nullable = false)
//   private Habit habit;
//
// TODO 5: already shown — findByCadence(Cadence cadence).
//
// TODO 6 (JOIN FETCH):
//   @Query("select distinct h from Habit h left join fetch h.checkIns")
//   List<Habit> findAllWithCheckIns();
//
// Why `distinct`? A collection fetch-join returns one row per child, duplicating
// the parent. `distinct` de-dupes the parents in memory. Watch the SQL log:
// you'll see exactly one `select ... from habits h left join check_ins c ...`.
// ============================================================================
