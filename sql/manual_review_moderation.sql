-- Manual DB changes for review moderation (PostgreSQL)
-- Adds: status workflow (PENDING/APPROVED/DELETED), audit fields, single admin reply, and soft-delete fields.

-- 1) Columns
ALTER TABLE review
  ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'PENDING',
  ADD COLUMN IF NOT EXISTS approved_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS approved_by bigint NULL,
  ADD COLUMN IF NOT EXISTS admin_reply text NULL,
  ADD COLUMN IF NOT EXISTS replied_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS replied_by bigint NULL,
  ADD COLUMN IF NOT EXISTS deleted_at timestamptz NULL,
  ADD COLUMN IF NOT EXISTS deleted_by bigint NULL;

-- 2) Foreign keys (PostgreSQL doesn't support ADD CONSTRAINT IF NOT EXISTS)
ALTER TABLE review DROP CONSTRAINT IF EXISTS fk_review_approved_by_admin;
ALTER TABLE review
  ADD CONSTRAINT fk_review_approved_by_admin
  FOREIGN KEY (approved_by) REFERENCES admin(id);

ALTER TABLE review DROP CONSTRAINT IF EXISTS fk_review_replied_by_admin;
ALTER TABLE review
  ADD CONSTRAINT fk_review_replied_by_admin
  FOREIGN KEY (replied_by) REFERENCES admin(id);

ALTER TABLE review DROP CONSTRAINT IF EXISTS fk_review_deleted_by_admin;
ALTER TABLE review
  ADD CONSTRAINT fk_review_deleted_by_admin
  FOREIGN KEY (deleted_by) REFERENCES admin(id);

-- 3) Index for admin listing and public GET
CREATE INDEX IF NOT EXISTS idx_review_status_created_at ON review (status, created_at DESC);

-- 4) Optional: make existing reviews immediately visible
-- UPDATE review SET status='APPROVED' WHERE status='PENDING';
