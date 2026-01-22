import json
import os
import random
import string
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from datetime import datetime, timezone, timedelta
from enum import Enum
from typing import Any, Dict, List, Optional, Tuple

import firebase_admin
import requests
from firebase_admin import firestore
from firebase_functions import https_fn
from flask import Request, jsonify
from google.api_core.exceptions import AlreadyExists
from google.cloud import tasks_v2
from google.protobuf import timestamp_pb2
from mixpanel import Mixpanel

# Gemini AI SDK
from google import genai


class TournamentStatus(str, Enum):
    SCHEDULED = "scheduled"
    LIVE = "live"
    ENDED = "ended"
    SETTLED = "settled"
    CANCELLED = "cancelled"


@dataclass
class Tournament:
    date: str
    start_time: str
    end_time: str
    entry_cost: int
    total_prize_pool: int
    start_at: Optional[datetime] = None
    end_at: Optional[datetime] = None
    start_epoch_ms: Optional[int] = None
    end_epoch_ms: Optional[int] = None
    status: TournamentStatus = TournamentStatus.SCHEDULED
    prize_map: Dict[str, int] = field(default_factory=dict)
    created_at: Optional[Any] = None
    updated_at: Optional[Any] = None
    title: str = "SMILEY SHOWDOWN"
    type: str = "smiley"

    def to_firestore(self) -> Dict[str, Any]:
        """Serialize to a dict ready for Firestore writes."""
        return {
            "date": self.date,
            "start_time": self.start_time,
            "end_time": self.end_time,
            "start_at": self.start_at,
            "end_at": self.end_at,
            "start_epoch_ms": self.start_epoch_ms,
            "end_epoch_ms": self.end_epoch_ms,
            "entryCost": self.entry_cost,
            "totalPrizePool": self.total_prize_pool,
            "status": self.status.value,
            "prizeMap": self.prize_map,
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "title": self.title,
            "type": self.type,
        }


IST = timezone(timedelta(hours=5, minutes=30))
_db = None

# BTC Settlement Constants
BALANCE_URL_CKBTC = "https://yral-hot-or-not.go-bazzinga.workers.dev/v2/transfer_ckbtc"
TICKER_URL = "https://blockchain.info/ticker"
SATOSHIS_PER_BTC = 100_000_000

# Backend API Constants
BACKEND_TOURNAMENT_REGISTER_URL = "https://recsys-on-premise.fly.dev/tournament/register"
BACKEND_TOURNAMENT_VIDEOS_URL = "https://recsys-on-premise.fly.dev/tournament/{tournament_id}/videos"
DEFAULT_VIDEO_COUNT = 500

# Gemini AI Constants
GEMINI_MODEL = "gemini-2.0-flash"
CLOUDFLARE_PREFIX = "https://customer-2p3jflss4r4hmpnz.cloudflarestream.com/"
CLOUDFLARE_MP4_SUFFIX = "/downloads/default.mp4"
SEEDED_VOTE_COUNT = 5

# Default emoji fallback (used when Gemini analysis fails)
DEFAULT_EMOJIS = [
    {"id": "heart", "unicode": "❤️", "display_name": "Heart"},
    {"id": "laugh", "unicode": "😂", "display_name": "Laugh"},
    {"id": "fire", "unicode": "🔥", "display_name": "Fire"},
    {"id": "surprise", "unicode": "😮", "display_name": "Surprise"},
    {"id": "rocket", "unicode": "🚀", "display_name": "Rocket"},
]

# Environment detection
GCLOUD_PROJECT = os.environ.get("GCLOUD_PROJECT", "")
IS_PRODUCTION = GCLOUD_PROJECT == "yral-mobile"

# Environment-specific tournament configuration
if IS_PRODUCTION:
    # Production: Single tournament at 7 PM IST
    TOURNAMENT_SLOTS = [("19:00", "19:10")]
    TOURNAMENT_TITLE = "Daily Showdown"
    TOURNAMENT_ENTRY_COST = 10
else:
    # Staging: Multiple tournaments throughout day
    TOURNAMENT_SLOTS = [("12:45", "12:55"), ("14:00", "14:10"), ("16:00", "16:10")]
    TOURNAMENT_TITLE = "SMILEY SHOWDOWN"
    TOURNAMENT_ENTRY_COST = 100


def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db


# ─────────────────────  MIXPANEL HELPERS  ────────────────────────
def _get_username(principal_id: str) -> Optional[str]:
    """Get username from user's Firestore document."""
    try:
        user_doc = db().document(f"users/{principal_id}").get()
        if user_doc.exists:
            return user_doc.to_dict().get("username")
    except Exception as e:
        print(f"[mixpanel] Failed to get username for {principal_id}: {e}", file=sys.stderr)
    return None


def _track_reward_event(
    principal_id: str,
    username: Optional[str],
    reward_amount: float,
    tournament_id: str,
    position: int,
    reward_type: str,  # "prize" or "refund"
    currency: str = "INR"
) -> None:
    """
    Track tournament reward distribution event in Mixpanel.

    Args:
        principal_id: User's principal ID
        username: User's username (if available)
        reward_amount: Amount rewarded
        tournament_id: Tournament ID
        position: User's position (0 for refunds)
        reward_type: "prize" for winners, "refund" for cancelled/insufficient
        currency: Currency of reward (INR, BTC, COINS)
    """
    mixpanel_token = os.environ.get("MIXPANEL_TOKEN")
    if not mixpanel_token:
        print("[mixpanel] MIXPANEL_TOKEN not configured, skipping event", file=sys.stderr)
        return

    try:
        mp = Mixpanel(mixpanel_token)
        mp.track(principal_id, "tournament_reward_distributed", {
            "principal_id": principal_id,
            "username": username or "",
            "reward_amount": reward_amount,
            "tournament_id": tournament_id,
            "position": position,
            "reward_type": reward_type,
            "currency": currency,
        })
        print(f"[mixpanel] Tracked reward event: {principal_id}, {reward_type}, {reward_amount} {currency}")
    except Exception as e:
        print(f"[mixpanel] Failed to track event: {e}", file=sys.stderr)


