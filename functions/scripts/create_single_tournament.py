"""
Script to create a single tournament.
Run from functions directory with venv activated:
    python scripts/create_single_tournament.py
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

# Tournament config
TITLE = "New Year Bonanza"
DATE = "2025-12-30"
START_HOUR = 15  # 3 PM
START_MINUTE = 0
DURATION_MINS = 15
ENTRY_COST = 100
PRIZE_MAP = {
    "1": 10,
    "2": 5,
    "3": 1
}
TOTAL_PRIZE_POOL = sum(PRIZE_MAP.values())

# Calculate times
start_dt = datetime(2025, 12, 30, START_HOUR, START_MINUTE, 0, tzinfo=IST)
end_dt = start_dt + timedelta(minutes=DURATION_MINS)

print(f"Tournament: {TITLE}")
print(f"Date: {DATE}")
print(f"Time: {start_dt.strftime('%H:%M')} - {end_dt.strftime('%H:%M')} IST ({DURATION_MINS} mins)")
print(f"Entry Cost: {ENTRY_COST}")
print(f"Prize Pool: {TOTAL_PRIZE_POOL}")
print()

# Document ID
doc_id = f"{DATE}-{start_dt.strftime('%H-%M')}-bonanza"

print("Getting access token...")
access_token = get_access_token()
print("Got access token")
print()

tournament_data = {
    "title": TITLE,
    "date": DATE,
    "start_time": start_dt.strftime("%H:%M"),
    "end_time": end_dt.strftime("%H:%M"),
    "start_epoch_ms": int(start_dt.timestamp() * 1000),
    "end_epoch_ms": int(end_dt.timestamp() * 1000),
    "entryCost": ENTRY_COST,
    "totalPrizePool": TOTAL_PRIZE_POOL,
    "status": "scheduled",
    "participant_count": 0,
    "prizeMap": PRIZE_MAP
}

success, error = create_tournament(access_token, doc_id, tournament_data)
if success:
    print(f"Created tournament: {doc_id}")
    print(f"  Title: {TITLE}")
    print(f"  Time: {start_dt.strftime('%H:%M')} - {end_dt.strftime('%H:%M')} IST")
else:
    print(f"Failed to create tournament: {error}")
