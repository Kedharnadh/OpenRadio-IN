# OpenRadio-IN

> A community-driven collection of 294 Indian online radio stations across 34 languages with a progressive web app, native Android app, Chromecast support, and automatically generated playlists for VLC, Kodi, Home Assistant, and other compatible players.

PWA: https://kedharnadh.github.io/OpenRadio-IN/

---

[![GitHub License](https://img.shields.io/github/license/Kedharnadh/OpenRadio-IN)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/Kedharnadh/OpenRadio-IN)](https://github.com/Kedharnadh/OpenRadio-IN/stargazers)
[![GitHub Issues](https://img.shields.io/github/issues/Kedharnadh/OpenRadio-IN)](https://github.com/Kedharnadh/OpenRadio-IN/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/Kedharnadh/OpenRadio-IN)](https://github.com/Kedharnadh/OpenRadio-IN/pulls)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/Kedharnadh/OpenRadio-IN)](https://github.com/Kedharnadh/OpenRadio-IN/commits/main)

[![Android CI](https://github.com/Kedharnadh/OpenRadio-IN/actions/workflows/android.yml/badge.svg)](https://github.com/Kedharnadh/OpenRadio-IN/actions/workflows/android.yml)
[![Deploy PWA](https://github.com/Kedharnadh/OpenRadio-IN/actions/workflows/deploy-pages.yml/badge.svg)](https://github.com/Kedharnadh/OpenRadio-IN/actions/workflows/deploy-pages.yml)

[![Stations](https://img.shields.io/badge/stations-294-blue)](database/stations.json)
[![Languages](https://img.shields.io/badge/languages-34-green)](database/languages.json)
[![Playlists](https://img.shields.io/badge/playlists-37-orange)](playlists/)
[![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-3DDC84?logo=android)](android/)

---

## Star History

If you find OpenRadio-IN useful, please give it a star! It helps others discover the project.

[![Star History Chart](https://api.star-history.com/svg?repos=Kedharnadh/OpenRadio-IN&type=Date)](https://star-history.com/#Kedharnadh/OpenRadio-IN&Date)

---

## Features

- 294 radio stations across 34 Indian languages (Telugu, Tamil, Hindi, Kannada, Malayalam, Bengali, Gujarati, Marathi, Punjabi, and more)
- AIR (Akashvani), FM, News, Devotional, Classical, Community, and Internet Radio
- Progressive Web App (installable, offline-capable)
- Native Android app (Kotlin + Jetpack Compose)
- Google Cast / Chromecast support
- HLS proxy worker for AIR streams on Cast devices
- Search, filter by language/category, and favorites
- Recently played stations
- Now-playing view with station logo, album art, and track metadata
- Album art auto-fetch from iTunes and Deezer with multi-term fallback search
- Program schedule (EPG) with Now Playing / Up Next for AIR stations
- Volume control with mute toggle
- Sleep timer (15/30/60 min)
- Alarm timer
- Station sharing (Web Share API)
- Dark/light theme toggle
- Keyboard shortcuts (Space = play/pause, arrows = prev/next)
- 37 automatically generated playlists
- JSON-based station database
- Python build system with stream health checks
- GitHub Actions CI/CD

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
│   ├── stations.json         # 294 stations, 34 languages
│   ├── categories.json
│   ├── languages.json
│   ├── states.json
│   └── schema.json
│
├── build/                    # Python build / validation / health-check scripts
│   ├── generate_playlists.py # Generates per-language, per-category, and all.m3u
│   ├── health_check.py       # Concurrent stream probe (online/offline/unknown)
│   ├── generate_stats.py     # Generates output/statistics.json
│   ├── check_streams.py
│   ├── validate_database.py
│   └── ...
│
├── scripts/                  # Legacy playlist generation entry points
│   ├── generate_playlist.py
│   └── validate_playlist.py
│
├── playlists/                # 37 generated playlists (all.m3u, air.m3u, 34 languages, fm.m3u)
│
├── stations/                 # Source station files (e.g. stations/telugu)
│
├── imports/                  # Imported playlists
├── output/                   # Generated artifacts (statistics.json, ...)
├── config/                   # Build configuration (settings.json)
│
├── android/                  # Native Android app (Kotlin + Jetpack Compose)
│   ├── app/src/main/java/dev/openradio/android/
│   │   ├── App.kt            # Application init
│   │   ├── Prefs.kt          # SharedPreferences (favorites, recents, volume)
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   ├── PlayerViewModel.kt
│   │   │   ├── screens/HomeScreen.kt
│   │   │   ├── screens/NowPlayingSheet.kt
│   │   │   └── theme/Theme.kt
│   │   ├── data/
│   │   │   ├── Station.kt
│   │   │   ├── StationsRepository.kt
│   │   │   └── MetadataRepository.kt
│   │   ├── playback/
│   │   │   ├── AppPlayer.kt         # ExoPlayer + CastPlayer singleton
│   │   │   ├── PlaybackService.kt   # MediaLibraryService (Android Auto)
│   │   │   └── CastOptionsProvider.kt
│   │   └── alarm/AlarmReceiver.kt
│   ├── app/src/main/res/            # Launcher icons, themes, Android Auto XML
│   ├── build.gradle.kts
│   └── docs/ANDROID_AUTO.md
│
├── hls-proxy-server/         # Local HLS proxy (Express + ffmpeg) for development
│   ├── server.js
│   └── package.json
│
├── website/                  # PWA served on GitHub Pages
│   ├── index.html
│   ├── assets/js/app.js
│   ├── assets/css/style.css
│   ├── data/stations.json
│   ├── data/search-index.json
│   ├── hls-proxy-worker.js   # Cloudflare Worker (HLS + EPG + metadata proxy)
│   ├── cast-receiver.html    # Google Cast receiver (CAF v3)
│   ├── cast-utils.js
│   ├── cast-utils.test.js
│   ├── manifest.webmanifest
│   ├── sw.js                 # Service worker (cache-first, v30)
│   └── icons/                # SVG + PNG icons (transparent backgrounds, maskable)
│
├── docs/                     # Documentation
├── wrangler.toml             # Cloudflare Workers config for the HLS proxy
└── .github/                  # GitHub Actions workflows (deploy-pages.yml)
```

---

## Available Playlists

37 playlists are generated from the station database:

| Playlist | Description |
|----------|-------------|
| `all.m3u` | Every station (294) |
| `air.m3u` | All India Radio (AIR/Akashvani) stations |
| `fm.m3u` | FM stations |
| `telugu.m3u` | Telugu stations |
| `tamil.m3u` | Tamil stations |
| `hindi.m3u` | Hindi stations |
| `kannada.m3u` | Kannada stations |
| `malayalam.m3u` | Malayalam stations |
| `bengali.m3u` | Bengali stations |
| `marathi.m3u` | Marathi stations |
| `gujarati.m3u` | Gujarati stations |
| `punjabi.m3u` | Punjabi stations |
| ... | + 25 more language playlists |

Language playlists are generated for every language in the database. A station with multiple languages is added to each of its language playlists separately.

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
    },
    {
      "url": "http://backup.example.com/stream",
      "codec": "MP3",
      "priority": 2
    }
  ],
  "metadata_url": "https://example.com/api/nowplaying",
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
| `name_te` / `name_hi` | Localized names (Telugu, Hindi, ...) |
| `language` | Broadcast language(s) |
| `country` / `state` / `city` | Location |
| `categories` | Category tags (e.g. AIR, FM, News, Devotional) |
| `genre` | Genre tags |
| `epg_id` | Prasar Bharati cuesheet ID — enables the AIR program schedule |
| `logo` | Station logo URL |
| `streams` | Ordered list of stream URLs with codec and priority (1 = primary) |
| `metadata_url` | Now-playing metadata endpoint (AzuraCast/Icecast status JSON) |
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

Check stream health (probes all streams concurrently):
```bash
python build/health_check.py
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
- Alarm timer with station picker
- Station sharing via Web Share API
- Keyboard shortcuts: Space = play/pause, Arrow keys = prev/next
- Recently played stations tracking
- Album art auto-fetch from iTunes and Deezer
- Multi-term fallback artwork search (track, artist, station name)
- Pre-built search index for instant filtering

### Now Playing View

The now-playing overlay shows station art, title, language, codec, and live track metadata. It includes playback controls, volume slider, share/timer/alarm/favorite actions, and EPG schedule for AIR stations.

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
| `?url=<stream>&meta=1&metaUrl=<url>` | Reads metadata from a status endpoint (AzuraCast/Icecast) |
| `?url=<stream>&relay=1` | Raw HTTP relay for non-HLS streams (mixed-content fix) |
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

## Android App

A native Android app built with Kotlin and Jetpack Compose (Material 3 / Material You dynamic colors).

### Features

- Full PWA feature set: search, language filter, favorites, recents
- Now-playing bottom sheet with album art and track metadata
- Sleep timer, alarm, and station sharing
- Chromecast support via CastPlayer (ExoPlayer local + Cast transfer)
- Android Auto / Android Automotive (MediaLibraryService browses full station list)
- Live station data fetched from GitHub Pages, cached for offline use
- EPG schedules for AIR stations

### Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| Playback | Media3 ExoPlayer 1.10.1 |
| HLS | Media3 HLS extension |
| Casting | Google Cast SDK 21.5.0 |
| Images | Coil 2.7.0 |
| Networking | OkHttp 4.12.0 |
| Build | Gradle KTS, compileSdk 36, minSdk 26 |

### Build

```bash
cd android
./gradlew assembleRelease
```

Requires a `keystore/release.jks` signing key and `keystore.properties`.

See `android/docs/ANDROID_AUTO.md` for Android Auto architecture details.

---

## Contributing

Contributions are welcome! You can help by:

- Adding new radio stations
- Updating broken streams
- Improving metadata
- Reporting issues
- Improving documentation

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide, including code style and PR process.

---

## Community

### Spread the Word

- Star this repository to help others discover it
- Share on social media with **#OpenRadioIN**
- Write about it on your blog or dev community
- Submit it to [awesome lists](https://github.com/sindresorhus/awesome) (awesome-open-source, awesome-android, etc.)

### Get Involved

- **Add a station** — Know of an Indian radio station not listed? [Open an issue](https://github.com/Kedharnadh/OpenRadio-IN/issues/new?template=add-station.md) with the stream details
- **Report bugs** — Found something broken? [File an issue](https://github.com/Kedharnadh/OpenRadio-IN/issues/new)
- **Submit a PR** — Want to fix a bug or add a feature? Check the [Contributing guide](CONTRIBUTING.md)
- **Test on different devices** — Help test the Android app on various devices and Android versions

### Platform Links

- **PWA**: [kedharnadh.github.io/OpenRadio-IN](https://kedharnadh.github.io/OpenRadio-IN/)
- **GitHub**: [Kedharnadh/OpenRadio-IN](https://github.com/Kedharnadh/OpenRadio-IN)
- **Issues**: [Report a bug](https://github.com/Kedharnadh/OpenRadio-IN/issues)
- **Discussions**: [Share feedback](https://github.com/Kedharnadh/OpenRadio-IN/discussions)

---

## Version History

### v1.2.0
- Fixed album art spilling over to next station when switching (art state reset, image cache cleared immediately)
- Moved first-time player hint ("Tap the player bar below to open Now Playing") to left side
- Improved album art lookup for AAC stations with multi-term fallback search (track-only, station name)
- Increased metadata fetch timeout for slower stream servers
- Regenerated PWA icons with transparent backgrounds and added `maskable` purpose to manifest
- Added backup Priority 2 stream for Telugu NRI Radio (Zeno.fm)

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

1. **Star** this repository
2. **Share** it with friends who listen to Indian radio
3. **Report** broken streams so we can fix them
4. **Submit** new stations to expand the collection
5. **Contribute** code, docs, or feedback

Every contribution helps make OpenRadio-IN a better resource for everyone.
