#!/usr/bin/env python3
"""
Backfill top-level users with:
- is_smiley_game_banned = False (only if missing)
- number_of_airdropds = 0 (unconditionally if missing)

Usage:
  FIREBASE_PROJECT=yral-staging python3 scripts/backfill_users_ban_and_airdrops.py
  FIREBASE_PROJECT=yral-mobile  python3 scripts/backfill_users_ban_and_airdrops.py

Options via env:
  DRY_RUN         = "1"/"true" to log without writing
  BATCH_SIZE      = docs per page (default 500)
  WRITE_BATCH_MAX = writes per commit (default 450)
  LIMIT_DOCS      = stop after N docs processed (0 = no limit)
"""

import os, sys, time
import firebase_admin
from firebase_admin import credentials, firestore
from google.api_core import exceptions

PROJECT_ID       = os.environ.get("FIREBASE_PROJECT", "yral-staging")
DRY_RUN          = os.environ.get("DRY_RUN", "").lower() in ("1", "true", "yes")
BATCH_SIZE       = int(os.environ.get("BATCH_SIZE", "500"))
WRITE_BATCH_MAX  = int(os.environ.get("WRITE_BATCH_MAX", "450"))
LIMIT_DOCS       = int(os.environ.get("LIMIT_DOCS", "0"))

firebase_admin.initialize_app(credentials.ApplicationDefault(), {"projectId": PROJECT_ID})
db = firestore.client()

def with_retry(fn, *, what: str, max_tries: int = 6, base_sleep: float = 0.6, max_sleep: float = 8.0):
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
            sleep = min(max_sleep, base_sleep * (2 ** (tries - 1)))
            print(f"[WARN] {what} retry {tries}/{max_tries} after {e.__class__.__name__}, sleeping {sleep:.2f}s", file=sys.stderr, flush=True)
            time.sleep(sleep)

def commit_updates(pending):
    if not pending:
        return 0
    def _commit():
        batch = db.batch()
        for ref, payload in pending:
            batch.update(ref, payload)
        batch.commit()
    with_retry(_commit, what=f"commit {len(pending)} updates")
    return len(pending)

def main():
    processed = 0
    modified = 0
    last_snap = None
    pending = []
    page_num = 0

    print(f"Project={PROJECT_ID} DRY_RUN={DRY_RUN} BATCH_SIZE={BATCH_SIZE} WRITE_BATCH_MAX={WRITE_BATCH_MAX} LIMIT_DOCS={LIMIT_DOCS or '∞'}", flush=True)

    while True:
        def fetch_page():
            q = db.collection("users").order_by("__name__").limit(BATCH_SIZE)
            if last_snap is not None:
                q = q.start_after(last_snap)
            return list(q.stream(timeout=60))

        page = with_retry(fetch_page, what="fetch page of users")
        if not page:
            break

        page_num += 1
        for snap in page:
            processed += 1
            data = snap.to_dict() or {}
            updates = {}

            if "is_smiley_game_banned" not in data:
                updates["is_smiley_game_banned"] = False

            # Always set number_of_airdrops to 0 (correct field)
            updates["number_of_airdrops"] = 0

            # Remove old typo field if present
            if "number_of_airdropds" in data:
                updates["number_of_airdropds"] = firestore.DELETE_FIELD

            if updates:
                if DRY_RUN:
                    print(f"[DRY] would update {snap.reference.path} -> {updates}")
                else:
                    pending.append((snap.reference, updates))
                    if len(pending) >= WRITE_BATCH_MAX:
                        modified += commit_updates(pending)
                        pending.clear()

            if LIMIT_DOCS and processed >= LIMIT_DOCS:
                break

        if not DRY_RUN and pending:
            modified += commit_updates(pending)
            pending.clear()

        print(f"[Page {page_num}] scanned={processed} updated={modified}", flush=True)

        if LIMIT_DOCS and processed >= LIMIT_DOCS:
            break

        last_snap = page[-1]

    print(f"✅ Done. Scanned {processed} users. Updated {modified}. DRY_RUN={DRY_RUN}", flush=True)

if __name__ == "__main__":
    main()
