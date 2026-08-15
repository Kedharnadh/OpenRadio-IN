# OpenRadio-IN Android app

A native Android app for [OpenRadio-IN](https://github.com/Kedharnadh/OpenRadio-IN) built
with Kotlin + Jetpack Compose (Material 3 / Material You), Media3 ExoPlayer, Chromecast
and Android Auto support.

## Features

- **Material You** — dynamic wallpaper-based color scheme on Android 12+, default light/dark otherwise.
- **Full PWA feature set** — search, language filter, favorites, recents, sleep timer, alarm, share, EPG schedules.
- **Chromecast** — one `CastPlayer` that plays locally via ExoPlayer and transfers to a Cast receiver automatically.
- **Android Auto / Android Automotive** — the `PlaybackService` is a `MediaLibraryService`, so Auto shows the full station list and exposes search + next/previous.
- **Live station data from git** — the station database is fetched from the OpenRadio-IN repo's GitHub Pages `stations.json`, so stations added to the repo appear automatically after the next app refresh. A cached copy is kept for offline start.

## Project layout

```
android/
├── gradle/wrapper/            # Gradle 8.11.1 wrapper
├── app/
│   ├── build.gradle.kts       # deps, buildConfig URLs, min/target SDK
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/               # strings, themes, adaptive icon, automotive_app_desc.xml
│       └── java/in/openradio/android/
│           ├── App.kt             # Application: init player, prefs, station load
│           ├── Prefs.kt           # favorites / recents / volume
│           ├── alarm/             # alarm receiver + scheduler
│           ├── data/              # Station model+parser, git-backed repository, metadata/EPG
│           ├── playback/          # CastOptionsProvider, AppPlayer, PlaybackService
│           └── ui/                # MainActivity, ViewModel, Material3 theme, screens
```

## Building

Requirements: JDK 17, Android Studio with Android SDK 35.

```bash
cd android
./gradlew assembleDebug
```

(On Windows use `gradlew.bat assembleDebug`.)

There is no `local.properties` checked in; let Android Studio generate it, or create one with
`sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.

Then install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architecture notes

- `AppPlayer` (in `playback/`) is a process-wide singleton that owns:
  - an `ExoPlayer` for local playback,
  - a `CastPlayer.Builder(context).setLocalPlayer(exoPlayer)` so Chromecast is just a transfer,
  - one `MediaLibrarySession` shared by the app UI, the media notification and Android Auto.
  - If Google Play services (Cast) is missing at startup it falls back to the bare `ExoPlayer`.
- `PlaybackService` (`MediaLibraryService`) returns that session. Auto browses stations through
  `MediaLibrarySession.Callback`: `onGetLibraryRoot` → `onGetChildren` (the station list),
  `onSearch`, and `onAddMediaItems` (returns the whole queue so next/previous walks the list).
- Station items are `MediaItem`s with a `LiveConfiguration` (live radio), station name/logo
  metadata, and the HLS-first stream (matching the PWA's stream preference).
- Now-playing track + artwork are polled from the HLS proxy worker endpoint every 15 s and pushed
  into the session via `Player.setMediaItemMetadata`, so the notification and Auto see the live track.
- Favorites/recents are stored in SharedPreferences; volume is restored at startup.

## Testing Android Auto

1. Install on a phone and launch the app once (the service also works without launching).
2. Android Auto on your phone's screen (AA App / developer mode) or an Android Automotive
   head unit emulator will list **OpenRadio-IN** under Media.
3. Browse stations → tap one to play; the playback shows in the media notification and lock screen.

If Auto doesn't show the app, check that:
- the service is exported and its intent-filter action is `androidx.media3.session.MediaLibraryService`,
- the manifest declares `com.google.android.gms.car.application` → `automotive_app_desc.xml` with `<uses name="media"/>`.
- Rebuild and reinstall, then force-stop and restart the Auto screen / emulator.

## Testing Chromecast

Needs a real Cast-enabled device (Chromecast / Android TV / smart speaker) on the same network
as the phone. In the now-playing sheet tap the cast icon to connect; playback transfers to the
receiver and the on-device volume slider is disabled while casting.

## Play Store review notes

- Declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `POST_NOTIFICATIONS`.
- The service `foregroundServiceType="mediaPlayback"` — fine for media apps under the Google
  Play media service policy.
- Android Auto review checklist (if distributing): provide a test video, ensure playback works
  headless (service + session), handle playback errors gracefully (fallback stream is automatic),
  and test on the Android Auto emulator.