# ─────────────────────  BACKEND API HELPERS  ────────────────────────
def _register_tournament_backend(admin_key: str, video_count: int = DEFAULT_VIDEO_COUNT) -> tuple[str | None, str | None]:
    """
    Register tournament with backend API to get videos for the tournament.

    Args:
        admin_key: The backend admin API key
        video_count: Number of videos to associate with the tournament

    Returns:
        Tuple of (backend_tournament_id, error_message)
    """
    headers = {
        "x-admin-key": admin_key,
        "Content-Type": "application/json",
    }
    body = {"video_count": video_count}

    try:
        resp = requests.post(BACKEND_TOURNAMENT_REGISTER_URL, json=body, timeout=30, headers=headers)
        if resp.status_code == 200:
            data = resp.json()
            backend_tournament_id = data.get("tournament_id")
            if backend_tournament_id:
                print(f"[backend] Registered tournament: {backend_tournament_id} with {video_count} videos")
                return backend_tournament_id, None
            return None, "No tournament_id in response"
        return None, f"Status: {resp.status_code}, Body: {resp.text}"
    except requests.RequestException as e:
        # Log detailed backend exception server-side, but return a generic error message
        print(f"[backend] Tournament registration request failed: {e}", file=sys.stderr)
        return None, "Backend request failed"


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


def _form_video_url(video_id: str) -> str:
    """Form Cloudflare MP4 URL from video ID."""
    return f"{CLOUDFLARE_PREFIX}{video_id}{CLOUDFLARE_MP4_SUFFIX}"


def _analyze_video_for_emojis(video_url: str, api_key: str) -> Dict[str, Any]:
    """
    Analyze video with Gemini to determine relevant emojis.

    Returns: {
        "emojis": [{"id": str, "unicode": str, "display_name": str}, ...],
        "top_pick_id": str,
        "confidence": float,
        "reason": str,
        "error": str | None
    }
    """
    temp_path = None
    video_file = None
    client = None

    try:
        client = genai.Client(api_key=api_key)

        # Download video
        video_resp = requests.get(video_url, timeout=60, stream=True)
        if video_resp.status_code != 200:
            return {
                "emojis": DEFAULT_EMOJIS,
                "top_pick_id": DEFAULT_EMOJIS[0]["id"],
                "confidence": 0.5,
                "reason": "Download failed",
                "error": f"HTTP {video_resp.status_code}"
            }

        # Save to temp file
        with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as f:
            for chunk in video_resp.iter_content(chunk_size=8192):
                f.write(chunk)
            temp_path = f.name

        # Upload to Gemini
        video_file = client.files.upload(file=temp_path)

        # Wait for processing
        max_wait = 30
        waited = 0
        while video_file.state.name == "PROCESSING" and waited < max_wait:
            time.sleep(2)
            waited += 2
            video_file = client.files.get(name=video_file.name)

        if video_file.state.name == "FAILED":
            return {
                "emojis": DEFAULT_EMOJIS,
                "top_pick_id": DEFAULT_EMOJIS[0]["id"],
                "confidence": 0.5,
                "reason": "Gemini processing failed",
                "error": "PROCESSING_FAILED"
            }

        # Analyze with Gemini
        prompt = """Analyze this video and suggest exactly 5 emojis that best represent its emotional content.
You can suggest ANY emoji that fits - be creative and match the video's vibe.

Consider:
- The dominant emotion (joy, excitement, surprise, disgust, love, fear, etc.)
- The video's energy level and vibe
- What reaction viewers would most likely have
- Cultural relevance (Indian audience, tier 2/3 cities)

Return ONLY a valid JSON object with EXACTLY 5 emojis:
{
  "emojis": [
    {"id": "unique_lowercase_id", "unicode": "actual emoji character", "display_name": "Human Name"},
    {"id": "...", "unicode": "...", "display_name": "..."},
    {"id": "...", "unicode": "...", "display_name": "..."},
    {"id": "...", "unicode": "...", "display_name": "..."},
    {"id": "...", "unicode": "...", "display_name": "..."}
  ],
  "top_pick": "id_of_most_fitting_emoji",
  "confidence": 0.0 to 1.0,
  "reason": "brief explanation"
}

Example output:
{
  "emojis": [
    {"id": "laugh", "unicode": "😂", "display_name": "Laugh"},
    {"id": "fire", "unicode": "🔥", "display_name": "Fire"},
    {"id": "heart_eyes", "unicode": "😍", "display_name": "Heart Eyes"},
    {"id": "clap", "unicode": "👏", "display_name": "Clap"},
    {"id": "mind_blown", "unicode": "🤯", "display_name": "Mind Blown"}
  ],
  "top_pick": "laugh",
  "confidence": 0.85,
  "reason": "Funny video with impressive moments"
}"""

        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=[video_file, prompt]
        )
        response_text = response.text.strip()

        # Parse JSON from response (handles code blocks)
        if "```json" in response_text:
            response_text = response_text.split("```json")[1].split("```")[0].strip()
        elif "```" in response_text:
            response_text = response_text.split("```")[1].split("```")[0].strip()

        result = json.loads(response_text)
        emojis = result.get("emojis", DEFAULT_EMOJIS)
        top_pick = result.get("top_pick", emojis[0]["id"] if emojis else "heart")
        confidence = float(result.get("confidence", 0.7))
        reason = result.get("reason", "")

        # Ensure exactly 5 emojis
        if len(emojis) < 5:
            emojis = DEFAULT_EMOJIS[:5]
        elif len(emojis) > 5:
            emojis = emojis[:5]

        return {
            "emojis": emojis,
            "top_pick_id": top_pick,
            "confidence": confidence,
            "reason": reason,
            "error": None
        }

    except (json.JSONDecodeError, KeyError, ValueError) as e:
        return {
            "emojis": DEFAULT_EMOJIS,
            "top_pick_id": DEFAULT_EMOJIS[0]["id"],
            "confidence": 0.5,
            "reason": "",
            "error": f"Parse error: {e}"
        }

    except Exception as e:
        return {
            "emojis": DEFAULT_EMOJIS,
            "top_pick_id": DEFAULT_EMOJIS[0]["id"],
            "confidence": 0.5,
            "reason": "",
            "error": str(e)
        }

    finally:
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


