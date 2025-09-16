import re, random, string, datetime as _dt, sys
import firebase_admin
from firebase_admin import auth, app_check, firestore, functions
from firebase_functions import https_fn
from firebase_functions.https_fn import HttpsError, CallableRequest
from flask import Request, jsonify, make_response
from google.api_core.exceptions import AlreadyExists, GoogleAPICallError
from typing import List, Dict, Any
import requests
from requests.exceptions import RequestException
import os
import random
import collections
from datetime import datetime, timezone, timedelta, date
from zoneinfo import ZoneInfo

firebase_admin.initialize_app()

WIN_REWARD = 3
LOSS_PENALTY = -1
SHARDS = 10

PID_REGEX = re.compile(r'^[A-Za-z0-9_-]{6,64}$')
SMILEY_GAME_CONFIG_PATH = "config/smiley_game_v2"
VIDEO_COLL = "videos"
DAILY_COLL = "leaderboards_daily"

BALANCE_URL = "https://yral-hot-or-not.go-bazzinga.workers.dev/update_balance/"

IST = timezone(timedelta(hours=5, minutes=30))

# ─────────────────────  DATABASE HELPER  ────────────────────────
_db = None
def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db

# ─────────────────────  TRANSACTION ID HELPER  ────────────────────────
def _tx_id() -> str:
    now = _dt.datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
    rnd = "".join(random.choices(string.ascii_lowercase + string.digits, k=4))
    return f"{now}_{rnd}"

# ─────────────────────  COIN HELPER  ────────────────────────
def tx_coin_change(principal_id: str, video_id: str | None, delta: int, reason: str) -> int:
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

# ─────────────────────  SMILEY CONFIG HELPER  ────────────────────────
_SMILEYS: List[Dict[str, str]] | None = None
def get_smileys() -> List[Dict[str, str]]:
    # global _SMILEYS
    # if _SMILEYS is None:
    #     snap = db().document(SMILEY_GAME_CONFIG_PATH).get()
    #     _SMILEYS = snap.get("available_smileys") or []
    # return _SMILEYS

    global _SMILEYS
    if _SMILEYS is None:
        _SMILEYS = [
            {
                "click_animation": "smiley_game/animations/smiley_game_laugh.json",
                "id": "laugh",
                "image_name": "laugh",
                "image_url": "smiley_game/game/laugh.png",
                "is_active": True,
                "image_fallback": "😂"
            },
            {
                "click_animation": "smiley_game/animations/smiley_game_heart.json",
                "id": "heart",
                "image_name": "heart",
                "image_url": "smiley_game/game/heart.png",
                "is_active": True,
                "image_fallback": "❤️"
            },
            {
                "click_animation": "smiley_game/animations/smiley_game_fire.json",
                "id": "fire",
                "image_name": "fire",
                "image_url": "smiley_game/game/fire.png",
                "is_active": True,
                "image_fallback": "🔥"
            },
            {
                "click_animation": "smiley_game/animations/smiley_game_surprise.json",
                "id": "surprise",
                "image_name": "surprise",
                "image_url": "smiley_game/game/surprise.png",
                "is_active": True,
                "image_fallback": "😲"
            },
            {
                "click_animation": "smiley_game/animations/smiley_game_rocket.json",
                "id": "rocket",
                "image_name": "rocket",
                "image_url": "smiley_game/game/rocket.png",
                "is_active": True,
                "image_fallback": "🚀"
            },
            {
                "click_animation": "smiley_game/animations/smiley_game_puke.json",
                "id": "puke",
                "image_name": "puke",
                "image_url": "smiley_game/game/puke.png",
                "is_active": True,
                "image_fallback": "🤮"
            }
        ]
    return _SMILEYS

# ─────────────────────  ERROR HELPER  ────────────────────────
def error_response(status: int, code: str, message: str):
    payload = {"error": {"code": code, "message": message}}
    return make_response(jsonify(payload), status)

