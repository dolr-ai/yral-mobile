"""
Script to create tournaments every 10 minutes, each lasting 10 minutes, until 12 AM IST.
Run from functions directory with venv activated:
    python scripts/create_10min_tournaments.py

Prize: 1st = ₹10, 2nd = ₹5, 3rd = ₹1
Uses Firestore REST API with gcloud access token.
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

# Calculate the next 10-minute mark
current_minute = now_ist.minute
next_10_min = ((current_minute // 10) + 1) * 10
if next_10_min >= 60:
    next_slot = (now_ist + timedelta(hours=1)).replace(minute=0, second=0, microsecond=0)
else:
    next_slot = now_ist.replace(minute=next_10_min, second=0, microsecond=0)

# End time: 12 AM IST tomorrow (midnight)
tomorrow = now_ist.date() + timedelta(days=1)
end_time_ist = datetime(tomorrow.year, tomorrow.month, tomorrow.day, 0, 0, 0, tzinfo=IST)

print(f"First tournament starts: {next_slot.strftime('%Y-%m-%d %H:%M:%S')} IST")
print(f"Last tournament ends at: {end_time_ist.strftime('%Y-%m-%d %H:%M:%S')} IST")

# Calculate all 10-minute slots
TOURNAMENT_DURATION_MINS = 10
TOURNAMENT_INTERVAL_MINS = 10
slots = []
current_slot = next_slot
while current_slot < end_time_ist:
    slot_end = current_slot + timedelta(minutes=TOURNAMENT_DURATION_MINS)
    if slot_end <= end_time_ist:
        slots.append((current_slot, slot_end))
    current_slot = current_slot + timedelta(minutes=TOURNAMENT_INTERVAL_MINS)

print(f"Total tournaments to create: {len(slots)}")
print()

if len(slots) == 0:
    print("No tournaments to create.")
    sys.exit(0)

# Show tournaments to be created
print("Tournaments to be created:")
for start_dt, end_dt in slots:
    print(f"  - {start_dt.strftime('%Y-%m-%d')} : {start_dt.strftime('%H:%M')} - {end_dt.strftime('%H:%M')} IST")
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
        "title": "Smiley Showdown",
        "date": date_str,
        "start_time": start_dt.strftime("%H:%M"),
        "end_time": end_dt.strftime("%H:%M"),
        "start_epoch_ms": int(start_dt.timestamp() * 1000),
        "end_epoch_ms": int(end_dt.timestamp() * 1000),
        "entryCost": 100,
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
