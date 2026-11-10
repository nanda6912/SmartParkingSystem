-- V7__Occupancy_Snapshots.sql
-- Permanent historical parking occupancy tracking.
-- Snapshots are captured from live parking_slots statuses on a
-- configurable interval; no historical data is fabricated.

-- =====================================================
-- OCCUPANCY SNAPSHOTS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS occupancy_snapshots (
    id BIGSERIAL PRIMARY KEY,

    captured_at TIMESTAMP NOT NULL,

    floor INTEGER NOT NULL,

    total_capacity INTEGER NOT NULL,
    occupied_slots INTEGER NOT NULL,
    available_slots INTEGER NOT NULL,
    locked_slots INTEGER NOT NULL,

    -- One snapshot per floor per capture interval.
    -- captured_at is quantized to the snapshot interval by the
    -- application before insert, so restarts and concurrent
    -- collectors cannot create duplicates for the same bucket.
    CONSTRAINT uq_occupancy_snapshot_floor_time
        UNIQUE (floor, captured_at)
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_occupancy_snapshots_captured_at
ON occupancy_snapshots(captured_at);

CREATE INDEX IF NOT EXISTS idx_occupancy_snapshots_floor_captured
ON occupancy_snapshots(floor, captured_at);
