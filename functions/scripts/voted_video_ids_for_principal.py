#!/usr/bin/env python3
"""
export_voted_video_ids.py
─────────────────────────
Fetch all unique video IDs for a principal based on their transactions
with reason ∈ {"WIN","LOSS"} and write them to a CSV.

USAGE
  # write to file
  python export_voted_video_ids.py <PROJECT_ID> <PRINCIPAL_ID> out.csv

  # or write to stdout (redirect yourself)
  python export_voted_video_ids.py <PROJECT_ID> <PRINCIPAL_ID>  > my_videos.csv

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

def main():
    if len(sys.argv) < 3:
        sys.exit("Usage: python export_voted_video_ids.py <PROJECT_ID> <PRINCIPAL_ID> [out.csv]")

    PROJECT_ID   = sys.argv[1].strip()
    PRINCIPAL_ID = sys.argv[2].strip()
    OUT_PATH     = sys.argv[3].strip() if len(sys.argv) >= 4 else None

    # ── Init Admin SDK with ADC or Service Account ─────────────────────
    firebase_admin.initialize_app(
        credentials.ApplicationDefault(),
        {"projectId": PROJECT_ID}
    )
    db = firestore.client()

    tx_coll = db.collection("users").document(PRINCIPAL_ID).collection("transactions")

    # Only consider vote results (WIN / LOSS). We do NOT order to avoid needing a composite index.
    q = tx_coll.where("reason", "in", ["WIN", "LOSS"])

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
            print(f"…scanned {scanned} tx rows, unique videos so far: {len(unique_vids)}", file=sys.stderr, flush=True)

    rows = sorted(unique_vids)

    # ── Write CSV ───────────────────────────────────────────────────────
    if OUT_PATH:
        with open(OUT_PATH, "w", newline="") as f:
            w = csv.writer(f)
            w.writerow(["video_id"])
            for vid in rows:
                w.writerow([vid])
    else:
        w = csv.writer(sys.stdout)
        w.writerow(["video_id"])
        for vid in rows:
            w.writerow([vid])

    print(f"✅ Scanned {scanned} transactions; wrote {len(rows)} unique video IDs.", file=sys.stderr)

if __name__ == "__main__":
    main()