# ─────────────────────  MAIN HANDLER  ────────────────────────
@https_fn.on_request(region="us-central1", enforce_app_check=True)
def exchange_principal_id(request: Request):
    try:
        # 1. validate & auth -------------------------------------------------
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        principal_id  = str(body.get("principal_id") or data.get("principal_id") or "").strip()
        if not PID_REGEX.fullmatch(principal_id):
            return error_response(400, "INVALID_PRINCIPAL_ID",
                                  "principal_id must be 6–64 chars A-Z a-z 0-9 _-")

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization header required")
        caller_token = auth.verify_id_token(auth_header.split(" ", 1)[1])
        old_uid = caller_token["uid"]                      # could equal principal_id

        actok = request.headers.get("X-Firebase-AppCheck")
        if actok:
            try:
                app_check.verify_token(actok)
            except Exception:
                return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        # 2. ensure Auth user for principal_id ------------------------------
        try:
            auth.get_user(principal_id)
        except auth.UserNotFoundError:
            auth.create_user(uid=principal_id)

        # 3. Firestore merge / init -----------------------------------------
        new_ref = db().document(f"users/{principal_id}")
        old_ref = db().document(f"users/{old_uid}") if old_uid != principal_id else None

        @firestore.transactional
        def _ensure_profile(tx: firestore.Transaction):
            if not new_ref.get(transaction=tx).exists:
                tx.set(new_ref,
                       {"created_at": firestore.SERVER_TIMESTAMP, "smiley_game_wins": 0, "smiley_game_losses": 0})

            if old_ref:
                tx.delete(old_ref)                 # discard temp profile

        _ensure_profile(db().transaction())

        # 4. delete old Auth user (best-effort) -----------------------------
        if old_uid != principal_id:
            try:
                auth.delete_user(old_uid)
            except auth.UserNotFoundError:
                pass

        # 5. return custom token + coins -----------------------------------
        token = auth.create_custom_token(principal_id).decode()
        return jsonify({"token": token}), 200

    # -------- known errors to JSON ---------------------------------------
    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token malformed or expired")
    except auth.TokenSignError as e:
        return error_response(500, "TOKEN_SIGN_ERROR", str(e))
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    # -------- fallback ----------------------------------------------------
    except Exception as e:  # noqa: BLE001
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")

def _push_delta(token: str, principal_id: str, delta: int) -> tuple[bool, str | None]:
    url = f"{BALANCE_URL}{principal_id}"
    headers = {
        "Authorization": token,
        "Content-Type": "application/json",
    }
    body = {
        "delta": str(delta),          # radix-10 string, e.g. "-210"
        "is_airdropped": False
    }
    try:
        resp = requests.post(url, json=body, timeout=5, headers=headers)
        if resp.status_code == 200:
            return True, None
        return False, f"Status: {resp.status_code}, Body: {resp.text}"
    except requests.RequestException as e:
        return False, str(e)
    
@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"], enforce_app_check=True)
def update_balance(request: Request):
    balance_update_token = os.environ["BALANCE_UPDATE_TOKEN"]
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOW_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        pid = str(data.get("principal_id", "")).strip()
        delta = int(data.get("delta", 0))
        is_airdropped = bool(data.get("is_airdropped", False))

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization token missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        # ───────── App Check enforcement ─────────
        actok = request.headers.get("X-Firebase-AppCheck")
        if not actok:
            return error_response(
                401, "APPCHECK_MISSING",
                "App Check token required"
            )

        try:
            app_check.verify_token(actok)
        except Exception:
            return error_response(
                401, "APPCHECK_INVALID",
                "App Check token invalid"
            )

        success, error_msg = _push_delta(balance_update_token, pid, delta)

        if not success:
            return error_response(502, "UPSTREAM_FAILED", f"Balance update failed: {error_msg}")

        coins = tx_coin_change(pid, None, delta, "AIRDROP")

        return jsonify({"coins": coins}), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:                                 # fallback
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")

@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"], enforce_app_check=True)
def tap_to_recharge(request: Request):
    try:
        # 1️⃣ POST check
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        # 2️⃣ Parse body
        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        pid = str(data.get("principal_id", "")).strip()
        if not pid:
            return error_response(400, "MISSING_PID", "principal_id required")

        # 3️⃣ Verify ID token
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization token missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        # ───────── App Check enforcement ─────────
        actok = request.headers.get("X-Firebase-AppCheck")
        if not actok:
            return error_response(
                401, "APPCHECK_MISSING",
                "App Check token required"
            )

        try:
            app_check.verify_token(actok)
        except Exception:
            return error_response(
                401, "APPCHECK_INVALID",
                "App Check token invalid"
            )

        # 5️⃣ Ensure balance is zero
        user_ref = db().document(f"users/{pid}")
        current  = user_ref.get().get("coins") or 0
        if current > 0:
            return error_response(
                409, "NON_ZERO_BALANCE",
                f"Balance is already {current}. Recharge works only at 0.")

        # 6️⃣ Push to wallet first
        DELTA = 100
        if not _push_delta(os.environ["BALANCE_UPDATE_TOKEN"], pid, DELTA):
            return error_response(
                502, "UPSTREAM_FAILED",
                "Wallet update failed, try again later.")

        # 7️⃣ Book it in Firestore (reason TAP_RECHARGE)
        coins = tx_coin_change(pid, None, DELTA, "TAP_RECHARGE")

        # 8️⃣ Success → return new total only
        return jsonify({"coins": coins}), 200

    # ── standard error wrappers ───────────────────────────────────────
    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")

