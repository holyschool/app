# Edupage2

Unofficial EduPage Android client built with Jetpack Compose and Material 3
Expressive. Timetable, grades, messages, meals, homework, notifications,
home-screen widgets, and a "prepare for tomorrow" swipe deck.

This repo contains the Android app (`app/`) and the EduPage API library
(`edupage-lib/`). The optional push-notification backend lives in a separate
private repo.

## Build

Requires JDK 17 and the Android SDK.

```bash
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. CI builds it automatically
on every push (see Actions).

## Project layout

- `app/` — the Android application
- `edupage-lib/` — pure-Kotlin EduPage API client used by the app
- `gradle/` — version catalog and wrapper

## Updates

The app checks GitHub releases of this repo (Settings → Data & about →
Check for updates) and can download new APKs directly.

## License

Private source-available project by the author. EduPage is a trademark of
its respective owner; this is an unofficial client.
