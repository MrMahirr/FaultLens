"""Log search latency benchmark against the bench stack (P08).

python bench/search.py [--base http://localhost:3600/api/v1] [--runs 30]

Logs in as the bench admin (completing the forced first-login password change once), then
times representative /logs queries the UI issues and prints p50/p95/max per scenario.
Standard library only.
"""

import argparse
import json
import statistics
import time
import urllib.error
import urllib.parse
import urllib.request

INITIAL = "bench-admin-initial-1"  # bench/docker-compose.yml
CHANGED = "bench-admin-changed-2"

SCENARIOS = [
    ("list, no filter", {}),
    ("severity=ERROR", {"severity": "ERROR"}),
    ("source + 24h", {"sourceId": "1", "from": "@24h"}),
    ("search common 'connection refused'", {"search": "connection refused"}),
    ("search rare 'u4242'", {"search": "u4242"}),
    ("search absent 'zzqx-not-there'", {"search": "zzqx-not-there"}),
    ("search + severity + 24h", {"search": "timeout", "severity": "ERROR", "from": "@24h"}),
]


def call(method, url, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read() or b"{}")


def login(base):
    try:
        return call("POST", base + "/auth/login", body={"username": "admin", "password": CHANGED})["data"]["token"]
    except urllib.error.HTTPError:
        restricted = call("POST", base + "/auth/login", body={"username": "admin", "password": INITIAL})["data"]["token"]
        return call("PUT", base + "/auth/password", restricted,
                    {"current_password": INITIAL, "new_password": CHANGED})["data"]["token"]


def resolve(params):
    out = dict(params, page="0", size="20")
    if out.get("from") == "@24h":
        out["from"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(time.time() - 86400))
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:3600/api/v1")
    parser.add_argument("--runs", type=int, default=30)
    args = parser.parse_args()

    token = login(args.base)
    print(f"{'scenario':40} {'p50 ms':>8} {'p95 ms':>8} {'max ms':>8} {'total rows':>11}")
    for name, params in SCENARIOS:
        url = args.base + "/logs?" + urllib.parse.urlencode(resolve(params))
        call("GET", url, token)  # warm-up
        samples, total = [], None
        for _ in range(args.runs):
            start = time.perf_counter()
            body = call("GET", url, token)
            samples.append((time.perf_counter() - start) * 1000)
            total = body["data"]["total_elements"]
        samples.sort()
        p95 = samples[max(0, int(round(0.95 * len(samples))) - 1)]
        print(f"{name:40} {statistics.median(samples):8.0f} {p95:8.0f} {samples[-1]:8.0f} {total:11}")


if __name__ == "__main__":
    main()
