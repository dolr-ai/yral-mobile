"""
Tournament client-facing APIs.

All HTTP function exports for tournament operations:
- tournaments: List tournaments
- tournament_status: Get tournament status
- register_for_tournament: Join a tournament
- my_tournaments: User's registered tournaments
- tournament_vote: Cast vote during live tournament
- tournament_leaderboard: Get leaderboard
"""

import random
import sys
from datetime import datetime, timezone, timedelta
from typing import Any, Dict, List, Optional

import firebase_admin
from firebase_admin import auth, firestore
from firebase_functions import https_fn
from flask import Request, jsonify, make_response

try:
    from .tournaments import TournamentStatus
except ImportError:
    from tournaments import TournamentStatus

# ─────────────────────  CONSTANTS  ────────────────────────
IST = timezone(timedelta(hours=5, minutes=30))
TOURNAMENT_SHARDS = 5
SMILEY_GAME_CONFIG_PATH = "config/smiley_game_v2"

# ─────────────────────  DATABASE HELPER  ────────────────────────
_db = None


def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db


# ─────────────────────  SMILEY CONFIG  ────────────────────────
_SMILEYS: List[Dict[str, str]] | None = None


def get_smileys() -> List[Dict[str, str]]:
    global _SMILEYS
    if _SMILEYS is None:
        snap = db().document(SMILEY_GAME_CONFIG_PATH).get()
        _SMILEYS = snap.get("available_smileys") or []
    return _SMILEYS


# ─────────────────────  ERROR HELPER  ────────────────────────
def error_response(status: int, code: str, message: str):
    payload = {"error": {"code": code, "message": message}}
    return make_response(jsonify(payload), status)


# ─────────────────────  COIN HELPER  ────────────────────────
def _tx_id() -> str:
    import string
    now = datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
    rnd = "".join(random.choices(string.ascii_lowercase + string.digits, k=4))
    return f"{now}_{rnd}"


def tx_coin_change(principal_id: str, video_id: str | None, delta: int, reason: str) -> int:
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
                "video_id": video_id,
                "at": firestore.SERVER_TIMESTAMP})

    _commit(db().transaction())
    return int(user_ref.get().get("coins") or 0)


# ─────────────────────  TIME HELPERS  ────────────────────────
def _today_ist_str() -> str:
    return datetime.now(IST).strftime("%Y-%m-%d")


def _time_left_ms(end_epoch_ms: int) -> int:
    """Calculate milliseconds until tournament ends."""
    now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
    return max(0, end_epoch_ms - now_ms)


def _compute_status(start_epoch_ms: int, end_epoch_ms: int) -> str:
    """Compute tournament status based on current time vs epochs.

    - If current time < start_epoch_ms: scheduled
    - If start_epoch_ms <= current time <= end_epoch_ms: live
    - If current time > end_epoch_ms: ended
    """
    now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
    if now_ms < start_epoch_ms:
        return "scheduled"
    elif now_ms <= end_epoch_ms:
        return "live"
    else:
        return "ended"


# ─────────────────────  TOURNAMENT HELPERS  ────────────────────────
def _get_tournament(tournament_id: str) -> tuple[Any, dict]:
    """Fetch tournament doc. Returns (snapshot, data). Raises if not found."""
    ref = db().collection("tournaments").document(tournament_id)
    snap = ref.get()
    if not snap.exists:
        return None, {}
    return snap, snap.to_dict() or {}


def _get_participant_count(tournament_id: str) -> int:
    """Get count of registered users in tournament."""
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
    count_result = users_ref.count().get()
    return int(count_result[0][0].value) if count_result else 0


def _get_user_registration(tournament_id: str, principal_id: str) -> Optional[dict]:
    """Check if user is registered for tournament."""
    ref = db().document(f"tournaments/{tournament_id}/users/{principal_id}")
    snap = ref.get()
    return snap.to_dict() if snap.exists else None


def _vote_doc_id(principal_id: str, video_id: str) -> str:
    """Generate consistent vote document ID."""
    return f"{principal_id}_{video_id}"


