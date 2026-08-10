-- =====================================================
-- Migration: Store chat timestamps as UTC (timestamptz)
--
-- The Message entity now maps createdAt/editedAt to java.time.Instant,
-- which Hibernate stores as "timestamp with time zone" (UTC). Existing
-- columns were created as "timestamp without time zone" and Hibernate
-- ddl-auto=update will NOT alter their type — run this script manually
-- (like V001/V002) to convert them.
--
-- NOTE: old values were written as server-local wall-clock time
-- (LocalDateTime.now()). To preserve the same absolute instant, the
-- conversion uses the CURRENT session timezone — run this with the same
-- timezone the app server uses (e.g. if the server runs in UTC, use
-- 'AT TIME ZONE ''UTC''' instead).
-- =====================================================

ALTER TABLE messages ALTER COLUMN created_at TYPE timestamptz
    USING created_at AT TIME ZONE current_setting('TimeZone');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'messages' AND column_name = 'edited_at') THEN
        ALTER TABLE messages ALTER COLUMN edited_at TYPE timestamptz
            USING edited_at AT TIME ZONE current_setting('TimeZone');
    END IF;
END $$;
