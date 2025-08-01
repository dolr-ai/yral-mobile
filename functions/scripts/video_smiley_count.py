#!/usr/bin/env python3
"""
export_video_smiley_counts.py
────────────────────────────
Usage
    python export_video_smiley_counts.py  <PROJECT_ID>  > tallies.csv

The script prints CSV to STDOUT:

video_id,heart,laugh,fire,cry,...
97b0c996f3…,4,10,2,0,...
fd22817eba…,1,5,3,2,...
"""

import csv, sys, collections, firebase_admin
from firebase_admin import credentials, firestore

# ── CLI args ───────────────────────────────────────────────────────────
if len(sys.argv) != 2:
    sys.exit("Usage: python export_video_smiley_counts.py <PROJECT_ID>")
PROJECT_ID = sys.argv[1]

# ── Admin SDK init (ADC or SERVICE_ACCOUNT json) ───────────────────────
firebase_admin.initialize_app(
    credentials.ApplicationDefault(),
    {"projectId": PROJECT_ID}
)
db = firestore.client()

# ── get global smiley list for consistent column order ─────────────────
CONFIG_PATH = "config/smiley_game_v1"
smileys = (db.document(CONFIG_PATH).get().get("available_smileys") or [])
smiley_ids = [s["id"] for s in smileys]              # e.g. ["heart","laugh","fire",…]

# ── CSV writer on stdout ───────────────────────────────────────────────
writer = csv.writer(sys.stdout)
writer.writerow(["video_id"] + smiley_ids)            # header row

# ── iterate every video doc & aggregate its shards ─────────────────────
video_counter = 1
videos_col = db.collection("videos")
for vid_snap in videos_col.stream():
    vid = vid_snap.id
    tally = collections.Counter()

    # sum shard_* docs
    for shard_snap in db.collection(f"videos/{vid}/tallies").stream():
        tally.update(shard_snap.to_dict() or {})

    # emit row (0 for any missing smiley)
    writer.writerow([vid] + [tally.get(sm, 0) for sm in smiley_ids])
    print(f"Done for video {video_counter}", file=sys.stderr, flush=True)
    video_counter += 1

print(f"✅  Exported {videos_col.count().get()[0][0].value} videos", file=sys.stderr)