@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"], enforce_app_check=True)
def cast_vote(request: Request):
    balance_update_token = os.environ["BALANCE_UPDATE_TOKEN"]
    try:
        # 1. validation & auth ────────────────────────────────────────────
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        pid  = str(body.get("principal_id") or data.get("principal_id") or "").strip()
        vid  = str(body.get("video_id") or data.get("video_id") or "").strip()
        sid  = str(body.get("smiley_id") or data.get("smiley_id") or "").strip()

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        actok = request.headers.get("X-Firebase-AppCheck")
        if actok:
            try:
                app_check.verify_token(actok)
            except Exception:
                return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        # load the global smiley list once per cold-start
        smileys     = get_smileys()                          # [{id, image_name, …}, …]
        smiley_map  = {s["id"]: s for s in smileys}
        
        smiley_map.pop("puke", None)

        if sid not in smiley_map:
            return error_response(400, "SMILEY_NOT_ALLOWED", "smiley_id not in config")

        # 2. references ───────────────────────────────────────────────────
        vid_ref   = db().collection(VIDEO_COLL).document(vid)
        vote_ref  = vid_ref.collection("votes").document(pid)
        user_ref = db().document(f"users/{pid}")
        shard_ref = lambda k: vid_ref.collection("tallies").document(f"shard_{k}")

        # 3. transaction: READS first, WRITES after ──────────────────────
        @firestore.transactional
        def _vote_tx(tx: firestore.Transaction) -> dict:
            user_snapshot = user_ref.get(transaction=tx)
            balance = user_snapshot.get("coins") or 0
            if balance < abs(LOSS_PENALTY):
                return {"result": "INSUFFICIENT", "coins": balance}

            already_voted = vote_ref.get(transaction=tx).exists
            if already_voted:
                return {"result": "DUP"}

            vid_exists  = vid_ref.get(transaction=tx).exists
            if not vid_exists:
                all_ids = [s["id"] for s in smileys]
                seed_a = random.choice(all_ids)
                
                pool = [smid for smid in all_ids if smid != seed_a]
                seed_b = random.choice(pool)

                tx.set(vid_ref, {
                    "created_at": firestore.SERVER_TIMESTAMP
                })

                zero = {s["id"]: 0 for s in smileys}
                zero[seed_a] = 1
                zero[seed_b] = 1
                tx.set(shard_ref(0), zero, merge=True)

                for k in range(1, SHARDS):
                    tx.set(shard_ref(k), {s["id"]: 0 for s in smileys}, merge=True)

            # write vote + counter
            tx.set(vote_ref, {
                "smiley_id": sid,
                "at": firestore.SERVER_TIMESTAMP
            })
            tx.update(
                shard_ref(random.randrange(SHARDS)),
                {sid: firestore.Increment(1)}
            )
            return {"result": "OK"}

        tx_out = _vote_tx(db().transaction())
        if tx_out["result"] == "DUP":
            return error_response(409, "DUPLICATE_VOTE", "You have already voted on this video. Scroll to Next Video to keep Voting.")

        if tx_out["result"] == "INSUFFICIENT":
            return error_response(402, "INSUFFICIENT_COINS", f"Balance {tx_out['coins']} < {abs(LOSS_PENALTY)} required")

        # 4. read tallies after commit ───────────────────────────────────
        raw_counts = collections.Counter()
        for k in range(SHARDS):
            shard = shard_ref(k).get().to_dict() or {}
            raw_counts.update(shard)

        counts = {sid: raw_counts.get(sid, 0) for sid in smiley_map.keys()}

        max_votes = max(counts.values())
        leaders = [sm for sm, v in counts.items() if v == max_votes]

        if len(leaders) == 1:
            winner_id = leaders[0]
            outcome = "WIN" if sid == winner_id else "LOSS"
        else:
            winner_id = None
            outcome = "LOSS"

        # 5. coin mutation ───────────────────────────────────────────────
        delta  = WIN_REWARD if outcome == "WIN" else LOSS_PENALTY
        coins  = tx_coin_change(pid, vid, delta, outcome)

        if not _push_delta(balance_update_token, pid, delta):
            _ = tx_coin_change(pid, vid, -delta, "ROLLBACK")
            return error_response(502, "UPSTREAM_FAILED", "We couldn’t record your vote. Please try voting again after sometime.")

        vote_ref.update({
            "outcome": outcome,
            "coin_delta": delta
        })

        if outcome == "WIN":
            user_ref.update({"smiley_game_wins": firestore.Increment(1)})
        else:
            user_ref.update({"smiley_game_losses": firestore.Increment(1)})

        # 6. success payload (voted smiley) ──────────────────────────────
        voted = smiley_map[sid]
        return jsonify({
            "outcome":    outcome,
            "smiley": {
                "id":         voted["id"],
                "image_url": voted["image_url"],
                "is_active":  voted["is_active"],
                "click_animation": voted["click_animation"]
            },
            "coins":       coins,
            "coin_delta":  delta
        }), 200

    # known error wrappers ───────────────────────────────────────────────
    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:                                 # fallback
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")

