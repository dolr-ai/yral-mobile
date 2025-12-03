import json
import os
from dataclasses import dataclass, field
from datetime import datetime, timezone, timedelta
from enum import Enum
from typing import Any, Dict, Optional

import firebase_admin
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
        }


IST = timezone(timedelta(hours=5, minutes=30))
_db = None


def db() -> firestore.Client:
    global _db
    if _db is None:
        if not firebase_admin._apps:
            firebase_admin.initialize_app()
        _db = firestore.client()
    return _db


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


@https_fn.on_request(region="us-central1")
def create_tournaments(cloud_event):
    """
    Cloud Scheduler target (run daily at 12am IST) to create tournaments
    """
    try:
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

        slots = [("12:45", "13:15"), ("14:00", "14:30"), ("16:00", "16:30")]

        created, skipped, errors = [], [], []

        for start_time, end_time in slots:
            doc_id = _make_doc_id(date_str, start_time, end_time)
            start_dt = _ist_datetime(date_str, start_time)
            end_dt = _ist_datetime(date_str, end_time)
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

            ref = db().collection("tournaments").document(doc_id)
            try:
                ref.create(tour.to_firestore())
                created.append(doc_id)
                _schedule_status_task(doc_id, TournamentStatus.LIVE, start_dt)
                _schedule_status_task(doc_id, TournamentStatus.ENDED, end_dt)
            except AlreadyExists:
                skipped.append({"id": doc_id, "reason": "Already exists"})
            except Exception as e:
                errors.append({"id": doc_id, "reason": str(e)})

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


@https_fn.on_request(region="us-central1")
def update_tournament_status(request: Request):
    """
    Cloud Task target to advance a tournament's status.
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

        # Simple forward-only guard
        order = [
            TournamentStatus.SCHEDULED,
            TournamentStatus.LIVE,
            TournamentStatus.ENDED,
            TournamentStatus.SETTLED,
        ]
        def _order_idx(status: TournamentStatus) -> int:
            return order.index(status) if status in order else -1

        if current_status and _order_idx(target_status) < _order_idx(current_status):
            return jsonify({"status": "skipped", "reason": f"Current status {current_status.value} ahead of {target_status.value}"}), 200

        ref.update({
            "status": target_status.value,
            "updated_at": firestore.SERVER_TIMESTAMP,
        })
        print(f"[update_tournament_status] {doc_id}: {current_status_raw} -> {target_status.value}")
        return jsonify({"status": "ok", "tournament_id": doc_id, "new_status": target_status.value}), 200

    except Exception as e:  # noqa: BLE001
        return jsonify({"error": "INTERNAL", "message": str(e)}), 500
