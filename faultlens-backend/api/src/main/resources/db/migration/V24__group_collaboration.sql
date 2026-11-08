-- P10 / 2.2: assign log groups, comment on them, merge them. See docs/adr/0004-log-group-lifecycle.md.
--
-- Rollback (manual):
--   DROP TABLE log_group_comments;
--   DROP INDEX idx_log_groups_assignee; DROP INDEX idx_log_groups_merged_into;
--   ALTER TABLE log_groups DROP COLUMN assignee, DROP COLUMN assigned_at,
--     DROP COLUMN merged_into_id, DROP COLUMN merged_at;
-- Entries that were routed to a merge target while merged stay in the target.

ALTER TABLE log_groups
    -- Username, like resolved_by: survives the user being deleted.
    ADD COLUMN assignee       VARCHAR(100),
    ADD COLUMN assigned_at    TIMESTAMPTZ,
    -- New entries of this group go to merged_into_id (processor); NULL = not merged.
    ADD COLUMN merged_into_id BIGINT REFERENCES log_groups (id) ON DELETE SET NULL,
    ADD COLUMN merged_at      TIMESTAMPTZ,
    ADD CONSTRAINT log_groups_not_merged_into_self CHECK (merged_into_id IS NULL OR merged_into_id <> id);

-- "Assigned to me".
CREATE INDEX idx_log_groups_assignee ON log_groups (assignee) WHERE assignee IS NOT NULL;
-- Processor: which of a batch's fingerprints are merged (few rows), and re-pointing on merge.
CREATE INDEX idx_log_groups_merged_into ON log_groups (merged_into_id) WHERE merged_into_id IS NOT NULL;

CREATE TABLE log_group_comments (
    id         BIGSERIAL    PRIMARY KEY,
    group_id   BIGINT       NOT NULL REFERENCES log_groups (id) ON DELETE CASCADE,
    author     VARCHAR(100) NOT NULL,
    -- Plain text (rendered as text, never as HTML).
    body       TEXT         NOT NULL CHECK (length(body) BETWEEN 1 AND 10000),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    edited_at  TIMESTAMPTZ
);
CREATE INDEX idx_log_group_comments_group ON log_group_comments (group_id, created_at);

-- log_group_events.type gains ASSIGNED, UNASSIGNED, COMMENTED, MERGED, MERGED_FROM, UNMERGED
-- (VARCHAR, no constraint to change). Payload keys: assignee, related_group_id.
