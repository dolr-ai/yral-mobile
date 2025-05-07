import re, firebase_admin
from firebase_admin import auth, app_check
from flask import abort, jsonify, Request
from firebase_functions import https_fn

firebase_admin.initialize_app()

ID_RE = re.compile(r'^[A-Za-z0-9_-]{6,64}$')  # adapt to your spec

@https_fn.on_request(region="us-central1")
def exchange_principal_id(request: Request):
    if request.method != "POST":
        abort(405)
    body = request.get_json(silent=True) or {}
    pid = body.get("principalId", "").strip()

    # ① Validate principal ID
    if not ID_RE.fullmatch(pid):
        abort(400, "invalid principalId")

    # ② Verify caller’s ID token
    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith("Bearer "):
        abort(401, "Missing ID token")
    token = auth.verify_id_token(auth_header.split(" ", 1)[1])
    old_uid = token["uid"]

    # ③ (Optional) verify App Check
    ac_token = request.headers.get("X-Firebase-AppCheck")
    if ac_token:
        app_check.verify_token(ac_token)

    # ④ Ensure the principal ID is still free
    try:
        auth.get_user(pid)
        abort(409, "principalId already taken")
    except auth.UserNotFoundError:
        pass

    # ⑤ Create new user + mint custom token
    auth.create_user(uid=pid)
    custom_token = auth.create_custom_token(pid)

    # ⑥ Delete transient anonymous user
    try:
        auth.delete_user(old_uid)
    except auth.UserNotFoundError:
        pass

    return jsonify({"token": custom_token.decode()})
