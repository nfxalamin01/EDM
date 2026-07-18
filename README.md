# EDM — Extreme Download Manager (Android)

**Tagline:** Fast. Smart. Reliable.

A native Android download manager built with **Kotlin + Jetpack Compose (Material 3)**,
**Room**, **OkHttp**, and a **foreground Service** engine for background,
multi-threaded, segmented downloads.

## How to open this project

1. Install **Android Studio (Koala or newer)**.
2. `File > Open` → select the `EDM/` folder (this project root).
3. Android Studio will generate the Gradle wrapper jar automatically on first
   sync (or run `gradle wrapper` once if you have Gradle installed locally —
   the wrapper *properties* file is already included, pointing at Gradle 8.7).
4. Let Gradle sync and download dependencies (Compose, Room, OkHttp, CameraX,
   ML Kit barcode scanning, WorkManager, security-crypto, Vico charts).
5. Run on a device/emulator with **minSdk 24 / targetSdk 34**.

## What's fully implemented

- **Segmented multi-thread download engine** (`engine/DownloadEngine.kt`,
  `SegmentDownloader.kt`) — splits a file into N byte-range chunks when the
  server returns `Accept-Ranges: bytes`, downloads them concurrently into the
  correct file offsets via `RandomAccessFile`, and falls back to a single
  stream otherwise.
- **Built-in URL analyzer** (`RangeSupportChecker.kt`) — HEAD/ranged-GET probe
  for file size, MIME type, filename, and Range support before download starts.
- Pause / resume / restart / stop, **smart retry with exponential backoff**,
  auto-reconnect on transient failures.
- **Foreground `DownloadService`** for background downloads with a live
  progress notification; respects Max-simultaneous-downloads via a semaphore
  and Wi-Fi-only mode via `NetworkUtils`.
- **Room database** (`data/db`) with full schema for downloads + per-segment
  progress, and DAOs backing Active / Paused / Completed / Failed / Favorites
  / Queue / Search / History screens.
- **Automatic filename fixing + folder organization** by category
  (`FileUtils.kt`) into `EDM/Videos`, `EDM/Documents`, `EDM/APKs`, etc.,
  duplicate-file detection, and checksum verification (`ChecksumUtil.kt`,
  MD5/SHA-256 auto-detected by length).
- **Clipboard link detection** and a **CameraX + ML Kit QR scanner** screen.
- **Encrypted settings storage** (`SettingsStore.kt`) via
  `EncryptedSharedPreferences`/Jetpack Security for thread count, speed limit,
  proxy, DNS, Wi-Fi-only, auto-resume, theme, language, etc.
- Full Compose UI: **Splash, Home (dashboard), Downloads, Queue, History,
  Favorites, Statistics, File Manager, Settings, About**, Material 3
  light/dark theme with dynamic color on Android 12+, bottom navigation.
- Boot receiver to auto-resume interrupted downloads.
- FileProvider wiring for sharing/opening completed files.

## What's scaffolded / needs finishing for production

These are flagged in code comments as extension points rather than fully
wired, since they depend on choices (charting library visuals, SAF folder
picker UX, exact proxy transport) best made interactively in Android Studio:

- **SD card / custom folder picker** — hook `SettingsScreen`'s download-folder
  option to Android's Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`)
  and persist the resulting URI permission.
- **Proxy / custom DNS enforcement** — values are captured and encrypted in
  `SettingsStore`, but `DownloadEngine`'s `OkHttpClient` should be rebuilt
  per-download with a `Proxy` object and a custom `Dns` implementation reading
  those values.
- **Download scheduling** — `scheduledTimeMillis` exists on the entity;
  wire it to `WorkManager`'s `OneTimeWorkRequest` with `setInitialDelay`.
- **Statistics charts** — the Vico chart library is already a Gradle
  dependency; `StatisticsScreen` currently shows numeric stat cards, swap in
  a `Chart` composable for weekly/monthly trend lines.
- **Battery-optimization button** — wire to
  `Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)`.
- App icon / launcher assets, and localized string resources for the
  language switcher (English scaffold only).

## Architecture

```
data/db          Room entities, DAO, database (clean, testable persistence layer)
data/repository  Single source of truth consumed by ViewModels
engine/          Pure download logic: analyzer, segment downloader, orchestrator
service/         Foreground Service + boot receiver (Android background execution)
util/            Files, checksums, notifications, clipboard, network, settings
viewmodel/       DownloadViewModel, SettingsViewModel (state for Compose UI)
ui/theme         Material 3 color scheme, typography, light/dark
ui/components    Reusable DownloadItemCard, StatCard
ui/screens       One file per screen listed in the spec
ui/navigation    Route definitions
```

This separation means the engine has no Android/Compose dependency and can be
unit-tested directly; the service is a thin Android wrapper; and the UI only
talks to ViewModels/Repository — never to the engine directly.