def _analyze_videos_for_emojis_batch(videos: List[Dict], api_key: str, max_workers: int = 5) -> List[Dict]:
    """Analyze multiple videos in parallel for emoji selection."""
    results = []

    def analyze_single(video: Dict, index: int) -> Dict:
        video_id = video.get("video_id") or video.get("videoID")
        if not video_id:
            return {"video_id": None, "error": "No video_id"}

        video_url = _form_video_url(video_id)
        print(f"[gemini] Analyzing video {index + 1} for emojis: {video_id[:16]}...")
        analysis = _analyze_video_for_emojis(video_url, api_key)

        return {
            "video_id": video_id,
            "video_url": video_url,
            "emojis": analysis["emojis"],
            "top_pick_id": analysis["top_pick_id"],
            "confidence": analysis["confidence"],
            "reason": analysis.get("reason", ""),
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
                    "emojis": DEFAULT_EMOJIS,
                    "top_pick_id": DEFAULT_EMOJIS[0]["id"],
                    "confidence": 0.5,
                    "reason": "",
                    "analysis_error": str(e),
                })

    return results


def _seed_votes_for_video(
    tournament_id: str,
    video_id: str,
    top_emoji_id: str,
    all_emoji_ids: List[str],
    seed_count: int = SEEDED_VOTE_COUNT,
):
    """Seed initial votes on the top emoji for a video and initialize all shards."""
    # Initialize video document if needed
    video_ref = db().document(f"tournaments/{tournament_id}/videos/{video_id}")
    video_ref.set({"created_at": firestore.SERVER_TIMESTAMP}, merge=True)

    # Initialize all 5 shards with zero counts for all emojis
    # This prevents 404 errors when users vote on random shards
    zero_counts = {emoji_id: 0 for emoji_id in all_emoji_ids}
    for shard_num in range(5):  # TOURNAMENT_SHARDS = 5
        shard_ref = db().document(f"tournaments/{tournament_id}/videos/{video_id}/tallies/shard_{shard_num}")
        if shard_num == 0:
            # Shard 0 gets the seeded votes
            shard_data = zero_counts.copy()
            shard_data[top_emoji_id] = seed_count
            shard_ref.set(shard_data, merge=True)
        else:
            # Other shards get zeros
            shard_ref.set(zero_counts, merge=True)

    print(f"[seed] Seeded {seed_count} votes for emoji '{top_emoji_id}' on video {video_id[:16]}")


def _store_video_emojis_and_seed(tournament_id: str, analyzed_videos: List[Dict]):
    """Store per-video emoji data in Firestore and seed initial votes."""
    videos_ref = db().collection(f"tournaments/{tournament_id}/videos")
    batch = db().batch()
    batch_count = 0

    for video_data in analyzed_videos:
        video_id = video_data.get("video_id")
        if not video_id:
            continue

        video_doc_ref = videos_ref.document(video_id)
        batch.set(video_doc_ref, {
            "emojis": video_data["emojis"],
            "top_pick_id": video_data["top_pick_id"],
            "ai_confidence": video_data.get("confidence", 0.5),
            "ai_reason": video_data.get("reason", ""),
            "analyzed_at": firestore.SERVER_TIMESTAMP,
        }, merge=True)

        batch_count += 1
        if batch_count >= 500:
            batch.commit()
            batch = db().batch()
            batch_count = 0

    if batch_count > 0:
        batch.commit()

    print(f"[store] Stored emoji data for {len(analyzed_videos)} videos")

    # Seed votes for each video
    for video_data in analyzed_videos:
        video_id = video_data.get("video_id")
        if video_id:
            all_emoji_ids = [e["id"] for e in video_data.get("emojis", [])]
            _seed_votes_for_video(
                tournament_id,
                video_id,
                video_data["top_pick_id"],
                all_emoji_ids,
            )


# ─────────────────────  COIN HELPERS  ────────────────────────
def _tx_id() -> str:
    """Generate unique transaction ID."""
    now = datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
    rnd = "".join(random.choices(string.ascii_lowercase + string.digits, k=4))
    return f"{now}_{rnd}"


def _tx_coin_change(principal_id: str, delta: int, reason: str, tournament_id: str | None = None) -> int:
    """Atomically change user's coin balance and log to ledger."""
    user_ref = db().document(f"users/{principal_id}")
    ledger_ref = user_ref.collection("transactions").document(_tx_id())

    @firestore.transactional
    def _commit(tx: firestore.Transaction):
        tx.set(user_ref,
               {"coins": firestore.Increment(delta)},
               merge=True)
        tx.set(ledger_ref,
               {"delta": delta,
                "reason": reason,
                "tournament_id": tournament_id,
                "at": firestore.SERVER_TIMESTAMP})

    _commit(db().transaction())
    return int(user_ref.get().get("coins") or 0)


