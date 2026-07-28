# 📻 OpenRadio-IN

> A community-driven collection of verified Indian online radio stations with automatically generated playlists for VLC, Kodi, Home Assistant, TiviMate, Sparkle TV, OTT Navigator, and other compatible players.


PWA app available at
https://kedharnadh.github.io/OpenRadio-IN/


![GitHub](https://img.shields.io/github/license/Kedharnadh/OpenRadio-IN)
![GitHub stars](https://img.shields.io/github/stars/Kedharnadh/OpenRadio-IN)
![GitHub issues](https://img.shields.io/github/issues/Kedharnadh/OpenRadio-IN)
![GitHub last commit](https://img.shields.io/github/last-commit/Kedharnadh/OpenRadio-IN)

---

## ✨ Features

- 🇮🇳 Verified Indian Radio Stations
- 📻 AIR (Akashvani) Stations
- 🎵 FM Stations
- 🛕 Devotional Stations
- 📰 News Stations
- 🌐 Internet Radio
- 🎼 Classical Music
- 🎯 Automatically generated playlists
- 🔍 JSON-based station database
- 🐍 Python build system
- ⚙️ GitHub Actions automation
- 🌐 GitHub Pages website (Coming Soon)

---

# Supported Players

OpenRadio-IN playlists work with:

- VLC Media Player
- Kodi
- Sparkle TV
- TiviMate
- OTT Navigator
- IPTV Pro
- Home Assistant
- Jellyfin
- Emby
- Plex (via IPTV plugins)

---

# Repository Structure

```
OpenRadio-IN
│
├── database/
│   └── stations.json
│
├── build/
│   ├── generate_playlists.py
│   ├── validate_database.py
│   ├── check_streams.py
│   └── generate_stats.py
│
├── playlists/
│   ├── all.m3u
│   ├── telugu.m3u
│   ├── air.m3u
│   └── ...
│
├── website/
│
├── docs/
│
└── .github/
```

---

# Available Playlists

| Playlist | Description |
|-----------|-------------|
| all.m3u | Every station |
| telugu.m3u | Telugu stations |
| air.m3u | All India Radio stations |
| fm.m3u | FM stations |
| devotional.m3u | Devotional stations |
| news.m3u | News stations |

More playlists will be added as the database grows.

---

# Database Format

Every station is stored in `database/stations.json`.

Example:

```json
{
  "id": "air_tirupati",
  "name": "AIR Tirupati",
  "language": "Telugu",
  "country": "India",
  "categories": [
    "AIR",
    "News"
  ],
  "logo": "https://example.com/logo.png",
  "streams": [
    {
      "url": "https://example.com/playlist.m3u8",
      "codec": "HLS",
      "priority": 1
    }
  ]
}
```

---

# Build Playlists

Generate all playlists:

```bash
python build/generate_playlists.py
```

Validate the database:

```bash
python build/validate_database.py
```

Generate statistics:

```bash
python build/generate_stats.py
```

---

# Website and PWA

OpenRadio-IN includes an installable Progressive Web App (PWA) in `website/`. It provides a mobile-friendly directory and player for the station database.

## Features

- Browse every available radio station
- Filter stations by language or category, including AIR and FM
- Search station names, languages, locations, and categories
- Play a stream directly in the browser
- Save favourite stations on the current device
- Resume the last selected station
- Install the app on supported desktop and mobile browsers
- Cache the application shell and station directory for offline browsing after the first successful visit
- Cast a selected stream to Google Chromecast devices

## Using the app

1. Open the GitHub Pages URL in a browser.
2. Search for a station or select a category filter.
3. Select **Play** on a station; the bottom player bar shows the active station and lets you play or pause it.
4. To install the PWA, select **Install app** when the browser offers it. On browsers that do not show this button, use the browser's **Add to Home Screen** or install option.
5. To cast, select the Cast icon, choose a Chromecast device on the same network, then select a station. The player bar controls the remote stream while casting.

Some radio providers do not permit Chromecast playback or do not expose the CORS headers required by Chromecast. Those streams can still be available in the browser but may not cast successfully.

## GitHub Pages deployment


The workflow copies `database/stations.json` into the published site, so station updates are included automatically in every deployment. Commit and push changes to `website/` or `database/stations.json` to publish an updated PWA.

---

# Contributing

Contributions are welcome!

You can help by:

- Adding new radio stations
- Updating broken streams
- Improving metadata
- Reporting issues
- Improving documentation

Please ensure every submitted stream is publicly accessible and legal to redistribute.

---

# Roadmap

## Version 0.5

- Searchable website
- Embedded web player
- Stream validation
- Statistics generation

## Version 0.6

- Tamil stations
- Kannada stations
- Malayalam stations
- Hindi stations

## Version 1.0

- 500+ verified stations
- Automatic GitHub releases
- Stream health monitoring
- Public JSON API
- GitHub Pages directory

---

# License

MIT License

---

# Acknowledgements

Thanks to:

- All India Radio (Akashvani)
- Public internet radio broadcasters
- Open source contributors
- Everyone who helps keep the station database up to date.

---

## ⭐ Support the Project

If you find OpenRadio-IN useful:

⭐ Star this repository

🐛 Report broken streams

📻 Submit new stations

🤝 Contribute improvements

Every contribution helps make OpenRadio-IN a better resource for everyone.
