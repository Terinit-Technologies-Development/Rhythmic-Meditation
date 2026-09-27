# SW-2026-004 — Pass 04 Handoff: Reader Alignment + Evening Wind-Down + Insights

Status: **implemented, tested, and committed across three repositories.**
Physical-device validation (specs 38–49) was **NOT executed in this
environment** (no paired device available) and is therefore explicitly
**outstanding** — per the non-negotiables, physical QA is not declared complete.
The foreground-service decision is consequently **deferred pending evidence**.

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

## 6. Physical device validation — OUTSTANDING (not executed)

Required before Pass 5 handoff (spec 38). Device identifier/model/Android
version: **N/A — not run in this environment**. Matrix to execute with
Routine `51b0818`+ / Reader `bad67b5`+ / Meditation `a498a04`+:

Morning (39) · real 30-minute screen-off session (40) · process death (42) ·
Essential interruption (43) · CD3 Reader baseline (44) · CD4 Reader gate (45) ·
CD4 Meditation gate (46) · substitution cap (47) · Evening Start/Snooze/Defer
(48) · app restarts matrix (49).

**Foreground-service decision (41): DEFERRED — evidence required.** Retain the
Pass 2 checkpoint architecture if the real 30-minute screen-off test proves
reliable; otherwise add the narrowest active-session foreground service (active
meditation only, quiet ongoing notification, unchanged monotonic/checkpoint
semantics, no wake lock unless independently necessary, stops on complete/
cancel). No implementation is included in this pass — the decision must be
evidence-based, and provider-runtime problems must never weaken gate
verification.

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

- Physical QA matrix + foreground-service decision outstanding (above).
- Evening's real-world trigger awaits the Routine-side evening signal wiring
  (the model + surface + tests are complete; until a signal arrives the app
  honestly shows "not due").
- Reader's RecoveryCard shows bound-session progress only; the pass-3 Routine
  side still binds sessions through its gate flow (unchanged here by design).
- Weekly "restorative gates" Insights values sum whatever daily projections are
  readable (missing days contribute 0 — never fabricated).
- `EXPIRED` session minutes count toward "time spent meditating" (documented
  rule), never toward completed counts.
- Reader lint/instrumentation: Reader has no `androidTest` sources; unit tests
  + assembleDebug are its configured targets.

## 9. Exact remaining Pass 05 work

- Execute the physical-device matrix (39–49) on the paired device and record
  device identifier/model/Android version + per-scenario results.
- Make the evidence-based foreground-service decision (41) and implement it in
  Meditation if required.
- Wire Routine's Evening Wind-Down signal to Meditation
  (`RoutineEveningSignal` is the exact surface).
- Optional UX improvement (37): surface the richer provider states
  (`not-installed / untrusted / incompatible / unavailable / available`) in the
  Meditation/Insights UI — the Kotlin layer already distinguishes them.
- Any defects the device matrix exposes in enforcement/projection plumbing
  (Routine secondary changes only as needed).
