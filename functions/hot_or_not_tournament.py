"""
Hot or Not Tournament - AI-powered video prediction game.

Users vote "hot" or "not" on videos and win if their prediction matches the AI's verdict.
Gemini 2.0 Flash analyzes videos during tournament creation.
"""

# ─────────────────────  IMPORTS WITH LOGGING  ────────────────────────
# Added logging before each import to diagnose cold start crashes
import sys
print("[import] Starting imports...", file=sys.stderr)

import json
print("[import] json loaded", file=sys.stderr)
import os
print("[import] os loaded", file=sys.stderr)
import random
print("[import] random loaded", file=sys.stderr)
import string
print("[import] string loaded", file=sys.stderr)
import tempfile
print("[import] tempfile loaded", file=sys.stderr)
import time
print("[import] time loaded", file=sys.stderr)
from concurrent.futures import ThreadPoolExecutor, as_completed
print("[import] concurrent.futures loaded", file=sys.stderr)
from datetime import datetime, timezone, timedelta
print("[import] datetime loaded", file=sys.stderr)
from enum import Enum
print("[import] enum loaded", file=sys.stderr)
from typing import Any, Dict, List, Optional, Tuple
print("[import] typing loaded", file=sys.stderr)

import firebase_admin
print("[import] firebase_admin loaded", file=sys.stderr)
import requests
print("[import] requests loaded", file=sys.stderr)

# New unified Google GenAI SDK (replaces deprecated google.generativeai)
from google import genai
print("[import] google.genai loaded", file=sys.stderr)

from firebase_admin import auth, firestore
print("[import] firebase_admin.auth, firestore loaded", file=sys.stderr)
from firebase_functions import https_fn
print("[import] firebase_functions loaded", file=sys.stderr)
from flask import Request, jsonify, make_response
print("[import] flask loaded", file=sys.stderr)
from google.api_core.exceptions import GoogleAPICallError
print("[import] google.api_core.exceptions loaded", file=sys.stderr)
from google.cloud import tasks_v2
print("[import] google.cloud.tasks_v2 loaded", file=sys.stderr)
from google.protobuf import timestamp_pb2
print("[import] google.protobuf loaded", file=sys.stderr)
from mixpanel import Mixpanel
print("[import] All imports completed successfully", file=sys.stderr)

# ─────────────────────  CONSTANTS  ────────────────────────
HOT_OR_NOT_TOURNAMENT_COLL = "hot_or_not_tournaments"
CLOUDFLARE_PREFIX = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
CLOUDFLARE_MP4_SUFFIX = "/downloads/default.mp4"
GEMINI_MODEL = "gemini-2.0-flash"

# Backend API
BACKEND_TOURNAMENT_REGISTER_URL = "https://recsys-on-premise.fly.dev/tournament/register"
BACKEND_TOURNAMENT_VIDEOS_URL = "https://recsys-on-premise.fly.dev/tournament/{tournament_id}/videos"
DEFAULT_VIDEO_COUNT = 500

# Token API
BALANCE_URL_YRAL_TOKEN = "https://yral-hot-or-not.go-bazzinga.workers.dev/update_balance/"

# Environment
GCLOUD_PROJECT = os.environ.get("GCLOUD_PROJECT", "")
IS_PRODUCTION = GCLOUD_PROJECT == "yral-mobile"

# Tournament config
IST = timezone(timedelta(hours=5, minutes=30))
if IS_PRODUCTION:
    TOURNAMENT_SLOTS = [("19:20", "19:30")]  # 7:20 PM IST
    TOURNAMENT_ENTRY_COST = 10
    TOURNAMENT_TITLE = "Mast ya Bakwaas?"
else:
    TOURNAMENT_SLOTS = [("13:00", "13:10"), ("15:00", "15:10"), ("17:00", "17:10")]
    TOURNAMENT_ENTRY_COST = 100
    TOURNAMENT_TITLE = "Mast ya Bakwaas?"

# Prize distribution
PRIZE_MAP = {
    "1": 400, "2": 250, "3": 200, "4": 150, "5": 125,
    "6": 100, "7": 75, "8": 75, "9": 75, "10": 50
}
TOTAL_PRIZE_POOL = sum(PRIZE_MAP.values())

