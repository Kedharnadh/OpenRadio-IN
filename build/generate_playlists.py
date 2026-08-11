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
    languages = [lang.strip().lower() for lang in str(station.get("language", "")).split(",") if lang.strip() and lang.strip().lower() != "all"]

    logo = station.get("logo", "")

    categories = station.get("categories", [])

    # -----------------------------
    # Language playlists
    # -----------------------------
    # Multi-language stations are added to one playlist per language, e.g.
    # "Assamese, Hindi, English" appears in assamese.m3u, hindi.m3u and
    # english.m3u (never in a combined "assamese, hindi, english.m3u").
    for language in languages:

        if language not in playlists:
            playlists[language] = ["#EXTM3U"]
            playlist_streams[language] = set()

    # -----------------------------
    # Category playlists
    # -----------------------------
    for category in categories:

        category_name = category.strip().lower()

        # Skip ALL and any language playlists
        if category_name == "all" or category_name in languages:
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
        # Language playlists
        # -----------------------------
        for language in languages:

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
            if category_name == "all" or category_name in languages:
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

# Remove stale playlist files that are no longer generated.
for existing in sorted(OUTPUT_DIR.glob("*.m3u")):

    if existing.stem not in playlists:
        existing.unlink()

# ----------------------------------------
# Summary
# ----------------------------------------
print("\nPlaylists generated:\n")

for playlist_name in sorted(playlists.keys()):

    stations_count = (len(playlists[playlist_name]) - 1) // 2

    print(f"{playlist_name}.m3u ({stations_count} stations)")

print("\nDone!")