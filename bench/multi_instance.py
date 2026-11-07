"""Two-instance collector test against the bench stack (P08 acceptance: no duplicate logs).

  docker compose -p faultlens-bench -f bench/docker-compose.yml --profile multi up -d --build backend backend2
  python bench/multi_instance.py [--lines 2000]

Creates one LOCAL_FILE source (the log directory is shared by both instances), appends numbered
lines, and checks that every line was stored exactly once. Then stops the instance holding the
source's lease, checks the other one takes the source over, and appends a second batch.
"""

import argparse
import json
import os
import subprocess
import sys
import time
import uuid

sys.path.insert(0, os.path.dirname(__file__))
from search import call, login  # noqa: E402

COMPOSE = ["docker", "compose", "-p", "faultlens-bench", "-f", os.path.join(os.path.dirname(__file__), "docker-compose.yml"),
           "--profile", "multi"]
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
SERVICES = {"backend": "http://localhost:3600/api/v1", "backend2": "http://localhost:3601/api/v1"}


def compose(*args):
    return subprocess.run(COMPOSE + list(args), env=ENV, check=True, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.DEVNULL).stdout.strip()


def sql(query):
    return compose("exec", "-T", "postgres", "psql", "-U", "faultlens", "-d", "faultlens", "-tAc", query)


def owner(source_id):
    return compose("exec", "-T", "redis", "redis-cli", "GET", f"faultlens:collector:lease:{source_id}")


def append(service, path, marker, start, count):
    script = f'for i in $(seq {start} {start + count - 1}); do echo "ERROR {marker} line $i failed"; done >> {path}'
    compose("exec", "-T", service, "sh", "-c", script)


def stored(marker):
    total, distinct = sql(f"select count(*), count(distinct message) from log_entries where message like '%{marker} line %'").split("|")
    return int(total), int(distinct)


def wait_for(marker, expected, timeout=120):
    deadline = time.time() + timeout
    while time.time() < deadline:
        total, distinct = stored(marker)
        if total >= expected:
            break
        time.sleep(2)
    time.sleep(5)  # anything late would be a duplicate
    return stored(marker)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--lines", type=int, default=2000)
    args = parser.parse_args()

    marker = "mi" + uuid.uuid4().hex[:10]
    path = f"/var/log/bench/{marker}.log"
    compose("exec", "-T", "backend", "sh", "-c", f": > {path}")

    token = login(SERVICES["backend"])
    source = call("POST", SERVICES["backend"] + "/sources", token, {
        "name": f"multi-{marker}", "type": "LOCAL_FILE", "enabled": True,
        "config": json.dumps({"logFilePath": path})})["data"]
    sid = source["id"]
    print(f"source {sid} -> {path}")
    time.sleep(20)  # both instances reconcile at least once
    holder = owner(sid)
    print("lease holder:", holder)

    append("backend", path, marker, 1, args.lines)
    total, distinct = wait_for(marker, args.lines)
    print(f"phase 1: {args.lines} lines written, {total} stored, {distinct} distinct -> duplicates {total - distinct}, "
          f"missing {args.lines - distinct}")
    ok = total == distinct == args.lines

    # Failover: stop the holder, the other instance must take over within ttl + reconcile.
    holder_service = None
    for service in SERVICES:
        host = compose("exec", "-T", service, "hostname")
        if holder.startswith(host):
            holder_service = service
    survivor = next(s for s in SERVICES if s != holder_service)
    print(f"stopping {holder_service}; {survivor} should take over")
    compose("stop", holder_service)
    deadline = time.time() + 90
    while time.time() < deadline and not owner(sid).startswith(compose("exec", "-T", survivor, "hostname")):
        time.sleep(3)
    took = 90 - (deadline - time.time())
    print(f"new holder: {owner(sid)} after {took:.0f}s")
    time.sleep(3)

    append(survivor, path, marker, args.lines + 1, args.lines)
    total, distinct = wait_for(marker, 2 * args.lines)
    print(f"phase 2: {2 * args.lines} lines written in total, {total} stored, {distinct} distinct -> "
          f"duplicates {total - distinct}, missing {2 * args.lines - distinct}")
    ok = ok and total == distinct == 2 * args.lines

    compose("start", holder_service)
    call("POST", SERVICES[survivor] + f"/sources/{sid}/disable", login(SERVICES[survivor]))
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
