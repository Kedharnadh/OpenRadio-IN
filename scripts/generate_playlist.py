#!/usr/bin/env python3
import json
from pathlib import Path

stations=Path("stations")
out=Path("playlists/all.m3u")
lines=["#EXTM3U"]
for f in stations.rglob("*.json"):
    s=json.loads(f.read_text())
    lines.append(f'#EXTINF:-1 tvg-name="{s["name"]}" group-title="{s["language"]}",{s["name"]}')
    lines.append(s["stream"])
out.write_text("\n".join(lines),encoding="utf-8")
print("Generated",out)
