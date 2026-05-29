# HANDOFF — SSE streaming for the chat client

**Reviewer:** Sarvesh
**Branch:** `rishi/sse-streaming`
**Status when this doc was written:** all phases shipped, Motorola test pass §11 pending Rishi's manual run.

## What this PR does

Adds token-by-token SSE streaming to the existing chat flow, gated behind a feature flag (`ChatFeatureFlags.SseStreamingEnabled`). When the flag is OFF, the chat client behaves byte-identically to today's `POST .../messages` endpoint — the new code is dormant. When the flag is ON and the carve-outs allow it, sends flow through `POST .../messages/stream` and tokens render incrementally.

The companion backend work is the FastAPI streaming endpoint at `chat-ai.rishi.yral.com/api/v1/chat/conversations/{id}/messages/stream` (separate PR, separate timeline). This PR is mobile-only.

## Why the URL stays at `chat-ai.rishi.yral.com`

`AppConfigurations.CHAT_BASE_URL` keeps the production chat host. The streaming endpoint is being prepared at the same host on a separate PR; once that ships, the mobile flag flip alone activates streaming with zero further mobile work. There's a local-only `agent.rishi.yral.com` override that Rishi uses against the dev backend during development — the pre-commit hook at `.git/hooks/pre-commit` rejects any commit that reintroduces that override on origin.

The same applies to `SseStreamingEnabled.defaultValue = false` — the flag stays dormant on origin until backend cutover + GA. The same pre-commit hook blocks accidental `defaultValue = true` commits.

## Commits on the branch (in order)

1. `feat(chat): SSE 5a — core streaming plumbing (ChatStreamingDataSource, StreamEvent Flow API)` — adds the Ktor SSE data source, DTOs, the `StreamEvent` Flow contract, the feature flag, and the HTTP client SSE plugin install. Flag off by default.
2. `feat(chat): SSE rendering pipeline — path lock + cursor isolation + coalescing` — the verified flicker fix. Phase 5b (path lock) and Phase 5c (cursor-out-of-content + 250ms coalescing) ship together because Phase 5b alone left a known-buggy intermediate state.
3. `docs(chat): SSE planning, post-mortem, and mobile expert lessons` — `SSE-IMPLEMENTATION-PLAN.md`, `SSE-PHASE5B-PLAN.md`, `SSE-PLANNING-NOTES.md`, `POST-MORTEM-CHAT-AS-HUMAN.md`, and the append-only `MOBILE-EXPERT-LESSONS.md`. `AGENTS.md` gets a top-of-file pointer at the lessons file so future sessions read it first.
4. `feat(chat): SSE Phase 4 — typed AssistantError plumbing for stream errors` — replaces the inline `LegacyErrorPayloadDto` with a typed `AssistantError` domain model. Sets up Phase 6.
5. `feat(chat): SSE Phase 6 — AssistantErrorBubble + retry off the typed error` — the visible error bubble. Rendered when an SSE `error` event fires. Retry button is inside the bubble (deviates from spec §4.3's "retry on user message" — rationale in the bubble's KDoc).
6. `feat(chat): SSE Phase 7 — idle watchdog + single-stream send queue` — 30s idle watchdog synthesizes a TRANSIENT error so a stalled stream stops looking alive. Send queue enforces "at most one SSE collect in flight per conversation"; queued sends drain FIFO on the active stream's terminal event. User Locals still appear in overlay instantly (post-mortem rule: user sees their message instantly).
7. `feat(chat): SSE Phase 8 — streaming-aware sticky-bottom auto-scroll` — explicit `scrollToItem(0)` while a token buffer is growing and the user is at the bottom. No "↓ new message" pill in this PR (polish, deferred).
8. `feat(chat): SSE Phase 9 — consecutive-failure circuit breaker` — three consecutive SSE failures (Failed event, idle timeout, or connection error) silently force the rest of the session onto the non-streaming legacy endpoint. Reset on Done. Counter zeros on conversation switch.

## Files added

- `data/ChatStreamingDataSource.kt`
- `data/models/StreamTokenPayloadDto.kt`
- `data/models/StreamDonePayloadDto.kt`
- `data/models/AssistantErrorDto.kt`
- `data/ConversationContentCache.kt`
- `domain/models/StreamEvent.kt`
- `domain/models/AssistantError.kt`
- `ui/conversation/AssistantErrorBubble.kt`

## Files modified

