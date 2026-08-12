#!/usr/bin/env python3
"""Check live status of radio station streams and write a status field.

Usage:
    python build/health_check.py <input.json> [output.json]

Reads a stations array from <input.json>, probes each station's streams
(preferring the highest-priority one), and writes ``status`` and
``last_checked`` fields. Status is one of:

    online   - a stream responded with a 2xx/206
    offline  - a stream gave a definitive failure (HTTP error, DNS, refused)
    unknown  - probe failed for an indeterminate reason (timeout/geo-block)

The default status is ``unknown`` so that a flaky deploy environment never
labels working stations as dead.
"""

import concurrent.futures
import json
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

PROBE_TIMEOUT = 8
MAX_WORKERS = 6


def probe_stream(url, codec):
    """Return True/False/None for online/offline/unknown of a single stream."""
    try:
        req = urllib.request.Request(url, method="GET", headers={
            "User-Agent": "Mozilla/5.0 (OpenRadio-IN health check)",
            "Range": "bytes=0-0",
        })
        with urllib.request.urlopen(req, timeout=PROBE_TIMEOUT) as resp:
            return 200 <= resp.status < 400
    except urllib.error.HTTPError as err:
        # Definitive server-side failure.
        return False
    except (urllib.error.URLError, ConnectionError, OSError) as err:
        if isinstance(err, urllib.error.URLError) and isinstance(err.reason, (TimeoutError,)):
            return None
        # DNS failure / connection refused are definitive.
        return False
    except Exception:
        return None


def probe_station(station):
    streams = [s for s in (station.get("streams") or []) if s.get("url")]
    streams.sort(key=lambda s: s.get("priority", float("inf")))
    if not streams:
        return "unknown"
    had_failure = False
    for stream in streams:
        result = probe_stream(stream["url"], stream.get("codec"))
        if result is True:
            return "online"
        if result is False:
            # Definitive failure for this stream; keep probing backup streams.
            had_failure = True
        # None -> keep probing other streams
    return "offline" if had_failure else "unknown"


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1

    source = sys.argv[1]
    output = sys.argv[2] if len(sys.argv) > 2 else source
    with open(source, "r", encoding="utf-8") as fh:
        stations = json.load(fh)

    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    results = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as pool:
        future_map = {pool.submit(probe_station, s): s for s in stations}
        for future in concurrent.futures.as_completed(future_map):
            station = future_map[future]
            try:
                results[id(station)] = future.result()
            except Exception:
                results[id(station)] = "unknown"

    counts = {"online": 0, "offline": 0, "unknown": 0}
    for station in stations:
        status = results.get(id(station), "unknown")
        station["status"] = status
        station["last_checked"] = now
        counts[status] += 1

    with open(output, "w", encoding="utf-8") as fh:
        json.dump(stations, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    print(f"Checked {len(stations)} stations: "
          f"{counts['online']} online, {counts['offline']} offline, {counts['unknown']} unknown")
    return 0


if __name__ == "__main__":
    sys.exit(main())
