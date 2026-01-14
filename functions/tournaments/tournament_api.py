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

import os
import random
import sys
from datetime import datetime, timezone, timedelta
from typing import Any, Dict, List, Optional

import firebase_admin
import requests
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
BALANCE_URL_YRAL_TOKEN = "https://yral-hot-or-not.go-bazzinga.workers.dev/update_balance/"

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


# ─────────────────────  YRAL TOKEN BALANCE HELPER  ────────────────────────
def _push_delta_yral_token(token: str, principal_id: str, delta: int) -> tuple[bool, str | None]:
    """Update YRAL token balance via external API.

    Args:
        token: The BALANCE_UPDATE_TOKEN secret
        principal_id: User's principal ID
        delta: Amount to add (positive) or deduct (negative)

    Returns:
        Tuple of (success, error_message)
    """
    url = f"{BALANCE_URL_YRAL_TOKEN}{principal_id}"
    # API expects Bearer prefix if not already present
    auth_value = token if token.startswith("Bearer ") else f"Bearer {token}"
    headers = {
        "Authorization": auth_value,
        "Content-Type": "application/json",
    }
    body = {
        "delta": str(delta),  # radix-10 string, e.g. "-100"
        "is_airdropped": False
    }
    try:
        resp = requests.post(url, json=body, timeout=30, headers=headers)
        if resp.status_code == 200:
            return True, None
        return False, f"Status: {resp.status_code}, Body: {resp.text}"
    except requests.RequestException as e:
        return False, str(e)


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
def _get_tournament(tournament_id: str) -> tuple[Any, dict, str]:
    """Fetch tournament doc from either collection.

    Returns (snapshot, data, collection_name). Raises if not found.
    Checks 'tournaments' first, then 'hot_or_not_tournaments'.
    """
    # Try smiley tournaments first
    ref = db().collection("tournaments").document(tournament_id)
    snap = ref.get()
    if snap.exists:
        return snap, snap.to_dict() or {}, "tournaments"

    # Try hot_or_not_tournaments
    ref = db().collection("hot_or_not_tournaments").document(tournament_id)
    snap = ref.get()
    if snap.exists:
        return snap, snap.to_dict() or {}, "hot_or_not_tournaments"

    return None, {}, ""


def _get_participant_count(tournament_id: str, collection_name: str = "tournaments") -> int:
    """Get count of registered users in tournament."""
    users_ref = db().collection(f"{collection_name}/{tournament_id}/users")
    count_result = users_ref.count().get()
    return int(count_result[0][0].value) if count_result else 0


def _get_user_registration(tournament_id: str, principal_id: str, collection_name: str = "tournaments") -> Optional[dict]:
    """Check if user is registered for tournament."""
    ref = db().document(f"{collection_name}/{tournament_id}/users/{principal_id}")
    snap = ref.get()
    return snap.to_dict() if snap.exists else None


def _vote_doc_id(principal_id: str, video_id: str) -> str:
    """Generate consistent vote document ID."""
    return f"{principal_id}_{video_id}"


# ─────────────────────  LEADERBOARD HELPERS  ────────────────────────
# Initial diamonds given at registration
INITIAL_DIAMONDS = 20