- `domain/ChatRepository.kt` + `data/ChatRepositoryImpl.kt` — new `streamMessage(...)` returning `Flow<StreamEvent>`
- `data/di/ChatModule.kt` — DI for the new data source + cache
- `viewmodel/ConversationViewModel.kt` — most of the new behavior (sendMessage routing, SSE collect, coalescing, path lock, cache hydration, error state, send queue, idle watchdog, circuit breaker)
- `ui/conversation/ConversationMessageBubble.kt` + `ConversationMessagesList.kt` + `ChatConversationScreen.kt` — rendering pipeline + sticky-bottom scroll
- `libs/http/HttpClientFactory.kt` — Ktor SSE plugin install
- `libs/feature-flag/ChatFeatureFlags.kt` — new `SseStreamingEnabled` flag (default `false`)

## The Phase 5b ↔ 5c rendering contract (do not regress)

Documented verbatim in `MOBILE-EXPERT-LESSONS.md` and the `RegularBubble` KDoc. Summary:

- **Path lock**: `markdownLockedOverride` pins Markdown vs Text per stream. NEVER let the path switch mid-stream or at done.
- **Cursor isolation**: the streaming cursor must be a sibling composable, never appended to the content string the renderer parses.
- **Coalesce window**: 250ms is shipped. Don't tune without re-measuring on a Motorola.
- **Flush ordering**: `flushJob?.cancelAndJoin()` MUST run before any code that removes the streaming Local.

If a future session touches `RegularBubble`, `MessagesList`, or `startStreamingAssistantReply`, those four lines are load-bearing.

## Hard-won lessons file

`MOBILE-EXPERT-LESSONS.md` is append-only and committed at the repo root. It captures P1-P5 from the flicker-debugging arc (multi-flavor install hazard, Gradle build cache, macOS `strings` tooling, claims-vs-code drift, premature theorizing). `AGENTS.md` points at it from the top. Read it before any mobile work.

## Test plan — §11 from `SSE-IMPLEMENTATION-PLAN.md`

Twelve sub-sections to run on Motorola against the dev backend (`agent.rishi.yral.com` with the local-only overrides applied). Results table is filled in by Rishi during the Phase 10 manual pass below:

| # | Sub-section | Status |
|---|---|---|
| 11.1 | Functional happy path (Aasha, Ragini, Urvashi, Monika, Tara) | TBD |
| 11.2 | Steady-state observation (30s idle, no flicker, no spontaneous scrolls) | TBD |
| 11.3 | Multi-cycle (5 in succession, 10 over 10 minutes) | TBD |
| 11.4 | Re-entry (leave mid-stream, leave after done, flag toggle) | TBD |
| 11.5 | Network degradation (airplane mode pre-token vs mid-token) | TBD |
| 11.6 | Backgrounding (home button → 60s → resume) | TBD |
| 11.7 | Force-kill recovery | TBD |
| 11.8 | Carve-outs (takeover, image+caption, flag off) | TBD |
| 11.9 | Scroll (scroll up mid-stream; no "↓" pill in this PR — deferred polish) | Partial — pill not shipped |
| 11.10 | Phase 3.8 error rendering (BLOCKED_CONTENT, TRANSIENT + retry) | TBD |
| 11.11 | Cross-screen (Inbox, Wall, profile, media upload unaffected) | TBD |
| 11.12 | PR #1172 (Chat as Human) regression check | TBD |

## Known limitations / deferred work

- **No "↓ new message" pill** (Phase 8 polish). Sticky-bottom auto-scroll works; the pill for when-user-scrolled-up is a UX polish that can be added without revisiting the underlying scroll math.
- **No background-pause lifecycle observer** (Phase 7). Compose Multiplatform lacks a unified Lifecycle primitive. The spec's "wait 30s in background" test is covered by the idle watchdog firing on return. If we add the observer later, the hook is `activeStreamJob?.cancel()` from a screen-side `DisposableEffect`.
- **No per-error telemetry events** (Phase 9). `chatTelemetry.assistantError(code, provider, isStreaming)` from spec §4.5 should be designed alongside other Phase 12 observability work, not inline.
- **No NSFW conversation carve-out** (Phase 9). Waits on the backend exposing `is_nsfw` on the conversation DTO.
- **No 404-immediate fallback** (Phase 9). The circuit breaker provides a graceful 3-strikes fallback to the legacy endpoint. A faster path is possible but adds an exception-type dispatch in the streaming data source that isn't warranted yet.

## Companion docs

- `SSE-IMPLEMENTATION-PLAN.md` — phased plan + the §11 test matrix
- `SSE-PHASE5B-PLAN.md` — path-lock + stale-while-revalidate sub-plan with the Aasha gating test
- `SSE-PLANNING-NOTES.md` — running notes from planning rounds
- `MOBILE-EXPERT-LESSONS.md` — the append-only mobile-debugging lessons file
- `POST-MORTEM-CHAT-AS-HUMAN.md` — the prior feature's post-mortem (points at the lessons file)
