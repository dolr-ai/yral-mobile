"""
Daily Tournament - 24-hour tournament running from 00:00 IST to 23:59:59 IST.

Creates daily tournaments with no entry cost, no prizes, but with time limits and initial diamonds.
Supports both "smiley" and "hot_or_not" subtypes with Gemini AI analysis.
"""

import sys
import json
import os
from datetime import datetime, timezone, timedelta
from typing import Any, Dict, List, Optional

import firebase_admin
from firebase_admin import firestore
from firebase_functions import https_fn
from flask import Request, jsonify

# Try relative import first (for local dev), fallback to absolute import (for deployment)
try:
    from .tournaments import (
        IST,
        TournamentStatus,
        db,
        _register_tournament_backend,
        _fetch_tournament_videos,
        _analyze_videos_for_emojis_batch,
        _store_video_emojis_and_seed,
        _schedule_status_task,
    )
except ImportError:
    from tournaments import (
        IST,
        TournamentStatus,
        db,
        _register_tournament_backend,
        _fetch_tournament_videos,
        _analyze_videos_for_emojis_batch,
        _store_video_emojis_and_seed,
        _schedule_status_task,
    )

try:
    from hot_or_not_tournament import _analyze_videos_batch
except ImportError:
    try:
        from .hot_or_not_tournament import _analyze_videos_batch
    except ImportError:
        # Define fallback if import fails (should never happen in production)
        def _analyze_videos_batch(videos, api_key, max_workers=3):
            print("[warning] _analyze_videos_batch not available", file=sys.stderr)
            return []


# ─────────────────────  CONSTANTS  ────────────────────────
DEFAULT_VIDEO_COUNT = 500
DEFAULT_DAILY_TIME_LIMIT_MS = 300000  # 5 minutes (300,000 ms)
DEFAULT_INITIAL_DIAMONDS = 20

# Environment detection
GCLOUD_PROJECT = os.environ.get("GCLOUD_PROJECT", "")
IS_PRODUCTION = GCLOUD_PROJECT == "yral-mobile"

# Default titles
DEFAULT_SMILEY_TITLE = "Daily Emoji Battle"
DEFAULT_HOT_OR_NOT_TITLE = "Daily Mast ya Bakwaas?"


# ─────────────────────  HELPERS  ────────────────────────
def _get_next_day_times() -> tuple[datetime, datetime, str]:
    """
    Get the next day's 00:00 IST and 23:59:59 IST times.
    Returns: (start_dt, end_dt, date_str)
    """
    # Get current time in IST
    now_ist = datetime.now(IST)

    # Calculate next day's date
    next_day = now_ist + timedelta(days=1)
    date_str = next_day.strftime("%Y-%m-%d")

    # Start time: 00:00:00 IST
    start_dt = datetime.strptime(f"{date_str} 00:00:00", "%Y-%m-%d %H:%M:%S")
    start_dt = start_dt.replace(tzinfo=IST)

    # End time: 23:59:59 IST
    end_dt = datetime.strptime(f"{date_str} 23:59:59", "%Y-%m-%d %H:%M:%S")
    end_dt = end_dt.replace(tzinfo=IST)

    return start_dt, end_dt, date_str


def _create_tournament_document(
    tournament_id: str,
    tournament_subtype: str,
    date_str: str,
    start_dt: datetime,
    end_dt: datetime,
    title: str,
    daily_time_limit_ms: int,
    initial_diamonds: int,
) -> Dict[str, Any]:
    """
    Create tournament document data.

    Args:
        tournament_id: Backend tournament ID
        tournament_subtype: "smiley" or "hot_or_not"
        date_str: Date in YYYY-MM-DD format
        start_dt: Start datetime
        end_dt: End datetime
        title: Tournament title
        daily_time_limit_ms: Time limit in milliseconds
        initial_diamonds: Initial diamond count for users

    Returns:
        Dictionary ready for Firestore
    """
    return {
        "date": date_str,
        "start_time": "00:00",
        "end_time": "23:59:59",
        "start_at": start_dt,
        "end_at": end_dt,
        "start_epoch_ms": int(start_dt.timestamp() * 1000),
        "end_epoch_ms": int(end_dt.timestamp() * 1000),
        "entryCost": 0,
        "totalPrizePool": 0,
        "status": TournamentStatus.SCHEDULED.value,
        "prizeMap": {},
        "created_at": firestore.SERVER_TIMESTAMP,
        "updated_at": firestore.SERVER_TIMESTAMP,
        "title": title,
        "type": tournament_subtype,
        "active_participant_count": 0,
        # Daily-specific fields
        "is_daily": True,
        "daily_time_limit_ms": daily_time_limit_ms,
        "initial_diamonds": initial_diamonds,
    }


