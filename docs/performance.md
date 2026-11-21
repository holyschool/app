# Startup and frame performance

The goal is a cold start that reaches interactive content as close to 200 ms as the hardware
allows, and a UI that never drops a frame while doing it. This file records what was measured,
what was changed, and how to check it again.

Realistic expectation first: on a mid-range 2026 phone a Compose app with a network stack and
Hilt lands around **300–500 ms** cold-start TTID; a Pixel-class device gets to **150–250 ms**.
Only part of that is app code — process fork, class loading/verification and system_server work
are the OS's share. Everything here targets the app's share.

## How to measure

Always measure a **release** build. Debug builds are 2–5× slower because R8 is off and the
runtime is debuggable, so a debug number says nothing about the shipped app.

```bash
# Build a release build signed with the debug key (same R8/shrinking/profile setup, so the
# numbers are real, but it installs over an existing debug install and keeps the login).
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:assembleBenchmark

# Install and measure a cold start. -W prints TotalTime (first frame) and WaitTime (idle time).
adb install -r app/build/outputs/apk/benchmark/app-benchmark.apk
adb shell am force-stop com.wiffles.edupage
adb shell am start -W -n com.wiffles.edupage/.MainActivity

# The system's own view of the first frame
adb logcat -c && adb logcat -s ActivityTaskManager:I | grep Displayed
```

`MainScreen` calls `Activity.reportFullyDrawn()` once the cached shell is on screen, so
`WaitTime` reflects the point where the app is actually usable rather than the theme window.

For frame health, use JankStats or a Perfetto trace (`Trace.beginSection("...")`) and look at
frame durations on a scroll through the timetable and the messages list.

`StartupTrace` (a no-op unless `BuildConfig.STARTUP_TRACE` is on, i.e. debug and benchmark) writes
one line per startup phase from the moment the process was forked:

```bash
adb logcat -c && adb shell am force-stop com.wiffles.edupage
adb shell am start -n com.wiffles.edupage/.MainActivity
adb logcat -s EduStartup:I
```

To see where the first frame itself goes, take a system trace during the launch:

```bash
adb shell "atrace -t 4 -b 40000 sched freq idle am wm view gfx res" > trace.txt
```

## What this branch changed

| Change | Why |
| --- | --- |
| Pager composes **only the visible tab** | All five tabs used to compose on the first frame — five ViewModels, five cache reads, five screens of layout. Pre-composing a neighbour was also measured: it cost more than it saved and forced a second composition right after the first frame |
| The overview is a `LazyColumn`, not a scrollable `Column` | The column measured and laid out all ten cards before the first frame. The lazy list builds only what is on screen (measured: 110 ms → ~20 ms of measure) |
| `ShellViewModel` for the three shell preferences | The shell used to build `SettingsViewModel` (17 dependencies) just to read the meals/AI/default-tab flags. They are now read from `AppPreferences` by a ViewModel with one dependency; settings builds its own when it opens |
| Splash screen no longer held until composition starts | The hold hid the real first-frame cost and delayed cached content pointlessly |
| `DataWarmer` / `SessionRepository` injected as `dagger.Lazy` | Building the nine-dependency warm-up graph and the session check was main-thread work before the first frame |
| `LockStore` flags read from a plain preferences file | The encrypted store touches the Android keystore; doing that in the first composition cost 20–150 ms on first launch. PIN hash and salt stay encrypted |
| Settings recreate listener moved from `onCreate` into composition | `SettingsViewModel` (and its graph) was created on every launch just to listen for a rare event |
| WorkManager initializer removed from the manifest and initialized from a background coroutine | Its `ContentProvider` built the whole worker graph during process start |
| `AppPreferences.preload()` on the IO scope | The first `getSharedPreferences` parses an XML file; the theme reads a preference in the first composition |
| Window background set per theme (`values`/`values-night`) | Removes the light flash before the first Compose frame in dark mode |
| Baseline profile in `app/src/main/baseline-prof.txt` | AOT-compiles the startup path on install (see below) |
| `android.r8.optimizedResourceShrinking=true` | Smaller release APK, faster install and dex load |
| Compose BOM `2025.09.01` | Compose 1.9 / material3 1.4 include runtime and measurement improvements |

## Where the remaining time goes

On a Nothing Phone (2a), API 37, best of ten cold starts with the benchmark build:

| Phase | Time |
| --- | --- |
| process fork → `Application.onCreate` | ~30 ms |
| `Application.onCreate` → `Activity.onCreate` | ~45 ms |
| `Activity.onCreate` (splash, edge-to-edge, lazy session warm-up) → `setContent` | ~15 ms |
| first composition (shell + overview) | ~35 ms |
| first frame: measure / layout / draw | ~100 / ~10 / ~15 ms |
| first frame: render thread (first-draw shaders, texture uploads) | ~40 ms |
| `am start -W` TotalTime | **330–420 ms** |

The first-frame measure and draw are the largest remaining app-owned cost. They fall with fewer
nodes on screen (the lazy overview) and with a real baseline profile; the rest is class
loading/verification and the GPU's first-frame pipeline, which is the OS/driver's share.

## Baseline profiles

`app/src/main/baseline-prof.txt` is a **hand-seeded** profile covering the app's own startup
classes: `EdupageApp`, `MainActivity`, the Hilt component, the theme, `MainScreen`, `PastelIcon`
and the caches. `androidx.profileinstaller` (already pulled in transitively) installs it at first
run. Compose and AndroidX ship their own profiles inside their AARs, so only the
app's classes needed seeding.

It is a start, not the end state: a profile generated from a real trace is both more accurate and
covers scrolling (`LazyColumn` prefetch, item composition). To generate one, add a
`:baselineprofile` module:

```kotlin
// baselineprofile/build.gradle.kts
plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    id("androidx.baselineprofile")
}

android {
    targetProjectPath = ":app"
    // run on an API 33+ emulator/device
}

dependencies {
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.4")
}
```

```kotlin
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = "com.wiffles.edupage") {
        pressHome()
        startActivityAndWait()
        device.findObject(By.res("timetable_tab"))?.click()
        device.waitForIdle()
    }
}
```

Then run `./gradlew :app:generateReleaseBaselineProfile` on a device and replace
`app/src/main/baseline-prof.txt`. Add `androidx.benchmark.macro.junit4` +
`StartupTimingMetric` in the same module to track TTID/TTFD over time.

## Further work

- A Macrobenchmark `StartupTimingMetric` gate in CI (needs a device/emulator runner).
- Move the remaining screen-level cache reads behind a `preload()` on the IO scope so a tab
  switch never touches disk on the main thread.
- Audit unstable parameters (`List`, lambdas in data classes) on the timetable rows; strong
  skipping handles most of it, `@Immutable` where a list is genuinely read-only.
- Bundle main-thread `StrictMode` in debug builds to catch new disk/network reads on the first
  frame.