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

## Current status: Pass 2 — Meditation Engine + Morning Ritual

Pass 1 delivered the foundation (see git history). Pass 2 turns the scaffolding
into a trustworthy meditation engine: real monotonic qualification, screen-off
support, conservative recovery, and a fully dogfoodable Morning Meditation.

### Implemented in Pass 2

- **`runtime.MeditationRuntimeController`** — the meditation engine. Owns
  qualified-time accounting, interval lifecycle, checkpoints, conservative
  recovery, and translation of Android lifecycle/device events into Pass 1
  domain services. Exposes state via `StateFlow` and one-shot events.
- **Derived timing** — the UI never mutates the ledger:
  `qualifiedMs = closedQualifiedIntervals.sumOf { durationMs } + currentOpenIntervalDelta`.
  The timer only renders this calculation; persistence happens on interval
  close and on a 12s checkpoint cadence, never per frame or per second.
- **Qualification rules** — time counts only while `status == ACTIVE` AND a
  qualified interval is open AND the monotonic clock is valid AND the session
  is neither expired nor completed. Standard requirement: 30 minutes =
  **1,800 qualified seconds**. No points, no credits, no cooldown reduction.
- **Screen-off keeps qualifying** (`runtime.ScreenStateReader` →
  `PowerManager.isInteractive`): putting the phone down counts.
- **Interactive backgrounding pauses** (app leaves foreground while the screen
  is interactive): interval closes, session pauses, `APP_BACKGROUND` recorded.
- **Configuration changes never pause** — pause decisions use
  `ProcessLifecycleOwner`, not `Activity.onStop()`.
- **Pause/Resume** — manual pause closes the interval immediately; resume opens
  a fresh monotonic interval; paused time is never counted.
- **Process-death recovery** — persisted qualified-time checkpoints every ~12s
  (plus on lifecycle transitions). On uncertain recovery: credit only the
  checkpointed safe point, close the unsafe open tail, mark `PAUSED`, record
  `PROCESS_RESTORE`, require Resume. Undercounting seconds is acceptable;
  inventing minutes is not.
- **Reboot detection** — `elapsedRealtime` regression below the persisted
  baseline (primary, permission-free) plus `Settings.Global.BOOT_COUNT`
  (secondary). Reboot gaps are never credited and never estimated from wall
  clock.
- **Atomic, idempotent completion** — completed at the exact crossing of the
  requirement, credited total capped at the requirement, `completedAtEpochMs`
  persisted, completion event emitted exactly once.
- **Morning Meditation (real)** — Today shows real
  REQUIRED / IN-PROGRESS / PAUSED / COMPLETE state per local day
  (`MorningSessionPolicy`), Begin Session is idempotent per session id, no
  Skip exists, cancelled attempts never transfer progress.
