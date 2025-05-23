import re, random, string, datetime as _dt, sys
import firebase_admin
from firebase_admin import auth, app_check, firestore
from firebase_functions import https_fn
from flask import Request, jsonify, make_response
from google.api_core.exceptions import AlreadyExists, GoogleAPICallError
from typing import List, Dict, Any

firebase_admin.initialize_app()

DEFAULT_COINS = 2_000
WIN_REWARD = 30
LOSS_PENALTY = -10
SHARDS = 10

PID_REGEX = re.compile(r'^[A-Za-z0-9_-]{6,64}$')
SMILEY_GAME_CONFIG_PATH = "config/smiley_game_v1"
VIDEO_COLL = "videos"

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
    user_ref = _db.document(f"users/{principal_id}")
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

    _commit(_db.transaction())
    return int(user_ref.get().get("coins") or 0)

# ─────────────────────  SMILEY CONFIG HELPER  ────────────────────────
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

# ─────────────────────  MAIN HANDLER  ────────────────────────
@https_fn.on_request(region="us-central1")
def exchange_principal_id(request: Request):
    try:
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        principal_id = body.get("principal_id", "").strip()
        if not PID_REGEX.fullmatch(principal_id):
            return error_response(400, "INVALID_PRINCIPAL_ID",
                                  "principal_id must be 6–64 chars A-Z a-z 0-9 _-")

        # ── verify caller’s ID token ───────────────────────────
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization: Bearer <token> required")
        caller_token = auth.verify_id_token(auth_header.split(" ", 1)[1])
        old_uid = caller_token["uid"]

        # ── optional App Check ─────────────────────────────────
        ac_token = request.headers.get("X-Firebase-AppCheck")
        if ac_token:
            try:
                app_check.verify_token(ac_token)
            except Exception:   # noqa: BLE001
                return error_response(401, "APPCHECK_INVALID", "App Check token invalid")

        # ── Auth user: create if not present ──────────────────
        try:
            auth.get_user(principal_id)          # already exists
        except auth.UserNotFoundError:
            auth.create_user(uid=principal_id)   # first time

        custom_token = auth.create_custom_token(principal_id).decode()

        # ── Firestore profile & INIT ledger (idempotent) ──────
        user_ref = db().document(f"users/{principal_id}")
        ledger_ref = user_ref.collection("transactions").document(_tx_id())

        @firestore.transactional
        def _init_profile(tx: firestore.Transaction):
            snap = user_ref.get(transaction=tx)
            if not snap.exists:
                tx.set(user_ref,
                       {"coins": DEFAULT_COINS,
                        "created_at": firestore.SERVER_TIMESTAMP},
                       merge=True)
                try:
                    tx.set(ledger_ref,
                           {"delta": DEFAULT_COINS,
                            "reason": "INIT",
                            "video_id": None,
                            "at": firestore.SERVER_TIMESTAMP})
                except AlreadyExists:
                    pass   # racing INIT – safe to ignore

        _init_profile(db().transaction())

        # current balance (prevents extra read if doc just created)
        coins = (user_ref.get().get("coins") or DEFAULT_COINS)

        # ── best-effort cleanup of anonymous UID ───────────────
        try:
            if (old_uid != principal_id and caller_token.get("firebase", {}).get("sign_in_provider") == "anonymous"):
                auth.delete_user(old_uid)
        except auth.UserNotFoundError:
            pass

        # ✔️  SUCCESS
        return jsonify({"token": custom_token, "coins": coins}), 200

    # ────────── KNOWN EXCEPTIONS → JSON ERROR SHAPE ───────────
    except auth.InvalidIdTokenError:
        return error_response(401, "ID_TOKEN_INVALID", "ID token malformed or expired")
    except auth.TokenSignError as e:
        return error_response(500, "TOKEN_SIGN_ERROR", str(e))
    except GoogleAPICallError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))

    # ────────── CATCH-ALL LAST RESORT (logs & hides) ──────────
    except Exception as e:  # noqa: BLE001
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")
    
@https_fn.on_request(region="us-central1")
def cast_vote(request: Request):
    try:
        # 1. validation & auth ────────────────────────────────────────────
        if request.method != "POST":
            return error_response(405, "METHOD_NOT_ALLOWED", "POST required")

        body = request.get_json(silent=True) or {}
        pid  = str(body.get("principal_id", "")).strip()
        vid  = str(body.get("video_id", "")).strip()
        sid  = str(body.get("smiley_id", "")).strip()

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return error_response(401, "MISSING_ID_TOKEN", "Authorization missing")
        auth.verify_id_token(auth_header.split(" ", 1)[1])

        actok = request.headers.get("X-Firebase-AppCheck")
        if actok:
            try:
                app_check.verify_token(actok)
            except Exception:
                return error_response(401, "APPCHECK_INVALID", "App Check invalid")

        # load the global smiley list once per cold-start
        smileys     = get_smileys()                          # [{id, image_name, …}, …]
        smiley_map  = {s["id"]: s for s in smileys}
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
                zero = {s["id"]: 0 for s in smileys}
                tx.set(vid_ref, {
                    "created_at": firestore.SERVER_TIMESTAMP
                })
                for k in range(SHARDS):
                    tx.set(shard_ref(k), zero, merge=True)

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
            return error_response(409, "DUPLICATE_VOTE", "Already voted")

        if tx_out["result"] == "INSUFFICIENT":
            return error_response(402, "INSUFFICIENT_COINS", f"Balance {tx_out['coins']} < {abs(LOSS_PENALTY)} required")

        # 4. read tallies after commit ───────────────────────────────────
        counts = {s["id"]: 0 for s in smileys}
        for k in range(SHARDS):
            for sm, n in (shard_ref(k).get().to_dict() or {}).items():
                counts[sm] += n

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

        vote_ref.update({
            "outcome": outcome,
            "coin_delta": delta
        })

        # 6. success payload (voted smiley) ──────────────────────────────
        voted = smiley_map[sid]
        return jsonify({
            "outcome":    outcome,
            "smiley": {
                "id":         voted["id"],
                "image_name": voted["image_name"],
                "is_active":  voted["is_active"]
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