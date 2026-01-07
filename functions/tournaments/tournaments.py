import json
import os
import random
import string
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone, timedelta
from enum import Enum
from typing import Any, Dict, List, Optional

import firebase_admin
import requests
from firebase_admin import firestore
from firebase_functions import https_fn
from flask import Request, jsonify
from google.api_core.exceptions import AlreadyExists
from google.cloud import tasks_v2
from google.protobuf import timestamp_pb2
from mixpanel import Mixpanel


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
        }


IST = timezone(timedelta(hours=5, minutes=30))
_db = None

# BTC Settlement Constants
BALANCE_URL_CKBTC = "https://yral-hot-or-not.go-bazzinga.workers.dev/v2/transfer_ckbtc"
TICKER_URL = "https://blockchain.info/ticker"
SATOSHIS_PER_BTC = 100_000_000

# Backend API Constants
BACKEND_TOURNAMENT_REGISTER_URL = "https://recsys-on-premise.fly.dev/tournament/register"
DEFAULT_VIDEO_COUNT = 500

# Environment detection
GCLOUD_PROJECT = os.environ.get("GCLOUD_PROJECT", "")
IS_PRODUCTION = GCLOUD_PROJECT == "yral-mobile"

# Environment-specific tournament configuration
if IS_PRODUCTION:
    # Production: Single tournament at 7 PM IST
    TOURNAMENT_SLOTS = [("19:00", "19:10")]
    TOURNAMENT_TITLE = "Daily Showdown"
    TOURNAMENT_ENTRY_COST = 15
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


def _refund_tournament_users(tournament_id: str) -> List[dict]:
    """
    Refund all registered users for a cancelled tournament.
    Returns list of refund results.
    """
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
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


def _count_players_who_played(tournament_id: str) -> int:
    """Count users who actually played (cast at least one vote) in the tournament."""
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
    # A player "played" if they have any wins or losses (i.e., cast at least one vote)
    # We check for tournament_wins > 0 OR tournament_losses > 0
    # Firestore doesn't support OR queries easily, so we count both separately

    # Count users with at least 1 win
    wins_snaps = list(users_ref.where("tournament_wins", ">", 0).stream())
    players_with_wins = {snap.id for snap in wins_snaps}

    # Count users with at least 1 loss but no wins (they played but didn't win any)
    losses_snaps = list(users_ref.where("tournament_losses", ">", 0).stream())
    players_with_losses = {snap.id for snap in losses_snaps}

    # Union of both sets = all players who played
    all_players = players_with_wins | players_with_losses
    return len(all_players)


def _compute_settlement_leaderboard(tournament_id: str, limit: int = 10) -> List[dict]:
    """Compute top N users by diamond balance with strict ranking for settlement.

    Only includes users who have played at least 1 game (wins > 0 or losses > 0).

    Ranking order:
    1. Diamonds DESC (more diamonds = higher rank)
    2. Total games (wins + losses) DESC (tiebreaker: more games = higher rank)
    3. updated_at ASC (second tiebreaker: earlier = higher rank)
    """
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
    # Fetch extra users to account for filtering out those who never played
    # Need high multiplier because many non-players have 20 diamonds (initial balance)
    # which ranks higher than players who lost (< 20 diamonds)
    snaps = (
        users_ref.order_by("diamonds", direction=firestore.Query.DESCENDING)
                 .limit(limit * 50)
                 .stream()
    )

    # Collect all qualifying users first
    candidates = []
    for snap in snaps:
        data = snap.to_dict() or {}
        wins = int(data.get("tournament_wins") or 0)
        losses = int(data.get("tournament_losses") or 0)

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


def _settle_tournament_prizes(tournament_id: str, prize_map: Dict[str, int]) -> Dict[str, Any]:
    """
    Settle tournament prizes by sending BTC equivalent to winners.

    Args:
        tournament_id: The tournament to settle
        prize_map: Map of position (str) to INR prize amount

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
    leaderboard = _compute_settlement_leaderboard(tournament_id, limit=max_prize_position)

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
                user_ref = db().document(f"tournaments/{tournament_id}/users/{principal_id}")
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


@https_fn.on_request(region="us-central1", secrets=["BACKEND_ADMIN_KEY"])
def create_tournaments(cloud_event):
    """
    Cloud Scheduler target (run daily at 12am IST) to create tournaments
    """
    try:
        backend_admin_key = os.environ.get("BACKEND_ADMIN_KEY")
        if not backend_admin_key:
            print("[create_tournaments] BACKEND_ADMIN_KEY not configured", file=sys.stderr)
            return jsonify({"error": "INTERNAL", "message": "An internal error occurred"}), 500

        env_name = "production" if IS_PRODUCTION else "staging"
        print(f"[create_tournaments] Running in {env_name} mode (project: {GCLOUD_PROJECT})")

        date_str = _today_ist_str()
        entry_cost = TOURNAMENT_ENTRY_COST
        total_prize_pool = 1500
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

        slots = TOURNAMENT_SLOTS

        created, skipped, errors = [], [], []

        for start_time, end_time in slots:
            start_dt = _ist_datetime(date_str, start_time)
            end_dt = _ist_datetime(date_str, end_time)

            # Register with backend to get tournament ID and videos
            tournament_id, backend_error = _register_tournament_backend(backend_admin_key, video_count=DEFAULT_VIDEO_COUNT)
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
                title=TOURNAMENT_TITLE,
            )

            # Use backend tournament ID as Firestore document ID
            ref = db().collection("tournaments").document(tournament_id)
            try:
                ref.create(tour.to_firestore())
                created.append(tournament_id)
                _schedule_status_task(tournament_id, TournamentStatus.LIVE, start_dt)
                _schedule_status_task(tournament_id, TournamentStatus.ENDED, end_dt)
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

        ref = db().collection("tournaments").document(doc_id)
        snap = ref.get()
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
            refund_results = _refund_tournament_users(doc_id)
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
            # Update status to ENDED first
            ref.update({
                "status": target_status.value,
                "updated_at": firestore.SERVER_TIMESTAMP,
            })
            print(f"[update_tournament_status] {doc_id}: {current_status_raw} -> {target_status.value}")

            # Get prize map for settlement
            prize_map = _normalize_prize_map(snap.to_dict().get("prizeMap", {}))

            # Check if enough players played (at least 2 required for a valid competition)
            players_who_played = _count_players_who_played(doc_id)
            print(f"[update_tournament_status] {doc_id}: {players_who_played} players played")

            if players_who_played < 2:
                # Not enough players - refund all registered users
                print(f"[update_tournament_status] {doc_id}: insufficient players ({players_who_played}), refunding all")
                refund_results = _refund_tournament_users(doc_id)
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
                settlement_result = _settle_tournament_prizes(doc_id, prize_map)
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
