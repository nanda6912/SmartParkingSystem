-- V5__Booking_Events.sql
-- Permanent booking lifecycle event history and duration_minutes backfill.
-- Existing bookings rows are preserved; no existing migration is modified.

-- =====================================================
-- BOOKING EVENTS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS booking_events (
    id BIGSERIAL PRIMARY KEY,

    booking_id BIGINT,
    booking_code VARCHAR(5),

    event_type VARCHAR(40) NOT NULL,

    previous_status VARCHAR(30),
    new_status VARCHAR(30),

    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    parking_slot_id BIGINT,

    additional_details JSONB,

    CONSTRAINT fk_booking_event_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_booking_event_slot
        FOREIGN KEY (parking_slot_id)
        REFERENCES parking_slots(id)
        ON DELETE SET NULL
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_booking_events_booking_id
ON booking_events(booking_id);

CREATE INDEX IF NOT EXISTS idx_booking_events_timestamp
ON booking_events(event_timestamp);

CREATE INDEX IF NOT EXISTS idx_booking_events_type
ON booking_events(event_type);

CREATE INDEX IF NOT EXISTS idx_booking_events_booking_code
ON booking_events(booking_code);

-- =====================================================
-- DURATION MINUTES BACKFILL
-- Derive duration from booking_time/exit_time for exited
-- bookings where duration_minutes was never populated.
-- Matches Duration.toMinutes() truncation semantics.
-- =====================================================

UPDATE bookings
SET duration_minutes = FLOOR(EXTRACT(EPOCH FROM (exit_time - booking_time)) / 60)::BIGINT
WHERE exit_time IS NOT NULL
  AND booking_time IS NOT NULL
  AND (duration_minutes IS NULL OR duration_minutes = 0)
  AND exit_time >= booking_time;
