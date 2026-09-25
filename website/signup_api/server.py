import os
import re
import sqlite3
import threading
import time
from collections import OrderedDict, deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs


DB_PATH = Path(os.environ.get("SIGNUP_DB_PATH", "/data/signups.sqlite3"))
MAX_BODY_BYTES = 1024
RATE_WINDOW_SECONDS = 600
RATE_MAX_REQUESTS = 10
EMAIL_LOCAL = re.compile(r"[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+\Z")
DOMAIN_LABEL = re.compile(r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\Z")


def normalize_email(value):
    address = value.strip()
    if not address.isascii() or len(address) > 254 or address.count("@") != 1:
        return None
    local, domain = address.rsplit("@", 1)
    if not local or not local[0].isalnum() or len(local) > 64 or not EMAIL_LOCAL.fullmatch(local):
        return None
    if local.startswith(".") or local.endswith(".") or ".." in local:
        return None
    labels = domain.split(".")
    if len(labels) < 2 or any(not DOMAIN_LABEL.fullmatch(label) for label in labels):
        return None
    return address.lower()


def connect(db_path=DB_PATH):
    connection = sqlite3.connect(db_path, timeout=5)
    connection.execute("PRAGMA busy_timeout = 5000")
    return connection


def initialize(db_path=DB_PATH):
    os.umask(0o077)
    db_path.parent.mkdir(parents=True, exist_ok=True)
    with connect(db_path) as connection:
        connection.execute("PRAGMA journal_mode = WAL")
        connection.execute(
            "CREATE TABLE IF NOT EXISTS signups ("
            "email TEXT PRIMARY KEY, "
            "created_at INTEGER NOT NULL, "
            "invited_at INTEGER"
            ")"
        )
    db_path.chmod(0o600)


class RateLimiter:
    def __init__(self):
        self.attempts = OrderedDict()
        self.lock = threading.Lock()

    def allows(self, address):
        now = time.monotonic()
        with self.lock:
            recent = self.attempts.setdefault(address, deque())
            while recent and now - recent[0] >= RATE_WINDOW_SECONDS:
                recent.popleft()
            self.attempts.move_to_end(address)
            if len(self.attempts) > 4096:
                self.attempts.popitem(last=False)
            if len(recent) >= RATE_MAX_REQUESTS:
                return False
            recent.append(now)
            return True


class SignupHandler(BaseHTTPRequestHandler):
    db_path = DB_PATH
    limiter = RateLimiter()

    def do_GET(self):
        if self.path != "/healthz":
            self.send_error(404)
            return
        try:
            with connect(self.db_path) as connection:
                connection.execute("SELECT 1 FROM signups LIMIT 1").fetchone()
        except sqlite3.Error:
            self.send_error(503)
            return
        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", "3")
        self.end_headers()
        self.wfile.write(b"ok\n")

    def do_POST(self):
        if self.path != "/api/testflight-signups":
            self.send_error(404)
            return
        if self.headers.get("Origin") not in (None, "https://valorantesports.staticvar.dev"):
            self.send_error(403)
            return
        if self.headers.get_content_type() != "application/x-www-form-urlencoded":
            self.send_error(415)
            return
        try:
            size = int(self.headers.get("Content-Length", ""))
        except ValueError:
            self.send_error(411)
            return
        if size < 1 or size > MAX_BODY_BYTES:
            self.send_error(413)
            return
        try:
            fields = parse_qs(
                self.rfile.read(size).decode("utf-8"),
                keep_blank_values=True,
                strict_parsing=True,
                max_num_fields=2,
            )
        except (UnicodeDecodeError, ValueError):
            self.redirect("/testflight/error/")
            return
        if set(fields) - {"email", "website"} or len(fields.get("email", [])) != 1:
            self.redirect("/testflight/error/")
            return
        if fields.get("website", [""])[0]:
            self.redirect("/testflight/thanks/")
            return
        email = normalize_email(fields["email"][0])
        if email is None:
            self.redirect("/testflight/error/")
            return
        address = self.headers.get("X-Forwarded-For", self.client_address[0])
        if not self.limiter.allows(address):
            self.redirect("/testflight/unavailable/")
            return
        try:
            with connect(self.db_path) as connection:
                connection.execute(
                    "INSERT OR IGNORE INTO signups (email, created_at) VALUES (?, ?)",
                    (email, int(time.time())),
                )
        except sqlite3.Error:
            self.redirect("/testflight/unavailable/")
            return
        self.redirect("/testflight/thanks/")

    def redirect(self, destination):
        self.send_response(303)
        self.send_header("Location", destination)
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    initialize()
    ThreadingHTTPServer(("0.0.0.0", 8080), SignupHandler).serve_forever()