@https_fn.on_request(region="us-central1", secrets=["BALANCE_UPDATE_TOKEN"], enforce_app_check=True)
def cast_vote_v2(request: Request):
    balance_update_token = os.environ["BALANCE_UPDATE_TOKEN"]
    try:
        # 1. validation & auth ────────────────────────────────────────────
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {})
        pid  = str(body.get("principal_id") or data.get("principal_id") or "").strip()
        vid  = str(body.get("video_id") or data.get("video_id") or "").strip()
        sid  = str(body.get("smiley_id") or data.get("smiley_id") or "").strip()

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        actok = request.headers.get("X-Firebase-AppCheck")
        if actok:
            try:
                app_check.verify_token(actok)
            except Exception:
                return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        # load the global smiley list once per cold-start
        smileys     = get_smileys()                          # [{id, image_name, …}, …]
        smiley_map  = {s["id"]: s for s in smileys}
        
        smiley_map.pop("heart", None)

        if sid not in smiley_map:
            return error_response(400, "SMILEY_NOT_ALLOWED", "smiley_id not in config")

        # 2. references ───────────────────────────────────────────────────
        vid_ref   = db().collection(VIDEO_COLL).document(vid)
        vote_ref  = vid_ref.collection("votes").document(pid)
        user_ref = db().document(f"users/{pid}")
        shard_ref = lambda k: vid_ref.collection("tallies").document(f"shard_{k}")

        # 3. transaction: READS first, WRITES after ──────────────────────
        @firestore.transactional
        def _vote_tx(tx: firestore.Transaction) -> dict:
            user_snapshot = user_ref.get(transaction=tx)
            balance = user_snapshot.get("coins") or 0
            if balance < abs(LOSS_PENALTY):
                return {"result": "INSUFFICIENT", "coins": balance}

            already_voted = vote_ref.get(transaction=tx).exists
            if already_voted:
                return {"result": "DUP"}

            vid_exists  = vid_ref.get(transaction=tx).exists
            if not vid_exists:
                all_ids = [s["id"] for s in smileys if s["id"] not in ["heart"]]
                seed_a, seed_b = random.sample(all_ids, 2)

                tx.set(vid_ref, {
                    "created_at": firestore.SERVER_TIMESTAMP
                })

                zero = {s["id"]: 0 for s in smileys}
                zero[seed_a] = 1
                zero[seed_b] = 1
                tx.set(shard_ref(0), zero, merge=True)

                for k in range(1, SHARDS):
                    tx.set(shard_ref(k), {s["id"]: 0 for s in smileys}, merge=True)

            # write vote + counter
            tx.set(vote_ref, {
                "smiley_id": sid,
                "at": firestore.SERVER_TIMESTAMP
            })
            tx.update(
                shard_ref(random.randrange(SHARDS)),
                {sid: firestore.Increment(1)}
            )
            return {"result": "OK"}

        tx_out = _vote_tx(db().transaction())
        if tx_out["result"] == "DUP":
            return error_response(409, "DUPLICATE_VOTE", "You have already voted on this video. Scroll to Next Video to keep Voting.")

        if tx_out["result"] == "INSUFFICIENT":
            return error_response(402, "INSUFFICIENT_COINS", f"Balance {tx_out['coins']} < {abs(LOSS_PENALTY)} required")

        # 4. read tallies after commit ───────────────────────────────────
        raw_counts = collections.Counter()
        for k in range(SHARDS):
            shard = shard_ref(k).get().to_dict() or {}
            raw_counts.update(shard)

        counts = {sm_id: raw_counts.get(sm_id, 0) for sm_id in smiley_map.keys()}

        max_votes = max(counts.values())
        leaders = [sm for sm, v in counts.items() if v == max_votes]

        if len(leaders) == 1:
            winner_id = leaders[0]
            outcome = "WIN" if sid == winner_id else "LOSS"
        else:
            winner_id = None
            outcome = "LOSS"

        # 5. coin mutation ───────────────────────────────────────────────
        delta  = WIN_REWARD if outcome == "WIN" else LOSS_PENALTY
        coins  = tx_coin_change(pid, vid, delta, outcome)

        if not _push_delta(balance_update_token, pid, delta):
            _ = tx_coin_change(pid, vid, -delta, "ROLLBACK")
            return error_response(502, "UPSTREAM_FAILED", "We couldn’t record your vote. Please try voting again after sometime.")

        vote_ref.update({
            "outcome": outcome,
            "coin_delta": delta
        })

        if outcome == "WIN":
            user_ref.update({"smiley_game_wins": firestore.Increment(1)})
        else:
            user_ref.update({"smiley_game_losses": firestore.Increment(1)})

        try:
            _inc_daily_leaderboard(pid, outcome)
        except Exception as e:
            print(f"[daily-leaderboard] failed for {pid}: {e}", file=sys.stderr)

        # 6. success payload (voted smiley) ──────────────────────────────
        voted = smiley_map[sid]
        return jsonify({
            "outcome":    outcome,
            "smiley": {
                "id":         voted["id"],
                "image_url": voted["image_url"],
                "is_active":  voted["is_active"],
                "click_animation": voted["click_animation"],
                "image_fallback": voted["image_fallback"]
            },
            "coins":       coins,
            "coin_delta":  delta
        }), 200

    # known error wrappers ───────────────────────────────────────────────
    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:                                 # fallback
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


