# ADR 0002 — Notification channels, roles and sessions (P07)

- Status: accepted
- Date: 2026-11-05
- Related: ADR 0001 (alarm rule engine)

## Context

P07 replaced several stop-gaps:

- E-mail went through EmailJS with the keys stored per user and handled by the browser.
- Every account was effectively an admin.
- Sessions were a single 24-hour JWT kept in `localStorage` and a readable cookie.

## Decisions

### Notification channels

- `NotificationChannel` (package `com.faultlens.api.notification`) has `type()`, `isAvailable()`, `isBroadcast()`, `accepts(recipient)` and `send(notification, recipient)`.
- `AlarmNotifier` picks the channels an alarm asked for. Broadcast channels (in-app) are sent once. Personal channels go to every user who opted in.
- Each delivery is retried, and a failure never stops the others.
- Implementations:
  - `InAppChannel` sends over WebSocket.
  - `SlackChannel` posts to the user's incoming webhook. The URL must be `hooks.slack.com`.
  - `EmailChannel` uses `JavaMailSender` over SMTP (`spring.mail.*`, `SMTP_*` env) and sends to the user's profile address.
    - Messages are multipart: plain text plus a small inline HTML template. No template engine for one template.
    - Title and message can carry log content. They are HTML-escaped, and the subject is forced onto one line (no header injection).
    - With `SMTP_HOST` empty the channel is unavailable and is skipped.
  - `WebhookChannel` sends a JSON POST `{event,title,message,severity,at,url}`.
    - Any role may set a webhook for themselves, so `WebhookUrls` accepts only `https` URLs without credentials.
    - The host must resolve only to public addresses: no loopback, private, link-local/metadata, CGNAT, ULA, or IPv4-mapped forms of these.
    - The URL is checked on save and again before every send. Redirects are not followed.
    - `WEBHOOK_ALLOW_PRIVATE_HOSTS=true` is the opt-in for on-prem receivers.
    - Residual risk: DNS rebinding between the check and the connection.
- The test endpoint for e-mail sends only to the caller's own profile address. Accepting any address would turn the server's SMTP account into a relay.
- The EmailJS columns stay in `notification_settings`, unused (V17 comment), so existing rows are untouched. A later cleanup can drop them.

### Roles

- There are three roles: `VIEWER` reads; `EDITOR` edits sources and rules, handles alarms, records deployments and triggers analyses; `ADMIN` also deletes, changes system settings and manages users.
- Rules are enforced with `@PreAuthorize` (see `Roles`). The UI's `useCan()` only hides controls.
- An admin can neither delete nor re-role themselves, and the last admin can neither be demoted nor deleted.

### Sessions

- The access JWT lasts 15 minutes and lives in memory only. After a reload it is restored through the refresh cookie.
- Refresh tokens:
  - They are opaque, single use, and stored as SHA-256 in Redis (`GETDEL` rotation).
  - Each user has a per-user index, so a password change, an admin reset or a deletion ends all sessions.
  - The cookie is `faultlens_refresh`: HttpOnly, Secure, SameSite=Strict, `Path=/api/v1/auth`, 7 days.
- A refresh re-reads the user. Role changes and deletions therefore apply within one access-token lifetime.
- `proxy.ts` sees only `faultlens_session`, a non-credential marker that holds the session expiry.
- Login rate limit: 5 failures per username lock it for 15 minutes (429 `LOGIN_LOCKED`, `Retry-After`), stored in Redis.
  - The limit fails open without Redis.
  - Trade-off: anyone can lock a known username for 15 minutes. This was accepted over IP-based limits behind proxies.
