#!/usr/bin/env python3
"""
fix_laugh_tallies.py
────────────────────
Usage:
    python fix_laugh_tallies.py <PROJECT_ID>

For every video:
  • If 'laugh' has the highest total count among smileys,
    then set:
        shard_0.laugh = min(other smiley counts)
        all other shards' laugh = 0

This ensures the total laugh count = min(other smiley counts).
"""

import sys, collections, firebase_admin
from concurrent.futures import ThreadPoolExecutor, as_completed
from firebase_admin import credentials, firestore

# ── CLI args ───────────────────────────────────────────────────────────
if len(sys.argv) != 2:
    sys.exit("Usage: python fix_laugh_tallies.py <PROJECT_ID>")
PROJECT_ID = sys.argv[1]

# ── Firestore init ─────────────────────────────────────────────────────
if not firebase_admin._apps:
    firebase_admin.initialize_app(
        credentials.ApplicationDefault(),
        {"projectId": PROJECT_ID}
    )
db = firestore.client()

# ── load smiley config ─────────────────────────────────────────────────
CONFIG_PATH = "config/smiley_game_v2"
smileys = (db.document(CONFIG_PATH).get().get("available_smileys") or [])
smiley_ids = [s["id"] for s in smileys]

# ── helper: process a single video ─────────────────────────────────────
def process_video(vid: str):
    tallies_ref = db.collection(f"videos/{vid}/tallies")
    tally = collections.Counter()

    shards = list(tallies_ref.get())
    if not shards:
        return None

    for shard_snap in shards:
        tally.update(shard_snap.to_dict() or {})

    # skip if no laugh or no tallies
    laugh_count = tally.get("laugh", 0)
    if laugh_count == 0:
        return None

    # find if laugh is the highest
    max_smiley = max(tally, key=tally.get)
    if max_smiley != "laugh":
        return None

    # find minimum of other smiley counts
    other_counts = [v for k, v in tally.items() if k not in ("laugh", "heart")]
    if not other_counts:
        return None

    min_other = min(other_counts)
    if laugh_count <= min_other:
        return None  # already fine

    # ✅ fix tallies: shard_0.laugh = min_other, others = 0
    for shard_snap in shards:
        ref = shard_snap.reference
        if ref.id == "shard_0":
            ref.update({"laugh": min_other})
        else:
            ref.update({"laugh": 0})

    return (vid, laugh_count, min_other)

# ── main logic ─────────────────────────────────────────────────────────
print("🔍 Scanning videos to fix 'laugh' tallies...", file=sys.stderr)
videos = [snap.id for snap in db.collection("videos").stream()]
total_videos = len(videos)
print(f"📦 Found {total_videos} videos", file=sys.stderr)

fixed_count = 0
max_workers = 30  # adjust based on network speed

with ThreadPoolExecutor(max_workers=max_workers) as executor:
    futures = {executor.submit(process_video, vid): vid for vid in videos}
    for i, f in enumerate(as_completed(futures), 1):
        result = f.result()
        if result:
            vid, old_laugh, new_laugh = result
            fixed_count += 1
            print(f"🛠️  Fixed {vid}: laugh {old_laugh} → {new_laugh}", file=sys.stderr)

        if i % 500 == 0 or i == total_videos:
            print(f"✅ Processed {i}/{total_videos} videos", file=sys.stderr, flush=True)

print(f"🏁 Completed. Updated {fixed_count} videos.", file=sys.stderr)