def _refund_tournament_users(tournament_id: str, collection_name: str = "tournaments") -> List[dict]:
    """
    Refund all registered users for a cancelled tournament.

    Args:
        tournament_id: The tournament document ID
        collection_name: Either "tournaments" or "hot_or_not_tournaments"

    Returns list of refund results.
    """
    users_ref = db().collection(f"{collection_name}/{tournament_id}/users")
    registered_users = users_ref.where("status", "==", "registered").stream()

    refund_results = []

    for user_snap in registered_users:
        principal_id = user_snap.id
        user_data = user_snap.to_dict() or {}
        coins_paid = int(user_data.get("coins_paid") or 0)

        if coins_paid <= 0:
            continue

        try:
            # Refund coins
            _tx_coin_change(principal_id, coins_paid, "TOURNAMENT_REFUND", tournament_id)

            # Update registration status
            user_snap.reference.update({
                "status": "refunded",
                "refunded_at": firestore.SERVER_TIMESTAMP,
                "updated_at": firestore.SERVER_TIMESTAMP
            })

            refund_results.append({
                "principal_id": principal_id,
                "coins_refunded": coins_paid,
                "success": True
            })
            print(f"[refund] {principal_id} refunded {coins_paid} coins for {tournament_id}")

            # Track Mixpanel event for refund
            username = _get_username(principal_id)
            _track_reward_event(
                principal_id=principal_id,
                username=username,
                reward_amount=coins_paid,
                tournament_id=tournament_id,
                position=0,  # 0 indicates refund, not a position
                reward_type="refund",
                currency="COINS"
            )

        except Exception as e:
            refund_results.append({
                "principal_id": principal_id,
                "coins_refunded": 0,
                "success": False,
                "error": "Refund failed"
            })
            print(f"[refund] FAILED for {principal_id}: {e}", file=sys.stderr)

    return refund_results


# ─────────────────────  BTC SETTLEMENT HELPERS  ────────────────────────
def _fetch_btc_price_inr() -> float:
    """Fetch current BTC price in INR from blockchain.info ticker."""
    resp = requests.get(TICKER_URL, timeout=10)
    resp.raise_for_status()
    data = resp.json()
    last = float(data["INR"]["last"])
    if last <= 0:
        raise ValueError("BTC price must be positive")
    return last


def _inr_to_ckbtc(amount_inr: int, btc_price_inr: float) -> int:
    """Convert INR amount to ckBTC (satoshis)."""
    satoshis = amount_inr * (SATOSHIS_PER_BTC / btc_price_inr)
    # At least 1 satoshi to avoid zero-value transfers
    return max(1, int(round(satoshis)))


def _send_ckbtc(token: str, principal_id: str, amount_ckbtc: int, memo: str) -> tuple[bool, str | None]:
    """Send ckBTC to a principal. Returns (success, error_message)."""
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    }
    body = {
        "amount": amount_ckbtc,
        "recipient_principal": principal_id,
        "memo_text": memo,
    }
    try:
        resp = requests.post(BALANCE_URL_CKBTC, json=body, timeout=30, headers=headers)
    except requests.RequestException as e:
        print(f"[settlement] Request failed for {principal_id}: {e}", file=sys.stderr)
        return False, "Transfer request failed"
    if resp.status_code != 200:
        print(
            f"[settlement] Transfer failed for {principal_id}: status={resp.status_code} body={resp.text}",
            file=sys.stderr,
        )
        return False, "Transfer failed"
    payload = resp.json()
    if payload.get("success"):
        return True, None
    print(f"[settlement] Transfer unsuccessful for {principal_id}: {payload}", file=sys.stderr)
    return False, "Transfer failed"


def _count_players_who_played(tournament_id: str, collection_name: str = "tournaments") -> int:
    """Count users who actually played (cast at least one vote) in the tournament.

    Args:
        tournament_id: The tournament document ID
        collection_name: Either "tournaments" or "hot_or_not_tournaments"
    """
    users_ref = db().collection(f"{collection_name}/{tournament_id}/users")
    # A player "played" if they have any wins or losses (i.e., cast at least one vote)
    # Firestore doesn't support OR queries easily, so we count both separately

    # Hot or Not uses "wins"/"losses", smiley uses "tournament_wins"/"tournament_losses"
    is_hot_or_not = collection_name == "hot_or_not_tournaments"
    wins_field = "wins" if is_hot_or_not else "tournament_wins"
    losses_field = "losses" if is_hot_or_not else "tournament_losses"

    # Count users with at least 1 win
    wins_snaps = list(users_ref.where(wins_field, ">", 0).stream())
    players_with_wins = {snap.id for snap in wins_snaps}

    # Count users with at least 1 loss but no wins (they played but didn't win any)
    losses_snaps = list(users_ref.where(losses_field, ">", 0).stream())
    players_with_losses = {snap.id for snap in losses_snaps}

    # Union of both sets = all players who played
    all_players = players_with_wins | players_with_losses
    return len(all_players)