# ─────────────────────  DATABASE  ────────────────────────
_db = None

def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db


# ─────────────────────  CLOUD TASKS HELPERS  ────────────────────────
def _tasks_client() -> tasks_v2.CloudTasksClient:
    return tasks_v2.CloudTasksClient()


def _queue_path(client: tasks_v2.CloudTasksClient) -> str:
    project = os.environ.get("GCLOUD_PROJECT") or os.environ.get("GOOGLE_CLOUD_PROJECT")
    location = os.environ.get("TASKS_LOCATION", "us-central1")
    queue = os.environ.get("TASKS_QUEUE", "tournament-status-updates")
    if not project:
        raise RuntimeError("GCLOUD_PROJECT/GOOGLE_CLOUD_PROJECT env var missing")
    return client.queue_path(project, location, queue)


def _function_url(fn_name: str) -> str:
    project = os.environ.get("GCLOUD_PROJECT") or os.environ.get("GOOGLE_CLOUD_PROJECT")
    region = os.environ.get("FUNCTION_REGION", "us-central1")
    if not project:
        raise RuntimeError("GCLOUD_PROJECT/GOOGLE_CLOUD_PROJECT env var missing")
    return f"https://{region}-{project}.cloudfunctions.net/{fn_name}"


def _schedule_status_task(doc_id: str, target_status: str, run_at: datetime):
    """Schedule a Cloud Task to update tournament status at a specific time."""
    client = _tasks_client()
    parent = _queue_path(client)
    url = _function_url("update_tournament_status")

    payload = json.dumps({"tournament_id": doc_id, "status": target_status}).encode()
    schedule_ts = timestamp_pb2.Timestamp()
    schedule_ts.FromDatetime(run_at.astimezone(timezone.utc))

    task = tasks_v2.Task(
        http_request=tasks_v2.HttpRequest(
            http_method=tasks_v2.HttpMethod.POST,
            url=url,
            headers={"Content-Type": "application/json"},
            body=payload,
        ),
        schedule_time=schedule_ts,
    )

    response = client.create_task(request={"parent": parent, "task": task})
    print(f"[tasks] scheduled {target_status} for {doc_id} at {run_at.isoformat()} ({response.name})")


# ─────────────────────  HELPERS  ────────────────────────
def error_response(status: int, code: str, message: str):
    payload = {"error": {"code": code, "message": message}}
    return make_response(jsonify(payload), status)


def _track_tournament_creation_failed(
    error_code: str,
    error_message: str,
    stage: str,
    additional_data: Optional[Dict[str, Any]] = None
) -> None:
    """
    Track tournament creation failure event in Mixpanel.

    Args:
        error_code: Error code (e.g., "BACKEND_ERROR", "CONFIG_ERROR")
        error_message: Detailed error message
        stage: Stage where failure occurred (e.g., "backend_register", "fetch_videos", "gemini_analysis")
        additional_data: Any additional context to include
    """
    mixpanel_token = os.environ.get("MIXPANEL_TOKEN")
    if not mixpanel_token:
        print("[mixpanel] MIXPANEL_TOKEN not configured, skipping failure event", file=sys.stderr)
        return

    try:
        mp = Mixpanel(mixpanel_token)
        event_data = {
            "error_code": error_code,
            "error_message": error_message,
            "stage": stage,
            "tournament_type": "hot_or_not",
            "environment": "production" if IS_PRODUCTION else "staging",
            "timestamp": datetime.now(timezone.utc).isoformat(),
        }
        if additional_data:
            event_data.update(additional_data)

        # Use a system identifier for server-side events
        mp.track("system_hot_or_not", "tournament_creation_failed", event_data)
        print(f"[mixpanel] Tracked tournament_creation_failed: {error_code} at {stage}")
    except Exception as e:
        print(f"[mixpanel] Failed to track failure event: {e}", file=sys.stderr)


def _tx_id() -> str:
    now = datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
    rnd = "".join(random.choices(string.ascii_lowercase + string.digits, k=4))
    return f"{now}_{rnd}"

