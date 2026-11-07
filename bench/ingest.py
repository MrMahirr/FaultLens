"""Ingest throughput + consumer lag benchmark against the bench stack (P08).

python bench/ingest.py [--records 200000]

Publishes raw log events to bench Kafka (log-raw) as fast as kafka-producer-perf-test can, then
samples the processor's consumer lag and the log_entries row count until the backlog drains.
Standard library + docker CLI only (works from Git Bash, PowerShell or Linux shells).
"""

import argparse
import json
import os
import random
import subprocess
import tempfile
import time

COMPOSE = ["docker", "compose", "-p", "faultlens-bench", "-f",
           os.path.join(os.path.dirname(__file__), "docker-compose.yml")]
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")


def run(*args, capture=True):
    return subprocess.run(COMPOSE + list(args), env=ENV, check=True, text=True,
                          stdout=subprocess.PIPE if capture else None, stderr=subprocess.DEVNULL).stdout


def rows():
    return int(run("exec", "-T", "postgres", "psql", "-U", "faultlens", "-d", "faultlens", "-tAc",
                   "select count(*) from log_entries").strip())


def lag(group="faultlens-processor"):
    try:
        out = run("exec", "-T", "kafka", "/opt/kafka/bin/kafka-consumer-groups.sh", "--bootstrap-server",
                  "localhost:9192", "--describe", "--group", group)
    except subprocess.CalledProcessError:
        return -1
    total = 0
    for line in out.splitlines():
        parts = line.split()
        if len(parts) > 5 and parts[1] == "log-raw" and parts[5].isdigit():
            total += int(parts[5])
    return total


def payload_file():
    sev = ["INFO"] * 14 + ["WARN"] * 3 + ["ERROR"] * 3
    msgs = ["GET /api/v1/orders/{n} 200 in {ms}ms", "Connection refused: db-{r}.internal:5432 after {ms}ms",
            "Timeout waiting for lock on account u{n} after {ms}ms", "Retrying request to payment-gateway attempt {r}/3",
            "java.lang.IllegalStateException: order {n} already shipped"]
    fd, path = tempfile.mkstemp(suffix=".txt")
    with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as f:
        for _ in range(20000):
            m = random.choice(msgs).format(n=random.randint(1, 99999), ms=random.randint(5, 4000), r=random.randint(1, 9))
            f.write(json.dumps({"source": "bench", "service_name": "bench-svc", "severity": random.choice(sev),
                                "message": m, "raw_line": m,
                                "labels": {"sourceId": str(random.randint(1, 3))}}) + "\n")
    return path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--records", type=int, default=200000)
    args = parser.parse_args()

    path = payload_file()
    run("cp", path, "kafka:/tmp/payload.txt")
    os.remove(path)

    before = rows()
    print(f"rows before: {before}; publishing {args.records} events...")
    start = time.time()
    out = run("exec", "-T", "kafka", "/opt/kafka/bin/kafka-producer-perf-test.sh", "--topic", "log-raw",
              "--num-records", str(args.records), "--throughput", "-1", "--payload-file", "/tmp/payload.txt",
              "--bootstrap-server", "localhost:9192",
              "--command-property", "linger.ms=5", "batch.size=65536")
    print("producer:", out.strip().splitlines()[-1])

    max_lag, last = 0, 0
    while True:
        done = rows() - before
        current = lag()
        max_lag = max(max_lag, current)
        elapsed = time.time() - start
        if done != last or done >= args.records:
            print(f"{elapsed:7.1f}s processed={done:<8} lag={current}")
            last = done
        if done >= args.records:
            break
        if elapsed > 1800:
            print("giving up after 30 minutes")
            break
        time.sleep(2)
    elapsed = time.time() - start
    print(f"== {done} events end-to-end in {elapsed:.1f}s -> {done / elapsed:.0f} events/s; max lag {max_lag}")


if __name__ == "__main__":
    main()
