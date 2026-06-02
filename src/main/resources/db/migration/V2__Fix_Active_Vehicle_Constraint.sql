-- V2__Fix_Active_Vehicle_Constraint.sql
-- Fix duplicate vehicle booking constraint issue

-- =====================================================
-- REMOVE OLD BROKEN CONSTRAINT
-- =====================================================

ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS uk_active_vehicle;

-- =====================================================
-- REMOVE OLD INDEX IF PRESENT
-- =====================================================

DROP INDEX IF EXISTS uk_active_vehicle;

-- =====================================================
-- CREATE CORRECT PARTIAL UNIQUE INDEX
-- =====================================================

CREATE UNIQUE INDEX IF NOT EXISTS uk_active_vehicle_only
ON bookings(vehicle_number)
WHERE is_active = TRUE;