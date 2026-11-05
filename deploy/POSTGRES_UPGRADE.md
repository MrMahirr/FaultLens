# PostgreSQL 16 → 18 upgrade

A major PostgreSQL version cannot open the data directory of an older major version. The
compose file therefore uses a **new volume** (`postgres18_data`) for Postgres 18; the old
`postgres_data` volume (Postgres 16) is left untouched until you remove it yourself.

Flyway recreates the schema on an empty database, so you only need the steps below if you
want to **keep existing data**.

## Local development

Either start fresh (Flyway builds the schema, the admin account is recreated from
`ADMIN_INITIAL_PASSWORD`):

```bash
docker compose up -d
# optional, once you no longer need the old data:
docker volume rm faultlens_postgres_data
```

or copy the old data over:

```bash
# 1. Dump from a temporary Postgres 16 container on the old volume
docker run -d --name pg16-dump -e POSTGRES_PASSWORD=faultlens \
  -v faultlens_postgres_data:/var/lib/postgresql/data postgres:16-alpine
docker exec pg16-dump pg_dump -U faultlens -d faultlens -Fc -f /tmp/faultlens.dump
docker cp pg16-dump:/tmp/faultlens.dump ./faultlens.dump
docker rm -f pg16-dump

# 2. Start Postgres 18 alone (the backend must not run yet: Flyway would create the schema)
docker compose up -d postgres

# 3. Restore into the empty database
docker compose cp ./faultlens.dump postgres:/tmp/faultlens.dump
docker compose exec postgres pg_restore -U faultlens -d faultlens --no-owner /tmp/faultlens.dump

# 4. Start everything; Flyway sees the existing history and applies only newer migrations
docker compose up -d
```

## Production (managed or self-hosted database)

1. Announce a maintenance window and stop the backend (no writes during the dump).
2. `pg_dump -Fc` from the 16 server, using the **18** client tools (newer `pg_dump` can read
   older servers, not the other way round).
3. Create the 18 database and role, then `pg_restore --no-owner` into it.
4. Compare row counts of `log_entries`, `log_groups`, `analyses`, `alarms`, `users` and
   the latest `flyway_schema_history` version between the two servers.
5. Point `DB_HOST`/`DB_PORT` at the 18 server and start the backend; check
   `/actuator/health` and the latest logs and alarms in the UI.
6. Keep the 16 server read-only until the new one has run for a while; that is the rollback
   path (switch `DB_HOST` back).

Managed services (RDS, Cloud SQL, Azure) also offer in-place major upgrades (`pg_upgrade`
under the hood). Take a snapshot first; the verification in step 4 still applies.