def _push_delta_yral_token(token: str, principal_id: str, delta: int) -> Tuple[bool, Optional[str]]:
    """Push balance change to YRAL token service."""
    url = f"{BALANCE_URL_YRAL_TOKEN}{principal_id}"
    auth_value = token if token.startswith("Bearer ") else f"Bearer {token}"
    headers = {"Authorization": auth_value, "Content-Type": "application/json"}
    body = {"delta": str(delta), "is_airdropped": False}

    try:
        resp = requests.post(url, json=body, timeout=30, headers=headers)
        if resp.status_code == 200:
            return True, None
        return False, f"Status: {resp.status_code}, Body: {resp.text}"
    except requests.RequestException as e:
        return False, str(e)

# ─────────────────────  BACKEND API  ────────────────────────
def _register_tournament_backend(admin_key: str, video_count: int = DEFAULT_VIDEO_COUNT) -> Tuple[Optional[str], Optional[str]]:
    """Register tournament with backend to get tournament_id."""
    headers = {"x-admin-key": admin_key, "Content-Type": "application/json"}
    body = {"video_count": video_count}

    try:
        resp = requests.post(BACKEND_TOURNAMENT_REGISTER_URL, json=body, timeout=30, headers=headers)
        if resp.status_code == 200:
            data = resp.json()
            tournament_id = data.get("tournament_id")
            if tournament_id:
                print(f"[backend] Registered tournament: {tournament_id}")
                return tournament_id, None
            return None, "No tournament_id in response"
        return None, f"Status: {resp.status_code}, Body: {resp.text}"
    except requests.RequestException as e:
        return None, str(e)

def _fetch_tournament_videos(tournament_id: str) -> Tuple[Optional[List[Dict]], Optional[str]]:
    """Fetch video list from backend for a tournament."""
    url = BACKEND_TOURNAMENT_VIDEOS_URL.format(tournament_id=tournament_id)

    try:
        resp = requests.get(url, timeout=60, params={"with_metadata": "true"})
        if resp.status_code == 200:
            data = resp.json()
            videos = data.get("videos", [])
            print(f"[backend] Fetched {len(videos)} videos for tournament {tournament_id}")
            return videos, None
        return None, f"Status: {resp.status_code}, Body: {resp.text}"
    except requests.RequestException as e:
        return None, str(e)

# ─────────────────────  GEMINI ANALYSIS  ────────────────────────
def _form_video_url(video_id: str) -> str:
    """Form Cloudflare MP4 URL from video ID."""
    return f"{CLOUDFLARE_PREFIX}{video_id}{CLOUDFLARE_MP4_SUFFIX}"

def _analyze_video_with_gemini(video_url: str, api_key: str) -> Dict[str, Any]:
    """
    Analyze video with Gemini 2.0 Flash to determine if it will go viral.
    Downloads video, uploads to Gemini, analyzes, and cleans up.
    Uses the new unified google.genai SDK.
    Returns: {"verdict": "hot" | "not", "confidence": float, "reason": str, "error": str | None}
    """
    temp_path = None
    video_file = None
    client = None

    try:
        # Create client with API key (new SDK pattern)
        client = genai.Client(api_key=api_key)

        # Step 1: Download video from Cloudflare
        video_resp = requests.get(video_url, timeout=60, stream=True)
        if video_resp.status_code != 200:
            return {
                "verdict": random.choice(["hot", "not"]),
                "confidence": 0.5,
                "reason": "Download failed",
                "error": f"HTTP {video_resp.status_code}"
            }

        # Save to temp file
        with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as f:
            for chunk in video_resp.iter_content(chunk_size=8192):
                f.write(chunk)
            temp_path = f.name

        # Step 2: Upload to Gemini (new SDK: client.files.upload)
        video_file = client.files.upload(file=temp_path)

        # Wait for processing (with timeout)
        max_wait = 30  # seconds
        waited = 0
        while video_file.state.name == "PROCESSING" and waited < max_wait:
            time.sleep(2)
            waited += 2
            video_file = client.files.get(name=video_file.name)

        if video_file.state.name == "FAILED":
            return {
                "verdict": random.choice(["hot", "not"]),
                "confidence": 0.5,
                "reason": "Gemini processing failed",
                "error": "PROCESSING_FAILED"
            }

        # Step 3: Analyze with Gemini (new SDK: client.models.generate_content)
        prompt = """Determine if this video is mast(hit) or bakwaas(flop).
Think like a person living in tier 2 and tier 3 cities of India, be a bit frugal in identifying it as mast since most videos are going to be analyzed as mast.
Respond with ONLY a JSON object:
{"verdict": "mast" or "bakwaas", "confidence": 0.0 to 1.0, "reason": "brief explanation"}"""

        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=[video_file, prompt]
        )
        response_text = response.text.strip()

        # Parse JSON from response
        if "```json" in response_text:
            response_text = response_text.split("```json")[1].split("```")[0].strip()
        elif "```" in response_text:
            response_text = response_text.split("```")[1].split("```")[0].strip()

        result = json.loads(response_text)
        verdict = result.get("verdict", "bakwaas").lower()
        confidence = float(result.get("confidence", 0.5))
        reason = result.get("reason", "")

        # Map mast/bakwaas to hot/not for consistency
        if verdict == "mast":
            verdict = "hot"
        elif verdict == "bakwaas":
            verdict = "not"
        elif verdict not in ("hot", "not"):
            verdict = "not"

        return {"verdict": verdict, "confidence": confidence, "reason": reason, "error": None}

    except (json.JSONDecodeError, KeyError, ValueError) as e:
        verdict = random.choice(["hot", "not"])
        return {"verdict": verdict, "confidence": 0.5, "reason": "", "error": f"Parse error: {e}"}

    except Exception as e:
        verdict = random.choice(["hot", "not"])
        return {"verdict": verdict, "confidence": 0.5, "reason": "", "error": str(e)}

    finally:
        # Cleanup: delete temp file and Gemini file
        if temp_path and os.path.exists(temp_path):
            try:
                os.unlink(temp_path)
            except Exception:
                pass
        if video_file and client:
            try:
                client.files.delete(name=video_file.name)
            except Exception:
                pass

