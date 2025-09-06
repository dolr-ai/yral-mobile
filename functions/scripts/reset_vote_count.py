#!/usr/bin/env python3
"""
reset_fire_votes.py
───────────────────
Zero out the `fire` field in every shard doc under videos/*/tallies/*.
Usage:
  # Staging
  FIREBASE_PROJECT=yral-staging python3 scripts/reset_fire_votes.py
  # Prod
  FIREBASE_PROJECT=yral-mobile  python3 scripts/reset_fire_votes.py
Options via environment variables:
  LIMIT_DOCS      = integer; process at most this many shard docs (0 = unlimited)
  DRY_RUN         = "1" or "true" to only log what would change (no writes)
  BATCH_SIZE      = number of shard docs per read page (default 1000)
  WRITE_BATCH_MAX = writes per commit (default 500; Firestore limit)
Notes:
- Uses collection_group("tallies"), ordered by __name__, paged with start_after.
- Skips docs where `fire` is missing or already 0 (saves writes).
- Retries both page fetches and commits with exponential backoff + jitter.
"""

import os, sys, time, random
import firebase_admin
from firebase_admin import credentials, firestore
from google.api_core import exceptions

# ───────── Config ─────────
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
def commit_updates(pending_refs):
    if not pending_refs:
        return 0
    def _do_commit():
        batch = db.batch()
        for ref in pending_refs:
            batch.update(ref, {"surprise": 0})
        # short commit, server handles per-write retries
        batch.commit()
    with_retry(_do_commit, what=f"commit {len(pending_refs)} updates")
    return len(pending_refs)

# ───────── Main pagination loop ─────────
processed = 0       # shard docs examined
modified  = 0       # shard docs updated
last_snap = None    # page cursor
page_num  = 0

pending_refs = []   # refs queued for next commit

print(f"Project: {PROJECT_ID} | DRY_RUN={DRY_RUN} | LIMIT_DOCS={LIMIT_DOCS or '∞'} | BATCH_SIZE={BATCH_SIZE}", flush=True)

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
        current = data.get("surprise", 0)
        if isinstance(current, int) and current > 0:
            if DRY_RUN:
                print(f"[DRY] would zero {snap.reference.path} (surprise: {current} -> 0)")
            else:
                pending_refs.append(snap.reference)
                if len(pending_refs) >= WRITE_BATCH_MAX:
                    modified += commit_updates(pending_refs)
                    pending_refs.clear()
        # Optional hard limit for smoke test
        if LIMIT_DOCS and processed >= LIMIT_DOCS:
            break

    # Flush end-of-page batch
    if not DRY_RUN and pending_refs:
        modified += commit_updates(pending_refs)
        pending_refs.clear()

    # Progress
    print(f"[Page {page_num}] scanned {processed} shards, zeroed {modified}", flush=True)

    # Pagination cursor
    last_snap = page[-1]

    if LIMIT_DOCS and processed >= LIMIT_DOCS:
        break

# Final summary
print(f"✅ Done. Scanned {processed} shard docs. Zeroed {modified}. DRY_RUN={DRY_RUN}", flush=True)