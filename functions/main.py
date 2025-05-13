import re, random, string, datetime as _dt, sys
import firebase_admin
from firebase_admin import auth, app_check, firestore
from firebase_functions import https_fn
from flask import Request, jsonify, make_response
from google.api_core.exceptions import AlreadyExists

firebase_admin.initialize_app()

PID_REGEX = re.compile(r'^[A-Za-z0-9_-]{6,64}$')
DEFAULT_COINS = 2_000

_db = None
def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db

def _tx_id() -> str:
    now = _dt.datetime.utcnow().strftime("%Y%m%dT%H%M%SZ")
    rnd = "".join(random.choices(string.ascii_lowercase + string.digits, k=4))
    return f"{now}_{rnd}"

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
    except firestore.FirestoreError as e:
        return error_response(500, "FIRESTORE_ERROR", str(e))

    # ────────── CATCH-ALL LAST RESORT (logs & hides) ──────────
    except Exception as e:  # noqa: BLE001
        print("Unhandled error:", e, file=sys.stderr)
        return error_response(500, "INTERNAL", "Internal server error")