"""
Script to create 60 tournaments, each lasting 1 minute, for the next hour.
Run from functions directory with venv activated:
    python scripts/create_minute_tournaments.py
"""

import sys
import subprocess
import requests
from datetime import datetime, timezone, timedelta

# Get gcloud access token
def get_access_token():
    result = subprocess.run(
        ["gcloud", "auth", "print-access-token"],
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        print(f"Failed to get access token: {result.stderr}")
        print("\nRun: gcloud auth login")
        sys.exit(1)
    return result.stdout.strip()

# Firestore REST API base URL
PROJECT_ID = "yral-staging"
FIRESTORE_BASE = f"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents"

def create_tournament(access_token, doc_id, tournament_data):
    """Create a tournament document using Firestore REST API."""
    url = f"{FIRESTORE_BASE}/tournaments?documentId={doc_id}"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    # Convert to Firestore document format
    fields = {}
    for key, value in tournament_data.items():
        if isinstance(value, str):
            fields[key] = {"stringValue": value}
        elif isinstance(value, int):
            fields[key] = {"integerValue": str(value)}
        elif isinstance(value, float):
            fields[key] = {"doubleValue": value}
        elif isinstance(value, dict):
            map_fields = {}
            for k, v in value.items():
                map_fields[k] = {"integerValue": str(v)}
            fields[key] = {"mapValue": {"fields": map_fields}}
        elif isinstance(value, datetime):
            fields[key] = {"timestampValue": value.isoformat()}

    body = {"fields": fields}

    response = requests.post(url, headers=headers, json=body)
    if response.status_code in [200, 201]:
        return True, None
    else:
        return False, response.text

# IST timezone (GMT+5:30)
IST = timezone(timedelta(hours=5, minutes=30))

# Get current time in IST
now_ist = datetime.now(IST)
print(f"Current time (IST): {now_ist.strftime('%Y-%m-%d %H:%M:%S')}")

# Start from the next minute
start_time = (now_ist + timedelta(minutes=1)).replace(second=0, microsecond=0)

# Create 60 tournaments, each 1 minute long
TOURNAMENT_COUNT = 60
TOURNAMENT_DURATION_MINS = 1

slots = []
for i in range(TOURNAMENT_COUNT):
    slot_start = start_time + timedelta(minutes=i)
    slot_end = slot_start + timedelta(minutes=TOURNAMENT_DURATION_MINS)
    slots.append((slot_start, slot_end))

print(f"First tournament: {slots[0][0].strftime('%H:%M:%S')} - {slots[0][1].strftime('%H:%M:%S')} IST")
print(f"Last tournament: {slots[-1][0].strftime('%H:%M:%S')} - {slots[-1][1].strftime('%H:%M:%S')} IST")
print(f"Total tournaments: {len(slots)}")
print()

# Confirm before proceeding
if "--yes" in sys.argv:
    confirm = "yes"
else:
    confirm = input("Proceed? (yes/no): ").strip().lower()
if confirm != "yes":
    print("Cancelled")
    sys.exit(0)

print()
print("Getting access token...")
access_token = get_access_token()
print("Got access token")
print()

created_count = 0
failed_count = 0

for start_dt, end_dt in slots:
    date_str = start_dt.strftime("%Y-%m-%d")
    start_time_str = start_dt.strftime("%H-%M")

    # Document ID
    doc_id = f"{date_str}-{start_time_str}"

    tournament_data = {
        "title": "Test Tournament",
        "date": date_str,
        "start_time": start_dt.strftime("%H:%M"),
        "end_time": end_dt.strftime("%H:%M"),
        "start_epoch_ms": int(start_dt.timestamp() * 1000),
        "end_epoch_ms": int(end_dt.timestamp() * 1000),
        "entryCost": 10,
        "totalPrizePool": 16,
        "status": "scheduled",
        "participant_count": 0,
        "prizeMap": {
            "1": 10,
            "2": 5,
            "3": 1
        }
    }

    success, error = create_tournament(access_token, doc_id, tournament_data)
    if success:
        print(f"Created: {doc_id} ({start_dt.strftime('%H:%M')} - {end_dt.strftime('%H:%M')})")
        created_count += 1
    else:
        print(f"Failed: {doc_id} - {error}")
        failed_count += 1

print()
print(f"Done! Created: {created_count}, Failed: {failed_count}")
