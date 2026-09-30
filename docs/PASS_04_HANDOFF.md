# SW-2026-004 — Pass 04 Handoff: Reader Alignment + Evening Wind-Down + Insights

Status: **Pass 04 is implemented and automated checks pass.** Physical validation
was executed on the Redmi Note 13 Pro+ 5G; the results and partial rows are
recorded in section 6. The narrow active-session foreground service is
**required and verified** by the 30-minute screen-off run. Routine and Reader
v1.2 in-place upgrade trials are now recorded; the full cross-app qualification
matrix remains incomplete. See
`Rhythmic-Routine/docs/releases/SW-2026-004-CLOSEOUT.md` for the current
ecosystem status.

---

## 1. Repositories

| Repo | Branch | Commit | Diff | Tests before → after |
| --- | --- | --- | --- | --- |
| **Rhythmic Reader** (primary) | `feat/pass-04-reader-alignment` | `bad67b5` | preview protocol v2 + UI migration + tests | **94 → 106** (0 failures) |
| **Rhythmic Meditation** (primary) | `feat/pass-04-evening-insights` | `a498a04` | Evening Wind-Down + Insights + Room v3 | **91 → 175** (0 failures) |
| **Rhythmic Routine** (secondary) | `feat/pass-03-restorative-gates` | `51b0818` | compatibility/projection only | JS 405 (unchanged) · native 41 → 49 |

Baselines: Routine `93d3a8d` (v1.2.0) + Pass 3 `9be8c29` · Reader `3515dcb`
(v1.2.0 master, baseline recorded BEFORE modifications as required) · Meditation
`aebd142` (P1) / `af4c894` (P2).

**Routine changes and why (spec 57):**
1. `RoutineAttentionPreviewProvider` → protocol **2**, additive columns
   (`requirementKind`, `gateStatus`, `selectedProvider`, `cooldownActive`,
   `restorativeReadingSeconds`, `restorativeQualifiedPages`) — Reader must be
   able to distinguish CD3 baseline from CD4+ restorative (spec 6). V1 columns
   and their meaning are untouched; versioned, not replaced destructively.
2. New `AttentionInsightProvider` (protocol 1) — the minimal read-only
   projection Meditation Insights needs (spec 28). No policy change.
3. `RestorativeEnforcement` — gate state now carries `selectedProvider` +
   `dailyCooldownOrdinal`; added the frozen policy mirror
   (`requirementKindForOrdinal`) used by the preview. No policy change.

---

## 2. Final contracts