############################## Leaderboard ##############################
def _bucket_bounds_ist() -> tuple[str, int, int]:
    """
    Returns (bucket_id, start_ms, end_ms) for today's IST day window.
    bucket_id format: 'YYYY-MM-DD' in IST.
    """
    now_utc = datetime.now(timezone.utc)
    now_ist = now_utc.astimezone(IST)

    # Start of day (midnight IST)
    start_ist = datetime(now_ist.year, now_ist.month, now_ist.day, 0, 0, 0, tzinfo=IST)
    end_ist = start_ist + timedelta(days=1)

    bucket_id = start_ist.strftime("%Y-%m-%d")
    start_ms = int(start_ist.timestamp() * 1000)
    end_ms   = int(end_ist.timestamp() * 1000)
    return bucket_id, start_ms, end_ms

def _inc_daily_leaderboard(pid: str, outcome: str) -> None:
    """Increment today's IST daily leaderboard counters for this user."""
    bucket_id, start_ms, end_ms = _bucket_bounds_ist()
    day_ref   = db().document(f"{DAILY_COLL}/{bucket_id}")
    entry_ref = day_ref.collection("users").document(pid)

    @firestore.transactional
    def _tx(tx: firestore.Transaction):
        # Ensure the day doc exists & store window bounds (helpful for reads/debug)
        tx.set(day_ref, {
            "bucket_id": bucket_id,
            "start_ms": start_ms,
            "end_ms": end_ms,
            "updated_at": firestore.SERVER_TIMESTAMP
        }, merge=True)

        # Increment the user row for today
        updates = {
            "principal_id": pid,
            "smiley_game_wins": firestore.Increment(1 if outcome == "WIN"  else 0),
            "smiley_game_losses": firestore.Increment(1 if outcome == "LOSS" else 0),
            "updated_at": firestore.SERVER_TIMESTAMP
        }
        tx.set(entry_ref, updates, merge=True)

    _tx(db().transaction())

def _bucket_id_for_ist_day(d: date) -> str:
    """Return 'YYYY-MM-DD' for a given IST date."""
    return datetime(d.year, d.month, d.day, tzinfo=IST).strftime("%Y-%m-%d")

def _ist_today_date() -> date:
    return datetime.now(IST).date()

def _friendly_date_str(d: date) -> str:
    """e.g., 'Aug 15' (no leading zero for day)."""
    return datetime(d.year, d.month, d.day, tzinfo=IST).strftime("%b %d").replace(" 0", " ")

def _dense_top_rows_for_day(bucket_id: str) -> tuple[List[Dict], int, int]:
    """Top 10 rows with dense ranking for a given IST bucket."""
    coll = db().collection(f"{DAILY_COLL}/{bucket_id}/users")
    snaps = (
        coll.order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
            .limit(10)
            .stream()
    )
    rows: List[Dict] = []
    current_rank, last_wins = 0, None
    for snap in snaps:
        wins = int(snap.get("smiley_game_wins") or 0)
        if wins != last_wins:
            current_rank += 1  # dense ranking
            last_wins = wins
        rows.append({
            "principal_id": snap.id,
            "wins": wins,
            "position": current_rank,
        })
    return rows, last_wins, current_rank

def _user_row_for_day(bucket_id: str, pid: str) -> Dict:
    """Compute user's wins and dense position within that day's collection."""
    day_users = db().collection(f"{DAILY_COLL}/{bucket_id}/users")
    entry = day_users.document(pid).get()
    user_wins = int((entry.get("smiley_game_wins") if entry.exists else 0) or 0)

    # Dense position: count docs with wins strictly greater than user's
    # Firestore aggregation count() → list of results, take the first
    rank_q = day_users.where("smiley_game_wins", ">", user_wins).count().get()
    higher = int(rank_q[0][0].value)
    position = higher + 1

    return {
        "principal_id": pid,
        "wins": user_wins,
        "position": position,
    }

def _has_any_docs_for_day(bucket_id: str) -> bool:
    """Fast existence check: returns True if the day has at least one user doc."""
    users_coll = db().collection(f"{DAILY_COLL}/{bucket_id}/users")
    try:
        return next(users_coll.limit(1).stream(), None) is not None
    except GoogleAPICallError as e:
        # Treat read error as "no data" per requirement to skip such a day
        print(f"[skip] users scan error for {bucket_id}: {e}", file=sys.stderr)
        return False

