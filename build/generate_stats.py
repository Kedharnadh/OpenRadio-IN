import json
from pathlib import Path

stations = json.loads(Path("database/stations.json").read_text(encoding="utf-8"))

stats = {
    "totalStations": len(stations),
    "languages": {},
    "categories": {}
}

for station in stations:
    lang = station["language"]
    stats["languages"][lang] = stats["languages"].get(lang, 0) + 1

    for category in station.get("categories", []):
        stats["categories"][category] = stats["categories"].get(category, 0) + 1

output_dir = Path("output")
output_dir.mkdir(exist_ok=True)

Path("output/statistics.json").write_text(
    json.dumps(stats, indent=2),
    encoding="utf-8"
)

print("✅ Statistics generated")