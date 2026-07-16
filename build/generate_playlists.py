import json
from pathlib import Path

# Load stations
with open("database/stations.json", "r", encoding="utf-8") as f:
    stations = json.load(f)

# Create output folder
output_dir = Path("output/playlists")
output_dir.mkdir(parents=True, exist_ok=True)

playlists = {
    "all": ["#EXTM3U"],
    "telugu": ["#EXTM3U"],
    "air": ["#EXTM3U"],
}

for station in stations:

    entry = (
        f'#EXTINF:-1 '
        f'tvg-id="{station["id"]}" '
        f'tvg-name="{station["name"]}" '
        f'tvg-logo="{station["logo"]}" '
        f'group-title="{station["category"]}",'
        f'{station["name"]}'
    )

    for stream in station.get("streams", []):
        url = stream["url"]

        # All stations
        playlists["all"].append(entry)
        playlists["all"].append(url)

        # Telugu stations
        if station["language"].lower() == "telugu":
            playlists["telugu"].append(entry)
            playlists["telugu"].append(url)

        # AIR stations
        if station["category"].upper() == "AIR":
            playlists["air"].append(entry)
            playlists["air"].append(url)

# Save playlists
for name, lines in playlists.items():
    (output_dir / f"{name}.m3u").write_text(
        "\n".join(lines),
        encoding="utf-8"
    )

print("✅ Generated:")
print("   all.m3u")
print("   telugu.m3u")
print("   air.m3u")