# ─────────────────────  LEADERBOARD HELPERS  ────────────────────────
def _compute_tournament_leaderboard(tournament_id: str, limit: int = 10) -> List[dict]:
    """Compute top N users by tournament_wins with dense ranking."""
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
    snaps = (
        users_ref.where("tournament_wins", ">", 0)
                 .order_by("tournament_wins", direction=firestore.Query.DESCENDING)
                 .limit(limit)
                 .stream()
    )

    rows = []
    current_rank, last_wins = 0, None
    principal_ids = []

    for snap in snaps:
        data = snap.to_dict() or {}
        wins = int(data.get("tournament_wins") or 0)
        if wins != last_wins:
            current_rank += 1
            last_wins = wins
        rows.append({
            "principal_id": snap.id,
            "wins": wins,
            "losses": int(data.get("tournament_losses") or 0),
            "position": current_rank
        })
        principal_ids.append(snap.id)

    # Fetch usernames
    if principal_ids:
        user_refs = [db().collection("users").document(pid) for pid in principal_ids]
        user_docs = db().get_all(user_refs)
        username_map = {
            doc.id: (doc.to_dict() or {}).get("username") for doc in user_docs
        }
        for row in rows:
            row["username"] = username_map.get(row["principal_id"])

    return rows


def _get_user_tournament_position(
    tournament_id: str,
    principal_id: str,
    top_rows: List[dict]
) -> dict:
    """Get user's position in tournament leaderboard."""
    # Check if in top rows first
    for row in top_rows:
        if row["principal_id"] == principal_id:
            return row

    # Query user's registration
    user_ref = db().document(f"tournaments/{tournament_id}/users/{principal_id}")
    user_snap = user_ref.get()

    # Get username
    profile_ref = db().document(f"users/{principal_id}")
    profile_snap = profile_ref.get()
    username = (profile_snap.to_dict() or {}).get("username") if profile_snap.exists else None

    if not user_snap.exists:
        return {
            "principal_id": principal_id,
            "wins": 0,
            "losses": 0,
            "position": 0,
            "username": username
        }

    user_data = user_snap.to_dict() or {}
    user_wins = int(user_data.get("tournament_wins") or 0)
    user_losses = int(user_data.get("tournament_losses") or 0)

    if user_wins == 0:
        return {
            "principal_id": principal_id,
            "wins": 0,
            "losses": user_losses,
            "position": 0,
            "username": username
        }

    # Count users with more wins
    users_ref = db().collection(f"tournaments/{tournament_id}/users")
    count_q = users_ref.where("tournament_wins", ">", user_wins).count().get()
    higher = int(count_q[0][0].value)

    return {
        "principal_id": principal_id,
        "wins": user_wins,
        "losses": user_losses,
        "position": higher + 1,
        "username": username
    }


# ─────────────────────  VOTING HELPERS  ────────────────────────
def _aggregate_tallies(tournament_id: str, video_id: str) -> dict:
    """Aggregate all shard tallies for a video within tournament."""
    tallies: Dict[str, int] = {}
    for k in range(TOURNAMENT_SHARDS):
        shard_ref = db().document(
            f"tournaments/{tournament_id}/videos/{video_id}/tallies/shard_{k}"
        )
        shard_data = shard_ref.get().to_dict() or {}
        for smiley_id, count in shard_data.items():
            tallies[smiley_id] = tallies.get(smiley_id, 0) + int(count)
    return tallies


def _determine_outcome(smiley_id: str, tallies: dict) -> str:
    """Determine WIN/LOSS based on majority. Returns 'WIN' or 'LOSS'."""
    if not tallies:
        return "LOSS"
    max_votes = max(tallies.values())
    leaders = [s for s, v in tallies.items() if v == max_votes]
    if len(leaders) == 1 and leaders[0] == smiley_id:
        return "WIN"
    return "LOSS"


# ═══════════════════════════════════════════════════════════════════
#                              APIS
# ═══════════════════════════════════════════════════════════════════


