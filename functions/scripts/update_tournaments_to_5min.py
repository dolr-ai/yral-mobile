"""
Script to update scheduled tournaments from 10 minutes to 5 minutes duration.
Run from functions directory with venv activated:
    python scripts/update_tournaments_to_5min.py

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

# IST timezone (GMT+5:30)
IST = timezone(timedelta(hours=5, minutes=30))

def get_tournaments_for_date(access_token, date_str):
    """Get all tournaments for a specific date."""
    url = f"{FIRESTORE_BASE}:runQuery"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    body = {
        "structuredQuery": {
            "from": [{"collectionId": "tournaments"}],
            "where": {
                "fieldFilter": {
                    "field": {"fieldPath": "date"},
                    "op": "EQUAL",
                    "value": {"stringValue": date_str}
                }
            }
        }
    }

    response = requests.post(url, headers=headers, json=body)
    if response.status_code != 200:
        print(f"Failed to query tournaments: {response.text}")
        return []

    results = response.json()
    tournaments = []

    for result in results:
        if "document" in result:
            doc = result["document"]
            name = doc["name"]
            doc_id = name.split("/")[-1]
            fields = doc.get("fields", {})

            # Extract fields
            tournament = {
                "id": doc_id,
                "date": fields.get("date", {}).get("stringValue", ""),
                "start_time": fields.get("start_time", {}).get("stringValue", ""),
                "end_time": fields.get("end_time", {}).get("stringValue", ""),
                "start_epoch_ms": int(fields.get("start_epoch_ms", {}).get("integerValue", 0)),
                "end_epoch_ms": int(fields.get("end_epoch_ms", {}).get("integerValue", 0)),
                "status": fields.get("status", {}).get("stringValue", ""),
            }
            tournaments.append(tournament)

    return tournaments

def update_tournament(access_token, doc_id, new_end_time, new_end_epoch_ms):
    """Update a tournament's end time."""
    url = f"{FIRESTORE_BASE}/tournaments/{doc_id}?updateMask.fieldPaths=end_time&updateMask.fieldPaths=end_epoch_ms"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

    body = {
        "fields": {
            "end_time": {"stringValue": new_end_time},
            "end_epoch_ms": {"integerValue": str(new_end_epoch_ms)}
        }
    }

    response = requests.patch(url, headers=headers, json=body)
    if response.status_code == 200:
        return True, None
    else:
        return False, response.text

def main():
    # Get current time in IST
    now_ist = datetime.now(IST)
    today_str = now_ist.strftime("%Y-%m-%d")

    print(f"Current time (IST): {now_ist.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Looking for tournaments on: {today_str}")
    print()

    print("Getting access token...")
    access_token = get_access_token()
    print("Got access token")
    print()

    # Get all tournaments for today
    tournaments = get_tournaments_for_date(access_token, today_str)

    if not tournaments:
        print("No tournaments found for today.")
        return

    print(f"Found {len(tournaments)} tournaments for today")
    print()

    # Filter to only scheduled tournaments that are 10 minutes long
    # and haven't started yet
    now_ms = int(now_ist.timestamp() * 1000)

    to_update = []
    for t in tournaments:
        duration_ms = t["end_epoch_ms"] - t["start_epoch_ms"]
        duration_mins = duration_ms / (1000 * 60)

        # Only update if:
        # 1. Currently scheduled (hasn't started yet)
        # 2. Duration is 10 minutes
        if t["start_epoch_ms"] > now_ms and abs(duration_mins - 10) < 0.1:
            # Calculate new end time (5 minutes instead of 10)
            new_end_epoch_ms = t["start_epoch_ms"] + (5 * 60 * 1000)  # 5 minutes in ms

            # Parse start time to calculate new end time string
            start_dt = datetime.fromtimestamp(t["start_epoch_ms"] / 1000, tz=IST)
            new_end_dt = start_dt + timedelta(minutes=5)
            new_end_time = new_end_dt.strftime("%H:%M")

            to_update.append({
                **t,
                "duration_mins": duration_mins,
                "new_end_time": new_end_time,
                "new_end_epoch_ms": new_end_epoch_ms
            })

    if not to_update:
        print("No scheduled 10-minute tournaments found that need updating.")
        print()
        print("Tournament breakdown:")
        for t in tournaments:
            duration_ms = t["end_epoch_ms"] - t["start_epoch_ms"]
            duration_mins = duration_ms / (1000 * 60)
            status = "scheduled" if t["start_epoch_ms"] > now_ms else ("live" if t["end_epoch_ms"] > now_ms else "ended")
            print(f"  - {t['id']}: {t['start_time']} - {t['end_time']} ({duration_mins:.0f} min) [{status}]")
        return

    print(f"Tournaments to update from 10 min to 5 min: {len(to_update)}")
    print()
    print("Changes to be made:")
    for t in to_update:
        print(f"  - {t['id']}: {t['start_time']} - {t['end_time']} ({t['duration_mins']:.0f} min) -> {t['start_time']} - {t['new_end_time']} (5 min)")
    print()

    # Confirm before proceeding
    if "--yes" in sys.argv:
        confirm = "yes"
    else:
        confirm = input("Proceed with update? (yes/no): ").strip().lower()

    if confirm != "yes":
        print("Cancelled")
        return

    print()
    print("Updating tournaments...")

    updated_count = 0
    failed_count = 0

    for t in to_update:
        success, error = update_tournament(
            access_token,
            t["id"],
            t["new_end_time"],
            t["new_end_epoch_ms"]
        )
        if success:
            print(f"Updated: {t['id']} ({t['start_time']} - {t['new_end_time']})")
            updated_count += 1
        else:
            print(f"Failed: {t['id']} - {error}")
            failed_count += 1

    print()
    print(f"Done! Updated: {updated_count}, Failed: {failed_count}")

if __name__ == "__main__":
    main()
