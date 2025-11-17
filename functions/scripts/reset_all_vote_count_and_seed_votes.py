#!/usr/bin/env python3
"""
reset_all_vote_count_and_seed_votes.py
───────────────────
Zero out every emoji tally field in shard docs under videos/*/tallies/*,
but seed two random non-heart emojis per video with 1,000 votes (heart stays 0).
Usage:
  # Staging
  FIREBASE_PROJECT=yral-staging python3 scripts/reset_all_vote_count_and_seed_votes.py
  # Prod
  FIREBASE_PROJECT=yral-mobile  python3 scripts/reset_all_vote_count_and_seed_votes.py
Options via environment variables:
  LIMIT_DOCS      = integer; process at most this many shard docs (0 = unlimited)
  DRY_RUN         = "1" or "true" to only log what would change (no writes)
  BATCH_SIZE      = number of shard docs per read page (default 1000)
  WRITE_BATCH_MAX = writes per commit (default 500; Firestore limit)
Notes:
- Uses collection_group("tallies"), ordered by __name__, paged with start_after.
- Skips docs where every emoji field is missing or already 0 (saves writes).
- Retries both page fetches and commits with exponential backoff + jitter.
"""

import os, sys, time, random
import firebase_admin
from firebase_admin import credentials, firestore
from google.api_core import exceptions

# ───────── Config ─────────
EMOJI_FIELDS     = ("laugh", "heart", "fire", "surprise", "rocket", "puke")
NON_HEART_EMOJIS = tuple(field for field in EMOJI_FIELDS if field != "heart")
SEED_EMOJI_COUNT = 2
SEED_VOTE_VALUE  = 1000
PROJECT_ID       = os.environ.get("FIREBASE_PROJECT", "yral-staging")
LIMIT_DOCS       = int(os.environ.get("LIMIT_DOCS", "0"))
DRY_RUN          = os.environ.get("DRY_RUN", "").lower() in ("1", "true", "yes")
BATCH_SIZE       = int(os.environ.get("BATCH_SIZE", "1000"))      # docs per page
WRITE_BATCH_MAX  = int(os.environ.get("WRITE_BATCH_MAX", "500"))  # writes per commit

# ───────── Init Admin SDK (ADC) ─────────
firebase_admin.initialize_app(credentials.ApplicationDefault(), {"projectId": PROJECT_ID})
db = firestore.client()

# ───────── Retry helper ─────────
def with_retry(fn, *, what:str, max_tries:int=8, base_sleep:float=0.8, max_sleep:float=12.0):
    tries = 0
    while True:
        try:
            return fn()
        except (exceptions.DeadlineExceeded,
                exceptions.ServiceUnavailable,
                exceptions.Aborted,
                exceptions.InternalServerError) as e:
            tries += 1
            if tries >= max_tries:
                print(f"[FATAL] {what} failed after {tries} tries: {e}", file=sys.stderr)
                raise
            # exp backoff + jitter
            sleep = min(max_sleep, base_sleep * (2 ** (tries - 1))) * (0.6 + 0.8 * random.random())
            print(f"[WARN] {what} retry {tries}/{max_tries} after {e.__class__.__name__}: sleeping {sleep:.2f}s", file=sys.stderr, flush=True)
            time.sleep(sleep)

# ───────── Commit helper (rebuild batch on retry) ─────────
def commit_updates(pending_updates):
    if not pending_updates:
        return 0
    def _do_commit():
        batch = db.batch()
        for ref, payload in pending_updates:
            batch.update(ref, payload)
        # short commit, server handles per-write retries
        batch.commit()
    with_retry(_do_commit, what=f"commit {len(pending_updates)} updates")
    return len(pending_updates)

# ───────── Main pagination loop ─────────
processed = 0       # shard docs examined
modified  = 0       # shard docs updated
last_snap = None    # page cursor
page_num  = 0

pending_updates = []  # (ref, payload) queued for next commit

print(f"Project: {PROJECT_ID} | DRY_RUN={DRY_RUN} | LIMIT_DOCS={LIMIT_DOCS or '∞'} | BATCH_SIZE={BATCH_SIZE}", flush=True)

video_state = {}  # video_id -> {"chosen": tuple, "seeded": bool}

def _video_id_from_ref(doc_ref):
    parent = doc_ref.parent
    return parent.parent.id if parent and parent.parent else None

def _chosen_emojis_for_video(video_id: str):
    state = video_state.get(video_id)
    if state:
        return state
    k = min(SEED_EMOJI_COUNT, len(NON_HEART_EMOJIS))
    chosen = tuple(random.sample(NON_HEART_EMOJIS, k)) if k else tuple()
    state = {"chosen": chosen, "seeded": False}
    video_state[video_id] = state
    return state

while True:
    # Build page query fresh each loop (safer for retries)
    def fetch_page():
        q = db.collection_group("tallies").order_by("__name__").limit(BATCH_SIZE)
        if last_snap is not None:
            q = q.start_after(last_snap)
        # materialize page (short per-RPC timeout)
        return list(q.stream(timeout=60))

    page = with_retry(fetch_page, what="fetch page of tallies")
    if not page:
        break

    page_num += 1
    for snap in page:
        processed += 1
        data = snap.to_dict() or {}
        video_id = _video_id_from_ref(snap.reference)
        if not video_id:
            print(f"[WARN] Unable to resolve video id for {snap.reference.path}, skipping", file=sys.stderr)
            continue

        state = _chosen_emojis_for_video(video_id)
        should_seed_here = not state["seeded"] and bool(state["chosen"])

        payload = {}
        for field in EMOJI_FIELDS:
            if field == "heart":
                target = 0
            elif should_seed_here and field in state["chosen"]:
                target = SEED_VOTE_VALUE
            else:
                target = 0

            current = data.get(field, 0)
            if not isinstance(current, int):
                current = 0

            if current != target:
                payload[field] = target

        if should_seed_here:
            state["seeded"] = True

        if payload:
            if DRY_RUN:
                changes = ", ".join(f"{field}: {data.get(field, 0)} -> {payload[field]}" for field in payload)
                print(f"[DRY] would update {snap.reference.path} ({changes})")
            else:
                pending_updates.append((snap.reference, payload))
                if len(pending_updates) >= WRITE_BATCH_MAX:
                    modified += commit_updates(pending_updates)
                    pending_updates.clear()
        # Optional hard limit for smoke test
        if LIMIT_DOCS and processed >= LIMIT_DOCS:
            break

    # Flush end-of-page batch
    if not DRY_RUN and pending_updates:
        modified += commit_updates(pending_updates)
        pending_updates.clear()

    # Progress
    print(f"[Page {page_num}] scanned {processed} shards, updated {modified}", flush=True)

    # Pagination cursor
    last_snap = page[-1]

    if LIMIT_DOCS and processed >= LIMIT_DOCS:
        break

# Final summary
print(f"✅ Done. Scanned {processed} shard docs. Updated {modified}. DRY_RUN={DRY_RUN}", flush=True)
