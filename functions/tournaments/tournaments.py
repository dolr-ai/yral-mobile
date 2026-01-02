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
BACKEND_TOURNAMENT_REGISTER_URL = "https://stage-recsys-on-premise.fly.dev/tournament/register"
DEFAULT_VIDEO_COUNT = 500


def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db


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

        except Exception as e:
            refund_results.append({
                "principal_id": principal_id,
                "coins_refunded": 0,
                "success": False,
                "error": str(e)
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
        return False, str(e)
    if resp.status_code != 200:
        return False, f"Status {resp.status_code}: {resp.text}"
    payload = resp.json()
    if payload.get("success"):
        return True, None
    return False, str(payload)


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
    """Compute top N users by diamond balance with dense ranking for settlement."""
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
    snaps = (
        users_ref.order_by("diamonds", direction=firestore.Query.DESCENDING)
                 .limit(limit)
                 .stream()
    )

    rows = []
    current_rank, last_diamonds = 0, None

    for snap in snaps:
        data = snap.to_dict() or {}
        diamonds = int(data.get("diamonds") or 0)
        if diamonds != last_diamonds:
            current_rank += 1
            last_diamonds = diamonds
        rows.append({
            "principal_id": snap.id,
            "diamonds": diamonds,
            "wins": int(data.get("tournament_wins") or 0),
            "position": current_rank
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
        return {
            "success": False,
            "error": f"Failed to fetch BTC price: {e}",
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
    project = os.environ.get("GOOGLE_CLOUD_PROJECT", "yral-staging")
    location = os.environ.get("TASKS_LOCATION", "us-central1")
    queue = os.environ.get("TASKS_QUEUE", "tournament-status-updates")
    if not project:
        raise RuntimeError("GOOGLE_CLOUD_PROJECT env var missing")
    return client.queue_path(project, location, queue)


def _function_url(fn_name: str) -> str:
    project = os.environ.get("GOOGLE_CLOUD_PROJECT", "yral-staging")
    region = os.environ.get("FUNCTION_REGION", "us-central1")
    if not project:
        raise RuntimeError("GOOGLE_CLOUD_PROJECT env var missing")
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
            return jsonify({"error": "BACKEND_ADMIN_KEY not configured"}), 500

        date_str = _today_ist_str()
        entry_cost = 100
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

        slots = [("12:45", "12:55"), ("14:00", "14:10"), ("16:00", "16:10")]  # 10 min each

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
                errors.append({"id": tournament_id, "reason": str(e)})

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
        return jsonify({"error": "INTERNAL", "message": str(e)}), 500


@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"])
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
        return jsonify({"error": "INTERNAL", "message": str(e)}), 500
