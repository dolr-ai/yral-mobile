"""
Hot or Not Tournament - AI-powered video prediction game.

Users vote "hot" or "not" on videos and win if their prediction matches the AI's verdict.
Gemini 2.5 Flash analyzes videos during tournament creation.
"""

import json
import os
import random
import string
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone, timedelta
from enum import Enum
from typing import Any, Dict, List, Optional, Tuple

import firebase_admin
import requests
import google.generativeai as genai
from firebase_admin import auth, firestore
from firebase_functions import https_fn
from flask import Request, jsonify, make_response
from google.api_core.exceptions import GoogleAPICallError

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
    TOURNAMENT_SLOTS = [("20:00", "20:10")]  # 8 PM IST
    TOURNAMENT_ENTRY_COST = 15
    TOURNAMENT_TITLE = "Hot or Not Showdown"
else:
    TOURNAMENT_SLOTS = [("13:00", "13:10"), ("15:00", "15:10"), ("17:00", "17:10")]
    TOURNAMENT_ENTRY_COST = 100
    TOURNAMENT_TITLE = "HOT OR NOT"

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

# ─────────────────────  HELPERS  ────────────────────────
def error_response(status: int, code: str, message: str):
    payload = {"error": {"code": code, "message": message}}
    return make_response(jsonify(payload), status)

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
    Analyze video with Gemini 2.5 Flash to determine if it will go viral.
    Downloads video, uploads to Gemini, analyzes, and cleans up.
    Returns: {"verdict": "hot" | "not", "confidence": float, "reason": str, "error": str | None}
    """
    temp_path = None
    video_file = None

    try:
        genai.configure(api_key=api_key)
        model = genai.GenerativeModel(GEMINI_MODEL)

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

        # Step 2: Upload to Gemini
        video_file = genai.upload_file(temp_path, mime_type="video/mp4")

        # Wait for processing (with timeout)
        max_wait = 30  # seconds
        waited = 0
        while video_file.state.name == "PROCESSING" and waited < max_wait:
            time.sleep(2)
            waited += 2
            video_file = genai.get_file(video_file.name)

        if video_file.state.name == "FAILED":
            return {
                "verdict": random.choice(["hot", "not"]),
                "confidence": 0.5,
                "reason": "Gemini processing failed",
                "error": "PROCESSING_FAILED"
            }

        # Step 3: Analyze with Gemini
        prompt = """Determine if this video is mast(hit) or bakwaas(flop).