def _analyze_videos_batch(videos: List[Dict], api_key: str, max_workers: int = 5) -> List[Dict]:
    """Analyze multiple videos in parallel using ThreadPoolExecutor.

    Note: max_workers reduced to 5 to avoid overwhelming Gemini API rate limits
    when downloading/uploading videos.
    """
    results = []

    def analyze_single(video: Dict, index: int) -> Dict:
        video_id = video.get("video_id") or video.get("videoID")
        if not video_id:
            return {"video_id": None, "error": "No video_id"}

        video_url = _form_video_url(video_id)
        print(f"[gemini] Analyzing video {index + 1}: {video_id[:16]}...")
        analysis = _analyze_video_with_gemini(video_url, api_key)

        verdict = analysis["verdict"]
        print(f"[gemini] Video {index + 1} -> {verdict.upper()} (conf: {analysis['confidence']})")

        return {
            "video_id": video_id,
            "video_url": video_url,
            "ai_verdict": verdict,
            "ai_confidence": analysis["confidence"],
            "ai_reason": analysis.get("reason", ""),
            "analysis_error": analysis.get("error"),
        }

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(analyze_single, v, i): v for i, v in enumerate(videos)}

        for future in as_completed(futures):
            try:
                result = future.result()
                results.append(result)
            except Exception as e:
                video = futures[future]
                video_id = video.get("video_id") or video.get("videoID")
                results.append({
                    "video_id": video_id,
                    "ai_verdict": random.choice(["hot", "not"]),
                    "ai_confidence": 0.5,
                    "ai_reason": "",
                    "analysis_error": str(e),
                })

    print(f"[gemini] Completed analysis of {len(results)} videos")
    return results

