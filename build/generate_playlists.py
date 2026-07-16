import json
from pathlib import Path

# ----------------------------------------
# Configuration
# ----------------------------------------
DATABASE_FILE = Path("database/stations.json")
OUTPUT_DIR = Path("playlists")

# ----------------------------------------
# Load stations
# ----------------------------------------
with DATABASE_FILE.open("r", encoding="utf-8") as f:
    stations = json.load(f)

# Sort stations alphabetically
stations = sorted(stations, key=lambda x: x.get("name", "").lower())

# ----------------------------------------
# Create output directory
# ----------------------------------------
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# ----------------------------------------
# Playlist storage
# ----------------------------------------
playlists = {
    "all": ["#EXTM3U"]
}

# Used to avoid duplicate entries
playlist_streams = {
    "all": set()
}

# ----------------------------------------
# Generate playlists
# ----------------------------------------
for station in stations:

    station_id = station.get("id", "")
    station_name = station.get("name", "")
    language = station.get("language", "").strip().lower()

    logo = station.get("logo", "")

    categories = station.get("categories", [])

    # -----------------------------
    # Language playlist
    # -----------------------------
    if language:

        if language not in playlists:
            playlists[language] = ["#EXTM3U"]
            playlist_streams[language] = set()

    # -----------------------------
    # Category playlists
    # -----------------------------
    for category in categories:

        category_name = category.strip().lower()

        # Skip ALL and Language
        if category_name in ("all", language):
            continue

        if category_name not in playlists:
            playlists[category_name] = ["#EXTM3U"]
            playlist_streams[category_name] = set()

    # -----------------------------
    # IPTV metadata
    # -----------------------------
    group_title = ", ".join(categories)

    entry = (
        f'#EXTINF:-1 '
        f'tvg-id="{station_id}" '
        f'tvg-name="{station_name}" '
        f'tvg-logo="{logo}" '
        f'group-title="{group_title}",'
        f'{station_name}'
    )

    # -----------------------------
    # Streams
    # -----------------------------
    for stream in station.get("streams", []):

        url = stream.get("url", "").strip()

        if not url:
            continue

        # -----------------------------
        # ALL playlist
        # -----------------------------
        if url not in playlist_streams["all"]:
            playlists["all"].append(entry)
            playlists["all"].append(url)
            playlist_streams["all"].add(url)

        # -----------------------------
        # Language playlist
        # -----------------------------
        if language:

            if url not in playlist_streams[language]:
                playlists[language].append(entry)
                playlists[language].append(url)
                playlist_streams[language].add(url)

        # -----------------------------
        # Category playlists
        # -----------------------------
        for category in categories:

            category_name = category.strip().lower()

            # Skip language and ALL
            if category_name in ("all", language):
                continue

            if url not in playlist_streams[category_name]:
                playlists[category_name].append(entry)
                playlists[category_name].append(url)
                playlist_streams[category_name].add(url)

# ----------------------------------------
# Save playlists
# ----------------------------------------
for playlist_name in sorted(playlists.keys()):

    playlist_file = OUTPUT_DIR / f"{playlist_name}.m3u"

    playlist_file.write_text(
        "\n".join(playlists[playlist_name]),
        encoding="utf-8"
    )

# ----------------------------------------
# Summary
# ----------------------------------------
print("\n✅ Playlists generated:\n")

for playlist_name in sorted(playlists.keys()):

    stations_count = (len(playlists[playlist_name]) - 1) // 2

    print(f"{playlist_name}.m3u ({stations_count} stations)")

print("\n🎉 Done!")