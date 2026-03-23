#!/usr/bin/env python3
import argparse
import hashlib
import hmac
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path
from time import time


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--key-id", required=True)
    parser.add_argument("--secret")
    parser.add_argument("--secret-env")
    parser.add_argument("--body-file")
    parser.add_argument("--body")
    parser.add_argument("--timestamp")
    parser.add_argument("--nonce")
    return parser.parse_args()


def load_body(args: argparse.Namespace) -> str:
    if args.body and args.body_file:
      raise ValueError("use either --body or --body-file")
    if args.body_file:
        return Path(args.body_file).read_text(encoding="utf-8")
    if args.body:
        return args.body
    return sys.stdin.read()


def main() -> int:
    args = parse_args()
    secret = args.secret
    if args.secret_env:
        secret = secret or __import__("os").environ.get(args.secret_env)
    if not secret:
        raise ValueError("secret is required via --secret or --secret-env")
    body = load_body(args)
    timestamp = args.timestamp or str(int(time()))
    nonce = args.nonce or str(uuid.uuid4())
    parsed_url = urllib.parse.urlparse(args.url)
    path = parsed_url.path or "/"

    body_sha256 = hashlib.sha256(body.encode("utf-8")).hexdigest()
    canonical_payload = "\n".join(
        [
            "v1",
            "POST",
            path,
            args.key_id,
            timestamp,
            nonce,
            body_sha256,
        ]
    )
    signature = hmac.new(
        secret.encode("utf-8"),
        canonical_payload.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

    request = urllib.request.Request(
        args.url,
        data=body.encode("utf-8"),
        method="POST",
        headers={
            "Content-Type": "application/json",
            "X-Operational-Key-Id": args.key_id,
            "X-Operational-Timestamp": timestamp,
            "X-Operational-Nonce": nonce,
            "X-Operational-Body-SHA256": body_sha256,
            "X-Operational-Signature": signature,
        },
    )

    try:
        with urllib.request.urlopen(request) as response:
            payload = response.read().decode("utf-8")
            print(
                json.dumps(
                    {
                        "httpStatus": response.status,
                        "timestamp": timestamp,
                        "nonce": nonce,
                        "bodySha256": body_sha256,
                        "response": json.loads(payload) if payload else None,
                    }
                )
            )
            return 0
    except urllib.error.HTTPError as error:
        payload = error.read().decode("utf-8")
        print(
            json.dumps(
                {
                    "httpStatus": error.code,
                    "timestamp": timestamp,
                    "nonce": nonce,
                    "bodySha256": body_sha256,
                    "response": json.loads(payload) if payload else None,
                }
            )
        )
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
