import json
from pathlib import Path

# -----------------------------
# Load stations database
# -----------------------------
with open("database/stations.json", "r", encoding="utf-8") as f:
    stations = json.load(f)

# -----------------------------
# Create output directory
# -----------------------------
output_dir = Path("playlists")
output_dir.mkdir(parents=True, exist_ok=True)

# -----------------------------
# Default playlists
# -----------------------------
playlists = {
    "all": ["#EXTM3U"],
}

# -----------------------------
# Generate playlists
# -----------------------------
for station in stations:

    # Make sure categories exist
    categories = station.get("categories", [])

    # Create language playlist automatically
    language = station["language"].lower()

    if language not in playlists:
        playlists[language] = ["#EXTM3U"]

    # Create category playlists automatically
    for category in categories:
        category_name = category.lower()

        if category_name not in playlists:
            playlists[category_name] = ["#EXTM3U"]

    # IPTV metadata
    entry = (
        f'#EXTINF:-1 '
        f'tvg-id="{station["id"]}" '
        f'tvg-name="{station["name"]}" '
        f'tvg-logo="{station["logo"]}" '
        f'group-title="{", ".join(categories)}",'
        f'{station["name"]}'
    )

    for stream in station.get("streams", []):
        url = stream["url"]

        # Add to ALL
        playlists["all"].append(entry)
        playlists["all"].append(url)

        # Add to Language playlist
        playlists[language].append(entry)
        playlists[language].append(url)

        # Add to every Category playlist
        for category in categories:
            playlists[category.lower()].append(entry)
            playlists[category.lower()].append(url)

# -----------------------------
# Save playlists
# -----------------------------
for playlist_name, lines in playlists.items():

    (output_dir / f"{playlist_name}.m3u").write_text(
        "\n".join(lines),
        encoding="utf-8"
    )

print("\n✅ Playlists generated:\n")

for playlist in sorted(playlists.keys()):
    print(f"   {playlist}.m3u")