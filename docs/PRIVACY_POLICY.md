# Privacy Policy — OpenRadio-IN

Last updated: 11 September 2026

This policy applies to the OpenRadio-IN Android app, the PWA at
https://kedharnadh.github.io/OpenRadio-IN/, and its supporting services.

## 1. Overview

OpenRadio-IN is a free radio-streaming app. It does **not** create accounts, collect names,
emails, or payment details, and it does **not** sell or share personal data. It only plays
internet radio stations.

## 2. Station data

Station lists are fetched from a public GitHub repo
(<https://github.com/Kedharnadh/OpenRadio-IN>) and cached on-device for offline use. No personal
data is sent with these requests beyond normal connection metadata.

## 3. Analytics & crash reporting

- **Android app**: Google Analytics for Firebase and Firebase Crashlytics collect aggregated
  diagnostics (crash logs, device type, OS version, analytics device IDs). Managed by Google under
  <https://policies.google.com/privacy>. Analytics can be disabled in the app's settings.
- **Website (PWA)**: Google Analytics (property G-48120XZZQ4) may set cookies to measure anonymous
  usage.

## 4. Audio proxy

Some streams are relayed through an HLS proxy on Cloudflare Workers
(`openradio-hls-proxy.kedharnadh1.workers.dev`). The proxy only forwards audio and keeps no logs.

## 5. Third-party services

Google Play Services (Cast), Cloudflare (hosting/proxy), and GitHub Pages (station data). Each is
subject to its own privacy policy.

## 6. Permissions

Internet, foreground/media playback service, and notifications — only what's needed to stream and
show playback controls.

## 7. Retention & deletion

Local data (favourites, volume, alarm) stays on your device; clear the app's data or uninstall to
remove it. Analytics aggregates are kept per the retention of the analytics service (up to 14
months) and can be disabled.

## 8. Children

Not directed at children under 13; no knowing collection of children's personal data.

## 9. Changes & contact

Updates are posted on this page. Contact via GitHub issues:
<https://github.com/Kedharnadh/OpenRadio-IN/issues>.