@https_fn.on_request(region="us-central1", enforce_app_check=True)
def leaderboard(request: Request):
    """
    Request
      { "data": { "principal_id": "<pid>", "mode": "daily" | "all_time" } }

    Response
      {
        "user_row": { principal_id, wins, position },
        "top_rows": [ … up to 10 … ],
        "time_left_ms": <int | null>   # ms left in today's IST window if daily, else null
      }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        pid  = str(data.get("principal_id", "")).strip()
        mode = str(data.get("mode", "")).lower().strip()
        is_daily = (mode == "daily")

        if not pid:
            return error_response(400, "MISSING_PID", "principal_id required")

        # ───────── Auth & App Check ─────────
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        actok = request.headers.get("X-Firebase-AppCheck")
        if not actok:
            return error_response(401, "APPCHECK_MISSING", "App Check token required")
        try:
            app_check.verify_token(actok)
        except Exception:
            return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        if not is_daily:
            # ===== All-time leaderboard =====
            users = db().collection("users")
            user_ref = users.document(pid)

            user_snap = user_ref.get()
            user_wins = int(user_snap.get("smiley_game_wins") or 0)

            rank_q = (
                users.where("smiley_game_wins", ">", user_wins)
                     .count()
                     .get()
            )
            user_position = int(rank_q[0][0].value) + 1

            top_snaps = (
                users.order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                     .limit(10)
                     .stream()
            )

            top_rows, current_rank, last_wins = [], 0, None
            for snap in top_snaps:
                wins = int(snap.get("smiley_game_wins") or 0)
                if wins != last_wins:
                    current_rank += 1   # dense ranking
                    last_wins = wins
                top_rows.append({
                    "principal_id": snap.id,
                    "wins": wins,
                    "position": current_rank
                })

            user_row = {
                "principal_id": pid,
                "wins": user_wins,
                "position": user_position
            }

            return jsonify({
                "user_row": user_row,
                "top_rows": top_rows,
                "time_left_ms": None
            }), 200

        # ===== Daily leaderboard (IST day) =====
        bucket_id, _start_ms, end_ms = _bucket_bounds_ist()
        now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        time_left_ms = max(0, end_ms - now_ms)

        day_users = db().collection(f"{DAILY_COLL}/{bucket_id}/users")
        entry_ref = day_users.document(pid)

        entry_snap = entry_ref.get()
        user_wins = int((entry_snap.get("smiley_game_wins") if entry_snap.exists else 0) or 0)

        rank_q = (
            day_users.where("smiley_game_wins", ">", user_wins)
                     .count()
                     .get()
        )
        user_position = int(rank_q[0][0].value) + 1

        top_snaps = (
            day_users.order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                     .limit(10)
                     .stream()
        )

        top_rows, current_rank, last_wins = [], 0, None
        for snap in top_snaps:
            wins = int(snap.get("smiley_game_wins") or 0)
            if wins != last_wins:
                current_rank += 1    # dense ranking
                last_wins = wins
            top_rows.append({
                "principal_id": snap.id,
                "wins": wins,
                "position": current_rank
            })

        need = 10 - len(top_rows)
        if need > 0:
            # Pull a pool of candidates from all-time users, then shuffle
            candidates = (
            db().collection("users")
                .order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                .limit(20)
                .stream()
            )
            existing_ids = {r["principal_id"] for r in top_rows}
            cand_ids = [u.id for u in candidates if u.id not in existing_ids]
            random.shuffle(cand_ids)
            fill_ids = cand_ids[:need]

            # Dense ranking: all zeros share the same position.
            if last_wins != 0:
                current_rank += 1
                last_wins = 0

            for pid_fill in fill_ids:
                top_rows.append({
                    "principal_id": pid_fill,
                    "wins": 0,
                    "position": current_rank
                })

        # Safety: ensure exactly 10
        top_rows = top_rows[:10]

        user_row = {
            "principal_id": pid,
            "wins": user_wins,
            "position": user_position
        }

        return jsonify({
            "user_row": user_row,
            "top_rows": top_rows,
            "time_left_ms": time_left_ms
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print("Leaderboard error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")

@https_fn.on_request(region="us-central1", enforce_app_check=True)
def leaderboard_v2(request: Request):
    """
    Request
      { "data": { "principal_id": "<pid>", "mode": "daily" | "all_time" } }

    Response
      {
        "user_row": { principal_id, wins, position },
        "top_rows": [ … up to 10 … ],
        "time_left_ms": <int | null>   # ms left in today's IST window if daily, else null
      }
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}

        pid  = str(data.get("principal_id", "")).strip()
        mode = str(data.get("mode", "")).lower().strip()
        is_daily = (mode == "daily")

        if not pid:
            return error_response(400, "MISSING_PID", "principal_id required")

        # ───────── Auth & App Check ─────────
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        actok = request.headers.get("X-Firebase-AppCheck")
        if not actok:
            return error_response(401, "APPCHECK_MISSING", "App Check token required")
        try:
            app_check.verify_token(actok)
        except Exception:
            return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        if not is_daily:
            # ===== All-time leaderboard =====
            users = db().collection("users")
            user_ref = users.document(pid)

            user_snap = user_ref.get()
            user_wins = int(user_snap.get("smiley_game_wins") or 0)

            rank_q = (
                users.where("smiley_game_wins", ">", user_wins)
                     .count()
                     .get()
            )
            user_position = int(rank_q[0][0].value) + 1

            top_snaps = (
                users.order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                     .limit(10)
                     .stream()
            )

            top_rows, current_rank, last_wins = [], 0, None
            for snap in top_snaps:
                wins = int(snap.get("smiley_game_wins") or 0)
                if wins != last_wins:
                    current_rank += 1   # dense ranking
                    last_wins = wins
                top_rows.append({
                    "principal_id": snap.id,
                    "wins": wins,
                    "position": current_rank
                })

            user_row = {
                "principal_id": pid,
                "wins": user_wins,
                "position": user_position
            }

            return jsonify({
                "user_row": user_row,
                "top_rows": top_rows,
                "time_left_ms": None
            }), 200

        # ===== Daily leaderboard (IST day) =====
        bucket_id, _start_ms, end_ms = _bucket_bounds_ist()
        now_ms = int(datetime.now(timezone.utc).timestamp() * 1000)
        time_left_ms = max(0, end_ms - now_ms)

        day_users = db().collection(f"{DAILY_COLL}/{bucket_id}/users")
        entry_ref = day_users.document(pid)

        entry_snap = entry_ref.get()
        user_wins = int((entry_snap.get("smiley_game_wins") if entry_snap.exists else 0) or 0)

        rank_q = (
            day_users.where("smiley_game_wins", ">", user_wins)
                     .count()
                     .get()
        )
        user_position = int(rank_q[0][0].value) + 1

        top_snaps = (
            day_users.order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                     .limit(10)
                     .stream()
        )

        top_rows, current_rank, last_wins = [], 0, None
        for snap in top_snaps:
            wins = int(snap.get("smiley_game_wins") or 0)
            if wins != last_wins:
                current_rank += 1    # dense ranking
                last_wins = wins
            top_rows.append({
                "principal_id": snap.id,
                "wins": wins,
                "position": current_rank
            })

        need = 10 - len(top_rows)
        if need > 0:
            # Pull a pool of candidates from all-time users, then shuffle
            candidates = (
            db().collection("users")
                .order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                .limit(20)
                .stream()
            )
            existing_ids = {r["principal_id"] for r in top_rows}
            cand_ids = [u.id for u in candidates if u.id not in existing_ids]
            random.shuffle(cand_ids)
            fill_ids = cand_ids[:need]

            # Dense ranking: all zeros share the same position.
            if last_wins != 0:
                current_rank += 1
                last_wins = 0

            for pid_fill in fill_ids:
                top_rows.append({
                    "principal_id": pid_fill,
                    "wins": 0,
                    "position": current_rank
                })

        # Safety: ensure exactly 10
        top_rows = top_rows[:10]

        user_row = {
            "principal_id": pid,
            "wins": user_wins,
            "position": user_position
        }

        return jsonify({
            "user_row": user_row,
            "top_rows": top_rows,
            "time_left_ms": time_left_ms
        }), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print("Leaderboard error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")

