-- =====================================================
-- Migration: Drop stale columns from companies table
-- 
-- These columns were from the old Company entity and
-- are no longer mapped in the current JPA entity.
-- Hibernate ddl-auto=update cannot drop columns,
-- so stale NOT NULL columns cause INSERT failures.
-- =====================================================

-- Drop the address column that caused the NOT NULL violation
ALTER TABLE companies DROP COLUMN IF EXISTS address;

-- Drop other stale columns from the old entity schema
ALTER TABLE companies DROP COLUMN IF EXISTS name;
ALTER TABLE companies DROP COLUMN IF EXISTS email;
ALTER TABLE companies DROP COLUMN IF EXISTS phone_number;
ALTER TABLE companies DROP COLUMN IF EXISTS city;
ALTER TABLE companies DROP COLUMN IF EXISTS country;
ALTER TABLE companies DROP COLUMN IF EXISTS size;
ALTER TABLE companies DROP COLUMN IF EXISTS is_active;
ALTER TABLE companies DROP COLUMN IF EXISTS isActive;

-- Drop stale columns that are now nullable in the new entity
-- (old entity had these as NOT NULL, new entity makes them optional)
ALTER TABLE companies ALTER COLUMN website DROP NOT NULL;
ALTER TABLE companies ALTER COLUMN logo_url DROP NOT NULL;

-- =====================================================
-- Note: If you prefer to keep existing data, comment out
-- the DROP statements above and use ALTER instead:
--
-- ALTER TABLE companies ALTER COLUMN address DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN name DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN email DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN phone_number DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN city DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN country DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN size DROP NOT NULL;
-- ALTER TABLE companies ALTER COLUMN is_active DROP NOT NULL;
-- =====================================================
