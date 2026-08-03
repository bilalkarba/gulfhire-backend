-- =====================================================
-- Migration: Add Cloudinary public-id columns to workers
--
-- These columns store the Cloudinary public_id of the
-- uploaded profile picture, CV and video CV so the
-- previous asset can be deleted when a worker replaces
-- or removes a file.
--
-- Note: Hibernate ddl-auto=update will add these columns
-- automatically on startup. This script exists for
-- environments where migrations are applied manually
-- (as with V001).
-- =====================================================

ALTER TABLE workers ADD COLUMN IF NOT EXISTS profile_picture_public_id VARCHAR(255);
ALTER TABLE workers ADD COLUMN IF NOT EXISTS cv_public_id VARCHAR(255);
ALTER TABLE workers ADD COLUMN IF NOT EXISTS video_cv_public_id VARCHAR(255);
