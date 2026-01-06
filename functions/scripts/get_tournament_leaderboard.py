"""
Script to get tournament leaderboard with usernames.
Run from functions directory with venv activated:
    python scripts/get_tournament_leaderboard.py
"""

import sys
import subprocess
import requests

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

def get_tournament_users(access_token, tournament_id):
    """Get all users in a tournament who played (have wins or losses)."""
    url = f"{FIRESTORE_BASE}/tournaments/{tournament_id}/users"
    headers = {
        "Authorization": f"Bearer {access_token}",
    }

    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print(f"Failed to get users: {response.text}")
        return []

    data = response.json()
    documents = data.get("documents", [])

    users = []
    for doc in documents:
        fields = doc.get("fields", {})
        principal_id = doc.get("name", "").split("/")[-1]

        wins = int(fields.get("tournament_wins", {}).get("integerValue", 0))
        losses = int(fields.get("tournament_losses", {}).get("integerValue", 0))
        diamonds = int(fields.get("diamonds", {}).get("integerValue", 0))
        username = fields.get("username", {}).get("stringValue", "")
        display_name = fields.get("display_name", {}).get("stringValue", "")

        # Only include users who played
        if wins > 0 or losses > 0:
            users.append({
                "principal_id": principal_id,
                "username": username or display_name or principal_id[:20] + "...",
                "wins": wins,
                "losses": losses,
                "diamonds": diamonds,
            })

    return users

def get_tournament_info(access_token, tournament_id):
    """Get tournament details."""
    url = f"{FIRESTORE_BASE}/tournaments/{tournament_id}"
    headers = {
        "Authorization": f"Bearer {access_token}",
    }

    response = requests.get(url, headers=headers)
    if response.status_code != 200:
        print(f"Failed to get tournament: {response.text}")
        return None

    data = response.json()
    fields = data.get("fields", {})

    prize_map_raw = fields.get("prizeMap", {}).get("mapValue", {}).get("fields", {})
    prize_map = {}
    for k, v in prize_map_raw.items():
        prize_map[int(k)] = int(v.get("integerValue", 0))

    return {
        "title": fields.get("title", {}).get("stringValue", ""),
        "status": fields.get("status", {}).get("stringValue", ""),
        "prize_map": prize_map,
        "total_prize_pool": int(fields.get("totalPrizePool", {}).get("integerValue", 0)),
    }

# Tournament ID
TOURNAMENT_ID = "2025-12-30-15-10-bonanza"

print(f"Fetching leaderboard for tournament: {TOURNAMENT_ID}")
print()

print("Getting access token...")
access_token = get_access_token()

# Get tournament info
tournament = get_tournament_info(access_token, TOURNAMENT_ID)
if tournament:
    print(f"Tournament: {tournament['title']}")
    print(f"Status: {tournament['status']}")
    print(f"Total Prize Pool: {tournament['total_prize_pool']}")
    print(f"Prize Map: {tournament['prize_map']}")
    print()

# Get users
users = get_tournament_users(access_token, TOURNAMENT_ID)

if not users:
    print("No players found who played in this tournament.")
    sys.exit(0)

# Sort by wins (descending), then by diamonds (descending)
users.sort(key=lambda x: (-x["wins"], -x["diamonds"]))

# Apply dense ranking
print(f"Players who played: {len(users)}")
print()
print("=" * 80)
print(f"{'Rank':<6} {'Username':<30} {'Wins':<8} {'Losses':<8} {'Diamonds':<10}")
print("=" * 80)

current_rank = 0
last_wins = None

for user in users:
    if user["wins"] != last_wins:
        current_rank += 1
        last_wins = user["wins"]

    prize = tournament["prize_map"].get(current_rank, 0) if tournament else 0
    prize_str = f" (Prize: Rs.{prize})" if prize > 0 else ""

    print(f"#{current_rank:<5} {user['username']:<30} {user['wins']:<8} {user['losses']:<8} {user['diamonds']:<10}{prize_str}")

print("=" * 80)
