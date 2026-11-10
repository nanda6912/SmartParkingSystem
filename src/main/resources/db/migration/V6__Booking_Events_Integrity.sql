-- V6__Booking_Events_Integrity.sql
-- Harden booking_events against duplicate events and enforce
-- permanent booking retention at the database level.

-- =====================================================
-- IDEMPOTENT EVENT UNIQUENESS
-- One BOOKING_CREATED / VEHICLE_EXITED / PAYMENT_RECORDED
-- event per booking. BOOKING_STATUS_CHANGED is excluded
-- because multiple legitimate transitions may exist.
-- =====================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_events_idempotent_types
ON booking_events(booking_id, event_type)
WHERE booking_id IS NOT NULL
  AND event_type IN ('BOOKING_CREATED', 'VEHICLE_EXITED', 'PAYMENT_RECORDED');

-- =====================================================
-- RETENTION ENFORCEMENT
-- Bookings referenced by event history cannot be deleted.
-- ON DELETE SET NULL would silently sever the audit link;
-- RESTRICT makes accidental deletion fail loudly instead.
-- =====================================================

ALTER TABLE booking_events
DROP CONSTRAINT IF EXISTS fk_booking_event_booking;

ALTER TABLE booking_events
ADD CONSTRAINT fk_booking_event_booking
    FOREIGN KEY (booking_id)
    REFERENCES bookings(id)
    ON DELETE RESTRICT;
