"""
Script to create "New Year Bonanza" tournaments every half hour until 12 AM IST.
Run from functions directory with venv activated:
    python scripts/create_half_hourly_tournaments.py

This creates 15-minute tournaments at every half-hour mark.
Prize: 1st = ₹10, 2nd = ₹5, 3rd = ₹1
Uses Firestore REST API with gcloud access token.
"""

import sys
import subprocess
import requests
from datetime import datetime, timezone, timedelta
import json

# Get gcloud access token
def get_access_token():
    result = subprocess.run(
        ["gcloud", "auth", "print-access-token"],
        capture_output=True,
        text=True
    )
    if result.returncode != 0:
        print(f"❌ Failed to get access token: {result.stderr}")
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
            # For prizeMap, convert to map
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
print(f"📅 Current time (IST): {now_ist.strftime('%Y-%m-%d %H:%M:%S')}")

# Calculate the next half-hour mark
if now_ist.minute < 30:
    next_slot = now_ist.replace(minute=30, second=0, microsecond=0)
else:
    next_slot = (now_ist + timedelta(hours=1)).replace(minute=0, second=0, microsecond=0)

# End time: 12 AM IST tomorrow (midnight)
tomorrow = now_ist.date() + timedelta(days=1)
end_time_ist = datetime(tomorrow.year, tomorrow.month, tomorrow.day, 0, 0, 0, tzinfo=IST)

print(f"🎯 First tournament starts: {next_slot.strftime('%Y-%m-%d %H:%M:%S')} IST")
print(f"🏁 Last tournament ends at: {end_time_ist.strftime('%Y-%m-%d %H:%M:%S')} IST")

# Calculate all half-hour slots (tournaments start every 30 mins, but last 15 mins)
TOURNAMENT_DURATION_MINS = 15
slots = []
current_slot = next_slot
while current_slot < end_time_ist:
    slot_end = current_slot + timedelta(minutes=TOURNAMENT_DURATION_MINS)
    if slot_end <= end_time_ist:
        slots.append((current_slot, slot_end))
    # Move to next half-hour mark
    current_slot = current_slot + timedelta(minutes=30)

print(f"📊 Total tournaments to create: {len(slots)}")
print()

if len(slots) == 0:
    print("No tournaments to create (already past midnight tomorrow).")
    sys.exit(0)

# Show tournaments to be created
print("Tournaments to be created:")
for start_dt, end_dt in slots:
    print(f"  - {start_dt.strftime('%Y-%m-%d')} : {start_dt.strftime('%H:%M')} - {end_dt.strftime('%H:%M')} IST")
print()

# Confirm before proceeding (use --yes to skip)
if "--yes" in sys.argv:
    confirm = "yes"
else:
    confirm = input("Do you want to proceed? (yes/no): ").strip().lower()
if confirm != "yes":
    print("❌ Cancelled")
    sys.exit(0)

print()
print("🔑 Getting access token...")
access_token = get_access_token()
print("✅ Got access token")
print()

created_count = 0
failed_count = 0

for start_dt, end_dt in slots:
    date_str = start_dt.strftime("%Y-%m-%d")
    start_time = start_dt.strftime("%H:%M")
    end_time = end_dt.strftime("%H:%M")

    # Document ID: date-starttime (e.g., 2025-12-27-14-30)
    doc_id = f"{date_str}-{start_time.replace(':', '-')}"

    # Tournament data - New Year Bonanza
    tournament_data = {
        "title": "New Year Bonanza",
        "date": date_str,
        "start_time": start_time,
        "end_time": end_time,
        "start_epoch_ms": int(start_dt.timestamp() * 1000),
        "end_epoch_ms": int(end_dt.timestamp() * 1000),
        "entryCost": 10,
        "totalPrizePool": 16,  # 10 + 5 + 1
        "status": "scheduled",
        "participant_count": 0,
    }

    # Prize map: 1st = ₹10, 2nd = ₹5, 3rd = ₹1
    tournament_data["prizeMap"] = {
        "1": 10,
        "2": 5,
        "3": 1
    }

    success, error = create_tournament(access_token, doc_id, tournament_data)
    if success:
        print(f"✅ Created: {doc_id} ({start_time} - {end_time} IST)")
        created_count += 1
    else:
        print(f"❌ Failed: {doc_id} - {error}")
        failed_count += 1

print()
print(f"🎉 Done! Created: {created_count}, Failed: {failed_count}")