@https_fn.on_request(region="us-central1")
def tournaments(request: Request):
    """
    List tournaments with optional date/status filters.

    POST /tournaments
    Request:
        { "data": { "date": "2025-12-14", "status": "scheduled", "principal_id": "..." } }
        All fields optional. Defaults to today's date if not provided.
        If principal_id is provided, includes is_registered and user_stats for each tournament.

    Response:
        { "tournaments": [ {..., "is_registered": true, "user_stats": {...}}, ... ] }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        date_filter = str(data.get("date") or "").strip()
        status_filter = str(data.get("status") or "").strip().lower()
        principal_id = str(data.get("principal_id") or "").strip()

        # Default to today if no date provided
        if not date_filter:
            date_filter = _today_ist_str()

        # Validate date format
        try:
            datetime.strptime(date_filter, "%Y-%m-%d")
        except ValueError:
            return error_response(400, "INVALID_DATE", "date must be YYYY-MM-DD format")

        # Validate status if provided
        if status_filter:
            try:
                TournamentStatus(status_filter)
            except ValueError:
                valid = [s.value for s in TournamentStatus]
                return error_response(400, "INVALID_STATUS", f"status must be one of: {valid}")

        # Query tournaments for the date (status is computed dynamically)
        query = db().collection("tournaments").where("date", "==", date_filter)
        snaps = list(query.stream())

        result = []
        for snap in snaps:
            t_data = snap.to_dict() or {}

            # Compute status dynamically based on current time vs epochs
            start_epoch_ms = t_data.get("start_epoch_ms", 0)
            end_epoch_ms = t_data.get("end_epoch_ms", 0)
            computed_status = _compute_status(start_epoch_ms, end_epoch_ms)

            # Filter by status if provided
            if status_filter and computed_status != status_filter:
                continue

            participant_count = _get_participant_count(snap.id)

            tournament_entry = {
                "id": snap.id,
                "title": t_data.get("title", "SMILEY SHOWDOWN"),
                "date": t_data.get("date"),
                "start_time": t_data.get("start_time"),
                "end_time": t_data.get("end_time"),
                "start_epoch_ms": start_epoch_ms,
                "end_epoch_ms": end_epoch_ms,
                "entry_cost": t_data.get("entryCost"),
                "total_prize_pool": t_data.get("totalPrizePool"),
                "status": computed_status,
                "prize_map": t_data.get("prizeMap", {}),
                "participant_count": participant_count,
                "is_registered": False,
                "user_stats": None
            }

            # Check if user is registered (if principal_id provided)
            if principal_id:
                reg_data = _get_user_registration(snap.id, principal_id)
                if reg_data:
                    tournament_entry["is_registered"] = True
                    tournament_entry["user_stats"] = {
                        "registered_at": reg_data.get("registered_at"),
                        "coins_paid": reg_data.get("coins_paid"),
                        "diamonds": reg_data.get("diamonds", 0),
                        "tournament_wins": reg_data.get("tournament_wins", 0),
                        "tournament_losses": reg_data.get("tournament_losses", 0),
                        "status": reg_data.get("status")
                    }

            result.append(tournament_entry)

        # Sort: live first, then scheduled, then ended; within each group sort by start_time
        def sort_key(t):
            status = t.get("status", "").lower()
            # Priority: live=0, scheduled=1, ended=2
            status_priority = {"live": 0, "scheduled": 1, "ended": 2}.get(status, 3)
            return (status_priority, t.get("start_time", ""))

        result.sort(key=sort_key)

        return jsonify({"tournaments": result}), 200

    except Exception as e:
        print(f"tournaments error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


@https_fn.on_request(region="us-central1")
def tournament_status(request: Request):
    """
    Lightweight tournament status check.

    POST /tournament_status
    Request:
        { "data": { "tournament_id": "2025-12-14-12:45-13:15" } }

    Response:
        {
            "tournament_id": "...",
            "status": "live",
            "time_left_ms": 123456,
            "participant_count": 42
        }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        tournament_id = str(data.get("tournament_id", "")).strip()
        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")

        snap, t_data = _get_tournament(tournament_id)
        if not snap:
            return error_response(404, "TOURNAMENT_NOT_FOUND", f"Tournament {tournament_id} not found")

        status = t_data.get("status")
        participant_count = _get_participant_count(tournament_id)

        response = {
            "tournament_id": tournament_id,
            "status": status,
            "participant_count": participant_count
        }

        # Add time_left_ms only for LIVE tournaments
        if status == TournamentStatus.LIVE.value:
            end_epoch_ms = t_data.get("end_epoch_ms", 0)
            response["time_left_ms"] = _time_left_ms(end_epoch_ms)

        return jsonify(response), 200

    except Exception as e:
        print(f"tournament_status error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


@https_fn.on_request(region="us-central1")
def register_for_tournament(request: Request):
    """
    Register user for a tournament (pay entry fee).

    POST /register_for_tournament
    Request:
        { "data": { "tournament_id": "...", "principal_id": "..." } }

    Response:
        {
            "status": "registered",
            "tournament_id": "...",
            "coins_paid": 100,
            "coins_remaining": 400
        }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Auth
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization header required")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        tournament_id = str(data.get("tournament_id", "")).strip()
        principal_id = str(data.get("principal_id", "")).strip()

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")
        if not principal_id:
            return error_response(400, "MISSING_PRINCIPAL_ID", "principal_id required")

        # Get tournament
        tournament_ref = db().collection("tournaments").document(tournament_id)
        user_ref = db().document(f"users/{principal_id}")
        reg_ref = db().document(f"tournaments/{tournament_id}/users/{principal_id}")

        @firestore.transactional
        def _register_tx(tx: firestore.Transaction) -> dict:
            # READS first
            t_snap = tournament_ref.get(transaction=tx)
            if not t_snap.exists:
                return {"error": "TOURNAMENT_NOT_FOUND", "message": f"Tournament {tournament_id} not found"}

            t_data = t_snap.to_dict() or {}
            entry_cost = int(t_data.get("entryCost") or 0)

            # Compute status dynamically from epochs
            start_epoch_ms = t_data.get("start_epoch_ms", 0)
            end_epoch_ms = t_data.get("end_epoch_ms", 0)
            status = _compute_status(start_epoch_ms, end_epoch_ms)

            if status not in ["scheduled", "live"]:
                return {"error": "TOURNAMENT_NOT_OPEN", "message": f"Tournament is {status}, registration closed"}

            user_snap = user_ref.get(transaction=tx)
            user_coins = int((user_snap.to_dict() or {}).get("coins") or 0)

            if user_coins < entry_cost:
                return {"error": "INSUFFICIENT_COINS", "message": f"Balance {user_coins} < {entry_cost} required"}

            reg_snap = reg_ref.get(transaction=tx)
            if reg_snap.exists:
                return {"error": "ALREADY_REGISTERED", "message": "Already registered for this tournament"}

            # WRITES
            # Deduct coins
            tx.update(user_ref, {"coins": firestore.Increment(-entry_cost)})

            # Create registration with diamonds (1.5x entry cost)
            initial_diamonds = int(entry_cost * 1.5)
            tx.set(reg_ref, {
                "registered_at": firestore.SERVER_TIMESTAMP,
                "coins_paid": entry_cost,
                "diamonds": initial_diamonds,
                "tournament_wins": 0,
                "tournament_losses": 0,
                "status": "registered",
                "updated_at": firestore.SERVER_TIMESTAMP
            })

            # Log transaction
            ledger_ref = user_ref.collection("transactions").document(_tx_id())
            tx.set(ledger_ref, {
                "delta": -entry_cost,
                "reason": "TOURNAMENT_ENTRY",
                "tournament_id": tournament_id,
                "at": firestore.SERVER_TIMESTAMP
            })

            return {"success": True, "entry_cost": entry_cost, "diamonds": initial_diamonds}

        result = _register_tx(db().transaction())

        if "error" in result:
            error_code = result["error"]
            if error_code == "TOURNAMENT_NOT_FOUND":
                return error_response(404, error_code, result["message"])
            elif error_code == "INSUFFICIENT_COINS":
                return error_response(402, error_code, result["message"])
            else:
                return error_response(409, error_code, result["message"])

        # Get updated balance
        updated_user = user_ref.get()
        coins_remaining = int((updated_user.to_dict() or {}).get("coins") or 0)

        return jsonify({
            "status": "registered",
            "tournament_id": tournament_id,
            "coins_paid": result["entry_cost"],
            "coins_remaining": coins_remaining,
            "diamonds": result["diamonds"]
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except Exception as e:
        print(f"register_for_tournament error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


@https_fn.on_request(region="us-central1")
def my_tournaments(request: Request):
    """
    List tournaments user has played in (has at least one vote).

    POST /my_tournaments
    Request:
        { "data": { "principal_id": "..." } }

    Response:
        { "tournaments": [ {...}, ... ] }

    Note: Only returns tournaments where user has actually played (has wins or losses).
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Auth
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization header required")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        principal_id = str(data.get("principal_id", "")).strip()
        if not principal_id:
            return error_response(400, "MISSING_PRINCIPAL_ID", "principal_id required")

        # Query recent tournaments (last 30 days)
        today = datetime.now(IST).date()
        date_range = [today + timedelta(days=d) for d in range(-30, 1)]
        date_strings = [d.strftime("%Y-%m-%d") for d in date_range]

        result = []

        for date_str in date_strings:
            # Get tournaments for this date
            t_snaps = db().collection("tournaments").where("date", "==", date_str).stream()

            for t_snap in t_snaps:
                # Check if user is registered
                reg_ref = db().document(f"tournaments/{t_snap.id}/users/{principal_id}")
                reg_snap = reg_ref.get()

                if reg_snap.exists:
                    reg_data = reg_snap.to_dict() or {}

                    # Only include tournaments where user has actually played
                    tournament_wins = reg_data.get("tournament_wins", 0) or 0
                    tournament_losses = reg_data.get("tournament_losses", 0) or 0

                    if tournament_wins > 0 or tournament_losses > 0:
                        t_data = t_snap.to_dict() or {}
                        participant_count = _get_participant_count(t_snap.id)

                        # Compute status dynamically
                        start_epoch_ms = t_data.get("start_epoch_ms", 0)
                        end_epoch_ms = t_data.get("end_epoch_ms", 0)
                        computed_status = _compute_status(start_epoch_ms, end_epoch_ms)

                        result.append({
                            "id": t_snap.id,
                            "title": t_data.get("title", "SMILEY SHOWDOWN"),
                            "date": t_data.get("date"),
                            "start_time": t_data.get("start_time"),
                            "end_time": t_data.get("end_time"),
                            "start_epoch_ms": start_epoch_ms,
                            "end_epoch_ms": end_epoch_ms,
                            "entry_cost": t_data.get("entryCost"),
                            "total_prize_pool": t_data.get("totalPrizePool"),
                            "status": computed_status,
                            "prize_map": t_data.get("prizeMap", {}),
                            "participant_count": participant_count,
                            "user_stats": {
                                "registered_at": reg_data.get("registered_at"),
                                "coins_paid": reg_data.get("coins_paid"),
                                "diamonds": reg_data.get("diamonds", 0),
                                "tournament_wins": tournament_wins,
                                "tournament_losses": tournament_losses,
                                "status": reg_data.get("status")
                            }
                        })

        # Sort by date and start_time (newest first)
        result.sort(key=lambda x: (x.get("date", ""), x.get("start_time", "")), reverse=True)

        return jsonify({"tournaments": result}), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except Exception as e:
        print(f"my_tournaments error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


@https_fn.on_request(region="us-central1")
def tournament_vote(request: Request):
    """
    Cast vote during live tournament.

    POST /tournament_vote
    Request:
        {
            "data": {
                "tournament_id": "...",
                "principal_id": "...",
                "video_id": "...",
                "smiley_id": "fire"
            }
        }

    Response:
        {
            "outcome": "WIN",
            "smiley": {...},
            "tournament_wins": 6,
            "tournament_losses": 3,
            "diamonds": 124,
            "position": 15
        }

    Diamond System:
        - Diamonds are initialized at registration (entry_cost * 1.5)
        - Win: +1 diamond
        - Loss: -1 diamond
        - Cannot vote if diamonds = 0
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Auth
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization header required")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        tournament_id = str(data.get("tournament_id", "")).strip()
        principal_id = str(data.get("principal_id", "")).strip()
        video_id = str(data.get("video_id", "")).strip()
        smiley_id = str(data.get("smiley_id", "")).strip()

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")
        if not principal_id:
            return error_response(400, "MISSING_PRINCIPAL_ID", "principal_id required")
        if not video_id:
            return error_response(400, "MISSING_VIDEO_ID", "video_id required")
        if not smiley_id:
            return error_response(400, "MISSING_SMILEY_ID", "smiley_id required")

        # Validate smiley_id
        smileys = get_smileys()
        smiley_map = {s["id"]: s for s in smileys}
        if smiley_id not in smiley_map:
            return error_response(400, "SMILEY_NOT_ALLOWED", "Invalid smiley_id")

        # References
        tournament_ref = db().collection("tournaments").document(tournament_id)
        reg_ref = db().document(f"tournaments/{tournament_id}/users/{principal_id}")
        vote_doc_id = _vote_doc_id(principal_id, video_id)
        vote_ref = db().document(f"tournaments/{tournament_id}/votes/{vote_doc_id}")
        video_ref = db().document(f"tournaments/{tournament_id}/videos/{video_id}")
        shard_ref = lambda k: db().document(
            f"tournaments/{tournament_id}/videos/{video_id}/tallies/shard_{k}"
        )

        @firestore.transactional
        def _vote_tx(tx: firestore.Transaction) -> dict:
            # READS first
            t_snap = tournament_ref.get(transaction=tx)
            if not t_snap.exists:
                return {"error": "TOURNAMENT_NOT_FOUND", "message": f"Tournament {tournament_id} not found"}

            t_data = t_snap.to_dict() or {}

            # Compute status dynamically from epochs
            start_epoch_ms = t_data.get("start_epoch_ms", 0)
            end_epoch_ms = t_data.get("end_epoch_ms", 0)
            status = _compute_status(start_epoch_ms, end_epoch_ms)

            if status != "live":
                return {"error": "TOURNAMENT_NOT_LIVE", "message": f"Tournament is {status}, voting not allowed"}

            reg_snap = reg_ref.get(transaction=tx)
            if not reg_snap.exists:
                return {"error": "NOT_REGISTERED", "message": "You are not registered for this tournament"}

            # Check diamond balance
            reg_data = reg_snap.to_dict() or {}
            current_diamonds = int(reg_data.get("diamonds") or 0)
            if current_diamonds <= 0:
                return {"error": "NO_DIAMONDS", "message": "You have no diamonds left. Cannot play anymore."}

            vote_snap = vote_ref.get(transaction=tx)
            if vote_snap.exists:
                return {"error": "DUPLICATE_VOTE", "message": "You have already voted on this video"}

            # Check if video doc exists, create if not
            video_snap = video_ref.get(transaction=tx)
            if not video_snap.exists:
                # Initialize video with seed votes (like cast_vote_v2)
                all_ids = [s["id"] for s in smileys if s["id"] != "heart"]
                seed_ids = random.sample(all_ids, min(3, len(all_ids)))

                tx.set(video_ref, {"created_at": firestore.SERVER_TIMESTAMP})

                # Initialize shard 0 with seed votes
                zero = {s["id"]: 0 for s in smileys}
                for seed_id in seed_ids:
                    zero[seed_id] = 1000
                tx.set(shard_ref(0), zero, merge=True)

                # Initialize other shards with zeros
                for k in range(1, TOURNAMENT_SHARDS):
                    tx.set(shard_ref(k), {s["id"]: 0 for s in smileys}, merge=True)

            # WRITES
            # Record vote
            tx.set(vote_ref, {
                "principal_id": principal_id,
                "video_id": video_id,
                "smiley_id": smiley_id,
                "at": firestore.SERVER_TIMESTAMP
            })

            # Increment tally shard
            tx.update(
                shard_ref(random.randrange(TOURNAMENT_SHARDS)),
                {smiley_id: firestore.Increment(1)}
            )

            return {"success": True}

        tx_result = _vote_tx(db().transaction())

        if "error" in tx_result:
            error_code = tx_result["error"]
            if error_code == "TOURNAMENT_NOT_FOUND":
                return error_response(404, error_code, tx_result["message"])
            elif error_code == "NOT_REGISTERED":
                return error_response(403, error_code, tx_result["message"])
            elif error_code == "NO_DIAMONDS":
                return error_response(403, error_code, tx_result["message"])
            else:
                return error_response(409, error_code, tx_result["message"])

        # Determine outcome (outside transaction)
        tallies = _aggregate_tallies(tournament_id, video_id)
        outcome = _determine_outcome(smiley_id, tallies)

        # Update vote with outcome
        vote_ref.update({
            "outcome": outcome,
            "updated_at": firestore.SERVER_TIMESTAMP
        })

        # Update user's tournament stats and diamonds
        # Win: +1 diamond, Loss: -1 diamond
        if outcome == "WIN":
            reg_ref.update({
                "tournament_wins": firestore.Increment(1),
                "diamonds": firestore.Increment(1),
                "updated_at": firestore.SERVER_TIMESTAMP
            })
        else:
            reg_ref.update({
                "tournament_losses": firestore.Increment(1),
                "diamonds": firestore.Increment(-1),
                "updated_at": firestore.SERVER_TIMESTAMP
            })

        # Get updated stats
        updated_reg = reg_ref.get()
        reg_data = updated_reg.to_dict() or {}
        tournament_wins = int(reg_data.get("tournament_wins") or 0)
        tournament_losses = int(reg_data.get("tournament_losses") or 0)
        diamonds = int(reg_data.get("diamonds") or 0)

        # Calculate position
        top_rows = _compute_tournament_leaderboard(tournament_id, limit=10)
        user_row = _get_user_tournament_position(tournament_id, principal_id, top_rows)
        position = user_row.get("position", 0)

        # Build response
        voted_smiley = smiley_map[smiley_id]
        return jsonify({
            "outcome": outcome,
            "smiley": {
                "id": voted_smiley["id"],
                "image_url": voted_smiley.get("image_url"),
                "is_active": voted_smiley.get("is_active"),
                "click_animation": voted_smiley.get("click_animation"),
                "image_fallback": voted_smiley.get("image_fallback")
            },
            "tournament_wins": tournament_wins,
            "tournament_losses": tournament_losses,
            "diamonds": diamonds,
            "position": position
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except Exception as e:
        print(f"tournament_vote error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


@https_fn.on_request(region="us-central1")
def tournament_leaderboard(request: Request):
    """
    Get tournament leaderboard.

    POST /tournament_leaderboard
    Request:
        { "data": { "tournament_id": "...", "principal_id": "..." } }

    Response:
        {
            "tournament_id": "...",
            "status": "ended",
            "top_rows": [ {...}, ... ],
            "user_row": {...},
            "prize_map": {...}
        }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # Auth
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization header required")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        tournament_id = str(data.get("tournament_id", "")).strip()
        principal_id = str(data.get("principal_id", "")).strip()

        if not tournament_id:
            return error_response(400, "MISSING_TOURNAMENT_ID", "tournament_id required")
        if not principal_id:
            return error_response(400, "MISSING_PRINCIPAL_ID", "principal_id required")

        # Get tournament
        snap, t_data = _get_tournament(tournament_id)
        if not snap:
            return error_response(404, "TOURNAMENT_NOT_FOUND", f"Tournament {tournament_id} not found")

        status = t_data.get("status")
        prize_map = t_data.get("prizeMap", {})

        # Allow leaderboard viewing after ENDED or SETTLED
        if status not in [TournamentStatus.ENDED.value, TournamentStatus.SETTLED.value]:
            return error_response(409, "TOURNAMENT_STILL_ACTIVE",
                                  f"Leaderboard available after tournament ends (current: {status})")

        # Get top rows
        top_rows = _compute_tournament_leaderboard(tournament_id, limit=10)

        # Add prizes to top rows
        for row in top_rows:
            position = str(row["position"])
            row["prize"] = prize_map.get(position)

        # Get user row
        user_row = _get_user_tournament_position(tournament_id, principal_id, top_rows)
        user_position = str(user_row.get("position", 0))
        user_row["prize"] = prize_map.get(user_position) if user_position != "0" else None

        return jsonify({
            "tournament_id": tournament_id,
            "status": status,
            "top_rows": top_rows,
            "user_row": user_row,
            "prize_map": prize_map
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except Exception as e:
        print(f"tournament_leaderboard error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")
