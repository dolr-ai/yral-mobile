import sys
from typing import Any
from flask import Request

# ─────────────────────  LOGGING HELPERS  ────────────────────────
_SENSITIVE_HEADERS = {"authorization", "x-firebase-appcheck"}
_SENSITIVE_PAYLOAD_KEYS = {"token", "id_token", "authorization", "auth_token", "access_token"}


def _sanitize_headers(headers: dict[str, str]) -> dict[str, str]:
    sanitized = {}
    for key, value in headers.items():
        sanitized[key] = "[REDACTED]" if key.lower() in _SENSITIVE_HEADERS else value
    return sanitized


def _sanitize_payload(obj: Any, max_string: int = 512, max_items: int = 25) -> Any:
    """Recursively scrub sensitive data and trim oversized payloads for logging."""
    if isinstance(obj, dict):
        cleaned = {}
        for key, value in obj.items():
            if key.lower() in _SENSITIVE_PAYLOAD_KEYS:
                cleaned[key] = "[REDACTED]"
            else:
                cleaned[key] = _sanitize_payload(value, max_string, max_items)
        return cleaned
    if isinstance(obj, list):
        trimmed = obj[:max_items]
        sanitized_list = [_sanitize_payload(item, max_string, max_items) for item in trimmed]
        if len(obj) > max_items:
            sanitized_list.append("...truncated...")
        return sanitized_list
    if isinstance(obj, str):
        return obj if len(obj) <= max_string else f"{obj[:max_string]}...[truncated]"
    return obj


def log_request(function_name: str, request: Request) -> None:
    """Log the incoming request with sensitive bits redacted."""
    try:
        body = request.get_json(silent=True)
    except Exception:
        body = None

    try:
        log_entry = {
            "method": request.method,
            "path": request.path,
            "query": request.args.to_dict(flat=True),
            "headers": _sanitize_headers(dict(request.headers)),
            "json": _sanitize_payload(body),
        }
        print(f"[REQUEST] {function_name}", log_entry)
    except Exception as e:  # noqa: BLE001
        print(f"[REQUEST_LOG_ERROR] {function_name}: {e}", file=sys.stderr)


def log_response(function_name: str, status_code: int, payload: Any) -> None:
    """Log the outgoing response with sensitive bits redacted."""
    try:
        print(f"[RESPONSE] {function_name} {status_code}", _sanitize_payload(payload))
    except Exception as e:  # noqa: BLE001
        print(f"[RESPONSE_LOG_ERROR] {function_name}: {e}", file=sys.stderr)