Respond with ONLY a JSON object:
{"verdict": "mast" or "bakwaas", "confidence": 0.0 to 1.0, "reason": "brief explanation"}"""

        response = model.generate_content([video_file, prompt])
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
        if video_file:
            try:
                genai.delete_file(video_file.name)
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
@https_fn.on_request(region="us-central1", timeout_sec=540, memory=512, secrets=["BALANCE_UPDATE_TOKEN", "GEMINI_API_KEY", "BACKEND_ADMIN_KEY"])
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
            return error_response(500, "CONFIG_ERROR", "GEMINI_API_KEY not configured")
        if not backend_admin_key:
            return error_response(500, "CONFIG_ERROR", "BACKEND_ADMIN_KEY not configured")

        # Parse request
        body = request.get_json(silent=True) or {}
        video_count = int(body.get("video_count", DEFAULT_VIDEO_COUNT))

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
            return error_response(502, "BACKEND_ERROR", f"Failed to register tournament: {err}")

        # 2. Fetch videos
        videos, err = _fetch_tournament_videos(tournament_id)
        if err:
            return error_response(502, "BACKEND_ERROR", f"Failed to fetch videos: {err}")

        if not videos:
            return error_response(502, "BACKEND_ERROR", "No videos returned from backend")

        # 3. Analyze videos with Gemini (parallel)
        print(f"[gemini] Starting analysis of {len(videos)} videos...")
        analyzed_videos = _analyze_videos_batch(videos, gemini_api_key, max_workers=10)
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
            "entryCost": TOURNAMENT_ENTRY_COST,
            "totalPrizePool": TOTAL_PRIZE_POOL,
            "prizeMap": PRIZE_MAP,
            "status": "scheduled",
            "title": TOURNAMENT_TITLE,
            "video_count": len(analyzed_videos),
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
        return error_response(500, "INTERNAL", str(e))


# ─────────────────────  REGISTRATION  ────────────────────────
@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"])
def register_hot_or_not_tournament(request: Request):
    """Register user for a Hot or Not tournament."""
    balance_update_token = os.environ.get("BALANCE_UPDATE_TOKEN", "")

    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Parse request
        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        tournament_id = str(body.get("tournament_id") or data.get("tournament_id") or "").strip()
        principal_id = str(body.get("principal_id") or data.get("principal_id") or "").strip()

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")
        if not principal_id:
            return error_response(400, "MISSING_PRINCIPAL_ID", "principal_id required")

        # Verify auth
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        # Validate tournament
        tournament_ref = db().collection(HOT_OR_NOT_TOURNAMENT_COLL).document(tournament_id)
        tournament_snap = tournament_ref.get()

        if not tournament_snap.exists:
            return error_response(404, "TOURNAMENT_NOT_FOUND", "Tournament not found")

        tournament_data = tournament_snap.to_dict() or {}
        status = tournament_data.get("status", "")

        if status not in ("scheduled", "live"):
            return error_response(400, "TOURNAMENT_NOT_OPEN", f"Tournament is {status}")

        # Check duplicate registration
        user_ref = tournament_ref.collection("users").document(principal_id)
        if user_ref.get().exists:
            return error_response(409, "ALREADY_REGISTERED", "Already registered for this tournament")

        # Deduct entry cost
        entry_cost = tournament_data.get("entryCost", TOURNAMENT_ENTRY_COST)
        success, err = _push_delta_yral_token(balance_update_token, principal_id, -entry_cost)

        if not success:
            return error_response(402, "PAYMENT_FAILED", f"Failed to deduct entry cost: {err}")

        # Create registration
        user_ref.set({
            "registered_at": firestore.SERVER_TIMESTAMP,
            "coins_paid": entry_cost,
            "diamonds": 20,
            "wins": 0,
            "losses": 0,
            "status": "registered",
            "updated_at": firestore.SERVER_TIMESTAMP,
        })

        return jsonify({
            "status": "registered",
            "tournament_id": tournament_id,
            "coins_paid": entry_cost,
            "diamonds": 20,
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "Invalid token")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print(f"[error] register_hot_or_not_tournament: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", str(e))


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

            return {
                "success": True,
                "outcome": outcome,
                "vote": vote,
                "ai_verdict": ai_verdict,
                "diamonds": new_diamonds,
                "diamond_delta": diamond_delta,
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

        return jsonify({
            "outcome": result["outcome"],
            "vote": result["vote"],
            "ai_verdict": result["ai_verdict"],
            "diamonds": result["diamonds"],
            "diamond_delta": result["diamond_delta"],
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "Invalid token")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print(f"[error] hot_or_not_tournament_vote: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", str(e))


# ─────────────────────  LEADERBOARD  ────────────────────────
@https_fn.on_request(region="us-central1")
def hot_or_not_tournament_leaderboard(request: Request):
    """Get leaderboard for a Hot or Not tournament."""
    try:
        # Parse request
        tournament_id = request.args.get("tournament_id") or ""
        principal_id = request.args.get("principal_id") or ""

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")

        # Get tournament
        tournament_ref = db().collection(HOT_OR_NOT_TOURNAMENT_COLL).document(tournament_id)
        tournament_snap = tournament_ref.get()

        if not tournament_snap.exists:
            return error_response(404, "TOURNAMENT_NOT_FOUND", "Tournament not found")

        tournament_data = tournament_snap.to_dict() or {}

        # Get all users with stats
        users_ref = tournament_ref.collection("users")
        users_query = users_ref.order_by("diamonds", direction=firestore.Query.DESCENDING).limit(100)
        users_snaps = users_query.stream()

        leaderboard = []
        user_row = None
        position = 0

        for snap in users_snaps:
            position += 1
            user_data = snap.to_dict() or {}

            row = {
                "principal_id": snap.id,
                "diamonds": user_data.get("diamonds", 0),
                "wins": user_data.get("wins", 0),
                "losses": user_data.get("losses", 0),
                "position": position,
                "prize": PRIZE_MAP.get(str(position)),
            }

            if position <= 10:
                leaderboard.append(row)

            if snap.id == principal_id:
                user_row = row

        return jsonify({
            "tournament_id": tournament_id,
            "status": tournament_data.get("status"),
            "title": tournament_data.get("title", TOURNAMENT_TITLE),
            "date": tournament_data.get("date"),
            "start_epoch_ms": tournament_data.get("start_epoch_ms"),
            "end_epoch_ms": tournament_data.get("end_epoch_ms"),
            "top_rows": leaderboard,
            "user_row": user_row,
            "prize_map": PRIZE_MAP,
        }), 200

    except Exception as e:
        print(f"[error] hot_or_not_tournament_leaderboard: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", str(e))


# ─────────────────────  LIST TOURNAMENTS  ────────────────────────
@https_fn.on_request(region="us-central1")
def hot_or_not_tournaments(request: Request):
    """List Hot or Not tournaments with optional filters."""
    try:
        status_filter = request.args.get("status")
        limit_count = int(request.args.get("limit", 10))

        query = db().collection(HOT_OR_NOT_TOURNAMENT_COLL)

        if status_filter:
            query = query.where("status", "==", status_filter)

        query = query.order_by("start_epoch_ms", direction=firestore.Query.DESCENDING).limit(limit_count)

        tournaments = []
        for snap in query.stream():
            data = snap.to_dict() or {}
            tournaments.append({
                "tournament_id": snap.id,
                "date": data.get("date"),
                "start_time": data.get("start_time"),
                "end_time": data.get("end_time"),
                "start_epoch_ms": data.get("start_epoch_ms"),
                "end_epoch_ms": data.get("end_epoch_ms"),
                "status": data.get("status"),
                "title": data.get("title"),
                "entryCost": data.get("entryCost"),
                "totalPrizePool": data.get("totalPrizePool"),
                "video_count": data.get("video_count"),
            })

        return jsonify({"tournaments": tournaments}), 200

    except Exception as e:
        print(f"[error] hot_or_not_tournaments: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", str(e))


# ─────────────────────  VIDEO ANALYSIS RESULTS  ────────────────────────
@https_fn.on_request(region="us-central1")
def hot_or_not_tournament_videos(request: Request):
    """Get AI analysis results for videos in a tournament."""
    try:
        tournament_id = request.args.get("tournament_id") or ""

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")

        # Get tournament
        tournament_ref = db().collection(HOT_OR_NOT_TOURNAMENT_COLL).document(tournament_id)
        tournament_snap = tournament_ref.get()

        if not tournament_snap.exists:
            return error_response(404, "TOURNAMENT_NOT_FOUND", "Tournament not found")

        # Get videos with analysis
        videos_ref = tournament_ref.collection("videos")
        videos_snaps = videos_ref.stream()

        videos = []
        hot_count = 0
        not_count = 0

        for snap in videos_snaps:
            data = snap.to_dict() or {}
            verdict = data.get("ai_verdict", "not")

            if verdict == "hot":
                hot_count += 1
            else:
                not_count += 1

            videos.append({
                "video_id": snap.id,
                "ai_verdict": verdict,
                "ai_confidence": data.get("ai_confidence", 0.5),
                "ai_reason": data.get("ai_reason", ""),
                "video_url": data.get("video_url", ""),
            })

        return jsonify({
            "tournament_id": tournament_id,
            "total_videos": len(videos),
            "hot_count": hot_count,
            "not_count": not_count,
            "videos": videos,
        }), 200

    except Exception as e:
        print(f"[error] hot_or_not_tournament_videos: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", str(e))
