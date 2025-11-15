-- ============================================================================
-- HAPI FHIR Multi-Tenant Partition Initialization
-- ============================================================================
-- This script creates partitions for clinic-based multi-tenancy
--
-- Partition Strategy:
-- - Partition 0: Default/System partition
-- - Partition 1: Hamburg Clinic (ddx-hamburg-clinic)
-- - Partition 2: Berlin Clinic (ddx-berlin-clinic)
-- - Partition 3: Munich Clinic (ddx-munich-clinic)
-- - Partition 4: Frankfurt Clinic (ddx-frankfurt-clinic)
-- - Partition 5: Cologne Clinic (ddx-cologne-clinic)
-- ============================================================================

-- Create partitions in HFJ_PARTITION table
-- Note: HAPI FHIR will auto-create these when first used, but we can pre-create them

INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC) 
VALUES (0, 'DEFAULT', 'Default system partition')
ON CONFLICT (PART_ID) DO NOTHING;

INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC) 
VALUES (1, 'HAMBURG', 'Hamburg Clinic - ddx-hamburg-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC) 
VALUES (2, 'BERLIN', 'Berlin Clinic - ddx-berlin-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC) 
VALUES (3, 'MUNICH', 'Munich Clinic - ddx-munich-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC) 
VALUES (4, 'FRANKFURT', 'Frankfurt Clinic - ddx-frankfurt-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

INSERT INTO HFJ_PARTITION (PART_ID, PART_NAME, PART_DESC) 
VALUES (5, 'COLOGNE', 'Cologne Clinic - ddx-cologne-clinic')
ON CONFLICT (PART_ID) DO NOTHING;

-- Verify partitions
SELECT PART_ID, PART_NAME, PART_DESC FROM HFJ_PARTITION ORDER BY PART_ID;
