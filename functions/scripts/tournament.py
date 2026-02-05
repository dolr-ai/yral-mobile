#!/usr/bin/env python3
"""
Tournament creation script with command line interface.

Usage:
    python scripts/tournament.py -duration <mins> -number <count> <type>

Examples:
    python scripts/tournament.py -duration 5 -number 10 hot-or-not
    python scripts/tournament.py -duration 5 -number 10 smiley

Creates tournaments that are:
- <duration> minutes long
- <duration> minutes apart from each other
- Starting from the next available slot (rounded up to nearest minute)

Standard configuration (hardcoded):
- Entry cost: 5 YRAL tokens
- Prize pool: 5 INR (3 INR for 1st, 2 INR for 2nd)
- Video count: 10 videos per tournament

Run from functions directory with venv activated:
    cd functions
    source venv/bin/activate  # or venv\\Scripts\\activate on Windows
    python scripts/tournament.py -duration 5 -number 5 hot-or-not
"""

import argparse
import subprocess
import sys
import requests
from datetime import datetime, timezone, timedelta

# IST timezone (GMT+5:30)
IST = timezone(timedelta(hours=5, minutes=30))

# Standard tournament configuration
ENTRY_COST = 5
PRIZE_MAP = {"1": 3, "2": 2}
TOTAL_PRIZE_POOL = sum(PRIZE_MAP.values())  # 5
VIDEO_COUNT = 10  # IMPORTANT: Don't omit this, default is 500!

# Cloud Function URLs
PROJECT_ID = "yral-staging"
SMILEY_URL = f"https://us-central1-{PROJECT_ID}.cloudfunctions.net/create_tournaments"
HOT_OR_NOT_URL = f"https://us-central1-{PROJECT_ID}.cloudfunctions.net/create_hot_or_not_tournament"


def get_access_token():
    """Get gcloud access token for authentication."""
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


def create_smiley_tournament(access_token, title, start_time, end_time, date):
    """Create a smiley tournament via Cloud Function."""
    url = SMILEY_URL
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }
    payload = {
        "title": title,
        "entry_cost": ENTRY_COST,
        "total_prize_pool": TOTAL_PRIZE_POOL,
        "prize_map": PRIZE_MAP,
        "start_time": start_time,
        "end_time": end_time,
        "date": date,
        "video_count": VIDEO_COUNT  # CRITICAL: prevents default of 500
    }

    response = requests.post(url, headers=headers, json=payload, timeout=120)
    return response


def create_hot_or_not_tournament(access_token, title, start_time, end_time, date):
    """Create a hot-or-not tournament via Cloud Function."""
    url = HOT_OR_NOT_URL
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }
    payload = {
        "title": title,
        "entry_cost": ENTRY_COST,
        "total_prize_pool": TOTAL_PRIZE_POOL,
        "prize_map": PRIZE_MAP,
        "start_time": start_time,
        "end_time": end_time,
        "date": date,
        "video_count": VIDEO_COUNT  # CRITICAL: prevents default of 500
    }

    response = requests.post(url, headers=headers, json=payload, timeout=120)
    return response


def main():
    parser = argparse.ArgumentParser(
        description="Create multiple tournaments with specified duration and spacing",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python scripts/tournament.py -duration 5 -number 10 hot-or-not
    python scripts/tournament.py -duration 5 -number 5 smiley

Standard configuration:
    - Entry cost: 5 YRAL tokens
    - Prize pool: 5 INR (3 INR for 1st, 2 INR for 2nd)
    - Video count: 10 videos per tournament
        """
    )
    parser.add_argument(
        "-duration",
        type=int,
        required=True,
        help="Duration of each tournament in minutes (also used as spacing between tournaments)"
    )
    parser.add_argument(
        "-number",
        type=int,
        required=True,
        help="Number of tournaments to create"
    )
    parser.add_argument(
        "type",
        choices=["hot-or-not", "smiley"],
        help="Tournament type: 'hot-or-not' or 'smiley'"
    )

    args = parser.parse_args()

    duration_mins = args.duration
    num_tournaments = args.number
    tournament_type = args.type

    # Calculate start time: next minute rounded up + 1 minute buffer
    now = datetime.now(IST)
    # Round up to next minute and add 1 minute buffer for Cloud Function processing
    start_dt = now.replace(second=0, microsecond=0) + timedelta(minutes=2)

    print("=" * 60)
    print(f"Tournament Creation Script")
    print("=" * 60)
    print(f"Type:            {tournament_type}")
    print(f"Duration:        {duration_mins} minutes each")
    print(f"Count:           {num_tournaments} tournaments")
    print(f"Spacing:         {duration_mins} minutes apart")
    print(f"Entry Cost:      {ENTRY_COST} YRAL tokens")
    print(f"Prize Pool:      {TOTAL_PRIZE_POOL} INR (1st: 3, 2nd: 2)")
    print(f"Videos:          {VIDEO_COUNT} per tournament")
    print(f"Project:         {PROJECT_ID}")
    print("=" * 60)
    print()

    # Get access token
    print("Getting gcloud access token...")
    access_token = get_access_token()
    print("Access token obtained.")
    print()

    # Create tournaments
    created = 0
    failed = 0

    for i in range(num_tournaments):
        tournament_start = start_dt + timedelta(minutes=i * duration_mins)
        tournament_end = tournament_start + timedelta(minutes=duration_mins)

        date_str = tournament_start.strftime("%Y-%m-%d")
        start_time_str = tournament_start.strftime("%H:%M")
        end_time_str = tournament_end.strftime("%H:%M")

        if tournament_type == "hot-or-not":
            title = f"Test Hot or Not #{i + 1}"
        else:
            title = f"Test Smiley #{i + 1}"

        print(f"[{i + 1}/{num_tournaments}] Creating: {title}")
        print(f"    Date: {date_str}")
        print(f"    Time: {start_time_str} - {end_time_str} IST")

        try:
            if tournament_type == "hot-or-not":
                response = create_hot_or_not_tournament(
                    access_token, title, start_time_str, end_time_str, date_str
                )
            else:
                response = create_smiley_tournament(
                    access_token, title, start_time_str, end_time_str, date_str
                )

            if response.status_code in [200, 201]:
                print(f"    Status: SUCCESS")
                try:
                    result = response.json()
                    if "tournament_id" in result:
                        print(f"    ID: {result['tournament_id']}")
                except:
                    pass
                created += 1
            else:
                print(f"    Status: FAILED ({response.status_code})")
                print(f"    Error: {response.text[:200]}")
                failed += 1
        except Exception as e:
            print(f"    Status: ERROR")
            print(f"    Exception: {str(e)}")
            failed += 1

        print()

    # Summary
    print("=" * 60)
    print("Summary")
    print("=" * 60)
    print(f"Created: {created}/{num_tournaments}")
    print(f"Failed:  {failed}/{num_tournaments}")

    if created > 0:
        first_start = start_dt
        last_end = start_dt + timedelta(minutes=(num_tournaments - 1) * duration_mins + duration_mins)
        print()
        print(f"First tournament starts: {first_start.strftime('%Y-%m-%d %H:%M')} IST")
        print(f"Last tournament ends:    {last_end.strftime('%Y-%m-%d %H:%M')} IST")

    print("=" * 60)

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