@https_fn.on_request(region="us-central1", enforce_app_check=True)
def leaderboard_history(request: Request):
    """
    Request:
      { "data": { "principal_id": "<pid>" } }

    Response: 200 OK with an ARRAY (newest → oldest). Each item:
      {
        "date": "Aug 15",
        "top_rows": [ { principal_id, wins, position } ... up to 10 ],
        "user_row": { principal_id, wins, position } | null
      }

    Notes:
    - If a given day has no docs (or any read error), that day is omitted entirely.
    - Latest day returned is "yesterday" in IST.
    """
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        data = body.get("data", {}) or {}
        pid = str(data.get("principal_id", "")).strip()

        if not pid:
            return error_response(400, "MISSING_PID", "principal_id required")

        # ── Auth & App Check ────────────────────────────────────────────────
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        actok = request.headers.get("X-Firebase-AppCheck")
        if not actok:
            return error_response(401, "APPCHECK_MISSING", "App Check token required")
        try:
            app_check.verify_token(actok)
        except Exception:
            return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        # ── Build last 7 IST dates EXCLUDING today (newest → oldest) ───────
        today = _ist_today_date()
        days = [today - timedelta(days=offset) for offset in range(1, 8)]  # 1..7 days ago

        result: List[Dict] = []
        for d in days:
            bucket_id = _bucket_id_for_ist_day(d)
            date_label = _friendly_date_str(d)
            top_rows = []
            last_wins, current_rank = None, 0

            # Skip if the day has no docs (or read error)
            if not _has_any_docs_for_day(bucket_id):
                # Pull a pool of candidates from all-time users, then shuffle
                need = 10
                candidates = (
                db().collection("users")
                    .order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                    .limit(20)
                    .stream()
                )
                existing_ids = {r["principal_id"] for r in top_rows}
                cand_ids = [u.id for u in candidates if u.id not in existing_ids]
                random.shuffle(cand_ids)
                fill_ids = cand_ids[:need]

                # Dense ranking: all zeros share the same position.
                if last_wins != 0:
                    current_rank += 1
                    last_wins = 0

                for pid_fill in fill_ids:
                    top_rows.append({
                        "principal_id": pid_fill,
                        "wins": 0,
                        "position": current_rank
                    })
            else:
                # Top rows (dense)
                try:
                    top_rows, last_wins, current_rank = _dense_top_rows_for_day(bucket_id)
                except GoogleAPICallError as e:
                    # Per requirement, skip this day fully on read error
                    print(f"[skip] top rows read error for {bucket_id}: {e}", file=sys.stderr)
                    continue

            need = 10 - len(top_rows)
            if need > 0:
                # Pull a pool of candidates from all-time users, then shuffle
                candidates = (
                db().collection("users")
                    .order_by("smiley_game_wins", direction=firestore.Query.DESCENDING)
                    .limit(20)
                    .stream()
                )
                existing_ids = {r["principal_id"] for r in top_rows}
                cand_ids = [u.id for u in candidates if u.id not in existing_ids]
                random.shuffle(cand_ids)
                fill_ids = cand_ids[:need]

                # Dense ranking: all zeros share the same position.
                if last_wins != 0:
                    current_rank += 1
                    last_wins = 0

                for pid_fill in fill_ids:
                    top_rows.append({
                        "principal_id": pid_fill,
                        "wins": 0,
                        "position": current_rank
                    })

            # Safety: ensure exactly 10
            top_rows = top_rows[:10]

            # Compute user_row; null it if user appears in top_rows
            try:
                user_row = _user_row_for_day(bucket_id, pid)
            except GoogleAPICallError as e:
                user_row = {"principal_id": pid, "wins": 0, "position": 1}

            result.append({
                "date": date_label,      # 'Aug 15'
                "top_rows": top_rows,    # up to 10, dense-ranked
                "user_row": user_row     # or null if in top_rows
            })

        # Already newest → oldest because we iterated 1..7 days ago in order
        return jsonify(result), 200

    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))
    except Exception as e:
        print("leaderboard_daily_last7_excl_today error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")


