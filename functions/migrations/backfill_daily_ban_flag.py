#!/usr/bin/env python3
"""
Backfill daily leaderboard user docs with is_smiley_game_banned=False when missing.

Usage:
  FIREBASE_PROJECT=yral-staging python3 migrations/backfill_daily_ban_flag.py
  FIREBASE_PROJECT=yral-mobile  python3 migrations/backfill_daily_ban_flag.py

Options via env:
  DRY_RUN         = "1"/"true" to log without writing
  BATCH_SIZE      = docs per page inside a day's users (default 500)
  WRITE_BATCH_MAX = writes per commit (default 450)
  LIMIT_DAYS      = process at most N day documents (0 = no limit)
  LIMIT_USERS     = process at most N user docs per day (0 = no limit)
"""

import os, sys, time
import firebase_admin
from firebase_admin import credentials, firestore
from google.api_core import exceptions

DAILY_COLL       = "leaderboards_daily"
PROJECT_ID       = os.environ.get("FIREBASE_PROJECT", "yral-staging")
DRY_RUN          = os.environ.get("DRY_RUN", "").lower() in ("1", "true", "yes")
BATCH_SIZE       = int(os.environ.get("BATCH_SIZE", "500"))
WRITE_BATCH_MAX  = int(os.environ.get("WRITE_BATCH_MAX", "450"))
LIMIT_DAYS       = int(os.environ.get("LIMIT_DAYS", "0"))
LIMIT_USERS      = int(os.environ.get("LIMIT_USERS", "0"))
DAY_BATCH_SIZE   = int(os.environ.get("DAY_BATCH_SIZE", "200"))

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

def process_day(day_ref):
    processed_users = 0
    updated_users = 0
    pending = []
    last_snap = None

    while True:
        def fetch_page():
            q = day_ref.collection("users").order_by("__name__").limit(BATCH_SIZE)
            if last_snap is not None:
                q = q.start_after(last_snap)
            return list(q.stream(timeout=60))

        page = with_retry(fetch_page, what=f"fetch users for {day_ref.id}")
        if not page:
            break

        for snap in page:
            processed_users += 1
            data = snap.to_dict() or {}
            if "is_smiley_game_banned" in data:
                pass
            else:
                if DRY_RUN:
                    print(f"[DRY] would set is_smiley_game_banned=False on {snap.reference.path}")
                else:
                    pending.append((snap.reference, {"is_smiley_game_banned": False}))
                    if len(pending) >= WRITE_BATCH_MAX:
                        updated_users += commit_updates(pending)
                        pending.clear()

            if LIMIT_USERS and processed_users >= LIMIT_USERS:
                break

        if pending and not DRY_RUN:
            updated_users += commit_updates(pending)
            pending.clear()

        if LIMIT_USERS and processed_users >= LIMIT_USERS:
            break

        last_snap = page[-1]

    return processed_users, updated_users

def main():
    processed_days = 0
    total_users = 0
    total_updated = 0
    last_day_snap = None

    print(f"Project={PROJECT_ID} DRY_RUN={DRY_RUN} BATCH_SIZE={BATCH_SIZE} WRITE_BATCH_MAX={WRITE_BATCH_MAX} LIMIT_DAYS={LIMIT_DAYS or '∞'} LIMIT_USERS={LIMIT_USERS or '∞'}", flush=True)

    while True:
        def fetch_day_page():
            q = db.collection(DAILY_COLL).order_by("__name__").limit(DAY_BATCH_SIZE)
            if last_day_snap is not None:
                q = q.start_after(last_day_snap)
            return list(q.stream(timeout=60))

        day_page = with_retry(fetch_day_page, what="fetch days page")
        if not day_page:
            break

        for day_doc in day_page:
            processed_days += 1
            day_ref = db.document(day_doc.reference.path)
            print(f"[Day {processed_days}] {day_ref.id}", flush=True)
            day_users, day_updated = process_day(day_ref)
            total_users += day_users
            total_updated += day_updated
            print(f"  processed_users={day_users} updated={day_updated}", flush=True)

            if LIMIT_DAYS and processed_days >= LIMIT_DAYS:
                break

        if LIMIT_DAYS and processed_days >= LIMIT_DAYS:
            break

        last_day_snap = day_page[-1]

    print(f"✅ Done. Days={processed_days}, Users scanned={total_users}, Updated={total_updated}, DRY_RUN={DRY_RUN}", flush=True)

if __name__ == "__main__":
    main()
