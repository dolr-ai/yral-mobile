#!/usr/bin/env python3
"""
backfill_smiley_game_win_loss.py
Counts WIN / LOSS transactions per user and writes
    smiley_game_wins / smiley_game_losses
onto users/{pid}.
"""

import firebase_admin
from firebase_admin import credentials, firestore
from google.cloud.firestore_v1 import FieldFilter

# ───────────────── Admin SDK ───────────────────────────────────────────
cred = credentials.ApplicationDefault()                # or GOOGLE_APPLICATION_CREDENTIALS
firebase_admin.initialize_app(cred, {"projectId": "yral-staging"})  # prod id if needed
db = firestore.client()

# ───────────────── Pagination settings ─────────────────────────────────
BATCH_SIZE = 1_000          # users per RPC page
processed  = 0
last_doc   = None           # page token

while True:
    # read one page of users ordered by doc-name
    page_q = db.collection("users").order_by("__name__").limit(BATCH_SIZE)
    if last_doc:
        page_q = page_q.start_after(last_doc)

    users_page = list(page_q.stream(timeout=60))   # 60-s per page
    if not users_page:
        break                                      # finished all users

    for user_snap in users_page:
        processed += 1
        pid      = user_snap.id
        user_ref = user_snap.reference
        tx_coll  = user_ref.collection("transactions")

        # ---- WIN count --------------------------------------------------
        win_rows = (
            tx_coll.where(filter=FieldFilter("reason", "==", "WIN"))
                   .count()
                   .get(timeout=30)              # aggregation RPC
        )
        wins = int(win_rows[0][0].value)

        # ---- LOSS count -------------------------------------------------
        loss_rows = (
            tx_coll.where(filter=FieldFilter("reason", "==", "LOSS"))
                   .count()
                   .get(timeout=30)
        )
        losses = int(loss_rows[0][0].value)

        # Skip users still at 0 / 0
        if wins == 0 and losses == 0:
            print("skipped")
            continue

        user_ref.update({
            "smiley_game_wins":   wins,
            "smiley_game_losses": losses
        })
        print(f"{pid}: wins={wins}  losses={losses}")

    # progress every 1 000 users
    if processed % 1_000 == 0:
        print(f"processed {processed} users…", flush=True)

    last_doc = users_page[-1]        # set page token for next loop

print("Back-fill complete ✅")