#################### BTC to INR ####################
TICKER_URL = "https://blockchain.info/ticker"
@https_fn.on_request(region="us-central1", enforce_app_check=True)
def btc_inr_value(request: Request):
    """
    GET /btc_inr_value

    Headers:
      Authorization: Bearer <FIREBASE_ID_TOKEN>
      X-Firebase-AppCheck: <APPCHECK_TOKEN>

    Response (200):
      { "inr": 0.00 }
    """
    try:
        if request.method != "GET":
            return error_response(405, "METHOD_NOT_ALLOWED", "GET required")

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization token missing")
        id_token = auth_header.split(" ", 1)[1]
        try:
            auth.verify_id_token(id_token)
        except auth.InvalidIdTokenError:
            return error_response(401, "ID_TOKEN_INVALID", "ID token invalid or expired")

        actok = request.headers.get("X-Firebase-AppCheck")
        if not actok:
            return error_response(401, "APPCHECK_MISSING", "App Check token required")
        try:
            app_check.verify_token(actok)
        except Exception:
            return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        try:
            resp = requests.get(TICKER_URL, timeout=6)
        except requests.RequestException as e:
            print("Network error:", e, file=sys.stderr)
            return error_response(502, "UPSTREAM_UNREACHABLE", "Price source not reachable")

        if resp.status_code != 200:
            return error_response(502, "UPSTREAM_BAD_STATUS",
                                  f"Price source returned {resp.status_code}")

        try:
            data = resp.json()
            inr = data.get("INR") or {}
            last = round(float(inr["last"]), 2)
        except Exception as e:
            print("Parse error:", e, file=sys.stderr)
            return error_response(502, "UPSTREAM_BAD_PAYLOAD", "Unexpected price payload")

        return jsonify({"inr": last}), 200

    except GoogleAPICallError as e:
        return error_response(500, "GOOGLE_API_ERROR", str(e))
    except Exception as e:
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")