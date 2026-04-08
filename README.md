# Android Video Player

[![Build APK](https://github.com/TITANICBHAI/android-video-player/actions/workflows/build-apk.yml/badge.svg)](https://github.com/TITANICBHAI/android-video-player/actions/workflows/build-apk.yml)

A YouTube-style local video player for Android built with Kotlin, Media3 ExoPlayer, and Material3.

## Features

### Video Library
- Scans device storage via MediaStore for all local videos
- Grid/list view with Glide-loaded thumbnails, title, duration, and size
- Browse by folder with breadcrumb navigation
- Search by filename, sort by name / date / size / duration (8 options)

### Player
- **ExoPlayer (Media3)** with a fully custom controls overlay
- **Double-tap** left/right to seek ±10 seconds with ripple animation
- **Swipe gestures** — right side controls volume, left side controls brightness
- **Playback speed** selector: 0.25x, 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x
- Auto-hiding controls with 3.5s inactivity timer
- Custom drawn seekbar with buffered progress indicator
- **Pinch to zoom** — fit / fill / crop display modes
- **Controls lock** button to prevent accidental touches

### Advanced
- **Picture-in-Picture (PiP)** with media button controls
- **Watch history** stored in Room database — resumes from last position
- Fullscreen rotation with edge-to-edge display (WindowInsetsController)
- Material3 dynamic dark / light theming

## Tech Stack

| Layer | Library |
|-------|---------|
| Player | Media3 ExoPlayer 1.5.1 |
| Database | Room 2.6.1 |
| Images | Glide 4.16.0 |
| UI | Material3 + ViewBinding |
| Async | Kotlin Coroutines + Flow |

## CI/CD — GitHub Actions

GitHub Actions builds APK variants on every push to `main`:

| Job | Artifact | Retention |
|-----|----------|-----------|
| Debug APK | `VidPlayer-debug-{sha}` | 30 days |
| Release APK (unsigned) | `VidPlayer-release-unsigned-{sha}` | 90 days |
| Lint report | `lint-report-{sha}` | 14 days |

Download the latest APK from the [Actions tab](https://github.com/TITANICBHAI/android-video-player/actions).

## Building Locally

```bash
# Clone
git clone https://github.com/TITANICBHAI/android-video-player.git
cd android-video-player

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

Requires **JDK 17** and Android SDK targeting **API 34**.

## Permissions

- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_VIDEO` — scan local video files
- `WRITE_SETTINGS` — adjust screen brightness during playback