def _compute_settlement_leaderboard(tournament_id: str, limit: int = 10, collection_name: str = "tournaments") -> List[dict]:
    """Compute top N users by diamond balance with strict ranking for settlement.

    Only includes users who have played at least 1 game (wins > 0 or losses > 0).

    Ranking order:
    1. Diamonds DESC (more diamonds = higher rank)
    2. Total games (wins + losses) DESC (tiebreaker: more games = higher rank)
    3. updated_at ASC (second tiebreaker: earlier = higher rank)

    Args:
        tournament_id: The tournament document ID
        limit: Number of top users to return
        collection_name: Either "tournaments" or "hot_or_not_tournaments"
    """
    users_ref = db().collection(f"{collection_name}/{tournament_id}/users")
    # Fetch extra users to account for filtering out those who never played
    # Need high multiplier because many non-players have 20 diamonds (initial balance)
    # which ranks higher than players who lost (< 20 diamonds)
    snaps = (
        users_ref.order_by("diamonds", direction=firestore.Query.DESCENDING)
                 .limit(limit * 50)
                 .stream()
    )

    # Hot or Not uses "wins"/"losses", smiley uses "tournament_wins"/"tournament_losses"
    is_hot_or_not = collection_name == "hot_or_not_tournaments"
    wins_field = "wins" if is_hot_or_not else "tournament_wins"
    losses_field = "losses" if is_hot_or_not else "tournament_losses"

    # Collect all qualifying users first
    candidates = []
    for snap in snaps:
        data = snap.to_dict() or {}
        wins = int(data.get(wins_field) or 0)
        losses = int(data.get(losses_field) or 0)

        # Skip users who never played
        if wins == 0 and losses == 0:
            continue

        diamonds = int(data.get("diamonds") or 0)
        total_games = wins + losses
        updated_at = data.get("updated_at")  # Firestore timestamp

        candidates.append({
            "principal_id": snap.id,
            "diamonds": diamonds,
            "wins": wins,
            "total_games": total_games,
            "updated_at": updated_at,
        })

    # Sort by: diamonds DESC, total_games DESC, updated_at ASC
    def sort_key(x):
        updated = x.get("updated_at")
        updated_ts = updated.timestamp() if updated else float('inf')
        return (-x["diamonds"], -x["total_games"], updated_ts)

    candidates.sort(key=sort_key)

    # Take top N and assign strict positions (1, 2, 3, ...)
    rows = []
    for i, candidate in enumerate(candidates[:limit], start=1):
        rows.append({
            "principal_id": candidate["principal_id"],
            "diamonds": candidate["diamonds"],
            "wins": candidate["wins"],
            "position": i
        })

    return rows


def _settle_tournament_prizes(tournament_id: str, prize_map: Dict[str, int], collection_name: str = "tournaments") -> Dict[str, Any]:
    """
    Settle tournament prizes by sending BTC equivalent to winners.

    Args:
        tournament_id: The tournament to settle
        prize_map: Map of position (str) to INR prize amount
        collection_name: Either "tournaments" or "hot_or_not_tournaments"

    Returns:
        Settlement results including successes and failures
    """
    token = os.environ.get("BALANCE_UPDATE_TOKEN")
    if not token:
        return {
            "success": False,
            "error": "BALANCE_UPDATE_TOKEN not configured",
            "rewards_sent": 0,
            "rewards_failed": 0,
            "details": []
        }

    # Fetch current BTC price
    try:
        btc_price_inr = _fetch_btc_price_inr()
    except Exception as e:
        print(f"[settlement] Failed to fetch BTC price: {e}", file=sys.stderr)
        return {
            "success": False,
            "error": "Failed to fetch BTC price",
            "rewards_sent": 0,
            "rewards_failed": 0,
            "details": []
        }

    print(f"[settlement] Tournament {tournament_id}: BTC price INR = {btc_price_inr}")

    # Get leaderboard (top positions that have prizes)
    max_prize_position = max((int(p) for p in prize_map.keys()), default=10)
    leaderboard = _compute_settlement_leaderboard(tournament_id, limit=max_prize_position, collection_name=collection_name)

    results = []
    rewards_sent = 0
    rewards_failed = 0
    total_inr = 0
    total_ckbtc = 0

    for row in leaderboard:
        position = str(row["position"])
        principal_id = row["principal_id"]
        prize_inr = prize_map.get(position)

        if not prize_inr:
            continue

        # IDEMPOTENCY CHECK: Skip if user already received reward
        user_ref = db().document(f"{collection_name}/{tournament_id}/users/{principal_id}")
        user_snap = user_ref.get()
        if user_snap.exists:
            user_data = user_snap.to_dict()
            if user_data.get("status") == "rewarded" or user_data.get("prize_sent_at"):
                print(f"[settlement] Skipping {principal_id} (#{position}) - already rewarded")
                # Add to results as already-sent
                results.append({
                    "principal_id": principal_id,
                    "position": int(position),
                    "prize_inr": user_data.get("prize_inr", prize_inr),
                    "prize_ckbtc": user_data.get("prize_ckbtc", 0),
                    "success": True,
                    "error": None,
                    "skipped": True  # Flag indicating this was already sent
                })
                rewards_sent += 1
                total_inr += user_data.get("prize_inr", prize_inr)
                total_ckbtc += user_data.get("prize_ckbtc", 0)
                continue

        # Convert INR to ckBTC
        prize_ckbtc = _inr_to_ckbtc(prize_inr, btc_price_inr)
        memo = f"Tournament prize #{position} - {tournament_id}"

        # Send ckBTC
        success, error = _send_ckbtc(token, principal_id, prize_ckbtc, memo)

        result = {
            "principal_id": principal_id,
            "position": int(position),
            "prize_inr": prize_inr,
            "prize_ckbtc": prize_ckbtc,
            "success": success,
            "error": error
        }
        results.append(result)

        if success:
            rewards_sent += 1
            total_inr += prize_inr
            total_ckbtc += prize_ckbtc
            print(f"[settlement] Sent {prize_ckbtc} ckBTC (INR {prize_inr}) to {principal_id} (#{position})")

            # Record settlement in user's tournament registration
            try:
                user_ref = db().document(f"{collection_name}/{tournament_id}/users/{principal_id}")
                user_ref.update({
                    "prize_inr": prize_inr,
                    "prize_ckbtc": prize_ckbtc,
                    "prize_sent_at": firestore.SERVER_TIMESTAMP,
                    "status": "rewarded",
                    "updated_at": firestore.SERVER_TIMESTAMP
                })
            except Exception as e:
                print(f"[settlement] Failed to update user record: {e}", file=sys.stderr)

            # Track Mixpanel event for successful reward
            username = _get_username(principal_id)
            _track_reward_event(
                principal_id=principal_id,
                username=username,
                reward_amount=prize_inr,
                tournament_id=tournament_id,
                position=int(position),
                reward_type="prize",
                currency="INR"
            )
        else:
            rewards_failed += 1
            print(f"[settlement] FAILED to send to {principal_id}: {error}", file=sys.stderr)

    return {
        "success": rewards_failed == 0,
        "btc_price_inr": btc_price_inr,
        "rewards_sent": rewards_sent,
        "rewards_failed": rewards_failed,
        "total_inr": total_inr,
        "total_ckbtc": total_ckbtc,
        "details": results
    }


