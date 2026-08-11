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
- Audio equalizer (bass/treble)
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
│   ├── index.html
│   ├── assets/js/app.js
│   ├── assets/css/style.css
│   ├── hls-proxy-worker.js
│   ├── cast-receiver.html
│   ├── manifest.webmanifest
│   ├── sw.js
│   └── icons/
│
├── docs/
│
└── .github/
```

---

## Available Playlists

| Playlist | Description |
|----------|-------------|
| all.m3u | Every station |
| air.m3u | All India Radio (AIR/Akashvani) stations |
| fm.m3u | FM stations |
| telugu.m3u | Telugu stations |

Language playlists (e.g. `hindi.m3u`, `tamil, english.m3u`, `marathi, hindi, english.m3u`) are generated for every distinct language combination in the database.

---

## Database Format

Every station is stored in `database/stations.json`.

```json
{
  "id": "air_tirupati",
  "name": "AIR Tirupati",
  "language": "Telugu",
  "country": "India",
  "categories": ["AIR", "News"],
  "epg_id": 411,
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
- Audio equalizer (bass/treble shelving filters)
- Station sharing via Web Share API
- Keyboard shortcuts: Space = play/pause, Arrow keys = prev/next
- Recently played stations tracking

### EPG (Program Schedule)

AIR stations with an `epg_id` show the current ("Now Playing") and next ("Up Next") scheduled program in the now-playing view. The app fetches the official Akashvani cuesheets from `cuesheets.prasarbharati.org` directly in the browser (the site sends `Access-Control-Allow-Origin: *`), falling back to the HLS proxy worker's `?epg=<id>` endpoint when the direct fetch is unreachable. The schedule is cached per station per day and refreshes every minute.

### HLS Proxy (for Chromecast)

AIR stations use HLS streams from `radio.wavespb.com`, which blocks Google Cast devices. The proxy worker at `website/hls-proxy-worker.js` routes these streams through Cloudflare Workers so they play correctly on Cast devices. It also exposes an `?epg=<stationId>` route that returns a parsed, cached copy of the Akashvani cuesheet (`{ date, programs: [...] }`) used as a fallback when the browser cannot reach the cuesheet site directly.

Deploy with:
```bash
cd website
npx wrangler deploy
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
- Audio equalizer (bass/treble)
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
