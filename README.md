# Rhythmic Meditation

Native Android companion app for the Rhythmic ecosystem. Rhythmic Meditation is
an **inward-attention provider**: it records meditation evidence and nothing
else. **Rhythmic Routine remains the policy authority** for cooldowns,
requirements, and device enforcement.

Core principles:

- Meditation does **not** buy screen time.
- Meditation does **not** shorten cooldowns.
- Routine owns policy; Meditation owns meditation evidence only.
- All core flows are local-first and offline-capable (no network permission).
- Calm, minimal, non-gamified.
- No overlay / accessibility / device-control logic lives in this app.

---

## Current status: Pass 1 — Native Foundation + App Skeleton + Shared Recovery Contract

Pass 1 delivers the structural backbone only. It intentionally does **not**
implement final qualifying-session logic, full ritual flows, or full Routine
integration.

### Implemented in Pass 1

- Native Android project (Kotlin + Jetpack Compose), single `app` module
- Calm design system: `ui/theme` (tokens: MeditationGreen, MistBlue, WarmCream,
  SlateText, SoftDivider), serif display + sans body typography, soft shapes
- Navigation shell (`ui/navigation`): bottom nav (Today / Sessions / Insights /
  Settings) + full-screen routes (Active Session, Completion, Evening,
  Restorative Choice)
- Screen shells with placeholder content (`ui/screens/*`) matching the mockups:
  Today, Sessions, Active Session, Completion, Cooldown Restorative Choice,
  Evening Wind-Down, Insights, Settings
- Room persistence: 4 entities, 4 DAOs, database (`data/local`)
- DataStore preferences (`data/local/prefs`) — small settings only; the session
  ledger lives in Room
- Domain models: `MeditationSession`, `MeditationInterval`,
  `SessionInterruptionEvent`, `MeditationInsightSnapshot` (+ enums)
- Session state machine (`MeditationSessionStateMachine`) and session lifecycle
  service (`MeditationSessionService`) with create-or-replace / idempotent
  recovery semantics and interval-based qualified-time accumulation
- Monotonic timing abstraction (`domain/timing/TimeProvider` +
  `SystemTimeProvider` + `ElapsedTimeCalculator`)
- Protocol model + recovery contract DTOs (`domain/protocol`), validation for
  malformed payloads (`MeditationRecoveryRequestValidator`,
  `MeditationContractFields`)
- Integration scaffolding (`integration/*`): Bundle codec, intent parser,
  deny-by-default caller verification, `MeditationStatusRepository` interface +
  local implementation, exported `ContentProvider` shell
- Repositories: session, interval, interruption events, insights
  (`data/repository`, Room-backed)
- Per-screen ViewModels backed by repository flows (`ui/screens/*/…ViewModel`)
- Unit tests for domain/state/validation/timing/session semantics; Room
  roundtrip instrumentation tests
- Manual DI container (`data/AppContainer`) — no DI framework

### Not implemented (reserved for later passes)

- Real qualifying meditation-time ticking and accumulation UI
- Full active-session lifecycle (screen-off, calls, essential-access pauses,
  process restore)
- Morning ritual flow, evening flow completion logic
- Real requirement/cooldown data from Routine (all such UI is placeholder copy)
- Full IPC handling of recovery intents (activity side) and provider write
  semantics; peer pairing + signature digest configuration
- Insight generation/aggregation (snapshots are stored but not computed)
- Session history list, local data management (export / clear)
- Onboarding, profile

---

## Stack

| Layer | Choice |
| --- | --- |
| Platform | Android (minSdk 26, targetSdk 36, compileSdk 37) |
| Language | Kotlin (AGP 9 built-in Kotlin — see below) |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| Persistence | Room 2.8.5 (KSP), DataStore Preferences 1.1.7 |
| Concurrency | Coroutines + Flow |
| Testing | JUnit 4, kotlinx-coroutines-test, AndroidX Test (instrumentation) |
| Architecture | Lightweight layered: `ui` → `domain` ← `data`, `integration` for IPC |

No cloud, no backend, no auth, no analytics SDKs. The app has **no INTERNET
permission**.

---

## Build & test

Requirements: JDK 17, Android SDK (platform 37, build-tools 36.x),
`ANDROID_HOME` set (or `local.properties` with `sdk.dir`).

```bash
# Build debug APK
./gradlew assembleDebug

# JVM unit tests (domain, protocol, timing, session semantics)
./gradlew test

# Instrumentation tests (Room roundtrip; requires a connected device/emulator)
./gradlew connectedAndroidTest
```

Windows: use `gradlew.bat` instead of `./gradlew`.

### Build toolchain notes (read before touching build files)