def _today_ist_str() -> str:
    return datetime.now(IST).strftime("%Y-%m-%d")


def _normalize_prize_map(raw: Any) -> Dict[str, int]:
    if not isinstance(raw, dict):
        return {}
    normalized: Dict[str, int] = {}
    for k, v in raw.items():
        if v is None:
            continue
        try:
            normalized[str(k)] = int(v)
        except (TypeError, ValueError):
            continue
    return normalized


def _safe_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _make_doc_id(date_str: str, start_time: str, end_time: str) -> str:
    return f"{date_str}-{start_time}-{end_time}"


def _ist_datetime(date_str: str, time_str: str) -> datetime:
    hour, minute = map(int, time_str.split(":", 1))
    return datetime.strptime(date_str, "%Y-%m-%d").replace(
        hour=hour, minute=minute, second=0, microsecond=0, tzinfo=IST
    )


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


def _schedule_status_task(doc_id: str, target_status: TournamentStatus, run_at: datetime):
    client = _tasks_client()
    parent = _queue_path(client)
    url = _function_url("update_tournament_status")

    payload = json.dumps({"tournament_id": doc_id, "status": target_status.value}).encode()
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
    print(f"[tasks] scheduled {target_status.value} for {doc_id} at {run_at.isoformat()} ({response.name})")


