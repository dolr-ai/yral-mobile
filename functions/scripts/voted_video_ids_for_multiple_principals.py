#!/usr/bin/env python3
"""
voted_video_ids_for_multiple_principals.py
─────────────────────────
Fetch unique video IDs for a hardcoded list of principals (reason ∈ {"WIN","LOSS"})
and write a WIDE CSV:
  <principal_1>,<principal_2>,...,<principal_n>
  vid_a1,       vid_b1,       ...,vid_n1
  vid_a2,       vid_b2,       ...,vid_n2
  ...

USAGE
  # write to file
  python voted_video_ids_for_multiple_principals.py <PROJECT_ID> out.csv

  # or write to stdout (redirect yourself)
  python voted_video_ids_for_multiple_principals.py <PROJECT_ID> > wide_videos.csv

AUTH
  Uses Application Default Credentials. Either:
    - gcloud auth application-default login
      (and optionally: gcloud config set project <PROJECT_ID>)
    - or set GOOGLE_APPLICATION_CREDENTIALS to a service-account JSON.
"""

import csv
import sys
import firebase_admin
from firebase_admin import credentials, firestore

# ── Hardcode your principals here ──────────────────────────────────────
PRINCIPAL_IDS = [
    ""
]

REASONS = ["WIN", "LOSS"]  # reasons to include

def main():
    if len(sys.argv) < 2:
        sys.exit("Usage: python voted_video_ids_for_multiple_principals.py <PROJECT_ID> [out.csv]")

    PROJECT_ID = sys.argv[1].strip()
    OUT_PATH   = sys.argv[2].strip() if len(sys.argv) >= 3 else None

    if not PRINCIPAL_IDS:
        sys.exit("No principal IDs provided. Populate PRINCIPAL_IDS in the script.")

    # ── Init Admin SDK with ADC or Service Account ─────────────────────
    firebase_admin.initialize_app(
        credentials.ApplicationDefault(),
        {"projectId": PROJECT_ID}
    )
    db = firestore.client()

    # pid -> sorted list of unique video_ids
    per_pid_videos = {}
    total_scanned = 0

    for idx, pid in enumerate(PRINCIPAL_IDS, start=1):
        tx_coll = db.collection("users").document(pid).collection("transactions")
        q = tx_coll.where("reason", "in", REASONS)

        unique_vids = set()
        scanned = 0

        # Stream all relevant transactions. No order_by → no composite index needed.
        for snap in q.stream(timeout=120):
            scanned += 1
            data = snap.to_dict() or {}
            vid = data.get("video_id")
            if isinstance(vid, str) and vid:
                unique_vids.add(vid)
            if scanned % 1000 == 0:
                print(f"[{pid}] …scanned {scanned} tx rows, unique videos so far: {len(unique_vids)}",
                      file=sys.stderr, flush=True)

        sorted_vids = sorted(unique_vids)
        per_pid_videos[p] = sorted_vids if (p := pid) else []  # keep stable key assignment

        total_scanned += scanned
        print(f"✅ [{idx}/{len(PRINCIPAL_IDS)}] {pid}: scanned {scanned} tx; "
              f"{len(sorted_vids)} unique video IDs.", file=sys.stderr)

    # ── Build wide rows: each principal is a column, rows are aligned by index ──
    headers = PRINCIPAL_IDS[:]  # one column per principal
    max_len = max((len(per_pid_videos.get(pid, [])) for pid in PRINCIPAL_IDS), default=0)

    # Prepare row-wise data
    wide_rows = []
    for i in range(max_len):
        row = []
        for pid in PRINCIPAL_IDS:
            vids = per_pid_videos.get(pid, [])
            row.append(vids[i] if i < len(vids) else "")
        wide_rows.append(row)

    # ── Write CSV ───────────────────────────────────────────────────────
    if OUT_PATH:
        with open(OUT_PATH, "w", newline="") as f:
            w = csv.writer(f)
            w.writerow(headers)
            w.writerows(wide_rows)
    else:
        w = csv.writer(sys.stdout)
        w.writerow(headers)
        w.writerows(wide_rows)

    total_pairs = sum(len(v) for v in per_pid_videos.values())
    print(f"🎉 Done. Scanned {total_scanned} transactions across "
          f"{len(PRINCIPAL_IDS)} principals; wrote {total_pairs} video IDs in wide format.",
          file=sys.stderr)

if __name__ == "__main__":
    main()