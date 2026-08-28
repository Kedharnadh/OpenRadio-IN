#!/usr/bin/env python3
"""Sync Android string resources from the PWA's i18n dictionary (app.js).

The PWA (website/assets/js/app.js) is the canonical source of UI translations.
This script reads its `I18N` object and regenerates the mapped <string> entries in
the Android res/values*(strings.xml) files, so both apps stay in sync without a
runtime network dependency.

- Only keys listed in MAPPING are copied. Android-only resources (app_name, ui_language,
  lang_en/te/hi, and any resource name not in MAPPING) are preserved unchanged.
- Run from anywhere:  python scripts/sync_android_strings.py
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
APP_JS = ROOT / "website" / "assets" / "js" / "app.js"
RES = ROOT / "android" / "app" / "src" / "main" / "res"

# PWA i18n key -> Android <string name>. Languages map to res folders.
LOCALE_DIRS = {
    "en": "values",
    "te": "values-te",
    "hi": "values-hi",
}

# PWA key          -> Android resource name
MAPPING = {
    "app.tagline": "app_tagline",
    "search.placeholder": "search_hint",
    "section.recent": "recently_played",
    "section.favorites": "favorite_stations",
    "section.all": "all_stations",
    "filter.all": "all_categories",
    "filter.allLanguages": "all_languages",
    "controls.play": "play",
    "controls.resume": "resume",
    "controls.pause": "pause",
    "controls.stop": "stop",
    "controls.previous": "previous",
    "controls.next": "next",
    "controls.volume": "mute",
    "controls.collapse": "collapse",
    "np.title": "now_playing",
    "np.share": "share",
    "np.timer.title": "sleep_timer",
    "alarm.timeLabel": "alarm_time_label",
    "alarm.stationLabel": "alarm_station_label",
    "alarm.set": "alarm_set_button",
    "alarm.off": "alarm_off_button",
    "verified": "verified",
    "community": "community",
    "status.online": "online",
    "status.offline": "offline",
    "status.unknown": "status_unknown",
    "status.tryingBackup": "status_trying_backup",
    "status.noStream": "status_no_stream",
}

HEADER = (
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
    "<resources>\n"
    "    <!-- NOTE: Mapped strings are auto-generated from website/assets/js/app.js (I18N)\n"
    "         by scripts/sync_android_strings.py. Edit them there, then re-run the script. -->\n"
)


def unescape_js(s: str) -> str:
    def repl(m):
        if m.group(1):
            return chr(int(m.group(1), 16))
        esc = m.group(2)
        return {"\\": "\\", "'": "'", '"': '"', "n": "\n", "t": "\t", "r": "\r"}.get(esc, esc)

    return re.sub(r"\\u([0-9a-fA-F]{4})|\\(.)", repl, s)


def parse_i18n(js: str):
    start = js.index("const I18N")
    i = js.index("{", start)
    depth = 0
    end = None
    for j in range(i, len(js)):
        if js[j] == "{":
            depth += 1
        elif js[j] == "}":
            depth -= 1
            if depth == 0:
                end = j
                break
    if end is None:
        raise SystemExit("Could not find I18N block in app.js")

    body = js[i + 1:end]
    locales = {}
    for lang in LOCALE_DIRS:
        m = re.search(r"(?:^|\n)\s*" + lang + r"\s*:\s*\{", body)
        if not m:
            continue
        open_i = body.index("{", m.start())
        d = 0
        close_i = None
        for k in range(open_i, len(body)):
            if body[k] == "{":
                d += 1
            elif body[k] == "}":
                d -= 1
                if d == 0:
                    close_i = k
                    break
        block = body[open_i + 1:close_i]
        pairs = re.findall(r"'((?:[^'\\]|\\.)*)'\s*:\s*'((?:[^'\\]|\\.)*)'", block)
        entries = {unescape_js(k): unescape_js(v) for k, v in pairs}
        locales[lang] = entries
    return locales


def read_android_strings(xml_path):
    """Return {name: raw_value} for every <string> in the file."""
    if not xml_path.exists():
        return {}
    text = xml_path.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r"<string\s+name=\"([^\"]+)\">(.*?)</string>", text, re.DOTALL):
        out[m.group(1)] = m.group(2)
    return out


def xml_value(s: str) -> str:
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    s = s.replace("'", "\\'")
    return s


def main():
    locales = parse_i18n(APP_JS.read_text(encoding="utf-8"))
    changed = []
    for lang, _dir in LOCALE_DIRS.items():
        if lang not in locales:
            print(f"  !! {lang} not found in I18N, skipping")
            continue
        res_dir = RES / _dir
        res_dir.mkdir(parents=True, exist_ok=True)
        path = res_dir / "strings.xml"
        current = read_android_strings(path)

        entries = dict(current)
        diffs = 0
        for pwa_key, android_name in MAPPING.items():
            if pwa_key in locales[lang]:
                new_val = xml_value(locales[lang][pwa_key])
                if entries.get(android_name) != new_val:
                    entries[android_name] = new_val
                    diffs += 1
            else:
                # PWA lacks the key in this language -> fall back to English text so we
                # never ship a missing translation.
                if pwa_key in locales["en"]:
                    fb = xml_value(locales["en"][pwa_key])
                    if entries.get(android_name) != fb:
                        entries[android_name] = fb
                        diffs += 1

        lines = [HEADER]
        for name in sorted(entries):
            lines.append(f'    <string name="{name}">{entries[name]}</string>')
        lines.append("</resources>\n")
        path.write_text("\n".join(lines), encoding="utf-8")
        if diffs:
            changed.append(f"{_dir}/{path.name} ({diffs} updated)")
    if changed:
        print("Updated:")
        for c in changed:
            print("  -", c)
    else:
        print("All translation files are up to date.")


if __name__ == "__main__":
    main()