- Gradle **9.3.1** (wrapper), Android Gradle Plugin **9.1.0**, Kotlin built-in
  support (AGP 9 default). The `kotlin-android` plugin is intentionally **not**
  applied. The Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`)
  **is** applied and must match the Kotlin version.
- `android.disallowKotlinSourceSets=false` in `gradle.properties` is required
  because KSP (Room) still registers generated sources through the
  `kotlin.sourceSets` DSL. AGP 9 documents this flag as the supported stopgap;
  revisit when upgrading AGP/KSP (AGP 10 may remove it).
- `compileSdk = 37` is required by Compose 1.12 / Lifecycle 2.11 (BOM
  2026.08.00). `targetSdk` stays 36 until the app opts into API 37 runtime
  behavior. `android.suppressUnsupportedCompileSdk` silences the AGP
  recommendation warning.
- Versions are pinned in `gradle/libs.versions.toml`.

---

## Package structure

```
com.terinit.rhythmicmeditation
├── app                  Application, MainActivity, (AppContainer lives in data)
├── ui
│   ├── theme            Color/Type/Shape/Theme tokens
│   ├── navigation       AppRoute, NavHost shell, bottom bar
│   ├── screens          today | session | completion | evening | restorative
│   │                    | insights | settings   (+ ViewModel per area)
│   └── components       CalmCard, buttons, InfoBanner, SoftProgressBar, brand
├── domain
│   ├── model            MeditationSession, MeditationInterval,
│   │                    SessionInterruptionEvent, MeditationInsightSnapshot
│   ├── session          MeditationSessionStateMachine, MeditationSessionService
│   ├── timing           TimeProvider, SystemTimeProvider, ElapsedTimeCalculator
│   └── protocol         MeditationProtocol, recovery request/status DTOs,
│                        request validator, malformed-payload field parser
├── data
│   ├── local/db         MeditationDatabase
│   ├── local/dao        4 DAOs
│   ├── local/entity     4 Room entities
│   ├── local/prefs      AppPreferences + AppPreferencesStore (DataStore)
│   ├── repository       interfaces + Room implementations
│   ├── mapper           entity<->domain, domain<->contract mapping
│   └── AppContainer     manual dependency container
├── integration
│   ├── contract         MeditationStatusRepository, CallerVerifier,
│   │                    LocalMeditationStatusRepository, Bundle codec
│   ├── intent           MeditationIntents (recovery intent build/parse)
│   └── provider         MeditationStatusProvider (ContentProvider shell)
└── util                 TimeFormat, Ids
```

Tests mirror the source tree under `app/src/test` (with `testutil` fakes) and
`app/src/androidTest` (Room roundtrip).

---

## Shared recovery contract (Routine ⇄ Meditation)

Defined in `domain/protocol` and `integration/*`:

- **Action**: `com.terinit.rhythmicmeditation.action.START_MEDITATION_RECOVERY`
- **Extra**: `extra_request_payload` (Bundle-encoded `MeditationRecoveryRequest`)
- **Status provider authority**: `com.terinit.rhythmicmeditation.status`
  (read-only; returns `MeditationRecoveryStatus` columns)
- **Protocol version**: 1 (`MeditationProtocol.PROTOCOL_VERSION`)
- **Permission**: `com.terinit.rhythmicmeditation.permission.STATUS_ACCESS`
  (signature-level) guards the provider; `CallerVerifier` additionally enforces
  deny-by-default package + signing-certificate checks. Peer identity/digests
  are configured when Routine pairing lands — until then all external callers
  are rejected.

Recovery semantics (`MeditationSessionService.createOrReplaceSession`):

- unknown id → new PENDING session
- identical request → no-op (idempotent; no extra writes)
- changed request → metadata replaced, recorded evidence (qualified seconds,
  counts) preserved; a live ACTIVE/PAUSED status survives

## Timing rules

All qualification math uses **elapsedRealtime (monotonic)** via `TimeProvider`.
Wall clock is stored for diagnostics only (`MeditationInterval.startedWallClockMs`
etc.) and is never used to compute qualified time — see
`ElapsedTimeCalculatorTest` for the tamper-resistance expectations.

## Design notes

- The mockups' photographic hero art is deliberately not bundled; screens use
  soft gradient backdrops (`CalmScreenBackground`). Art can be added later
  without structural change.
- `material-icons-extended` is used for UI glyphs; it makes the debug APK large
  (~20 MB) but release minification (when enabled) strips unused icons.
- Dark theme is not defined in Pass 1 (light-only art direction).
- Domain logic lives in `domain/`, never inside Composables. Composables receive
  state from ViewModels and emit callbacks.

## Handoff — Pass 2 targets

Build on top of (do not rework):

- `MeditationSessionService` (lifecycle + qualified-time accumulation),
  `MeditationSessionStateMachine`
- `MeditationIntervalRepository` / intervals as the recovery evidence trail
- `TimeProvider` / `ElapsedTimeCalculator`
- `integration/intent.MeditationIntents` + `LocalMeditationStatusRepository`
  for the IPC side of recovery
- `ActiveSessionScreen`/`ActiveSessionViewModel` for the real timer, pause /
  resume semantics, screen-off and interruption handling
- `TodayScreen` for the morning meditation flow
