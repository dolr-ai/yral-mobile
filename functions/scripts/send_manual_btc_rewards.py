#!/usr/bin/env python3
"""
send_manual_btc_rewards.py
──────────────────────────
Send BTC rewards to a fixed list of principals with INR-denominated amounts.
Usage:
  BALANCE_UPDATE_TOKEN=... python3 scripts/send_manual_btc_rewards.py
Edit REWARD_RECIPIENTS before running.
"""

import os
import sys
import time
from typing import List, Tuple

import requests
from requests import RequestException

BALANCE_URL_CKBTC = "https://yral-hot-or-not.go-bazzinga.workers.dev/v2/transfer_ckbtc"
TICKER_URL = "https://blockchain.info/ticker"
SATOSHIS_PER_BTC = 100_000_000
MEMO_TEXT = "Manual leaderboard reward"
REWARD_RECIPIENTS: List[Tuple[str, int]] = [
    
]


def fetch_btc_price_inr() -> float:
    resp = requests.get(TICKER_URL, timeout=6)
    resp.raise_for_status()
    data = resp.json()
    last = float(data["INR"]["last"])
    if last <= 0:
        raise ValueError("BTC price must be positive")
    return last


def inr_to_ckbtc(amount_inr: int, btc_price_inr: float) -> int:
    satoshis = amount_inr * (SATOSHIS_PER_BTC / btc_price_inr)
    # At least 1 satoshi to avoid zero-value transfers
    return max(1, int(round(satoshis)))


def send_ckbtc(token: str, principal_id: str, amount_ckbtc: int) -> tuple[bool, str | None]:
    headers = {
        "Authorization": token,
        "Content-Type": "application/json",
    }
    body = {
        "amount": amount_ckbtc,
        "recipient_principal": principal_id,
        "memo_text": MEMO_TEXT,
    }
    try:
        resp = requests.post(BALANCE_URL_CKBTC, json=body, timeout=30, headers=headers)
    except RequestException as e:
        return False, str(e)
    if resp.status_code != 200:
        return False, f"Status {resp.status_code}: {resp.text}"
    payload = resp.json()
    if payload.get("success"):
        return True, None
    return False, str(payload)


def main():
    if not REWARD_RECIPIENTS:
        print("No recipients configured in REWARD_RECIPIENTS.", file=sys.stderr)
        sys.exit(1)

    token = os.environ.get("BALANCE_UPDATE_TOKEN")
    if not token:
        print("BALANCE_UPDATE_TOKEN env var required.", file=sys.stderr)
        sys.exit(1)

    try:
        price_inr = fetch_btc_price_inr()
    except Exception as e:
        print(f"Failed to fetch BTC price: {e}", file=sys.stderr)
        sys.exit(1)

    print(f"[INFO] BTC price (INR): {price_inr}")
    factor = SATOSHIS_PER_BTC / price_inr
    print(f"[INFO] INR→ckBTC factor: {factor}")

    successes = 0
    for principal_id, amount_inr in REWARD_RECIPIENTS:
        amount_ckbtc = inr_to_ckbtc(amount_inr, price_inr)
        print(f"[SEND] {principal_id} | INR {amount_inr} -> ckBTC {amount_ckbtc}")
        ok, err = send_ckbtc(token, principal_id, amount_ckbtc)
        if ok:
            successes += 1
            print(f"[OK] {principal_id}")
        else:
            print(f"[FAIL] {principal_id}: {err}", file=sys.stderr)
        time.sleep(0.2)

    print(f"[DONE] Success {successes}/{len(REWARD_RECIPIENTS)}")


if __name__ == "__main__":
    main()
