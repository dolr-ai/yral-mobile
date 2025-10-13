#!/usr/bin/env python3
"""
video_smiley_counts.py
────────────────────────────
Usage
    python video_smiley_counts.py <PROJECT_ID> > tallies.csv

Exports smiley tallies for all videos (no vote counting, no ranking).
Optimized with parallel fetching for much faster execution.
Also includes a "total" column that sums all smiley counts.
"""

import csv, sys, collections, firebase_admin
from concurrent.futures import ThreadPoolExecutor, as_completed
from firebase_admin import credentials, firestore

# ── CLI args ───────────────────────────────────────────────────────────
if len(sys.argv) != 2:
    sys.exit("Usage: python video_smiley_counts.py <PROJECT_ID>")
PROJECT_ID = sys.argv[1]

# ── Admin SDK init ─────────────────────────────────────────────────────
if not firebase_admin._apps:
    firebase_admin.initialize_app(
        credentials.ApplicationDefault(),
        {"projectId": PROJECT_ID}
    )
db = firestore.client()

# ── get global smiley list ─────────────────────────────────────────────
CONFIG_PATH = "config/smiley_game_v2"
smileys = (db.document(CONFIG_PATH).get().get("available_smileys") or [])
smiley_ids = [s["id"] for s in smileys]
smiley_ids.append("heart")  # ensure consistent column ordering

# ── CSV writer on stdout ───────────────────────────────────────────────
writer = csv.writer(sys.stdout)
writer.writerow(["video_id"] + smiley_ids + ["total"])

# ── helper function to fetch tallies for one video ─────────────────────
def fetch_tally(vid: str):
    tally = collections.Counter()
    for shard_snap in db.collection(f"videos/{vid}/tallies").get():
        tally.update(shard_snap.to_dict() or {})
    total = sum(tally.get(sm, 0) for sm in smiley_ids)
    return vid, tally, total

# ── main execution ─────────────────────────────────────────────────────
print("🔍 Fetching smiley tallies for all videos (parallel mode)...", file=sys.stderr)
videos = [snap.id for snap in db.collection("videos").stream()]
total_videos = len(videos)
print(f"📦 Found {total_videos} videos", file=sys.stderr)

max_workers = 30  # adjust 10–30 based on network speed
with ThreadPoolExecutor(max_workers=max_workers) as executor:
    futures = {executor.submit(fetch_tally, vid): vid for vid in videos}
    for i, f in enumerate(as_completed(futures), 1):
        vid, tally, total = f.result()
        writer.writerow([vid] + [tally.get(sm, 0) for sm in smiley_ids] + [total])

        if i % 500 == 0 or i == total_videos:
            print(f"✅ Processed {i}/{total_videos} videos...", file=sys.stderr, flush=True)

print(f"🏁 Export complete. {total_videos} videos written.", file=sys.stderr)