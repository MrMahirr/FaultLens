#!/bin/sh
# Daily logical backup of the FaultLens database (P08), run by the "db-backup" service of
# docker-compose.prod.yml. Custom-format dumps (pg_restore-able, compressed) are written to
# /backups and kept for BACKUP_KEEP_DAYS days.
#
# Restore (into an empty database):
#   pg_restore -h <host> -U <user> -d faultlens --no-owner /backups/faultlens-YYYYmmdd-HHMMSS.dump
set -eu

: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"
PGDATABASE="${PGDATABASE:-faultlens}"
BACKUP_HOUR="${BACKUP_HOUR:-02}"          # UTC hour of the daily run
BACKUP_KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
export PGHOST PGUSER PGPASSWORD PGDATABASE

backup() {
    stamp="$(date -u +%Y%m%d-%H%M%S)"
    target="/backups/faultlens-${stamp}.dump"
    echo "$(date -u +%FT%TZ) backing up ${PGDATABASE}@${PGHOST} -> ${target}"
    if ! pg_dump -Fc -f "${target}.partial"; then
        rm -f "${target}.partial"
        return 1
    fi
    # A dump that pg_restore cannot list is not a backup.
    if ! pg_restore --list "${target}.partial" > /dev/null; then
        rm -f "${target}.partial"
        return 1
    fi
    mv "${target}.partial" "${target}"
    find /backups -name 'faultlens-*.dump' -mtime "+${BACKUP_KEEP_DAYS}" -print -delete
    echo "$(date -u +%FT%TZ) done ($(du -h "${target}" | cut -f1))"
}

if [ "${1:-}" = "--now" ]; then
    backup
    exit $?
fi

while :; do
    now=$(date -u +%s)
    next=$(date -u -d "$(date -u +%Y-%m-%d) ${BACKUP_HOUR}:00:00" +%s 2>/dev/null || echo 0)
    [ "$next" -le "$now" ] && next=$((next + 86400))
    echo "next backup at $(date -u -d "@${next}" +%FT%TZ)"
    sleep $((next - now))
    backup || echo "$(date -u +%FT%TZ) BACKUP FAILED" >&2
done