### Reader preview protocol
- **Version 2** (with explicit **v1 fallback** and unknown-version = "no
  target", never a crash).
- Authority: `com.terinit.rhythmicroutine[.qa].attention-preview`, path
  `next/{dateKey}`, read-protected by `com.terinit.rhythmicreader.permission.RECOVERY`.
- V1 columns (unchanged): `protocolVersion, dateKey, nextCooldownOrdinal,
  requiredActiveSeconds, requiredQualifiedPages`.
- V2 adds: `requirementKind` (`none|baseline-reading|restorative-choice|
  legacy-reading`), `gateStatus` (`none|pending-selection|in-progress|satisfied`),
  `selectedProvider` (`reader|meditation|none`), `cooldownActive` (0/1),
  `restorativeReadingSeconds`/`restorativeQualifiedPages` (1800/11 for
  restorative-choice).
- Reader queries the V2 projection first, then the V1 projection (graceful
  degradation against a v1.2 Routine).

### Reader Daily Evidence contract
**Unchanged — V2.** `content://com.terinit.rhythmicreader.evidence/daily/{dateKey}`,
columns `protocolVersion (=2), dateKey, verifiedActiveSeconds, qualifiedPages,
updatedAtEpochMs`, permission `com.terinit.rhythmicreader.permission.RECOVERY`.
Role: **CD3 cumulative daily baseline (3600 verified seconds AND 36 qualified
pages)** only. Genuine-reading qualification untouched (foreground + screen
interactive + document loaded + reader visible; ≥15 s page dwell with
rapid-flip veto).

### Reader Recovery Session contract
**Unchanged — V1**, now clearly scoped to **CD4+ discrete restorative gates**:
action `com.terinit.rhythmicreader.action.START_RECOVERY` →
`RecoveryEntryActivity` with `recovery.session_id / protocol_version (=1) /
required_seconds / required_pages / created_at / expires_at`; status via
`content://com.terinit.rhythmicreader.recovery/sessions/{sessionId}`.
Per-gate requirement **1800 active seconds + 11 qualified pages**, bound to the
exact session id (idempotent same-id resume with matching params; conflicting
params rejected; R4 can never satisfy G5). The two evidence roles are kept
strictly separate — daily evidence never satisfies a bound session and vice
versa — while one real reading event deterministically updates both projections
(never twice inside one ledger).

---

## 3. Evening Wind-Down (Meditation)

- **State schema**: `EveningMeditationRecord(attentionDayId (PK), state,
  dueAtEpochMs?, snoozedUntilEpochMs?, sessionId?, updatedAtEpochMs)` with
  `EveningMeditationState = NOT_DUE | DUE | SNOOZED | IN_PROGRESS | COMPLETED |
  DEFERRED`. Exactly one record per Attention Day.
- **Trigger integration**: Routine-owned. Narrow surface
  `integration/contract/RoutineEveningSignal` (`currentEveningSignal():
  EveningSignal { attentionDayId, dueAtEpochMs, transitionAtEpochMs }`) with a
  null-returning default; `EveningMeditationController.onEveningWindDownDue /
  onEveningWindDownEnded / syncFromSignal` consume it. **No schedule engine in
  Meditation**; no cloud scheduling; no new permissions.
- **Session identity**: `evening-<attentionDayId>` (retry suffixes `-1`, `-2`…
  only after a cancelled/expired attempt), kind `EVENING_WIND_DOWN`,
  `requiredSeconds = 1800`, qualified by the **Pass 2 engine** (monotonic,
  screen-off qualification, pause/resume, checkpoints, reboot-safe recovery).
- **Snooze persistence**: `snoozedUntilEpochMs = now + 15 min`, Room-persisted;
  survives restart; `effectiveState` returns DUE at expiry. Never completion,
  never a Morning change, never a substitution, never a cooldown change.
- **Defer persistence**: `DEFERRED` for this Attention Day only; history only
  afterwards; no penalty, no Morning change, no accountability reporting.
- **Hard boundaries (tested)**: Evening completion never consumes a Meditation
  substitution, never completes Morning Meditation, never satisfies a cooldown
  Restorative Gate; before Start there is no whole-phone restriction; Essential
  interruption pauses qualified time (Pass 2 engine semantics); reflection is
  prompt-only (3 prompts) and never persisted/shared.
- **UI**: mockup direction — "Evening Wind-Down / End the day with stillness. /
  30-Minute Evening Meditation / Start now · Snooze 15 min · Defer tonight",
  the informational copy about calls/essential activity, Today's Calm Summary +
  Bedtime Reflection cards, and distinct calm renderings for all six states
  ("Evening practice deferred" — never punitive wording).

---

## 4. Insights (Meditation)

- **Local aggregation schema** (`domain/insights/MeditationInsights`, pure):
  meditation minutes today; morning status; evening due/complete/deferred;
  standalone session count; cooldown-restorative count; weekly minutes
  (Mon..Sun calendar buckets) and weekly completed sessions; optional
  interruption count (never judgmental).
  **Completed-vs-partial rule (documented + tested)**: *general meditation
  minutes include genuinely qualified time from any non-INVALID session
  (COMPLETED, CANCELLED, EXPIRED, incomplete ACTIVE/PAUSED); completed-session
  counts use `status == COMPLETED` ONLY*. A resumed session is one record
  counted once; a midnight-crossing session lands entirely in the calendar day
  of completion/last activity (never split, never double-counted).
- **Routine projection** (protocol **1**):
  `content://com.terinit.rhythmicroutine[.qa].attention-insight/attention-day/{attentionDayId}`,
  columns `protocolVersion, attentionDayId, cooldownsTriggered,
  restorativeGatesCreated, restorativeGatesSatisfied,
  readerRestorativeCompletions, meditationRestorativeCompletions,
  meditationSubstitutionsUsed, meditationSubstitutionsRemaining`, read-protected
  by `com.terinit.rhythmicmeditation.permission.STATUS_ACCESS`.
  `firstRiskUseAt` deliberately **omitted** (no reliable source event — not
  manufactured). Substitution display ("Meditation restorative choices ·
  X of 2 remaining") uses `meditationSubstitutionsRemaining` ONLY — never
  inferred from local session history.
- **Reader projection**: existing Daily Evidence V2 (verified reading seconds +
  qualified pages for Reading vs Meditation). No book titles, contents,
  annotations, page text, or library metadata cross the boundary.
- **Sample-size rule**: behavioral observations only when **≥ 3 qualifying days
  in each comparison group** (e.g. ≥3 completed-morning days AND ≥3 comparison
  days); descriptive "tended to…" wording only; never causal, never judgmental;
  hidden otherwise.
- **Partial-data behavior**: Routine or Reader unavailable / untrusted /
  malformed → those cards hide or show quiet "not connected"/"unavailable"
  copy; local Insights keep working; **an optional chart failure is never a
  policy failure** (fail-open for Insights, fail-closed for policy — the policy
  paths in Routine/Meditation do not consult these clients).
- **Standalone**: all local metrics work with no companions; never fabricates
  cooldowns, gates, or substitutions.

---

## 5. Cross-app signing configuration

All IPC is signature-protected; stable internal signing identity must be shared
across the three apps on the validation device:

| Surface | Permission |
| --- | --- |
| Reader Daily Evidence / Recovery / entry activity | `com.terinit.rhythmicreader.permission.RECOVERY` (Reader-declared, signature) |
| Meditation status provider | `com.terinit.rhythmicmeditation.permission.STATUS_ACCESS` (signature) |
| Routine preview provider | `com.terinit.rhythmicreader.permission.RECOVERY` |
| Routine attention-insight provider | `com.terinit.rhythmicmeditation.permission.STATUS_ACCESS` |

Trust chains: Routine verifies Meditation's signer == Routine's signer
(`CompanionTrust`, deny-by-default) and vice versa (`CallerVerifier` in
Meditation, configured via `AppContainer.callerTrustPolicy`); Reader verifies
nothing beyond the signature permissions (it is evidence-only). Unknown
protocol versions parse to "unavailable"/"no target" everywhere — never crash,
never fail-open.

---

## 6. Physical device validation — EXECUTED 2026-09-28/29 (Pass 4)

**Device**: Xiaomi Redmi Note 13 Pro+ 5G (`23129RN51X`, codename `blue`),
serial `P7J7TGKNAY8DKJ5P`, **Android 16 / API 36**. All three apps installed
from debug builds sharing the platform debug keystore (same-signer IPC ✓).
Builds under test: Routine `0c52065`+ (dev client via Metro + `adb reverse`),
Reader `bad67b5`, Meditation `8a118bc`.

| # | Scenario | Result |
| --- | --- | --- |
| 39 | **Morning Buffer → Meditation Required → complete → verified** | **PASS**. Today showed "Morning Meditation Required / Begin Session" (no Skip); session `morning-2026-09-28` (MORNING_REQUIRED) completed; status evidence = `COMPLETED, 1800/1800`; Completion screen "Session complete · 30 min". Morning completion consumed no substitution. |
| 40 | **Real 30-minute session, screen mostly off** | **PASS (×3).** Run 1 (morning, charging): 20:41:33 → 21:11:33 screen off; `completedQualifiedSeconds = 1800` **exactly**; `completedAt − startedAt = 1,800,533 ms` (exact crossing); 0 pauses; process PID unbroken across 24×75s samples. Run 2 (evening): completed 1800/1800 after a conservative restore + Resume (see 42). **Run 3 (foreground service): single interval `46652069→48452069` = 1,800,000 ms exactly — the exact-crossing completion proven at the ledger level**, 0 pauses, auto-complete. Checkpoint cadence + cap: no drift in any run (1800/1800 ×3). |
| 41 | **Foreground-service decision** | **REQUIRED — implemented and VERIFIED end-to-end.** The decision evolved with the evidence: the first morning run (phone charging) survived 30 minutes screen-off → provisional "not required"; the **evening run was killed at ~00:00:30** (Android midnight maintenance, 17 min in) → verdict **REQUIRED**. Implemented the narrowest possible service (`runtime/ActiveSessionForegroundService`): exists only while a session is ACTIVE (state observer start/stop), `foregroundServiceType="specialUse"` (+ `PROPERTY_SPECIAL_USE_FGS_SUBTYPE=meditation_session_timing`), quiet ongoing notification verified on-device (`flags=ONGOING_EVENT|FOREGROUND_SERVICE|SILENT`, `sound=null`, `vibrate=null`), **no wake lock**, no timing logic inside (monotonic/checkpoint architecture unchanged). **Verification run (01:03:16→01:33): the service-kept process (PID 23813) survived the entire 30-minute screen-off window — including a ~35-minute USB outage that looked like a process death in telemetry — and the session auto-completed at exactly 1800/1800 with a single interval closed at the exact crossing (`46652069→48452069` = **1,800,000 ms exactly**). Service record gone after completion (stops immediately ✓). Supporting evidence that checkpoints remain the real safety net: the mid-run kill cost only the ≤12s un-checkpointed tail (restored 1317s of ~1329s).** |
| 42 | **Process death** | **PASS** (run inside the evening session at 23:28:58). Pre-force-stop: ACTIVE, open interval from elapsed 40,817,986, checkpoint **168s** @ 40,986,915. After `am force-stop` + relaunch: **PAUSED**, credited **exactly 168s**, interval closed at the checkpoint instant (unsafe tail discarded), `PROCESS_RESTORE` event "process restore; unverified tail discarded", Resume required. No manufactured time. |
| 43 | **Essential interruption** | **PASS** (23:41:44, Dialer foregrounded mid-session). Timer auto-**PAUSED**, progress **preserved at 244s** (= 168 restored + 76 new), `APP_BACKGROUND` event recorded, pauseCount 2 / interruptionCount 2; returning to Meditation showed Resume and the session continued to completion. |
| 44 | **CD3 Reader baseline (60/36)** | **PARTIAL — UI verified, state branch unit-tested.** On-device: Reader's Today screen shows the new discrete-vocabulary card — "Reading target · Shared by Rhythmic Routine / No reading requirement for this cooldown. The 90-minute cooldown continues on its own. / Preview only…" — confirming the Routine→Reader **preview protocol V2 path works end-to-end** and the obsolete cumulative wording is gone. The "Today's Reading 42/60 min · 25/36 pages" CD3 branch requires an allocated ordinal-3 cooldown; the Routine demo-switcher taps would not register under automation (its modal sits over a continuously ticking countdown that also defeats uiautomator's idle-wait). Rendering + policy covered by `RestorativeReadingAlignmentTest` ("CD3 daily baseline displays 60 36…") and `RestorativeProjectionTest` (kind mirror). |
| 45 | **CD4 Reader gate (30/11)** | **PARTIAL — same as 44.** The "Restorative Reading · 30 minutes · 11 pages" + bound-session branches are unit-tested (`RestorativeReadingAlignmentTest`: G4/R4 vs G5/R5, idempotent same-id resume, **bound recovery persists across a repository restart**, daily evidence ≠ recovery completion, threshold trio 30:00+10 / 29:59+11 / 30:00+11). On-device: Reader renders the neutral branch correctly and Reader runs standalone (spec 36 ✓). |
| 46 | **CD4 Meditation gate (1800s)** | **ENGINE PASS (2× on-device) + boundary PASS; Routine-UI trigger not automated.** The exact behaviors the gate depends on are proven on hardware: **two real sessions completed at exactly 1800/1800** with status evidence (`COMPLETED, completedQualifiedSeconds >= requiredQualifiedSeconds`, exact session ids) — the same `isVerifiedMeditationCompletion` trust path a gate uses. The Routine→Meditation recovery-intent handshake is implemented (Pass 3 `launchMeditationForGate` → Kotlin `MeditationContract` → `MainActivity.handleRecoveryIntent` → `RecoveryRequestHandler` with the same-signer policy) and handler-verified with injected verifiers; the UI tap to fire it was blocked by the demo-switcher automation issue above. |
| 47 | **Substitution cap (2/day)** | **Boundary PASS + exhaustive unit coverage.** On-device: after the full evening completion (an entire 1800/1800 session), Routine's card reads **"0 used today"** — Evening never consumes a substitution (spec 20 boundary ✓ in real life). Cap mechanics (2/day, consumed exactly once per satisfied meditation gate, blocks both the offer and satisfaction at exhaustion) covered by `pass03_restorative_gates` + `RestorativeProjectionTest` ("substitution remaining never exceeds the cap"). |
| 48 | **Evening Start / Snooze / Defer** | **PASS (3/3)**. Signal: Routine's evening projection marked the record **DUE** (`ad-20260928-0800`, dueAt 21:30) through the signature-protected provider. UI shows all three calm actions. **Snooze 15 min** → "Snoozed · back at 11:31 PM" (snoozedUntil = exactly +15 min), **survived force-stop + relaunch**, and the process-start signal did not override it. **Resume** → DUE again. **Defer tonight** → "Evening practice deferred" (non-punitive), **DEFERRED persisted across restart**. **Start now** → `EVENING SESSION 0:07 / 30:00` via the Pass 2 engine (see 42/43 for its restore/interruption behavior) → completion verified below. |
| 49 | **App restarts** | Meditation restart during active + paused sessions covered by 42 (conservative restore, fail-closed). Routine restart with active gate + Reader restart during bound recovery: spot-checked in batch B. |

Additional on-device security evidence: an ADB/shell query to
`content://com.terinit.rhythmicmeditation.status/...` is rejected with
`SecurityException: Permission Denial … requires
com.terinit.rhythmicmeditation.permission.STATUS_ACCESS` (signature) —
deny-by-default confirmed on hardware.

**Device-found defects (all fixed + committed):**
1. **Evidence-destroying cascade** (critical): `@Insert(REPLACE)` on the
   session DAO = SQLite DELETE+INSERT → `ON DELETE CASCADE` wiped
   `meditation_intervals` + `session_interruption_events` at completion.
   Fixed with `@Upsert`; regression test added and verified on-device.
2. **Missing `uses-permission`**: Meditation *defined* its signature permission
   but never *requested* it → could not read Routine's signature-protected
   projections (evening-signal / attention-insight) → optional features
   silently dead. Fixed; signal verified flowing end-to-end.
3. Peer trust completed (Pass 3 handoff item) — `SameSignerCallerTrustPolicy`
   (same-signer model, deny-by-default).
4. Routine's demo switcher was web-only → mounted in `__DEV__` builds for
   validation (release untouched).

---

## 7. Verification results (this environment)

| Target | Result |
| --- | --- |
| Reader `./gradlew test` | **106 / 106, 0 failures** (94 baseline + 12 new) |
| Reader `./gradlew assembleDebug` | pass |
| Meditation `:app:test` | **175 / 175, 0 failures** (91 baseline + 84 new) |
| Meditation `:app:assembleDebug` / `:app:assembleDebugAndroidTest` | pass |
| Routine `npm test` | **405 / 405** (policy suites unchanged) |
| Routine `npm run typecheck` / `npm run lint` | clean |
| Routine `:rhythm-device:testDebugUnitTest` | **49 / 49** (41 + 8 new) |
| Routine `:rhythm-device:compileDebugKotlin` | pass |

## 8. Known limitations

- Specs 44–46 have partial device coverage: the discrete UI states and the
  Routine→Reader preview V2 path are verified on-device, and the underlying
  behaviors (CD3 60/36 and CD4 30/11 display kinds, G4/R4 vs G5/R5 session
  binding, threshold trio, 2× exact 1800/1800 completions, "0 used today"
  substitution boundary) are proven either on-device or in tests — but the
  Routine **demo-switcher taps could not be driven reliably by automation**
  (its modal sits over a continuously ticking countdown that defeats
  uiautomator's idle-wait, and backdrop taps closed the sheet without firing
  the row action). A human running the demo switcher for ordinals 3→6 would
  complete those three rows in ~70 minutes.
- One telemetry caveat recorded for future QA: `pidof`-based monitoring cannot
  distinguish process death from a USB/ADB dropout (this produced a false
  "kill" at 01:08 during the FGS run). Ground truth should always be the
  session ledger (open interval + PROCESS_RESTORE), as used for the real
  midnight kill.
- `EXPIRED` session minutes count toward "time spent meditating" (documented
  rule), never toward completed counts.
- Weekly "restorative gates" Insights values sum whatever daily projections are
  readable (missing days contribute 0 — never fabricated).

## 9. Exact remaining Pass 05 work

- Drive the Routine demo switcher manually for cooldowns #3–#6 to close the
  partial rows of 44–46 (CD3 baseline view, CD4 Reader path, CD4 Meditation
  intent launch) — engine, trust, and UI branches are otherwise proven.
- Optional UX improvement (37): surface the richer provider states
  (`not-installed / untrusted / incompatible / unavailable / available`) in the
  Meditation/Insights UI — the Kotlin layer already distinguishes them.
- Reader lint/instrumentation targets: Reader has no `androidTest` sources;
  unit tests + assembleDebug are its configured targets.
- Any defects the remaining manual matrix rows expose.

## 10. SW-2026-004 closeout update — 2026-09-30

- Meditation unit tests: **175 passed, 0 failures**.
- `:app:assembleDebug` and `:app:lintDebug`: **passed** after hoisting
  `pendingRoute.asStateFlow()` to a stable `MainActivity` property.
- A final `:app:connectedDebugAndroidTest` attempt on the Redmi started 0 tests:
  the test runner's default debug key could not update the installed shared-QA-
  signer package (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), and the test setup
  left the prior package absent. The current debug APK was reinstalled with the
  shared ecosystem signer. Its pre-test private-data archive is intact; device
  data restoration is pending renewed ADB authorization. Prior
  instrumentation/provider evidence remains as described in section 6.
- The Reader/Routine/Meditation physical rows above retain their recorded
  evidence classifications; unit coverage and provider-session runs do not
  close the remaining Routine gate-trigger and full matrix rows.
- No release, store submission, or production promotion is authorized by this
  handoff.
