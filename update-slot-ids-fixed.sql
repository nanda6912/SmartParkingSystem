-- Update existing parking slots with new slotId format
-- This script generates slot IDs based on floor and slot number

-- Check current schema first
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'parking_slots';

-- Update Floor 1 slots (AG01-AG20, BG01-BG20, CG01-CG20, DG01-DG20, EG01-EG20)
UPDATE parking_slots 
SET slot_id = CASE 
    WHEN slot_number BETWEEN 1 AND 20 THEN 'AG' || LPAD(slot_number::text, 2, '0')
    WHEN slot_number BETWEEN 21 AND 40 THEN 'BG' || LPAD((slot_number - 20)::text, 2, '0')
    WHEN slot_number BETWEEN 41 AND 60 THEN 'CG' || LPAD((slot_number - 40)::text, 2, '0')
    WHEN slot_number BETWEEN 61 AND 80 THEN 'DG' || LPAD((slot_number - 60)::text, 2, '0')
    WHEN slot_number BETWEEN 81 AND 100 THEN 'EG' || LPAD((slot_number - 80)::text, 2, '0')
    ELSE 'AG' || LPAD(slot_number::text, 2, '0')
END
WHERE floor = 1 AND slot_id IS NULL;

-- Update Floor 2 slots (AF01-AF20, BF01-BF20, CF01-CF20, DF01-DF20, EF01-EF20)
UPDATE parking_slots 
SET slot_id = CASE 
    WHEN slot_number BETWEEN 1 AND 20 THEN 'AF' || LPAD(slot_number::text, 2, '0')
    WHEN slot_number BETWEEN 21 AND 40 THEN 'BF' || LPAD((slot_number - 20)::text, 2, '0')
    WHEN slot_number BETWEEN 41 AND 60 THEN 'CF' || LPAD((slot_number - 40)::text, 2, '0')
    WHEN slot_number BETWEEN 61 AND 80 THEN 'DF' || LPAD((slot_number - 60)::text, 2, '0')
    WHEN slot_number BETWEEN 81 AND 100 THEN 'EF' || LPAD((slot_number - 80)::text, 2, '0')
    ELSE 'AF' || LPAD(slot_number::text, 2, '0')
END
WHERE floor = 2 AND slot_id IS NULL;

-- Verify the update
SELECT floor, slot_number, slot_id, status FROM parking_slots ORDER BY floor, slot_number LIMIT 10;
