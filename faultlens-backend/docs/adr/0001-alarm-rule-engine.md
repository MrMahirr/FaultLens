# ADR 0001 — Alarm rule engine

- Status: accepted
- Date: 2026-11-05
- Plan: P07 (replaces the P06 per-group alarm cooldown)

## Context

Alarms were raised for every analysis (`AnalysisEventListener` → one "AI_ANALYSIS" alarm per
analysed error group, P06 added a 15-minute cooldown). The "Alarm rules" page was frontend mock
data. Operators need rules such as "more than 50 ERROR lines in 5 minutes", "a line matches
`Connection refused`" or "a new kind of error appeared", stored on the server, with cooldowns.

## Decision

**Rule types**

| Type | Fires when | Fields |
|---|---|---|
| `THRESHOLD` | count of entries with severity ≥ `severity` in the last `window_sec` reaches `threshold` | `severity`, `window_sec` (60–3600), `threshold` |
| `PATTERN` | an entry with severity ≥ `severity` matches the regular expression `pattern` | `severity`, `pattern` |
| `NEW_GROUP` | a log group with severity ≥ `severity` is created (an error never seen before) | `severity` |

All rules can be limited to sources (`source_ids`, empty = all), have a `cooldown_sec`, the
notification `channels` (`IN_APP`, `SLACK`, `EMAIL`) and an `enabled` flag. A separate `RATE`
type is not added: a threshold over a window already is a rate.

**Where rules are evaluated**: in the api module, on the processor's `LogProcessedEvent`
(`@TransactionalEventListener(AFTER_COMMIT)`), i.e. after entries are stored, in the same
process. Rules see every processed entry (not only errors, which is all `log-errors` carries).
When the processor is split out (P08) this becomes a Kafka topic of processed entries; the
evaluator's input stays the same.

**State in Redis**
- Sliding window: one counter per rule, source scope and minute (`INCR`, TTL = window + 60 s);
  the count is the sum of the last `window_sec / 60` buckets. Precision is one minute, which is
  enough for alarm windows of minutes and keeps the cost at one `INCR` per matching entry.
- Dedup / cooldown: `SET NX EX cooldown_sec` on `rule id + scope` (group id for `PATTERN` and
  `NEW_GROUP`, source for `THRESHOLD`). The P06 per-group cooldown for analysis alarms uses the
  same mechanism with a built-in key.
- Redis unavailable: counting and dedup fail open for dedup (raise the alarm) and closed for
  thresholds (no count, no alarm) — a missing alarm is preferred to a burst of false ones.

**Data model**: `alarm_rules` table (V15); `alarms.alarm_rule_id` references it (`ON DELETE SET
NULL`, so deleting a rule keeps its alarm history); `alarms.rule_id` stays as a text key for
built-in sources such as `AI_ANALYSIS`. `Alarm.severity` becomes the `Severity` enum (stored
values are already enum names).

## Consequences

- Analysis alarms keep working unchanged; operators add their own rules on top.
- A Redis outage stops threshold alarms until it recovers.
- One `INCR` per matching entry per threshold rule; with many rules and high volume this is the
  part to watch (P08 observability).