# ─────────────────────  CLOUD FUNCTION  ────────────────────────
@https_fn.on_request(
    region="us-central1",
    timeout_sec=3600,
    memory=2048,
    secrets=["BACKEND_ADMIN_KEY", "GEMINI_API_KEY"]
)
def create_daily_tournament(request: Request):
    """
    Create a daily tournament running from 00:00 IST to 23:59:59 IST.

    Accepts POST requests with optional parameters:
    - tournament_subtype: "smiley" or "hot_or_not" (default: "smiley")
    - video_count: Number of videos (default: 500)
    - title: Custom title (default varies by subtype)
    - daily_time_limit_ms: Time limit in milliseconds (default: 300000)
    - initial_diamonds: Initial diamonds for users (default: 20)

    The tournament is created for the NEXT day (not today).
    Cloud Tasks are scheduled to transition status at 00:00 IST and 23:59:59 IST.
    """
    try:
        if request.method != "POST":
            return jsonify({"error": "METHOD_NOT_ALLOWED", "message": "POST required"}), 405

        # Get secrets
        backend_admin_key = os.environ.get("BACKEND_ADMIN_KEY")
        gemini_api_key = os.environ.get("GEMINI_API_KEY")

        if not backend_admin_key:
            print("[create_daily_tournament] BACKEND_ADMIN_KEY not configured", file=sys.stderr)
            return jsonify({"error": "CONFIG_ERROR", "message": "BACKEND_ADMIN_KEY not configured"}), 500

        if not gemini_api_key:
            print("[create_daily_tournament] GEMINI_API_KEY not configured", file=sys.stderr)
            return jsonify({"error": "CONFIG_ERROR", "message": "GEMINI_API_KEY not configured"}), 500

        # Parse request parameters
        body = request.get_json(silent=True) or {}

        tournament_subtype = str(body.get("tournament_subtype", "smiley")).strip().lower()
        if tournament_subtype not in ("smiley", "hot_or_not"):
            return jsonify({
                "error": "INVALID_SUBTYPE",
                "message": "tournament_subtype must be 'smiley' or 'hot_or_not'"
            }), 400

        video_count = int(body.get("video_count", DEFAULT_VIDEO_COUNT))
        daily_time_limit_ms = int(body.get("daily_time_limit_ms", DEFAULT_DAILY_TIME_LIMIT_MS))
        initial_diamonds = int(body.get("initial_diamonds", DEFAULT_INITIAL_DIAMONDS))

        # Title defaults based on subtype
        if tournament_subtype == "smiley":
            default_title = DEFAULT_SMILEY_TITLE
        else:
            default_title = DEFAULT_HOT_OR_NOT_TITLE

        title = body.get("title", default_title)

        env_name = "production" if IS_PRODUCTION else "staging"
        print(f"[create_daily_tournament] Running in {env_name} mode (project: {GCLOUD_PROJECT})")
        print(f"[create_daily_tournament] Subtype: {tournament_subtype}, Video count: {video_count}")

        # Get next day's times (00:00 IST to 23:59:59 IST)
        start_dt, end_dt, date_str = _get_next_day_times()
        print(f"[create_daily_tournament] Creating tournament for {date_str}")
        print(f"[create_daily_tournament] Start: {start_dt.isoformat()}, End: {end_dt.isoformat()}")

        # 1. Register with backend to get tournament ID and videos
        tournament_id, backend_error = _register_tournament_backend(backend_admin_key, video_count=video_count)
        if backend_error:
            print(f"[create_daily_tournament] Backend registration failed: {backend_error}", file=sys.stderr)
            return jsonify({
                "error": "BACKEND_ERROR",
                "message": "Failed to register tournament with backend"
            }), 502

        print(f"[create_daily_tournament] Registered tournament: {tournament_id}")

        # 2. Fetch videos from backend
        videos, fetch_error = _fetch_tournament_videos(tournament_id)
        if fetch_error or not videos:
            print(f"[create_daily_tournament] Failed to fetch videos: {fetch_error}", file=sys.stderr)
            return jsonify({
                "error": "BACKEND_ERROR",
                "message": "Failed to fetch videos from backend"
            }), 502

        print(f"[create_daily_tournament] Fetched {len(videos)} videos")

        # 3. Create tournament document
        tournament_data = _create_tournament_document(
            tournament_id=tournament_id,
            tournament_subtype=tournament_subtype,
            date_str=date_str,
            start_dt=start_dt,
            end_dt=end_dt,
            title=title,
            daily_time_limit_ms=daily_time_limit_ms,
            initial_diamonds=initial_diamonds,
        )

        # Determine collection based on subtype
        if tournament_subtype == "smiley":
            collection_name = "tournaments"
        else:
            collection_name = "hot_or_not_tournaments"

        # Write tournament document to Firestore
        ref = db().collection(collection_name).document(tournament_id)
        ref.set(tournament_data)
        print(f"[create_daily_tournament] Created tournament document in {collection_name}/{tournament_id}")

        # 4. Analyze videos with Gemini
        if tournament_subtype == "smiley":
            # Smiley tournament: emoji analysis
            print(f"[gemini] Starting emoji analysis for {len(videos)} videos...")
            analyzed_videos = _analyze_videos_for_emojis_batch(videos, gemini_api_key, max_workers=5)
            print(f"[gemini] Completed emoji analysis for {len(analyzed_videos)} videos")

            # Store emoji data and seed initial votes
            _store_video_emojis_and_seed(tournament_id, analyzed_videos)
            print(f"[create_daily_tournament] Stored emoji data and seeded votes for {len(analyzed_videos)} videos")

        else:
            # Hot or Not tournament: pairwise comparison analysis
            print(f"[gemini] Starting pairwise analysis for {len(videos)} videos...")
            analyzed_videos = _analyze_videos_batch(videos, gemini_api_key, max_workers=3)
            print(f"[gemini] Completed pairwise analysis for {len(analyzed_videos)} videos")

            # Store AI verdicts for each video
            videos_ref = ref.collection("videos")
            batch = db().batch()
            batch_count = 0

            for video_data in analyzed_videos:
                video_id = video_data.get("video_id")
                if not video_id:
                    continue

                video_doc_ref = videos_ref.document(video_id)
                batch.set(video_doc_ref, {
                    "ai_verdict": video_data["ai_verdict"],
                    "ai_confidence": video_data.get("ai_confidence", 0.5),
                    "ai_reason": video_data.get("ai_reason", ""),
                    "video_url": video_data.get("video_url", ""),
                    "analyzed_at": firestore.SERVER_TIMESTAMP,
                })

                batch_count += 1
                if batch_count >= 500:
                    batch.commit()
                    batch = db().batch()
                    batch_count = 0

            if batch_count > 0:
                batch.commit()

            print(f"[create_daily_tournament] Stored AI verdicts for {len(analyzed_videos)} videos")

        # 5. Schedule Cloud Tasks for status transitions
        try:
            _schedule_status_task(tournament_id, TournamentStatus.LIVE, start_dt)
            _schedule_status_task(tournament_id, TournamentStatus.ENDED, end_dt)
            print(f"[create_daily_tournament] Scheduled status tasks for {tournament_id}")
        except Exception as task_err:
            print(f"[warning] Failed to schedule status tasks: {task_err}", file=sys.stderr)
            # Don't fail the whole creation, tasks can be retried manually

        # Success response
        print(f"[create_daily_tournament] Successfully created daily {tournament_subtype} tournament {tournament_id}")

        return jsonify({
            "status": "success",
            "tournament_id": tournament_id,
            "tournament_subtype": tournament_subtype,
            "collection": collection_name,
            "date": date_str,
            "start_time": "00:00",
            "end_time": "23:59:59",
            "start_at": start_dt.isoformat(),
            "end_at": end_dt.isoformat(),
            "video_count": len(videos),
            "analyzed_count": len(analyzed_videos),
            "title": title,
            "daily_time_limit_ms": daily_time_limit_ms,
            "initial_diamonds": initial_diamonds,
            "is_daily": True,
        }), 200

    except ValueError as e:
        print(f"[create_daily_tournament] Invalid parameter: {e}", file=sys.stderr)
        return jsonify({"error": "INVALID_PARAMETER", "message": str(e)}), 400

    except Exception as e:
        print(f"[create_daily_tournament] Unexpected error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return jsonify({"error": "INTERNAL", "message": "An internal error occurred"}), 500
