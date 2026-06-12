-- Exercise 3 — Author a Flyway migration: V2__habits_and_checkins.sql
--
-- Goal: Write a real, forward-only Flyway migration that creates the `habits`
--       and `check_ins` tables so the entities from exercise 2 validate against
--       a schema YOU authored (not one Hibernate generated).
--
-- Estimated time: 30 minutes.
--
-- HOW TO USE THIS FILE
--
--   1. This file is already a runnable, correct migration. Drop it into your
--      Crunch Tracker project at EXACTLY this path and name (the V2 follows the
--      V1__create_goals.sql you wrote in exercise 1):
--
--        src/main/resources/db/migration/V2__habits_and_checkins.sql
--
--   2. Boot the app (./mvnw spring-boot:run). Flyway applies V2; Hibernate
--      `validate` then checks the Habit/CheckIn entities against these tables.
--      If validation fails, the entity and this migration disagree — reconcile
--      them (the column TYPES and LENGTHS must match the @Column annotations).
--
--   3. Your task is to READ this migration and confirm you understand every
--      line, then EXTEND it with the two TODO constraints/indexes at the bottom
--      before you boot. They are commented out; uncomment and complete them.
--
-- ACCEPTANCE CRITERIA
--
--   [ ] File is named V2__habits_and_checkins.sql (two underscores after V2).
--   [ ] Flyway applies it: flyway_schema_history shows version 2, success = t.
--   [ ] Hibernate `validate` passes for Habit and CheckIn.
--   [ ] The two TODO items below are completed.
--   [ ] You did NOT edit V1 (the cardinal rule: never touch an applied migration).
--
-- ----------------------------------------------------------------------------

-- The habits table. Types mirror the Habit entity:
--   UUID id, VARCHAR(120) name, INTEGER target_per_week, VARCHAR(20) cadence,
--   TIMESTAMPTZ created_at, BIGINT version.
CREATE TABLE habits (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    target_per_week INTEGER      NOT NULL,
    cadence         VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0,

    -- A target of 0 or > 21 is nonsense (max 3/day * 7 days). Push the invariant
    -- into the database so a buggy service can never write a bad row.
    CONSTRAINT chk_habits_target CHECK (target_per_week BETWEEN 1 AND 21)
);

-- The cadence column is filtered by findByCadence(); index it.
CREATE INDEX idx_habits_cadence ON habits (cadence);


-- The check_ins table. The owning side of the one-to-many: it carries the FK.
--   UUID id, UUID habit_id (FK), DATE checked_on, VARCHAR(280) note.
CREATE TABLE check_ins (
    id          UUID PRIMARY KEY,
    habit_id    UUID NOT NULL,
    checked_on  DATE NOT NULL,
    note        VARCHAR(280),

    -- Deleting a habit removes its check-ins at the DB level too — mirrors the
    -- entity's cascade + orphanRemoval. Defense in depth.
    CONSTRAINT fk_check_ins_habit
        FOREIGN KEY (habit_id) REFERENCES habits (id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- TODO A — A habit can only be checked in ONCE per calendar day. Add a UNIQUE
--          constraint across (habit_id, checked_on) so a double check-in on the
--          same day is rejected by Postgres (the integration test relies on it).
--
--   Hint:
--     ALTER TABLE check_ins
--         ADD CONSTRAINT uq_check_ins_habit_day UNIQUE (habit_id, checked_on);
--
-- (Uncomment and complete the line below.)
-- ALTER TABLE check_ins ADD CONSTRAINT uq_check_ins_habit_day UNIQUE (/* TODO */);


-- ----------------------------------------------------------------------------
-- TODO B — Postgres does NOT auto-index foreign-key columns. The JOIN FETCH and
--          the batch-fetch `WHERE habit_id IN (...)` queries from lecture 1 will
--          do sequential scans without this. Add an index on check_ins.habit_id.
--
--   Hint:
--     CREATE INDEX idx_check_ins_habit ON check_ins (habit_id);
--
-- (Uncomment and complete the line below.)
-- CREATE INDEX idx_check_ins_habit ON check_ins (/* TODO */);


-- ----------------------------------------------------------------------------
-- NOTES ON THIS MIGRATION (read these — they're the point of the exercise)
--
--  * TIMESTAMPTZ, not TIMESTAMP. Always store the zone. It maps to Instant and
--    avoids the off-by-hours class of bug. `timestamp` (no zone) does not.
--
--  * The cadence/status columns are VARCHAR, not an integer, because the
--    entities use @Enumerated(EnumType.STRING). If you had used ORDINAL the
--    column would be INTEGER and reordering the enum would silently corrupt
--    every stored row. STRING is the safe default.
--
--  * VARCHAR(20) for cadence: the longest constant is "WEEKDAYS" (8 chars), so
--    20 is comfortable headroom. If you ever add a 25-char constant, you'll need
--    a NEW migration to widen it — you will NOT edit this file.
--
--  * This is a VERSIONED (V) migration. Flyway runs it exactly once and records
--    its checksum. Once you've pushed it or CI has run it, it is FROZEN. To
--    change the schema later, add V3__... — never reopen V2.
--
--  * No DROP, no destructive change here. Each migration is one forward step.
-- ----------------------------------------------------------------------------