# ─────────────────────  TOURNAMENT CREATION  ────────────────────────
@https_fn.on_request(region="us-central1", timeout_sec=1500, memory=2048, secrets=["BALANCE_UPDATE_TOKEN", "GEMINI_API_KEY", "BACKEND_ADMIN_KEY", "MIXPANEL_TOKEN"])
def create_hot_or_not_tournament(request: Request):
    """
    Create a new Hot or Not tournament with AI-analyzed videos.
    Called by Cloud Scheduler or manually for testing.
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Get secrets
        gemini_api_key = os.environ.get("GEMINI_API_KEY")
        backend_admin_key = os.environ.get("BACKEND_ADMIN_KEY")

        if not gemini_api_key:
            _track_tournament_creation_failed("CONFIG_ERROR", "GEMINI_API_KEY not configured", "config_check")
            return error_response(500, "CONFIG_ERROR", "GEMINI_API_KEY not configured")
        if not backend_admin_key:
            _track_tournament_creation_failed("CONFIG_ERROR", "BACKEND_ADMIN_KEY not configured", "config_check")
            return error_response(500, "CONFIG_ERROR", "BACKEND_ADMIN_KEY not configured")

        # Parse request
        body = request.get_json(silent=True) or {}
        video_count = int(body.get("video_count", DEFAULT_VIDEO_COUNT))

        # Custom parameter overrides
        title = body.get("title", TOURNAMENT_TITLE)
        entry_cost = int(body.get("entry_cost", TOURNAMENT_ENTRY_COST))
        prize_map = body.get("prize_map", PRIZE_MAP)
        total_prize_pool = int(body.get("total_prize_pool", TOTAL_PRIZE_POOL))

        # Get tournament timing (next slot or provided)
        now_ist = datetime.now(IST)
        date_str = body.get("date") or now_ist.strftime("%Y-%m-%d")

        # Find next available slot or use provided times
        start_time = body.get("start_time")
        end_time = body.get("end_time")

        if not start_time:
            # Find next slot
            for slot_start, slot_end in TOURNAMENT_SLOTS:
                slot_dt = datetime.strptime(f"{date_str} {slot_start}", "%Y-%m-%d %H:%M")
                slot_dt = slot_dt.replace(tzinfo=IST)
                if slot_dt > now_ist:
                    start_time = slot_start
                    end_time = slot_end
                    break

            if not start_time:
                # Use first slot of next day
                next_day = (now_ist + timedelta(days=1)).strftime("%Y-%m-%d")
                date_str = next_day
                start_time, end_time = TOURNAMENT_SLOTS[0]

        # 1. Register with backend
        tournament_id, err = _register_tournament_backend(backend_admin_key, video_count)
        if err:
            _track_tournament_creation_failed("BACKEND_ERROR", f"Failed to register tournament: {err}", "backend_register", {"video_count": video_count})
            return error_response(502, "BACKEND_ERROR", f"Failed to register tournament: {err}")

        # 2. Fetch videos
        videos, err = _fetch_tournament_videos(tournament_id)
        if err:
            _track_tournament_creation_failed("BACKEND_ERROR", f"Failed to fetch videos: {err}", "fetch_videos", {"tournament_id": tournament_id})
            return error_response(502, "BACKEND_ERROR", f"Failed to fetch videos: {err}")

        if not videos:
            _track_tournament_creation_failed("BACKEND_ERROR", "No videos returned from backend", "fetch_videos", {"tournament_id": tournament_id})
            return error_response(502, "BACKEND_ERROR", "No videos returned from backend")

        # 3. Analyze videos with Gemini (parallel)
        print(f"[gemini] Starting analysis of {len(videos)} videos...")
        analyzed_videos = _analyze_videos_batch(videos, gemini_api_key, max_workers=5)
        print(f"[gemini] Completed analysis of {len(analyzed_videos)} videos")

        # 4. Calculate epoch times
        start_dt = datetime.strptime(f"{date_str} {start_time}", "%Y-%m-%d %H:%M")
        start_dt = start_dt.replace(tzinfo=IST)
        end_dt = datetime.strptime(f"{date_str} {end_time}", "%Y-%m-%d %H:%M")
        end_dt = end_dt.replace(tzinfo=IST)

        start_epoch_ms = int(start_dt.timestamp() * 1000)
        end_epoch_ms = int(end_dt.timestamp() * 1000)

        # 5. Create tournament document
        tournament_ref = db().collection(HOT_OR_NOT_TOURNAMENT_COLL).document(tournament_id)
        tournament_ref.set({
            "date": date_str,
            "start_time": start_time,
            "end_time": end_time,
            "start_at": start_dt,
            "end_at": end_dt,
            "start_epoch_ms": start_epoch_ms,
            "end_epoch_ms": end_epoch_ms,
            "entryCost": entry_cost,
            "totalPrizePool": total_prize_pool,
            "prizeMap": prize_map,
            "status": "scheduled",
            "title": title,
            "type": "hot_or_not",
            "video_count": len(analyzed_videos),
            "active_participant_count": 0,
            "created_at": firestore.SERVER_TIMESTAMP,
            "updated_at": firestore.SERVER_TIMESTAMP,
        })

        # 6. Store AI verdicts for each video
        videos_ref = tournament_ref.collection("videos")
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

        # 7. Schedule status transitions via Cloud Tasks
        try:
            _schedule_status_task(tournament_id, "live", start_dt)
            _schedule_status_task(tournament_id, "ended", end_dt)
        except Exception as task_err:
            print(f"[warning] Failed to schedule status tasks: {task_err}", file=sys.stderr)
            # Don't fail the whole creation, tasks can be retried

        print(f"[tournament] Created Hot or Not tournament {tournament_id} with {len(analyzed_videos)} videos")

        return jsonify({
            "tournament_id": tournament_id,
            "date": date_str,
            "start_time": start_time,
            "end_time": end_time,
            "video_count": len(analyzed_videos),
            "status": "scheduled",
        }), 200

    except Exception as e:
        print(f"[error] create_hot_or_not_tournament: {e}", file=sys.stderr)
        _track_tournament_creation_failed("INTERNAL", str(e), "unknown", {"exception_type": type(e).__name__})
        return error_response(500, "INTERNAL", str(e))


def _compute_user_position(tournament_id: str, principal_id: str, user_diamonds: int, user_wins: int, user_losses: int) -> int:
    """
    Compute user's live position in Hot or Not tournament leaderboard.

    Ranking order:
    1. Diamonds DESC (more diamonds = higher rank)
    2. Total games (wins + losses) DESC (tiebreaker)
    3. updated_at ASC (second tiebreaker)

    Returns position (1-based) or 0 if user hasn't played.
    """
    # If user hasn't played, return 0 (not ranked)
    if user_wins == 0 and user_losses == 0:
        return 0

    user_total_games = user_wins + user_losses

    # Count users who rank higher
    users_ref = db().collection(f"{HOT_OR_NOT_TOURNAMENT_COLL}/{tournament_id}/users")
    snaps = users_ref.where("diamonds", ">=", user_diamonds).limit(500).stream()

    higher = 0
    for snap in snaps:
        if snap.id == principal_id:
            continue  # Skip self

        data = snap.to_dict() or {}
        w = int(data.get("wins") or 0)
        l = int(data.get("losses") or 0)

        # Only count users who have played
        if w == 0 and l == 0:
            continue

        diamonds = int(data.get("diamonds") or 0)
        total_games = w + l

        # Check if this user ranks higher
        if diamonds > user_diamonds:
            higher += 1
        elif diamonds == user_diamonds:
            if total_games > user_total_games:
                higher += 1

    return higher + 1


# ─────────────────────  VOTING  ────────────────────────
@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"])
def hot_or_not_tournament_vote(request: Request):
    """Cast a vote in a Hot or Not tournament."""
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Parse request
        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        tournament_id = str(body.get("tournament_id") or data.get("tournament_id") or "").strip()
        principal_id = str(body.get("principal_id") or data.get("principal_id") or "").strip()
        video_id = str(body.get("video_id") or data.get("video_id") or "").strip()
        vote = str(body.get("vote") or data.get("vote") or "").strip().lower()

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")
        if not principal_id:
            return error_response(400, "MISSING_PRINCIPAL_ID", "principal_id required")
        if not video_id:
            return error_response(400, "MISSING_VIDEO_ID", "video_id required")
        if vote not in ("hot", "not"):
            return error_response(400, "INVALID_VOTE", "vote must be 'hot' or 'not'")

        # Verify auth
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        # References
        tournament_ref = db().collection(HOT_OR_NOT_TOURNAMENT_COLL).document(tournament_id)
        user_ref = tournament_ref.collection("users").document(principal_id)
        video_ref = tournament_ref.collection("videos").document(video_id)
        vote_doc_id = f"{principal_id}_{video_id}"
        vote_ref = tournament_ref.collection("votes").document(vote_doc_id)

        # Transaction for atomic operations
        @firestore.transactional
        def _vote_tx(tx: firestore.Transaction) -> Dict:
            # Check tournament status
            tournament_snap = tournament_ref.get(transaction=tx)
            if not tournament_snap.exists:
                return {"error": "TOURNAMENT_NOT_FOUND"}

            tournament_data = tournament_snap.to_dict() or {}
            if tournament_data.get("status") != "live":
                return {"error": "TOURNAMENT_NOT_LIVE", "status": tournament_data.get("status")}

            # Check user registration
            user_snap = user_ref.get(transaction=tx)
            if not user_snap.exists:
                return {"error": "NOT_REGISTERED"}

            user_data = user_snap.to_dict() or {}
            diamonds = int(user_data.get("diamonds", 0))

            if diamonds <= 0:
                return {"error": "NO_DIAMONDS"}

            # Check if this is user's first game (for active_participant_count)
            previous_wins = int(user_data.get("wins") or 0)
            previous_losses = int(user_data.get("losses") or 0)
            is_first_game = (previous_wins + previous_losses) == 0

            # Check duplicate vote
            if vote_ref.get(transaction=tx).exists:
                return {"error": "DUPLICATE_VOTE"}

            # Get AI verdict
            video_snap = video_ref.get(transaction=tx)
            if not video_snap.exists:
                return {"error": "VIDEO_NOT_FOUND"}

            video_data = video_snap.to_dict() or {}
            ai_verdict = video_data.get("ai_verdict", "not")

            # Determine outcome
            outcome = "WIN" if vote == ai_verdict else "LOSS"
            diamond_delta = 1 if outcome == "WIN" else -1
            new_diamonds = max(0, diamonds + diamond_delta)

            # Record vote
            tx.set(vote_ref, {
                "principal_id": principal_id,
                "video_id": video_id,
                "vote": vote,
                "ai_verdict": ai_verdict,
                "outcome": outcome,
                "at": firestore.SERVER_TIMESTAMP,
            })

            # Update user stats
            win_increment = 1 if outcome == "WIN" else 0
            loss_increment = 1 if outcome == "LOSS" else 0

            tx.update(user_ref, {
                "diamonds": new_diamonds,
                "wins": firestore.Increment(win_increment),
                "losses": firestore.Increment(loss_increment),
                "updated_at": firestore.SERVER_TIMESTAMP,
            })

            # Increment active_participant_count if this is user's first game
            if is_first_game:
                tx.update(tournament_ref, {
                    "active_participant_count": firestore.Increment(1)
                })

            return {
                "success": True,
                "outcome": outcome,
                "vote": vote,
                "ai_verdict": ai_verdict,
                "diamonds": new_diamonds,
                "diamond_delta": diamond_delta,
                "is_first_game": is_first_game,
            }

        result = _vote_tx(db().transaction())

        if "error" in result:
            error_code = result["error"]
            messages = {
                "TOURNAMENT_NOT_FOUND": ("Tournament not found", 404),
                "TOURNAMENT_NOT_LIVE": (f"Tournament is {result.get('status', 'not live')}", 400),
                "NOT_REGISTERED": ("Not registered for this tournament", 403),
                "NO_DIAMONDS": ("No diamonds remaining", 402),
                "DUPLICATE_VOTE": ("Already voted on this video", 409),
                "VIDEO_NOT_FOUND": ("Video not found in tournament", 404),
            }
            msg, status = messages.get(error_code, ("Unknown error", 500))
            return error_response(status, error_code, msg)

        # Get updated user stats for position calculation
        user_snap = user_ref.get()
        user_data = user_snap.to_dict() or {}
        wins = int(user_data.get("wins") or 0)
        losses = int(user_data.get("losses") or 0)

        # Get updated tournament data for active_participant_count
        updated_tournament = tournament_ref.get()
        tournament_data = updated_tournament.to_dict() or {}
        active_participant_count = int(tournament_data.get("active_participant_count") or 0)

        # Calculate live position
        position = _compute_user_position(tournament_id, principal_id, result["diamonds"], wins, losses)

        return jsonify({
            "outcome": result["outcome"],
            "vote": result["vote"],
            "ai_verdict": result["ai_verdict"],
            "diamonds": result["diamonds"],
            "diamond_delta": result["diamond_delta"],
            "wins": wins,
            "losses": losses,
            "position": position,
            "active_participant_count": active_participant_count,
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "Invalid token")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print(f"[error] hot_or_not_tournament_vote: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", str(e))