@https_fn.on_request(region="us-central1", timeout_sec=1500, memory=2048, secrets=["BACKEND_ADMIN_KEY", "GEMINI_API_KEY"])
def create_tournaments(cloud_event):
    """
    Cloud Scheduler target (run daily at 12am IST) to create tournaments.
    Also accepts manual HTTP POST requests with custom parameters.
    Includes Gemini AI video analysis for dynamic emoji selection.
    """
    try:
        backend_admin_key = os.environ.get("BACKEND_ADMIN_KEY")
        gemini_api_key = os.environ.get("GEMINI_API_KEY")

        if not backend_admin_key:
            print("[create_tournaments] BACKEND_ADMIN_KEY not configured", file=sys.stderr)
            return jsonify({"error": "INTERNAL", "message": "An internal error occurred"}), 500

        if not gemini_api_key:
            print("[create_tournaments] GEMINI_API_KEY not configured", file=sys.stderr)
            return jsonify({"error": "INTERNAL", "message": "An internal error occurred"}), 500

        env_name = "production" if IS_PRODUCTION else "staging"
        print(f"[create_tournaments] Running in {env_name} mode (project: {GCLOUD_PROJECT})")

        # Parse request body for custom parameters (manual invocation)
        body = cloud_event.get_json(silent=True) or {}

        # Custom parameter overrides
        custom_title = body.get("title")
        custom_entry_cost = body.get("entry_cost")
        custom_prize_map = body.get("prize_map")
        custom_total_prize_pool = body.get("total_prize_pool")
        custom_video_count = body.get("video_count")
        custom_start_time = body.get("start_time")
        custom_end_time = body.get("end_time")

        date_str = _today_ist_str()
        entry_cost = int(custom_entry_cost) if custom_entry_cost is not None else TOURNAMENT_ENTRY_COST
        total_prize_pool = int(custom_total_prize_pool) if custom_total_prize_pool is not None else 1500
        title = custom_title if custom_title is not None else TOURNAMENT_TITLE
        video_count = int(custom_video_count) if custom_video_count is not None else DEFAULT_VIDEO_COUNT

        # Use custom prize map if provided, otherwise use default
        if custom_prize_map is not None:
            prize_map = _normalize_prize_map(custom_prize_map)
        else:
            prize_map: Dict[str, int] = {
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
            }

        # Use custom slot if both start_time and end_time provided, otherwise use default slots
        if custom_start_time and custom_end_time:
            slots = [(custom_start_time, custom_end_time)]
            print(f"[create_tournaments] Using custom slot: {custom_start_time} - {custom_end_time}")
        else:
            slots = TOURNAMENT_SLOTS

        created, skipped, errors = [], [], []

        for start_time, end_time in slots:
            start_dt = _ist_datetime(date_str, start_time)
            end_dt = _ist_datetime(date_str, end_time)

            # Register with backend to get tournament ID and videos
            tournament_id, backend_error = _register_tournament_backend(backend_admin_key, video_count=video_count)
            if backend_error:
                fallback_id = _make_doc_id(date_str, start_time, end_time)
                # Log detailed backend error server-side, but keep user-visible reason generic
                print(f"[create_tournaments] Backend registration failed for {fallback_id}: {backend_error}", file=sys.stderr)
                errors.append({"id": fallback_id, "reason": "Backend registration failed"})
                continue

            tour = Tournament(
                date=date_str,
                start_time=start_time,
                end_time=end_time,
                start_at=start_dt,
                end_at=end_dt,
                start_epoch_ms=int(start_dt.timestamp() * 1000),
                end_epoch_ms=int(end_dt.timestamp() * 1000),
                entry_cost=entry_cost,
                total_prize_pool=total_prize_pool,
                prize_map=prize_map,
                status=TournamentStatus.SCHEDULED,
                created_at=firestore.SERVER_TIMESTAMP,
                updated_at=firestore.SERVER_TIMESTAMP,
                title=title,
            )

            # Use backend tournament ID as Firestore document ID
            ref = db().collection("tournaments").document(tournament_id)
            try:
                ref.create(tour.to_firestore())
                created.append(tournament_id)
                _schedule_status_task(tournament_id, TournamentStatus.LIVE, start_dt)
                _schedule_status_task(tournament_id, TournamentStatus.ENDED, end_dt)

                # Fetch videos and analyze with Gemini for dynamic emoji selection
                videos, fetch_error = _fetch_tournament_videos(tournament_id)
                if fetch_error or not videos:
                    print(f"[create_tournaments] Failed to fetch videos for {tournament_id}: {fetch_error}", file=sys.stderr)
                    print(f"[create_tournaments] Continuing with tournament (no emoji analysis)")
                else:
                    print(f"[gemini] Starting emoji analysis for {len(videos)} videos...")
                    analyzed_videos = _analyze_videos_for_emojis_batch(videos, gemini_api_key, max_workers=5)
                    print(f"[gemini] Completed emoji analysis for {len(analyzed_videos)} videos")

                    # Store emoji data and seed initial votes
                    _store_video_emojis_and_seed(tournament_id, analyzed_videos)

            except AlreadyExists:
                skipped.append({"id": tournament_id, "reason": "Already exists"})
            except Exception as e:
                print(f"[create_tournaments] Failed to create {tournament_id}: {e}", file=sys.stderr)
                errors.append({"id": tournament_id, "reason": "Internal error"})

        status_code = 200 if not errors else 207
        print(f"[create_tournaments] date={date_str} created={created} skipped={skipped} errors={errors}")
        
        return jsonify({
            "status": "completed",
            "date": date_str,
            "created": created,
            "skipped": skipped,
            "errors": errors
        }), status_code

    except Exception as e:
        print(f"[create_tournaments] Unexpected error: {e}", file=sys.stderr)
        return jsonify({"error": "INTERNAL", "message": "An internal error occurred"}), 500


