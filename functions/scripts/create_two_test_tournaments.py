"""
Script to create 2 test tournaments on staging:
- 1 Smiley tournament
- 1 Hot or Not tournament

Both start 10 mins from now, last 10 mins, 5 YRAL points entry, 5rs prize pool.
Also schedules Cloud Tasks for status transitions (scheduled -> live -> ended).

Run from functions directory:
    python3 scripts/create_two_test_tournaments.py
"""

import sys
import subprocess
import requests
import requests
import json
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

# Project config
PROJECT_ID = "yral-staging"
REGION = "us-central1"
QUEUE_NAME = "tournament-status-updates"

FIRESTORE_BASE = f"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents"
TASKS_BASE = f"https://cloudtasks.googleapis.com/v2/projects/{PROJECT_ID}/locations/{REGION}/queues/{QUEUE_NAME}/tasks"

def create_tournament(access_token, collection, doc_id, tournament_data):
    """Create a tournament document using Firestore REST API."""
    url = f"{FIRESTORE_BASE}/{collection}?documentId={doc_id}"
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


def schedule_status_task(access_token, tournament_id, target_status, run_at):
    """Schedule a Cloud Task to update tournament status."""
    url = TASKS_BASE
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    function_url = f"https://{REGION}-{PROJECT_ID}.cloudfunctions.net/update_tournament_status"
    payload = json.dumps({"tournament_id": tournament_id, "status": target_status})

    # Convert to RFC3339 format
    schedule_time = run_at.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    task = {
        "httpRequest": {
            "httpMethod": "POST",
            "url": function_url,
            "headers": {"Content-Type": "application/json"},
            "body": payload.encode().hex()  # Cloud Tasks API wants base64, but hex works too
        },
        "scheduleTime": schedule_time
    }

    # Actually Cloud Tasks REST API wants body as base64
    import base64
    task["httpRequest"]["body"] = base64.b64encode(payload.encode()).decode()

    response = requests.post(url, headers=headers, json={"task": task})
    if response.status_code in [200, 201]:
        return True, None
    else:
        return False, response.text


# IST timezone
IST = timezone(timedelta(hours=5, minutes=30))

# Get current time in IST
now_ist = datetime.now(IST)
today_str = now_ist.strftime("%Y-%m-%d")

# Tournament timing: start 10 mins from now, last 10 mins
start_dt = now_ist + timedelta(minutes=10)
end_dt = start_dt + timedelta(minutes=10)

start_time = start_dt.strftime("%H:%M")
end_time = end_dt.strftime("%H:%M")

# Entry and prize config
ENTRY_COST = 5
TOTAL_PRIZE_POOL = 5
PRIZE_MAP = {
    "1": 3,
    "2": 2
}

# Unique suffix for doc IDs
unique_suffix = now_ist.strftime("%H-%M-%S")

print("Getting access token...")
access_token = get_access_token()
print("Got access token")
print()

print(f"Creating 2 test tournaments on yral-staging...")
print(f"Start: {start_time} IST (in ~10 mins)")
print(f"End: {end_time} IST")
print(f"Entry Cost: {ENTRY_COST} YRAL points")
print(f"Prize Pool: {TOTAL_PRIZE_POOL}rs (1st: 3rs, 2nd: 2rs)")
print()

tournaments_created = []

# === SMILEY TOURNAMENT ===
smiley_doc_id = f"test-smiley-{today_str}-{unique_suffix}"
smiley_data = {
    "title": "Test Smiley Tournament",
    "type": "smiley",
    "date": today_str,
    "start_time": start_time,
    "end_time": end_time,
    "start_at": start_dt,
    "end_at": end_dt,
    "start_epoch_ms": int(start_dt.timestamp() * 1000),
    "end_epoch_ms": int(end_dt.timestamp() * 1000),
    "entryCost": ENTRY_COST,
    "totalPrizePool": TOTAL_PRIZE_POOL,
    "status": "scheduled",
    "prizeMap": PRIZE_MAP,
    "participant_count": 0,
}

success, error = create_tournament(access_token, "tournaments", smiley_doc_id, smiley_data)
if success:
    print(f"Smiley Tournament created!")
    print(f"  ID: {smiley_doc_id}")
    print(f"  Collection: tournaments")
    tournaments_created.append(("smiley", smiley_doc_id))
else:
    print(f"Failed to create Smiley tournament: {error}")
    sys.exit(1)

print()

# === HOT OR NOT TOURNAMENT ===
hon_doc_id = f"test-hot-or-not-{today_str}-{unique_suffix}"
hon_data = {
    "title": "Test Hot or Not Tournament",
    "type": "hot_or_not",
    "date": today_str,
    "start_time": start_time,
    "end_time": end_time,
    "start_at": start_dt,
    "end_at": end_dt,
    "start_epoch_ms": int(start_dt.timestamp() * 1000),
    "end_epoch_ms": int(end_dt.timestamp() * 1000),
    "entryCost": ENTRY_COST,
    "totalPrizePool": TOTAL_PRIZE_POOL,
    "status": "scheduled",
    "prizeMap": PRIZE_MAP,
    "participant_count": 0,
    "video_count": 0,
}

success, error = create_tournament(access_token, "hot_or_not_tournaments", hon_doc_id, hon_data)
if success:
    print(f"Hot or Not Tournament created!")
    print(f"  ID: {hon_doc_id}")
    print(f"  Collection: hot_or_not_tournaments")
    tournaments_created.append(("hot_or_not", hon_doc_id))
else:
    print(f"Failed to create Hot or Not tournament: {error}")
    sys.exit(1)

print()

# === SCHEDULE STATUS TRANSITIONS ===
print("Scheduling status transitions...")
print()

for tournament_type, doc_id in tournaments_created:
    # Schedule LIVE transition at start time
    success, error = schedule_status_task(access_token, doc_id, "live", start_dt)
    if success:
        print(f"  {doc_id}: LIVE at {start_time} IST")
    else:
        print(f"  {doc_id}: Failed to schedule LIVE task: {error}")

    # Schedule ENDED transition at end time
    success, error = schedule_status_task(access_token, doc_id, "ended", end_dt)
    if success:
        print(f"  {doc_id}: ENDED at {end_time} IST")
    else:
        print(f"  {doc_id}: Failed to schedule ENDED task: {error}")

print()
print("Both tournaments created with status transitions scheduled!")
print()
print(f"Smiley Tournament ID: {smiley_doc_id}")
print(f"Hot or Not Tournament ID: {hon_doc_id}")
