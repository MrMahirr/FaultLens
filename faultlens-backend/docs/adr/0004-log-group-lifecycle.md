# ADR 0004 — Log group lifecycle and regressions

- Status: accepted
- Date: 2026-11-08
- Plan: P10 / 2.1 (Wave 2, error group lifecycle)

## Context

Log groups (one per error fingerprint) only grew: there was no way to say "this is fixed" or
"this is noise", so the groups list and the NEW_GROUP / THRESHOLD / PATTERN alarms kept showing
known problems. Operators need Sentry-style states, and above all to learn when a problem they
marked as fixed comes back (a regression).

## Decision

**States** (`log_groups.status`, `GroupStatus`): `UNRESOLVED` (default), `RESOLVED`, `IGNORED`
(optionally until `ignore_until`). Manual transitions go through `PATCH
/api/v1/logs/groups/{id}` (EDITOR+), checked by `GroupStateMachine`:

```
UNRESOLVED -> RESOLVED | IGNORED
RESOLVED   -> UNRESOLVED (reopen) | IGNORED
IGNORED    -> UNRESOLVED (reopen) | RESOLVED | IGNORED (new period)
```

Setting the current status again is a 409 (`GROUP_STATUS_UNCHANGED`): the client acted on a
stale screen. `ignore_until` only with `IGNORED` and in the future (400).

**System transitions happen in the grouping upsert itself** (processor, `LogGroupingService`),
in the same `INSERT … ON CONFLICT DO UPDATE` that increments the count:

- `RESOLVED` and the batch's newest entry is newer than `resolved_at` → `UNRESOLVED`,
  `regressed_at = now()`, `regression_count + 1`. Late entries older than the resolution only
  add to the count.
- `IGNORED` with an expired `ignore_until` → `UNRESOLVED`, no regression.

Deciding it in the statement (row lock held) means two concurrent batches cannot both see
"RESOLVED" and raise two regressions; it adds no query to the hot path. PostgreSQL 18's
`RETURNING old.status, new.status` tells the processor which transition happened.

The manual update is guarded by the status it was checked against (`WHERE status = ?`): if the
processor regressed the group in between, the user's change fails with 409
(`GROUP_STATUS_CONFLICT`) instead of overwriting the regression.

**Events stay in-process**, like `LogGroupCreatedEvent` (ADR 0001): the processor publishes
`LogGroupRegressedEvent`, and `AlarmRuleEvaluator` handles it after commit for the new rule type
**`REGRESSION`** (severity + sources, like NEW_GROUP). The dedup key includes the regression
number, so every reopening alarms once. The plan proposed a `group-events` Kafka topic; it is not
added because the producer and the only consumer run in the same JVM — a topic would add a DLQ, a
lag metric and a serialisation format for no new reader. When a second consumer appears (e.g.
incidents, 4.1, in another service), publish these events to Kafka from the same listener.

**Muting**: `LogProcessedEvent` carries the ids of the batch's groups that are `IGNORED`; THRESHOLD
and PATTERN rules skip their entries. New groups are never ignored, so NEW_GROUP is unaffected.
`GET /logs/groups` leaves IGNORED groups out unless `status` asks for them.

**Timeline**: `log_group_events (group_id, type, actor, payload jsonb, created_at)`, one table for
the whole lifecycle — `CREATED`, `REGRESSED`, `UNIGNORED` written by the processor (actor null),
`RESOLVED`, `IGNORED`, `REOPENED` by the API (actor = username). 2.2 adds assignment, comments and
merges to the same table. `GET /logs/groups/{id}/events` returns the newest 200, with the payload
exposed as typed fields (`from_status`, `ignore_until`, `regression_count`), not free JSON.

Users are referenced by **username** (`resolved_by`, `actor`), like `alarm_rules.created_by`:
history survives a user being deleted.

## Collaboration (P10 / 2.2, V24)

- **Assignment**: `log_groups.assignee` (username) via `PUT /logs/groups/{id}/assignee` (EDITOR+);
  the user must exist. The assignee gets an in-app notification (`GROUP_ASSIGNED`) on their own
  WebSocket sessions after commit; e-mail is not sent. Editors cannot list users, so
  `GET /logs/groups/assignees` returns names only. The plan's "alarm claim → assign" link is not
  built: alarms have no claim/assignee yet.
- **Comments**: `log_group_comments`, plain text up to 10 000 characters, rendered as text (no
  Markdown, no HTML — nothing to sanitise). Editors write; only the author edits; the author or an
  admin deletes.
- **Merge**: `merged_into_id`. The processor reads the batch's merged fingerprints in one indexed
  query *before* locking and aggregates them under the target's fingerprint, so rows are still
  locked in one global (fingerprint) order — the merge transaction locks its two rows in the same
  order. A Redis cache (as the plan suggested) is not needed for one indexed query per batch.
  Past entries stay where they are; merged groups leave the list; merges never chain (groups
  merged into the source are re-pointed to the target, and nothing can be merged into a merged
  group). Undo (`/unmerge`) is ADMIN only; entries routed meanwhile stay in the target. A merge
  racing a batch that already read the merge map is caught by the upsert (`RETURNING
  merged_into_id`): the entries still go to the target, only the source's count takes them.

## Not done (yet)

- `resolved_in_version` ("resolved in the next release"): needs the entry's version, which logs do
  not carry. Resolve means "from now on".
- Hiding entries of ignored groups from the Logs list: a join on every log query; the groups view
  and alarms are where muting matters.
- Groups that existed before V23 have no `CREATED` event; their timeline starts empty.

## Consequences

- One more `CASE` per column in the grouping upsert; no extra round trip.
- `log_group_events` grows by one row per group plus one per transition — small next to
  `log_entries`, removed with the group (`ON DELETE CASCADE`).
- Requires PostgreSQL 18 (`RETURNING old/new`), which the project already runs.