@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN", "MIXPANEL_TOKEN"])
def update_tournament_status(request: Request):
    """
    Cloud Task target to advance a tournament's status.
    Automatically settles prizes (sends BTC to winners) when transitioning to ENDED.
    """
    try:
        if request.method != "POST":
            return jsonify({"error": "METHOD_NOT_ALLOWED", "message": "POST required"}), 405

        body = request.get_json(silent=True) or {}
        doc_id = str(body.get("tournament_id") or "").strip()
        target_status_raw = str(body.get("status") or "").strip().lower()
        if not doc_id or not target_status_raw:
            return jsonify({"error": "INVALID_PAYLOAD", "message": "tournament_id and status required"}), 400

        try:
            target_status = TournamentStatus(target_status_raw)
        except ValueError:
            return jsonify({"error": "INVALID_STATUS", "message": f"Unknown status {target_status_raw}"}), 400

        # Check both tournament collections
        ref = db().collection("tournaments").document(doc_id)
        snap = ref.get()
        collection_name = "tournaments"

        if not snap.exists:
            # Try hot_or_not_tournaments collection
            ref = db().collection("hot_or_not_tournaments").document(doc_id)
            snap = ref.get()
            collection_name = "hot_or_not_tournaments"

        if not snap.exists:
            return jsonify({"error": "NOT_FOUND", "message": f"{doc_id} not found"}), 404

        current_status_raw = snap.get("status")
        try:
            current_status = TournamentStatus(current_status_raw)
        except Exception:
            current_status = None

        # Simple forward-only guard (CANCELLED can be set from any state)
        order = [
            TournamentStatus.SCHEDULED,
            TournamentStatus.LIVE,
            TournamentStatus.ENDED,
            TournamentStatus.SETTLED,
        ]
        def _order_idx(status: TournamentStatus) -> int:
            return order.index(status) if status in order else -1

        # Allow CANCELLED from any state, otherwise enforce forward-only
        if target_status != TournamentStatus.CANCELLED:
            if current_status and _order_idx(target_status) < _order_idx(current_status):
                return jsonify({"status": "skipped", "reason": f"Current status {current_status.value} ahead of {target_status.value}"}), 200

        # Handle CANCELLED status with refunds
        if target_status == TournamentStatus.CANCELLED:
            # Don't refund if already cancelled or settled
            if current_status in [TournamentStatus.CANCELLED, TournamentStatus.SETTLED]:
                return jsonify({"status": "skipped", "reason": f"Cannot cancel from {current_status.value}"}), 200

            # Update status first
            ref.update({
                "status": target_status.value,
                "updated_at": firestore.SERVER_TIMESTAMP,
            })

            # Process refunds
            refund_results = _refund_tournament_users(doc_id, collection_name)
            refunded_count = sum(1 for r in refund_results if r.get("success"))
            total_refunded = sum(r.get("coins_refunded", 0) for r in refund_results if r.get("success"))

            print(f"[update_tournament_status] {doc_id}: {current_status_raw} -> {target_status.value} (refunded {refunded_count} users, {total_refunded} coins)")
            return jsonify({
                "status": "ok",
                "tournament_id": doc_id,
                "new_status": target_status.value,
                "refunds": {
                    "users_refunded": refunded_count,
                    "total_coins_refunded": total_refunded,
                    "details": refund_results
                }
            }), 200

        # Handle ENDED status with settlement
        if target_status == TournamentStatus.ENDED:
            # IDEMPOTENCY CHECK: Skip if already successfully settled
            tournament_data = snap.to_dict()
            if current_status == TournamentStatus.SETTLED:
                print(f"[update_tournament_status] {doc_id}: skipping settlement (status already SETTLED)")
                return jsonify({"status": "skipped", "reason": "Already settled"}), 200

            existing_settlement = tournament_data.get("settlement_result")
            if existing_settlement and existing_settlement.get("success") is True:
                print(f"[update_tournament_status] {doc_id}: skipping settlement (settlement_result.success=True)")
                return jsonify({"status": "skipped", "reason": "Already settled"}), 200

            # Update status to ENDED first
            ref.update({
                "status": target_status.value,
                "updated_at": firestore.SERVER_TIMESTAMP,
            })
            print(f"[update_tournament_status] {doc_id}: {current_status_raw} -> {target_status.value}")

            # Get prize map for settlement
            prize_map = _normalize_prize_map(snap.to_dict().get("prizeMap", {}))

            # Check if enough players played (at least 2 required for a valid competition)
            players_who_played = _count_players_who_played(doc_id, collection_name)
            print(f"[update_tournament_status] {doc_id}: {players_who_played} players played")

            if players_who_played < 2:
                # Not enough players - refund all registered users
                print(f"[update_tournament_status] {doc_id}: insufficient players ({players_who_played}), refunding all")
                refund_results = _refund_tournament_users(doc_id, collection_name)
                refunded_count = sum(1 for r in refund_results if r.get("success"))
                total_refunded = sum(r.get("coins_refunded", 0) for r in refund_results if r.get("success"))

                # Mark as settled (no prizes distributed)
                ref.update({
                    "status": TournamentStatus.SETTLED.value,
                    "settlement_result": {
                        "success": True,
                        "message": f"Insufficient players ({players_who_played}), refunded all entry fees",
                        "players_who_played": players_who_played,
                        "refunds": {
                            "users_refunded": refunded_count,
                            "total_coins_refunded": total_refunded,
                        }
                    },
                    "settled_at": firestore.SERVER_TIMESTAMP,
                    "updated_at": firestore.SERVER_TIMESTAMP,
                })
                print(f"[update_tournament_status] {doc_id}: -> {TournamentStatus.SETTLED.value} (refunded {refunded_count} users, {total_refunded} coins)")

                return jsonify({
                    "status": "ok",
                    "tournament_id": doc_id,
                    "new_status": TournamentStatus.SETTLED.value,
                    "settlement": {
                        "message": f"Insufficient players ({players_who_played}), minimum 2 required",
                        "players_who_played": players_who_played,
                        "prizes_distributed": False
                    },
                    "refunds": {
                        "users_refunded": refunded_count,
                        "total_coins_refunded": total_refunded,
                        "details": refund_results
                    }
                }), 200

            if prize_map:
                # Settle tournament prizes (send BTC to winners)
                settlement_result = _settle_tournament_prizes(doc_id, prize_map, collection_name)
                settlement_result["players_who_played"] = players_who_played

                # Update status to SETTLED after settlement
                ref.update({
                    "status": TournamentStatus.SETTLED.value,
                    "settlement_result": settlement_result,
                    "settled_at": firestore.SERVER_TIMESTAMP,
                    "updated_at": firestore.SERVER_TIMESTAMP,
                })
                print(f"[update_tournament_status] {doc_id}: {target_status.value} -> {TournamentStatus.SETTLED.value} (settled)")

                return jsonify({
                    "status": "ok",
                    "tournament_id": doc_id,
                    "new_status": TournamentStatus.SETTLED.value,
                    "settlement": settlement_result
                }), 200
            else:
                # No prize map, just mark as settled without rewards
                ref.update({
                    "status": TournamentStatus.SETTLED.value,
                    "updated_at": firestore.SERVER_TIMESTAMP,
                })
                print(f"[update_tournament_status] {doc_id}: {target_status.value} -> {TournamentStatus.SETTLED.value} (no prize map)")
                return jsonify({
                    "status": "ok",
                    "tournament_id": doc_id,
                    "new_status": TournamentStatus.SETTLED.value,
                    "settlement": {"message": "No prize map configured"}
                }), 200

        # Normal status update (SCHEDULED -> LIVE, etc.)
        ref.update({
            "status": target_status.value,
            "updated_at": firestore.SERVER_TIMESTAMP,
        })
        print(f"[update_tournament_status] {doc_id}: {current_status_raw} -> {target_status.value}")
        return jsonify({"status": "ok", "tournament_id": doc_id, "new_status": target_status.value}), 200

    except Exception as e:  # noqa: BLE001
        print(f"[update_tournament_status] Unexpected error: {e}", file=sys.stderr)
        return jsonify({"error": "INTERNAL", "message": "An internal error occurred"}), 500