- **Active Session UX** — real timer (`12:48 / 30:00` direction from the
  mockups), Pause/Resume, End-early calm confirmation
  ("End this session? Your current meditation requirement will remain
  incomplete."), breathing orb, optional `MeditationMode` framing
  (STILLNESS / BREATH / BODY / IMAGINATION — identical qualification).
- **Completion UX** — real recorded minutes, kind-aware calm copy. Never
  reward/unlock language.
- **Session history** — local terminal-session ledger with session kind, date,
  qualified practice time, and interruption counts; the Sessions tab can also
  begin a standalone 30-minute practice.
- **Optional local cues** — synthesized start/complete bell + haptics, gated by
  DataStore preferences, never part of qualification. No network audio.
- **IPC hardened and usable** — `RecoveryRequestHandler`
  (arrive → validate → verify caller → create/load idempotently → route into
  session UI), caller verification on the status provider path, deny-by-default
  `CallerVerifier` unchanged (no debug bypasses; tests inject fake verifiers).
- **Checkpoint persistence** — `session_time_checkpoints` table (schema v2).

### Active-session foreground service

A narrowly scoped foreground service runs only while a meditation session is
ACTIVE. Physical QA found it necessary for long screen-off sessions; it carries
no timing logic or wake lock. The runtime's monotonic intervals and persisted
checkpoints remain the source of qualified time.

### Companion-app ownership and integration

- **Rhythmic Routine owns policy and enforcement.** Meditation launches Routine
  from its Today screen for cooldowns/restorative choices; it does not duplicate
  those choices or invent cooldown state locally.
- Routine can start an exact, session-bound Meditation recovery request. The
  request is validated, same-signer checked, persisted idempotently, and routed
  into the session UI; Routine reads the matching session status projection.
- Meditation consumes Routine's evening signal and attention-insight projection
  plus Reader's Daily Evidence V2 projection. These optional Insights reads are
  read-only and fail open; they never affect Meditation timing or Routine policy.
- Shared debug signing is opt-in for local cross-app QA builds. The keystore is
  supplied by a local Gradle property and is not stored in this repository.

### Remaining scope

- Local data-management controls.
- Completion of the physical cross-app qualification matrix; the current handoff
  records which Routine-triggered rows still need an unlocked-device run.

---

## Stack

| Layer | Choice |
| --- | --- |
| Platform | Android (minSdk 26, targetSdk 36, compileSdk 37) |
| Language | Kotlin (AGP 9 built-in Kotlin — see below) |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| Persistence | Room 2.8.5 (KSP, schema v3), DataStore Preferences 1.1.7 |
| Concurrency | Coroutines + Flow |
| Runtime signals | ProcessLifecycleOwner (`lifecycle-process`), PowerManager, Settings.Global.BOOT_COUNT |
| Testing | JUnit 4, kotlinx-coroutines-test, AndroidX Test + Compose UI tests |

No cloud, no backend, no auth, no analytics SDKs. The app has **no INTERNET
permission** and no wake locks.

---

## Build & test

Requirements: JDK 17, Android SDK (platform 37, build-tools 36.x),
`ANDROID_HOME` set (or `local.properties` with `sdk.dir`).

```bash
# Build debug APK
./gradlew assembleDebug

# Optional: share the QA debug signer with Routine/Reader for local IPC tests
./gradlew -PrhythmicSharedDebugKeystore="C:/path/to/shared-debug.keystore" assembleDebug

# JVM unit tests (engine, domain, protocol, timing, session semantics)
./gradlew test

# Instrumentation tests (Room roundtrip, recovery intents, Compose flows;
# requires a connected device/emulator)
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
- The shared signer property is optional. If the keystore uses nonstandard
  credentials, supply `rhythmicSharedDebugStorePassword`,
  `rhythmicSharedDebugKeyAlias`, and `rhythmicSharedDebugKeyPassword` as local
  Gradle properties or environment-backed project properties.
- Room schema is **v3** (v2 added `session_time_checkpoints`, v3 adds
  `evening_meditation`); destructive fallback is configured until real
  migrations are needed.

---

## Package structure

```
com.terinit.rhythmicmeditation
├── app                  Application (lifecycle/screen wiring), MainActivity
│                        (recovery-intent entry point)
├── ui
│   ├── theme            Color/Type/Shape/Theme tokens
│   ├── navigation       AppRoute, NavHost shell, bottom bar
│   ├── screens          today | session | completion | evening | restorative
│   │                    | insights | settings   (+ ViewModel per area)
│   └── components       CalmCard, buttons, InfoBanner, SoftProgressBar, brand
├── runtime              MeditationRuntimeController (the engine), runtime state
│                        + events, EveningMeditationController (evening
│                        wind-down), ScreenStateReader, BootIdentityReader
├── domain
│   ├── model            MeditationSession, MeditationInterval,
│   │                    SessionInterruptionEvent, MeditationInsightSnapshot,
│   │                    MeditationMode
│   ├── session          MeditationSessionStateMachine, MeditationSessionService
│   │                    (accrual, exact-crossing completion, conservative
│   │                    restore), MorningSessionPolicy
│   ├── evening          EveningMeditation (evening wind-down state machine —
│   │                    optional, deferable, non-punitive)
│   ├── insights         MeditationInsights (truthful local aggregation,
│   │                    date-key mapping, correlation threshold)
│   ├── timing           TimeProvider, SystemTimeProvider, ElapsedTimeCalculator
│   └── protocol         MeditationProtocol, recovery request/status DTOs,
│                        request validator, malformed-payload field parser
├── data
│   ├── local/db         MeditationDatabase (v3)
│   ├── local/dao        6 DAOs (incl. SessionTimeCheckpointDao,
│   │                    EveningMeditationDao)
│   ├── local/entity     6 Room entities (incl. SessionTimeCheckpointEntity,
│   │                    EveningMeditationEntity)
│   ├── local/prefs      AppPreferences + AppPreferencesStore (DataStore)
│   ├── repository       interfaces + Room implementations (incl. checkpoints,
│   │                    evening records)
│   ├── mapper           entity<->domain, domain<->contract mapping
│   └── AppContainer     manual dependency container
├── integration
│   ├── contract         MeditationStatusRepository, CallerVerifier,
│   │                    RecoveryRequestHandler, CallerIdentity,
│   │                    SystemCallerIdentityResolver, Local status repo,
│   │                    Bundle codec, RoutineEveningSignal (evening trigger),
│   │                    AttentionInsightClient + ReadingInsightClient
│   │                    (read-only, fail-open insight projections)
│   ├── intent           MeditationIntents (recovery intent build/parse)
│   └── provider         MeditationStatusProvider (verified, read-only)
└── util                 TimeFormat, Ids, SessionCues (local bells/haptics)
```

Tests mirror the source tree under `app/src/test` (with `testutil` fakes) and
`app/src/androidTest` (Room roundtrip, recovery intents, Compose flows).

---

## Trust model (why the engine looks like this)

- **Qualified time is evidence, not a counter.** It is derived from monotonic
  intervals (`elapsedRealtime`); wall clock is diagnostics only and is never
  used for qualification — enforced by tests including clock-jump cases.
- **Putting the phone down counts; using the phone doesn't.** Screen-off
  continues to qualify (the open interval accrues on resume); interactive use
  of another app closes the interval and pauses.
- **Failure modes can only lose time.** Process death → checkpointed time only.
  Reboot → no gap credit. Cancellation → history but never transferable credit.
  Completion is capped at the requirement.
- **IPC is deny-by-default.** `CallerVerifier` rejects unconfigured peers and
  unattested callers; there are no `BuildConfig.DEBUG` bypasses. Tests inject
  `FakeCallerTrustPolicy`.
- **No policy here.** Cooldowns, requirements, and enforcement stay in Routine.

## Design notes

- The mockups' photographic hero art is deliberately not bundled; screens use
  soft gradient backdrops (`CalmScreenBackground`). Art can be added later
  without structural change.
- `material-icons-extended` is used for UI glyphs; it makes the debug APK large
  (~20 MB) but release minification (when enabled) strips unused icons.
- Dark theme is not defined (light-only art direction).
- Domain logic lives in `domain/` + `runtime/`, never inside Composables.

---

## Physical-device QA checklist (Pass 2)

Instrumentation cannot faithfully prove process lifecycle edge cases. Verify on
real hardware before Pass 3 relies on the engine:

1. Start a session, press power (screen off) 10 min, unlock → elapsed reflects
   the gap and the session is still ACTIVE.
2. Start a session, switch to another app (screen on) → session PAUSED within a
   second; returning requires Resume.
3. Start a session, background with screen off, force-stop the app from
   recents, relaunch → PAUSED with "Session restored", only checkpointed time
   credited (≤ 12s undercount).
4. Start a session, reboot the device, relaunch → PAUSED, reboot notice, no gap
   credited.
5. Rotate during a session → session stays ACTIVE (no pause, no interruption).
6. Receive a phone call mid-session → conservative pause (by design in Pass 2).
7. Complete 30 real minutes → Completion screen with exactly 30 minutes.
8. If the process is killed too aggressively during screen-off meditation →
   implement the scoped foreground service described above.

---

## Routine integration & restorative sessions

**Recovery entry point**

- Package/component: `com.terinit.rhythmicmeditation.app.MainActivity`
  (singleTask; handles the intent in `onCreate`/`onNewIntent`) →
  `integration.contract.RecoveryRequestHandler` → routes to
  `AppRoute.ActiveSession` (or `AppRoute.Completion` when already completed)
- Intent action: `com.terinit.rhythmicmeditation.action.START_MEDITATION_RECOVERY`
- Extra: `extra_request_payload` → Bundle of `MeditationRecoveryRequest`
  (`session_id`, `protocol_version`, `session_kind`, `required_qualified_seconds`,
  `created_at_epoch_ms`, `expires_at_epoch_ms?`, `source_cooldown_id?`,
  `source_risk_group_id?`, `source_rhythmic_day_id?`) — encoded/decoded via
  `integration.contract.MeditationContractCodec` / pure
  `domain.protocol.MeditationContractFields`
- Validation: `MeditationRecoveryRequestValidator` (blank ids, non-positive
  seconds/versions, newer protocol versions, unknown/STANDALONE kinds, expiry
  ordering all rejected). Malformed payloads never create sessions.

**Status surface**

- Signature permission: `com.terinit.rhythmicmeditation.permission.STATUS_ACCESS`
  (protectionLevel `signature`) + `CallerVerifier` on top. The configured
  same-signer trust policy accepts Routine's production and QA package variants;
  unknown callers and mismatched certificates are rejected.
- ContentProvider authority: `com.terinit.rhythmicmeditation.status`, read-only
  query (session id via `selectionArgs[0]` or the URI path segment), columns:
  `sessionId`, `protocolVersion`, `status`, `requiredQualifiedSeconds`,
  `completedQualifiedSeconds`, `completedAtEpochMs`, `lastUpdatedAtEpochMs`
- Protocol version: **1** (`MeditationProtocol.PROTOCOL_VERSION`)

**Session semantics**

- Status enum: `PENDING, ACTIVE, PAUSED, COMPLETED, CANCELLED, EXPIRED, INVALID`
  (transitions in `MeditationSessionStateMachine`)
- Kind enum: `MORNING_REQUIRED, COOLDOWN_RESTORATIVE, EVENING_WIND_DOWN,
  STANDALONE` (`STANDALONE` is local-only and cannot arrive over the protocol)
- Morning vs cooldown-restorative: identical qualification (1,800s). Morning is
  locally keyed (`morning-<yyyy-MM-dd>` ids, `MorningSessionPolicy`). Routine
  sends its Attention Day id and uses `COOLDOWN_RESTORATIVE` plus
  `sourceCooldownId` for gate-bound sessions. Evidence exposed to Routine:
  `MeditationRecoveryStatus` above. Cancelled sessions keep history but never
  transfer credit.

**Runtime behavior summary for Routine's gate logic**

- Screen-off: keeps qualifying (open interval accrues across screen-off).
- Interactive backgrounding: closes interval + `PAUSED` + `APP_BACKGROUND`.
- Process death: only the last ~12s checkpoint survives; `PAUSED` +
  `PROCESS_RESTORE`; Resume required.
- Reboot: elapsed-regression + boot-count detection; no gap credit; `PAUSED`.
- Completion: fires at the exact crossing, total capped at
  `requiredQualifiedSeconds`, idempotent, `completedAtEpochMs` is wall clock
  (display only).

**Current checks:** **177 JVM tests, 0 failures**; debug APK, Android-test APK,
and lint pass. On the Redmi, 6 recovery-intent contract tests and 3 companion
integration tests passed under the shared QA signer (launcher visibility plus
the two read-only provider queries). The instrumented cross-app checks
query the actual installed providers using Meditation's target context.

**Physical-device results:** screen-off qualification, process-death recovery,
provider security, and prior Routine-bound completion runs are recorded in
[`docs/PASS_04_HANDOFF.md`](docs/PASS_04_HANDOFF.md). The remaining fresh
Routine-triggered gate acceptance rows still require an unlocked-device run.

**Known limitations**

- `lastUpdatedAtEpochMs` is derived from known session timestamps (no separate
  update column).
- Local data-management controls are not yet exposed in Settings.
- Calls/Essential Access are indistinguishable from plain interactive
  backgrounding.
- Morning requirements use Meditation's local date key; Routine-bound recovery
  sessions retain the Attention Day id supplied by Routine.
