-- V1__Synchronize_Schema.sql
-- Complete database schema synchronization for Smart Parking System

-- =====================================================
-- PARKING SLOTS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS parking_slots (
    id BIGSERIAL PRIMARY KEY,
    slot_number INTEGER NOT NULL,
    floor INTEGER NOT NULL,
    slot_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(255) NOT NULL,
    lock_until TIMESTAMP,
    version BIGINT
);

-- =====================================================
-- BOOKINGS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,

    booking_code VARCHAR(5) UNIQUE,

    parking_slot_id BIGINT NOT NULL,

    vehicle_number VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    vehicle_type VARCHAR(255) NOT NULL,

    booking_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP,

    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',

    parking_fee INTEGER,
    duration_minutes BIGINT DEFAULT 0,

    is_active BOOLEAN DEFAULT TRUE,

    payment_method VARCHAR(255),
    transaction_id VARCHAR(255),
    payment_time TIMESTAMP,

    CONSTRAINT fk_booking_slot
        FOREIGN KEY (parking_slot_id)
        REFERENCES parking_slots(id)
);

-- =====================================================
-- SCHEMA SYNCHRONIZATION
-- =====================================================

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS exit_time TIMESTAMP;

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS parking_fee INTEGER;

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS duration_minutes BIGINT DEFAULT 0;

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS payment_method VARCHAR(255);

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS payment_time TIMESTAMP;

ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS status VARCHAR(255);

-- =====================================================
-- DATA REPAIR FOR OLD DATABASES
-- =====================================================

UPDATE bookings
SET status =
CASE
    WHEN is_active = TRUE THEN 'ACTIVE'
    ELSE 'COMPLETED'
END
WHERE status IS NULL;

ALTER TABLE bookings
ALTER COLUMN status SET NOT NULL;

ALTER TABLE bookings
ALTER COLUMN duration_minutes SET DEFAULT 0;

-- =====================================================
-- UNIQUE CONSTRAINTS
-- =====================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'bookings_booking_code_key'
    ) THEN
        ALTER TABLE bookings
        ADD CONSTRAINT bookings_booking_code_key
        UNIQUE (booking_code);
    END IF;
END $$;

-- =====================================================
-- PERFORMANCE INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_bookings_vehicle_number
ON bookings(vehicle_number);

CREATE INDEX IF NOT EXISTS idx_bookings_status
ON bookings(status);

CREATE INDEX IF NOT EXISTS idx_bookings_active
ON bookings(is_active);

CREATE INDEX IF NOT EXISTS idx_booking_code
ON bookings(booking_code);

CREATE INDEX IF NOT EXISTS idx_parking_slots_status
ON parking_slots(status);