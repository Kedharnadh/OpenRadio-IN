# Contributing to OpenRadio-IN

Thank you for your interest in contributing to OpenRadio-IN! Every contribution helps make Indian online radio more accessible to everyone.

## Table of Contents

- [Getting Started](#getting-started)
- [Ways to Contribute](#ways-to-contribute)
- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Pull Request Process](#pull-request-process)
- [Adding a New Station](#adding-a-new-station)
- [Reporting Issues](#reporting-issues)

---

## Getting Started

1. **Fork** the repository on GitHub
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/<your-username>/OpenRadio-IN.git
   cd OpenRadio-IN
   ```
3. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. Make your changes, commit, and push
5. Open a **Pull Request** against `main`

---

## Ways to Contribute

| Contribution | Difficulty | Description |
|---|---|---|
| Add stations | Easy | Add new Indian radio stations to the database |
| Fix streams | Easy | Update broken stream URLs for existing stations |
| Improve metadata | Easy | Add logos, EPG IDs, or localized station names |
| Fix bugs | Medium | Fix issues in the PWA, Android app, or build scripts |
| Add features | Medium | Implement new functionality |
| Write tests | Medium | Add unit or instrumented tests |
| Improve docs | Easy | Fix typos, add examples, improve clarity |

---

## Development Setup

### Prerequisites

- Python 3.8+ (for build scripts)
- Node.js 20+ (for PWA / Wrangler)
- Android Studio (for Android app)
- JDK 17 (for Android builds)

### PWA / Website

```bash
# Install dependencies
npm install

# Run ESLint
npm run lint

# Fix lint issues
npm run lint:fix

# Format code
npm run format

# Run tests
npm test
```

### Android App

```bash
cd android

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint checks
./gradlew ktlintCheck

# Fix lint issues
./gradlew ktlintFormat
```

### Build Scripts

```bash
# Generate playlists
python build/generate_playlists.py

# Validate database
python build/validate_database.py

# Check stream health
python build/health_check.py
```

---

## Code Style

### Kotlin (Android)

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **ktlint** with official code style (configured via `.editorconfig`)
- Max line length: 120 characters
- Use trailing commas in multiline parameter lists
- Use `data class` for simple data holders
- Prefer `object` (singletons) over companion objects for utility classes
- Coroutines for all async work; no raw `Thread` usage

```kotlin
// Good
data class Station(
    val id: String,
    val name: String,
    val streams: List<Stream>,
)

// Good — use early returns over nesting
suspend fun refresh(): List<Station> = withContext(Dispatchers.IO) {
    val text = fetch(url) ?: return@withContext loadCache()
    saveCache(text)
    StationParser.parse(text)
}
```

### JavaScript (PWA / Workers)

- Use **ESLint** + **Prettier** (configured in `eslint.config.js` and `.prettierrc`)
- Use `const` / `let` — never `var`
- Use single quotes
- Use semicolons
- Prefer `===` over `==`
- ES modules (`import` / `export`)

```javascript
// Good
const station = stations.find((s) => s.id === id);
if (station) {
  playStream(station.streams[0].url);
}
```

### Python (Build Scripts)

- Follow [PEP 8](https://peps.python.org/pep-0008/)
- Use type hints where practical
- Use `pathlib.Path` over `os.path`

### General

- Keep commits focused — one logical change per commit
- Write clear commit messages: `<type>: <description>`
  - Examples: `feat: add Tamil NRI station`, `fix: resolve HLS proxy timeout`
- Add tests for new functionality when possible
- Do not commit secrets, API keys, or signing credentials

---

## Pull Request Process

1. **Ensure CI passes** — lint checks, build, and tests must all pass
2. **Write a clear description** of what changed and why
3. **Reference related issues** (e.g., "Fixes #42")
4. **Keep PRs small** — break large changes into reviewable chunks
5. **Request review** from maintainers if the change is significant

### PR Checklist

- [ ] Code compiles / builds without errors
- [ ] Lint checks pass (`npm run lint` / `./gradlew ktlintCheck`)
- [ ] Tests pass (`npm test` / `./gradlew test`)
- [ ] No hardcoded secrets or credentials
- [ ] New stations are added to `database/stations.json`
- [ ] Commit messages follow the convention

---

## Adding a New Station

1. Open `database/stations.json`
2. Add a new entry following the existing format:

```json
{
  "id": "unique_station_id",
  "name": "Station Name",
  "language": "Telugu",
  "country": "India",
  "state": "Andhra Pradesh",
  "city": "City Name",
  "categories": ["FM"],
  "genre": [],
  "homepage": "https://example.com",
  "logo": "https://example.com/logo.png",
  "streams": [
    {
      "url": "https://example.com/stream.m3u8",
      "codec": "HLS",
      "priority": 1
    }
  ],
  "verified": false,
  "status": "unknown"
}
```

3. Ensure the stream URL is publicly accessible and legal to redistribute
4. Regenerate playlists: `python build/generate_playlists.py`

---

## Reporting Issues

When reporting bugs, please include:

- **Device / OS** (e.g., Pixel 8, Android 15)
- **App version** (from Settings or Play Store)
- **Steps to reproduce** the issue
- **Expected vs actual behavior**
- Screenshots if applicable

For broken streams, include the station name and stream URL.

---

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
