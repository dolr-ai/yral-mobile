"""
Script to create a test tournament for testing APIs.
Run from functions directory with venv activated:
    python scripts/create_test_tournament.py [status]

Examples:
    python scripts/create_test_tournament.py           # Creates SCHEDULED tournament
    python scripts/create_test_tournament.py live      # Creates LIVE tournament
    python scripts/create_test_tournament.py ended     # Creates ENDED tournament
"""

import sys
import firebase_admin
from firebase_admin import firestore, credentials
from datetime import datetime, timezone, timedelta
import google.auth

# Initialize Firebase with application default credentials
if not firebase_admin._apps:
    try:
        # Try to use application default credentials
        cred, project = google.auth.default()
        firebase_admin.initialize_app(options={'projectId': 'yral-staging'})
    except Exception as e:
        print(f"❌ Auth error: {e}")
        print("\nRun: gcloud auth application-default login")
        print("Then: gcloud config set project yral-staging")
        sys.exit(1)

db = firestore.client()

# IST timezone
IST = timezone(timedelta(hours=5, minutes=30))

# Get status from args (default: scheduled)
status = sys.argv[1].lower() if len(sys.argv) > 1 else "scheduled"
if status not in ["scheduled", "live", "ended", "settled", "cancelled"]:
    print(f"❌ Invalid status: {status}")
    print("   Valid options: scheduled, live, ended, settled, cancelled")
    sys.exit(1)

# Get current time in IST
now_ist = datetime.now(IST)
today_str = now_ist.strftime("%Y-%m-%d")

# Create unique tournament times based on current time
start_dt = now_ist - timedelta(minutes=5)  # Started 5 mins ago
end_dt = now_ist + timedelta(hours=2)       # Ends in 2 hours

start_time = start_dt.strftime("%H:%M")
end_time = end_dt.strftime("%H:%M")

# Document ID (include seconds to make unique)
unique_suffix = now_ist.strftime("%H:%M:%S").replace(":", "-")
doc_id = f"test-{today_str}-{unique_suffix}"

# Tournament data
tournament_data = {
    "date": today_str,
    "start_time": start_time,
    "end_time": end_time,
    "start_at": start_dt,
    "end_at": end_dt,
    "start_epoch_ms": int(start_dt.timestamp() * 1000),
    "end_epoch_ms": int(end_dt.timestamp() * 1000),
    "entryCost": 100,
    "totalPrizePool": 1500,
    "status": status,
    "prizeMap": {
        "1": 400,
        "2": 250,
        "3": 200,
        "4": 150,
        "5": 120,
        "6": 100,
        "7": 90,
        "8": 80,
        "9": 60,
        "10": 50
    },
    "created_at": firestore.SERVER_TIMESTAMP,
    "updated_at": firestore.SERVER_TIMESTAMP,
}

# Create tournament
ref = db.collection("tournaments").document(doc_id)

try:
    ref.set(tournament_data)
    print(f"✅ Tournament created successfully!")
    print(f"\n📋 Tournament Details:")
    print(f"   ID: {doc_id}")
    print(f"   Date: {today_str}")
    print(f"   Start: {start_time} IST")
    print(f"   End: {end_time} IST")
    print(f"   Status: {status}")
    print(f"   Entry Cost: 100 coins")
    print(f"\n🧪 Test commands:")
    print(f"\n# Check status:")
    print(f'curl -X POST "https://us-central1-yral-staging.cloudfunctions.net/tournament_status" \\')
    print(f'  -H "Content-Type: application/json" \\')
    print(f'  -d \'{{"data": {{"tournament_id": "{doc_id}"}}}}\'')

    if status == "scheduled":
        print(f"\n# Register (needs auth token):")
        print(f'curl -X POST "https://us-central1-yral-staging.cloudfunctions.net/register_for_tournament" \\')
        print(f'  -H "Content-Type: application/json" \\')
        print(f'  -H "Authorization: Bearer YOUR_TOKEN" \\')
        print(f'  -d \'{{"data": {{"tournament_id": "{doc_id}", "principal_id": "YOUR_PRINCIPAL_ID"}}}}\'')

    if status == "live":
        print(f"\n# Vote (needs auth token + must be registered):")
        print(f'curl -X POST "https://us-central1-yral-staging.cloudfunctions.net/tournament_vote" \\')
        print(f'  -H "Content-Type: application/json" \\')
        print(f'  -H "Authorization: Bearer YOUR_TOKEN" \\')
        print(f'  -d \'{{"data": {{"tournament_id": "{doc_id}", "principal_id": "YOUR_PRINCIPAL_ID", "video_id": "test_video_1", "smiley_id": "fire"}}}}\'')

except Exception as e:
    print(f"❌ Error creating tournament: {e}")