def _compute_tournament_leaderboard(tournament_id: str, limit: int = 10, collection_name: str = "tournaments") -> List[dict]:
    """Compute top N users by diamond balance with strict ranking.

    Only includes users who have played at least 1 game (wins > 0 or losses > 0).

    Ranking order:
    1. Diamonds DESC (more diamonds = higher rank)
    2. Total games (wins + losses) DESC (tiebreaker: more games = higher rank)
    3. updated_at ASC (second tiebreaker: earlier = higher rank)
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

    # Collect all qualifying users first
    candidates = []
    for snap in snaps:
        data = snap.to_dict() or {}
        if is_hot_or_not:
            wins = int(data.get("wins") or 0)
            losses = int(data.get("losses") or 0)
        else:
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
            "losses": losses,
            "total_games": total_games,
            "updated_at": updated_at,
        })

    # Sort by: diamonds DESC, total_games DESC, updated_at ASC
    # For updated_at ASC, we use a large default for None values
    def sort_key(x):
        updated = x.get("updated_at")
        # Convert to timestamp for comparison, use max value if None
        updated_ts = updated.timestamp() if updated else float('inf')
        return (-x["diamonds"], -x["total_games"], updated_ts)

    candidates.sort(key=sort_key)

    # Take top N and assign strict positions (1, 2, 3, ...)
    rows = []
    principal_ids = []
    for i, candidate in enumerate(candidates[:limit], start=1):
        rows.append({
            "principal_id": candidate["principal_id"],
            "diamonds": candidate["diamonds"],
            "wins": candidate["wins"],
            "losses": candidate["losses"],
            "position": i
        })
        principal_ids.append(candidate["principal_id"])

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
    top_rows: List[dict],
    collection_name: str = "tournaments"
) -> dict:
    """Get user's position in tournament leaderboard.

    Ranking order:
    1. Diamonds DESC (more diamonds = higher rank)
    2. Total games (wins + losses) DESC (tiebreaker: more games = higher rank)
    3. updated_at ASC (second tiebreaker: earlier = higher rank)

    Only users who have played at least 1 game are ranked.
    Users who haven't played return position = 0.
    """
    # Check if in top rows first
    for row in top_rows:
        if row["principal_id"] == principal_id:
            return row

    # Hot or Not uses "wins"/"losses", smiley uses "tournament_wins"/"tournament_losses"
    is_hot_or_not = collection_name == "hot_or_not_tournaments"

    # Query user's registration
    user_ref = db().document(f"{collection_name}/{tournament_id}/users/{principal_id}")
    user_snap = user_ref.get()

    # Get username
    profile_ref = db().document(f"users/{principal_id}")
    profile_snap = profile_ref.get()
    username = (profile_snap.to_dict() or {}).get("username") if profile_snap.exists else None

    user_data = user_snap.to_dict() or {} if user_snap.exists else {}
    user_diamonds = int(user_data.get("diamonds") or 0)
    if is_hot_or_not:
        user_wins = int(user_data.get("wins") or 0)
        user_losses = int(user_data.get("losses") or 0)
    else:
        user_wins = int(user_data.get("tournament_wins") or 0)
        user_losses = int(user_data.get("tournament_losses") or 0)
    user_total_games = user_wins + user_losses
    user_updated_at = user_data.get("updated_at")

    # If user hasn't played, return position 0 (not ranked)
    if user_wins == 0 and user_losses == 0:
        return {
            "principal_id": principal_id,
            "diamonds": user_diamonds,
            "wins": user_wins,
            "losses": user_losses,
            "position": 0,
            "username": username
        }

    # Count users who rank higher using tiebreaker logic
    # Fetch users with diamonds >= user_diamonds (need to check tiebreakers for equal diamonds)
    users_ref = db().collection(f"{collection_name}/{tournament_id}/users")
    snaps = users_ref.where("diamonds", ">=", user_diamonds).limit(1000).stream()

    user_updated_ts = user_updated_at.timestamp() if user_updated_at else float('inf')

    higher = 0
    for snap in snaps:
        if snap.id == principal_id:
            continue  # Skip self

        data = snap.to_dict() or {}
        if is_hot_or_not:
            w = int(data.get("wins") or 0)
            l = int(data.get("losses") or 0)
        else:
            w = int(data.get("tournament_wins") or 0)
            l = int(data.get("tournament_losses") or 0)

        # Only count users who have played
        if w == 0 and l == 0:
            continue

        diamonds = int(data.get("diamonds") or 0)
        total_games = w + l
        updated_at = data.get("updated_at")
        updated_ts = updated_at.timestamp() if updated_at else float('inf')

        # Check if this user ranks higher
        # Higher rank = more diamonds, or same diamonds + more games, or same both + earlier updated_at
        if diamonds > user_diamonds:
            higher += 1
        elif diamonds == user_diamonds:
            if total_games > user_total_games:
                higher += 1
            elif total_games == user_total_games:
                if updated_ts < user_updated_ts:
                    higher += 1

    return {
        "principal_id": principal_id,
        "diamonds": user_diamonds,
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
    List tournaments with optional date/status/tournament_id filters.
    Returns both smiley and hot_or_not tournaments with a 'type' field.

    POST /tournaments
    Request:
        { "data": { "date": "2025-12-14", "status": "scheduled", "principal_id": "...", "tournament_id": "...", "type": "..." } }
        All fields optional. Defaults to today's date if not provided.
        If tournament_id is provided, returns only that tournament (ignores date filter).
        If principal_id is provided, includes is_registered and user_stats for each tournament.
        If type is provided ("smiley" or "hot_or_not"), filters by tournament type.

    Response:
        { "tournaments": [ {..., "type": "smiley", "is_registered": true, "user_stats": {...}}, ... ] }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        date_filter = str(data.get("date") or "").strip()
        status_filter = str(data.get("status") or "").strip().lower()
        principal_id = str(data.get("principal_id") or "").strip()
        tournament_id = str(data.get("tournament_id") or "").strip()
        type_filter = str(data.get("type") or "").strip().lower()

        # Validate type filter if provided
        if type_filter and type_filter not in ("smiley", "hot_or_not"):
            return error_response(400, "INVALID_TYPE", "type must be 'smiley' or 'hot_or_not'")

        # If tournament_id is provided, fetch that specific tournament
        if tournament_id:
            # Try smiley tournaments first
            snap = db().collection("tournaments").document(tournament_id).get()
            tournament_type = "smiley"
            if not snap.exists:
                # Try hot_or_not_tournaments
                snap = db().collection("hot_or_not_tournaments").document(tournament_id).get()
                tournament_type = "hot_or_not"
            if not snap.exists:
                return jsonify({"tournaments": []}), 200
            docs = [(snap, tournament_type)]
        else:
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

            docs = []

            # Query smiley tournaments (if not filtering for hot_or_not only)
            if not type_filter or type_filter == "smiley":
                smiley_query = db().collection("tournaments").where("date", "==", date_filter)
                for snap in smiley_query.stream():
                    docs.append((snap, "smiley"))

            # Query hot_or_not tournaments (if not filtering for smiley only)
            if not type_filter or type_filter == "hot_or_not":
                hot_or_not_query = db().collection("hot_or_not_tournaments").where("date", "==", date_filter)
                for snap in hot_or_not_query.stream():
                    docs.append((snap, "hot_or_not"))

        result = []
        for snap, tournament_type in docs:
            t_data = snap.to_dict() or {}

            # Compute status dynamically based on current time vs epochs
            start_epoch_ms = t_data.get("start_epoch_ms", 0)
            end_epoch_ms = t_data.get("end_epoch_ms", 0)
            computed_status = _compute_status(start_epoch_ms, end_epoch_ms)

            # Filter by status if provided
            if status_filter and computed_status != status_filter:
                continue

            # Get participant count from denormalized field (fast) or fallback to query
            collection_name = "tournaments" if tournament_type == "smiley" else "hot_or_not_tournaments"
            participant_count = t_data.get("participant_count")
            if participant_count is None:
                participant_count = _get_participant_count(snap.id, collection_name)

            tournament_entry = {
                "id": snap.id,
                "title": t_data.get("title", "SMILEY SHOWDOWN" if tournament_type == "smiley" else "HOT OR NOT"),
                "type": tournament_type,
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
                "user_stats": None,
            }

            # Check if user is registered (if principal_id provided)
            if principal_id:
                reg_data = _get_user_registration(snap.id, principal_id, collection_name)
                if reg_data:
                    tournament_entry["is_registered"] = True
                    tournament_entry["user_stats"] = {
                        "registered_at": reg_data.get("registered_at"),
                        "coins_paid": reg_data.get("coins_paid"),
                        "diamonds": reg_data.get("diamonds", 0),
                        "tournament_wins": reg_data.get("tournament_wins", reg_data.get("wins", 0)),
                        "tournament_losses": reg_data.get("tournament_losses", reg_data.get("losses", 0)),
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

        snap, t_data, collection_name = _get_tournament(tournament_id)
        if not snap:
            return error_response(404, "TOURNAMENT_NOT_FOUND", f"Tournament {tournament_id} not found")

        # Compute status dynamically based on current time vs epochs
        start_epoch_ms = t_data.get("start_epoch_ms", 0)
        end_epoch_ms = t_data.get("end_epoch_ms", 0)
        status = _compute_status(start_epoch_ms, end_epoch_ms)
        participant_count = t_data.get("participant_count")
        if participant_count is None:
            participant_count = _get_participant_count(tournament_id, collection_name)

        response = {
            "tournament_id": tournament_id,
            "status": status,
            "participant_count": participant_count
        }

        # Add time_left_ms only for LIVE tournaments
        if status == TournamentStatus.LIVE.value:
            response["time_left_ms"] = _time_left_ms(end_epoch_ms)

        return jsonify(response), 200

    except Exception as e:
        print(f"tournament_status error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"])
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
    balance_update_token = os.environ.get("BALANCE_UPDATE_TOKEN")
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

        # Get tournament and validate - check both collections
        tournament_ref = db().collection("tournaments").document(tournament_id)
        t_snap = tournament_ref.get()
        collection_name = "tournaments"

        # If not found in tournaments, check hot_or_not_tournaments
        if not t_snap.exists:
            tournament_ref = db().collection("hot_or_not_tournaments").document(tournament_id)
            t_snap = tournament_ref.get()
            collection_name = "hot_or_not_tournaments"

        if not t_snap.exists:
            return error_response(404, "TOURNAMENT_NOT_FOUND", f"Tournament {tournament_id} not found")

        reg_ref = db().document(f"{collection_name}/{tournament_id}/users/{principal_id}")

        t_data = t_snap.to_dict() or {}
        entry_cost = int(t_data.get("entryCost") or 0)

        # Compute status dynamically from epochs
        start_epoch_ms = t_data.get("start_epoch_ms", 0)
        end_epoch_ms = t_data.get("end_epoch_ms", 0)
        status = _compute_status(start_epoch_ms, end_epoch_ms)

        if status not in ["scheduled", "live"]:
            return error_response(409, "TOURNAMENT_NOT_OPEN", f"Tournament is {status}, registration closed")

        # Check if already registered
        reg_snap = reg_ref.get()
        if reg_snap.exists:
            return error_response(409, "ALREADY_REGISTERED", "Already registered for this tournament")

        # Deduct YRAL tokens from actual balance via external API
        if entry_cost > 0:
            if not balance_update_token:
                print("BALANCE_UPDATE_TOKEN not configured", file=sys.stderr)
                return error_response(500, "CONFIG_ERROR", "Balance update not configured")

            success, error_msg = _push_delta_yral_token(balance_update_token, principal_id, -entry_cost)
            if not success:
                print(f"Failed to deduct YRAL tokens for {principal_id}: {error_msg}", file=sys.stderr)
                return error_response(402, "INSUFFICIENT_COINS", "Failed to deduct entry fee. Please check your balance.")

        # Create registration in Firestore
        initial_diamonds = 20  # Fixed 20 diamonds for all tournaments

        # Use different field names for Hot or Not tournaments
        if collection_name == "hot_or_not_tournaments":
            reg_ref.set({
                "registered_at": firestore.SERVER_TIMESTAMP,
                "coins_paid": entry_cost,
                "diamonds": initial_diamonds,
                "wins": 0,
                "losses": 0,
                "status": "registered",
                "updated_at": firestore.SERVER_TIMESTAMP
            })
        else:
            reg_ref.set({
                "registered_at": firestore.SERVER_TIMESTAMP,
                "coins_paid": entry_cost,
                "diamonds": initial_diamonds,
                "tournament_wins": 0,
                "tournament_losses": 0,
                "status": "registered",
                "updated_at": firestore.SERVER_TIMESTAMP
            })

        # Increment participant count on tournament document (denormalized for fast reads)
        tournament_ref.update({
            "participant_count": firestore.Increment(1),
            "updated_at": firestore.SERVER_TIMESTAMP
        })

        # Log transaction in Firestore for audit
        user_ref = db().document(f"users/{principal_id}")
        ledger_ref = user_ref.collection("transactions").document(_tx_id())
        ledger_ref.set({
            "delta": -entry_cost,
            "reason": "TOURNAMENT_ENTRY",
            "tournament_id": tournament_id,
            "at": firestore.SERVER_TIMESTAMP
        })

        # Note: coins_remaining is no longer accurate from Firestore
        # The client should fetch the actual balance from the YRAL API
        return jsonify({
            "status": "registered",
            "tournament_id": tournament_id,
            "coins_paid": entry_cost,
            "coins_remaining": 0,  # Client should refresh balance from YRAL API
            "diamonds": initial_diamonds
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
    Returns both smiley and hot_or_not tournaments with a 'type' field.

    POST /my_tournaments
    Request:
        { "data": { "principal_id": "..." } }

    Response:
        { "tournaments": [ {..., "type": "smiley"}, {..., "type": "hot_or_not"}, ... ] }

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

        # Query recent tournaments (last 7 days only for performance)
        today = datetime.now(IST).date()
        cutoff_date = (today - timedelta(days=7)).strftime("%Y-%m-%d")

        result = []

        # Helper function to process tournaments from a collection
        def process_tournaments(collection_name: str, tournament_type: str, default_title: str):
            # Single query: get all tournaments from last 7 days
            t_snaps = list(
                db().collection(collection_name)
                .where("date", ">=", cutoff_date)
                .order_by("date", direction=firestore.Query.DESCENDING)
                .limit(50)
                .stream()
            )

            # Batch check user registrations
            for t_snap in t_snaps:
                reg_ref = db().document(f"{collection_name}/{t_snap.id}/users/{principal_id}")
                reg_snap = reg_ref.get()

                if not reg_snap.exists:
                    continue

                reg_data = reg_snap.to_dict() or {}

                # Only include tournaments where user has actually played
                tournament_wins = reg_data.get("tournament_wins", reg_data.get("wins", 0)) or 0
                tournament_losses = reg_data.get("tournament_losses", reg_data.get("losses", 0)) or 0

                if tournament_wins == 0 and tournament_losses == 0:
                    continue

                t_data = t_snap.to_dict() or {}

                # Compute status dynamically
                start_epoch_ms = t_data.get("start_epoch_ms", 0)
                end_epoch_ms = t_data.get("end_epoch_ms", 0)
                computed_status = _compute_status(start_epoch_ms, end_epoch_ms)

                result.append({
                    "id": t_snap.id,
                    "title": t_data.get("title", default_title),
                    "type": tournament_type,
                    "date": t_data.get("date"),
                    "start_time": t_data.get("start_time"),
                    "end_time": t_data.get("end_time"),
                    "start_epoch_ms": start_epoch_ms,
                    "end_epoch_ms": end_epoch_ms,
                    "entry_cost": t_data.get("entryCost"),
                    "total_prize_pool": t_data.get("totalPrizePool"),
                    "status": computed_status,
                    "prize_map": t_data.get("prizeMap", {}),
                    "participant_count": t_data.get("participant_count", 0),
                    "user_stats": {
                        "registered_at": reg_data.get("registered_at"),
                        "coins_paid": reg_data.get("coins_paid"),
                        "diamonds": reg_data.get("diamonds", 0),
                        "tournament_wins": tournament_wins,
                        "tournament_losses": tournament_losses,
                        "status": reg_data.get("status")
                    }
                })

        # Process smiley tournaments
        process_tournaments("tournaments", "smiley", "SMILEY SHOWDOWN")

        # Process hot_or_not tournaments
        process_tournaments("hot_or_not_tournaments", "hot_or_not", "HOT OR NOT")

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
            "prize_map": {...},
            "participant_count": 42,
            "date": "2025-12-24",
            "start_epoch_ms": 1234567890000,
            "end_epoch_ms": 1234567899000,
            "title": "Tournament Name"
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
        snap, t_data, collection_name = _get_tournament(tournament_id)
        if not snap:
            return error_response(404, "TOURNAMENT_NOT_FOUND", f"Tournament {tournament_id} not found")

        # Compute status dynamically based on current time vs epochs
        start_epoch_ms = t_data.get("start_epoch_ms", 0)
        end_epoch_ms = t_data.get("end_epoch_ms", 0)
        status = _compute_status(start_epoch_ms, end_epoch_ms)
        prize_map = t_data.get("prizeMap", {})

        # Allow leaderboard viewing after ENDED or SETTLED
        if status not in [TournamentStatus.ENDED.value, TournamentStatus.SETTLED.value]:
            return error_response(409, "TOURNAMENT_STILL_ACTIVE",
                                  f"Leaderboard available after tournament ends (current: {status})")

        # Get top rows
        top_rows = _compute_tournament_leaderboard(tournament_id, limit=10, collection_name=collection_name)

        # Add prizes to top rows
        for row in top_rows:
            position = str(row["position"])
            row["prize"] = prize_map.get(position)

        # Get user row
        user_row = _get_user_tournament_position(tournament_id, principal_id, top_rows, collection_name=collection_name)
        user_position = str(user_row.get("position", 0))
        user_row["prize"] = prize_map.get(user_position) if user_position != "0" else None

        # Get participant count from denormalized field or fallback to query
        participant_count = t_data.get("participant_count")
        if participant_count is None:
            participant_count = _get_participant_count(tournament_id, collection_name)

        return jsonify({
            "tournament_id": tournament_id,
            "status": status,
            "top_rows": top_rows,
            "user_row": user_row,
            "prize_map": prize_map,
            "participant_count": participant_count,
            "date": t_data.get("date", ""),
            "start_epoch_ms": t_data.get("start_epoch_ms", 0),
            "end_epoch_ms": t_data.get("end_epoch_ms", 0),
            "title": t_data.get("title", "Tournament"),
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except Exception as e:
        print(f"tournament_leaderboard error: {e}", file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")
