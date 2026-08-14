# OpenRadio-IN

> A community-driven collection of verified Indian online radio stations with a progressive web app, Chromecast support, and automatically generated playlists for VLC, Kodi, Home Assistant, and other compatible players.

PWA: https://kedharnadh.github.io/OpenRadio-IN/

![GitHub](https://img.shields.io/github/license/Kedharnadh/OpenRadio-IN)
![GitHub stars](https://img.shields.io/github/stars/Kedharnadh/OpenRadio-IN)
![GitHub issues](https://img.shields.io/github/issues/Kedharnadh/OpenRadio-IN)
![GitHub last commit](https://img.shields.io/github/last-commit/Kedharnadh/OpenRadio-IN)

---

## Features

- Indian Radio Stations (AIR, FM, News, Devotional, Classical, Internet Radio)
- Progressive Web App (installable, offline-capable)
- Google Cast / Chromecast support
- HLS proxy worker for AIR streams on Cast devices
- Search, filter, and favorites
- Recently played stations
- Now-playing view with station logo and metadata
- Program schedule (EPG) with Now Playing / Up Next for AIR stations
- Volume control with mute toggle
- Sleep timer
- Station sharing (Web Share API)
- Dark/light theme toggle
- Keyboard shortcuts (Space = play/pause, arrows = prev/next)
- Automatically generated playlists
- JSON-based station database
- Python build system
- GitHub Actions automation

---

## Supported Players

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

## Repository Structure

```
OpenRadio-IN
│
├── database/                 # Station database + metadata
│   ├── stations.json
│   ├── categories.json
│   ├── languages.json
│   ├── states.json
│   └── schema.json
│
├── build/                    # Python build / validation / health-check scripts
│   ├── generate_playlists.py
│   ├── validate_database.py
│   ├── check_streams.py
│   ├── health_check.py
│   ├── generate_stats.py
│   └── ...
│
├── scripts/                  # Legacy playlist generation entry points
│   ├── generate_playlist.py
│   └── validate_playlist.py
│
├── playlists/                # Generated playlists (all.m3u, air.m3u, language files, ...)
│
├── stations/                 # Source station files (e.g. stations/telugu)
│
├── imports/                  # Imported playlists
├── output/                   # Generated artifacts
├── config/                   # Build configuration
│
├── hls-proxy-server/         # Local HLS proxy (Express + ffmpeg) for development
│
├── website/                  # PWA served on GitHub Pages
│   ├── index.html
│   ├── assets/js/app.js
│   ├── assets/css/style.css
│   ├── data/stations.json
│   ├── hls-proxy-worker.js   # Cloudflare Worker (HLS + EPG + metadata proxy)
│   ├── cast-receiver.html
│   ├── cast-utils.js
│   ├── cast-utils.test.js
│   ├── manifest.webmanifest
│   ├── sw.js
│   └── icons/
│
├── docs/
├── wrangler.toml             # Cloudflare Workers config for the HLS proxy
└── .github/                  # GitHub Actions workflows
```

---

## Available Playlists

| Playlist | Description |
|----------|-------------|
| all.m3u | Every station |
| air.m3u | All India Radio (AIR/Akashvani) stations |
| fm.m3u | FM stations |
| telugu.m3u | Telugu stations |

Language playlists (e.g. `hindi.m3u`, `tamil.m3u`, `marathi.m3u`) are generated for every language in the database. A station with multiple languages (e.g. "Assamese, Hindi, English") is added to each of its language playlists separately.

---

## Database Format

Every station is stored in `database/stations.json` and published to the site as `website/data/stations.json`.

```json
{
  "id": "air_tirupati",
  "name": "AIR Tirupati",
  "language": "Telugu",
  "country": "India",
  "state": "Andhra Pradesh",
  "city": "Tirupati",
  "categories": ["AIR", "News"],
  "genre": [],
  "homepage": "https://prasarbharati.gov.in/",
  "epg_id": 411,
  "logo": "https://example.com/logo.png",
  "streams": [
    {
      "url": "https://example.com/playlist.m3u8",
      "codec": "HLS",
      "priority": 1
    }
  ],
  "verified": true,
  "status": "online",
  "last_checked": "2026-08-11T13:38:29+00:00"
}
```

Station fields:

| Field | Description |
|-------|-------------|
| `id` | Unique station identifier |
| `name` | Display name |
| `language` | Broadcast language(s) |
| `country` / `state` / `city` | Location |
| `categories` | Category tags (e.g. AIR, FM, News, Devotional) |
| `epg_id` | Prasar Bharati cuesheet ID — enables the AIR program schedule |
| `logo` | Station logo URL |
| `streams` | Ordered list of stream URLs with codec and priority |
| `verified` | Whether the stream was manually verified |
| `status` | Last health-check result (`online` / `offline` / `unknown`) |
| `last_checked` | Timestamp of the last stream health check |

---

## Build Playlists

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

## Website and PWA

The repository includes an installable, offline-capable radio directory in `website/`. It lists every station, filters by language or category, and supports instant search and browser playback.

The GitHub Actions workflow at `.github/workflows/deploy-pages.yml` deploys it to GitHub Pages whenever `main` is pushed. In the repository settings, set **Pages -> Build and deployment -> Source** to **GitHub Actions**.

The workflow copies `database/stations.json` into the published site, so station updates are included automatically in every deployment.

### PWA Features

- Installable (manifest + service worker)
- Offline app shell and station list caching
- Google Cast / Chromecast support for all stations
- HLS proxy worker for AIR streams on Cast devices
- Dark and light themes
- Volume control with mute toggle
- Sleep timer (15/30/60 min)
- Station sharing via Web Share API
- Keyboard shortcuts: Space = play/pause, Arrow keys = prev/next
- Recently played stations tracking

### EPG (Program Schedule)

AIR stations with an `epg_id` show the current ("Now Playing") and next ("Up Next") scheduled program in the now-playing view. The app fetches the official Akashvani cuesheets from `cuesheets.prasarbharati.org` directly in the browser (the site sends `Access-Control-Allow-Origin: *`), falling back to the HLS proxy worker's `?epg=<id>` endpoint when the direct fetch is unreachable. The schedule is cached per station per day and refreshes every minute.

### HLS Proxy (for Chromecast)

AIR stations use HLS streams from `radio.wavespb.com`, which blocks Google Cast devices. The proxy worker at `website/hls-proxy-worker.js` routes these streams through Cloudflare Workers so they play correctly on Cast devices. The worker also powers the app's now-playing metadata and EPG fallback.

Worker endpoints:

| Endpoint | Purpose |
|----------|---------|
| `?url=<hls_url>` | Streams a (possibly multi-variant) HLS playlist as audio |
| `?url=<hls_url>&probe=1` | Resolves the manifest and reports the media URL + content type |
| `?url=<stream>&meta=1` | Reads ICY/stream metadata for the now-playing track |
| `?epg=<epgId>` | Parses and caches an Akashvani cuesheet (`{ date, programs: [...] }`) |

**Deploy** — the `wrangler.toml` lives at the repository root, so run from there:

```bash
npx wrangler deploy
```

**Local development** — for a local HLS-to-MP3 proxy, use the bundled Express server:

```bash
cd hls-proxy-server
npm install
npm start
```

---

## Contributing

Contributions are welcome! You can help by:

- Adding new radio stations
- Updating broken streams
- Improving metadata
- Reporting issues
- Improving documentation

Please ensure every submitted stream is publicly accessible and legal to redistribute.

---

## Version History

### v1.1.0
- EPG program schedule for AIR stations: Now Playing / Up Next in the now-playing view
- Fetches Akashvani cuesheets directly in the browser, with the HLS proxy worker as fallback
- Per-station `epg_id` in the station database (14 AIR stations)

### v1.0.0
- Chromecast support with HLS proxy worker
- Now-playing view with station artwork
- Volume control, mute toggle
- Dark/light theme toggle
- Sleep timer
- Station sharing via Web Share API
- Keyboard shortcuts
- Recently played stations
- PNG icons for PWA install
- Offline station data caching via service worker

### v0.6
- Tamil, Kannada, Malayalam, Hindi stations

### v0.5
- Searchable website
- Embedded web player
- Stream validation
- Statistics generation

---

## License

MIT License

---

## Acknowledgements

Thanks to:
- All India Radio (Akashvani)
- Public internet radio broadcasters
- Open source contributors
- Everyone who helps keep the station database up to date

---

## Support the Project

If you find OpenRadio-IN useful:
- Star this repository
- Report broken streams
- Submit new stations
- Contribute improvements

Every contribution helps make OpenRadio-IN a better resource for everyone.
