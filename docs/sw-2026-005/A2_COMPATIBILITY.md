# SW-2026-005 A2 — Meditation companion compatibility

Baseline: Meditation `4afdc0a852b53f3ce581ae3c08927a6a3a19718d` (R0 baseline).
This note documents source/contract only. It is not physical or installed-APK acceptance.

## Attention insight protocol V1

Existing production and QA authorities remain unchanged:

- `com.terinit.rhythmicroutine.attention-insight`
- `com.terinit.rhythmicroutine.qa.attention-insight`

Existing exact-ID queries and columns are unchanged:

```
content://<authority>/attention-day/<exactAttentionDayId>
```

- Protocol version: `1`
- Columns: the existing nine attention-insight columns.
- Exact-ID query semantics: the returned row must have
  `attentionDayId == <exactAttentionDayId>`; otherwise it is
  `UNEXPECTED_ATTENTION_DAY` and is not connected.
- Empty exact row: `EMPTY_CURRENT_DAY` (reachable, not connected).
- Null cursor: classified as absent, denied, or `NULL_CURSOR` after an
  authority-presence probe.
- Malformed row / version mismatch / transport error remain separate typed
  reasons.

## Additive current selector

A2 consumes this coordinated additive V1 capability:

```
content://<authority>/attention-day/current
```

Exact constant: `AttentionInsightProtocol.CURRENT_SELECTOR = "current"`.

- The selector is an explicit Routine capability, not a calendar date guessed
  by Meditation.
- A current row must carry a nonblank opaque Attention Day identity and must
  not echo the selector itself. Current interpretation verifies protocol/count
  validity but cannot compare the opaque identity to a caller-known exact ID.
- Zero counts are valid and remain connected. The current row never creates a
  gate or obligation; it is read-only.
- Exact historical queries keep exact-match rejection. An opaque current
  identity is not accepted by `attentionDayResult(exactId)`.
- If the installed Routine provider lacks the selector (empty row or unknown
  URL), Meditation reports `CURRENT_PROJECTION_UNAVAILABLE`, not "not
  installed", not a guessed day, and not a fabricated count.
- Meditation's Insights UI uses the current result for today's Routine card.
  Weekly/historical cards continue to use exact-ID queries.
- Production/QA authority order, signature permission, caller verification, and
  package policy are unchanged. No new permission or security exemption was
  added.

Routine's provider implementation of `attention-day/current` is owned by the
B1/B2 integrator and is not present in this repository. Until that provider is
installed, old-provider compatibility is the visible
`CURRENT_PROJECTION_UNAVAILABLE` limitation.

## Meditation status identity metadata (V1 additive)

The status provider still returns the seven frozen V1 columns for any
legacy/no-metadata projection. If the caller requests an identity column, the
provider returns the full self-describing shape:

- `sessionKind`
- `sourceCooldownId`
- `sourceRhythmicDayId`

Protocol V1 is unchanged. Pre-metadata providers omit the columns; a consumer
must treat missing metadata as an upgrade/capability limitation, never as a
match. `MeditationRecoveryStatus.identityOrNull()` returns null when kind is
missing; consumers compare every expected identity field exactly.

`MeditationContractFields.statusFromFields/fieldsOf(status)` likewise keeps the
exact seven-key map for a pre-metadata status and adds the three snake_case
identity keys only when at least one identity value is present:

- `session_kind`
- `source_cooldown_id`
- `source_rhythmic_day_id`

## Recovery binding idempotency and concurrency

`createOrReplaceSession` rejects changed bindings with
`MeditationSessionConflictException`; it never rewrites qualified progress or a
completed record. An exact replay remains a no-op. Nullable expiry is allowed
only when the stored tuple also has that exact nullable value.

The service is a process singleton. `bindingMutex` serializes the
read-then-create path so simultaneous same-ID recovery accepts have
first-binding-wins semantics. Lifecycle updates continue to use the repository's
non-destructive `@Upsert`; no SQLite `REPLACE`, destructive migration, or
evidence deletion was introduced. `sourceRiskGroupId` remains request-only and
is not persisted by the current ledger; adding it would require a documented
additive migration.

## Tests added or updated

- `AttentionInsightClientTest`: zero-value valid current row, opaque current
  identity, exact-ID wrong-day rejection, current malformed/version rejection,
  current capability priority, old-client default limitation.
- `InsightsUiStateTest`: opaque current projection connected with zero counts;
  old-provider current limitation reachable but not connected and not counted.
- `MeditationStatusProviderTest`: frozen seven-column legacy shape, additive
  identity projection, stored identities, missing metadata, wrong kind/source.
- `MeditationContractFieldsTest`: legacy seven-key status map, additive identity
  round trip, wrong-type identity not trusted as identity.
- `MeditationSessionServiceTest`: deterministic simultaneous same-ID
  interleavings (first-binding-wins and identical idempotency), sequential
  conflicts, completed replay preservation, nullable/fixed expiry, validation
  order.
- `RecoveryRequestHandlerTest`: typed conflict propagation without clearing
  completion.

## Not proven

- No phone/signature reproduction was performed.
- Issue #13 is not proven fixed on hardware.
- Same-signed installed IPC and both physical devices remain unverified.
- Routine's additive `attention-day/current` provider is pending B1/B2
  integration; paired acceptance is pending.
- No signing, install, version, or release change is part of this patch.
