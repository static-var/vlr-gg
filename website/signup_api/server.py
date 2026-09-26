import os
import secrets
from html import escape
from email.parser import BytesParser
from contextlib import contextmanager
import re
import sqlite3
import threading
import time
from collections import OrderedDict, deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit, urlencode


DB_PATH = Path(os.environ.get("SIGNUP_DB_PATH", "/data/signups.sqlite3"))
MAX_BODY_BYTES = 1024
SUPPORT_MAX_BODY_BYTES = 32768
MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024
MAX_MULTIPART_BYTES = MAX_ATTACHMENT_BYTES + SUPPORT_MAX_BODY_BYTES
PUBLIC_ORIGIN = "https://valorantesports.staticvar.dev"
ADMIN_ORIGIN = os.environ.get("ADMIN_ORIGIN", "http://127.0.0.1:8081")
SUPPORT_FIELDS = {"email", "category", "platform", "subject", "message", "app_version", "device", "website"}
CATEGORIES = ("bug", "feature", "question", "other")
PLATFORMS = ("ios", "android")
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


@contextmanager
def connect(db_path=DB_PATH):
    connection = sqlite3.connect(db_path, timeout=5)
    try:
        connection.execute("PRAGMA busy_timeout = 5000")
        with connection:
            yield connection
    finally:
        connection.close()


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
        connection.execute(
            "CREATE TABLE IF NOT EXISTS support_requests ("
            "id INTEGER PRIMARY KEY, email TEXT NOT NULL, category TEXT NOT NULL, "
            "platform TEXT NOT NULL, subject TEXT NOT NULL, message TEXT NOT NULL, "
            "app_version TEXT NOT NULL DEFAULT '', device TEXT NOT NULL DEFAULT '', "
            "created_at INTEGER NOT NULL, resolved_at INTEGER)"
        )
        connection.execute(
            "CREATE TABLE IF NOT EXISTS support_attachments ("
            "request_id INTEGER PRIMARY KEY REFERENCES support_requests(id), "
            "filename TEXT NOT NULL, extension TEXT NOT NULL, content BLOB NOT NULL)"
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


def attachment_extension(content):
    if content.startswith(b"%PDF-"):
        return "pdf"
    if content.startswith(b"\x89PNG\r\n\x1a\n"):
        return "png"
    if content.startswith(b"\xff\xd8\xff"):
        return "jpg"
    if content.startswith((b"GIF87a", b"GIF89a")):
        return "gif"
    if len(content) >= 12 and content[:4] == b"RIFF" and content[8:12] == b"WEBP":
        return "webp"
    if len(content) >= 16 and content[4:8] == b"ftyp":
        box_size = int.from_bytes(content[:4], "big")
        if 16 <= box_size <= min(len(content), 1024) and box_size % 4 == 0:
            brands = {content[8:12]} | {content[i:i + 4] for i in range(16, box_size, 4)}
            if brands & {b"heic", b"heix", b"hevc", b"hevx", b"mif1", b"msf1"}:
                return "heic"
    return None


def parse_support_multipart(body, boundary):
    """Parse the bounded, flat form without accepting nested MIME or encoded parts."""
    if not boundary or not re.fullmatch(r"[A-Za-z0-9'()+_,./:=? -]{1,70}", boundary):
        raise ValueError("Invalid upload form. Please try again.")
    marker = b"--" + boundary.encode("ascii")
    parts = body.split(b"\r\n" + marker)
    if not parts[0].startswith(marker + b"\r\n") or parts[-1] not in (b"--", b"--\r\n"):
        raise ValueError("Incomplete upload. Please select the file and try again.")
    parts[0] = parts[0][len(marker):]
    if len(parts) > len(SUPPORT_FIELDS) + 2:
        raise ValueError("Only one attachment is allowed.")
    fields, attachments = {}, []
    for part in parts[:-1]:
        if not part.startswith(b"\r\n"):
            raise ValueError("Invalid upload form.")
        header, separator, content = part[2:].partition(b"\r\n\r\n")
        if not separator or len(header) > 4096:
            raise ValueError("Invalid upload form.")
        headers = BytesParser().parsebytes(header + b"\r\n\r\n", headersonly=True)
        name = headers.get_param("name", header="Content-Disposition")
        if (headers.get_content_disposition() != "form-data" or headers.get("Content-Transfer-Encoding")
                or headers.get_content_maintype() == "multipart"):
            raise ValueError("Invalid upload form.")
        filename = headers.get_filename()
        if name == "attachment" and filename is not None:
            attachments.append((filename, content))
        elif name not in SUPPORT_FIELDS or filename is not None or len(content) > SUPPORT_MAX_BODY_BYTES:
            raise ValueError("Invalid form field.")
        else:
            fields.setdefault(name, []).append(content.decode("utf-8"))
    return fields, attachments


class SignupHandler(BaseHTTPRequestHandler):
    db_path = DB_PATH
    limiter = RateLimiter()
    upload_slot = threading.BoundedSemaphore(1)

    def setup(self):
        super().setup()
        self.connection.settimeout(15)

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
        if self.path == "/api/support-requests":
            self.handle_support()
            return
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

    def handle_support(self):
        if self.headers.get("Origin") not in (None, PUBLIC_ORIGIN):
            self.send_error(403)
            return
        address = self.headers.get("X-Forwarded-For", self.client_address[0])
        if not self.limiter.allows(address):
            self.support_error({}, "Too many requests. Please wait a few minutes and try again.", 429)
            return
        if not self.upload_slot.acquire(blocking=False):
            self.support_error({}, "Another upload is being saved. Please try again shortly.", 503)
            return
        try:
            self.save_support()
        finally:
            self.upload_slot.release()

    def save_support(self):
        attachments = []
        if self.headers.get_content_type() == "multipart/form-data":
            try:
                size = int(self.headers.get("Content-Length", ""))
            except ValueError:
                self.send_error(411)
                return
            if size < 1 or size > MAX_MULTIPART_BYTES:
                self.support_error({}, "The attachment must be 5 MB or smaller.", 413)
                return
            try:
                self.connection.settimeout(60)
                try:
                    body = self.rfile.read(size)
                finally:
                    self.connection.settimeout(15)
                fields, attachments = parse_support_multipart(body, self.headers.get_param("boundary"))
                del body
            except (UnicodeDecodeError, ValueError) as error:
                self.support_error({}, str(error) if isinstance(error, ValueError) else "Invalid form text.")
                return
        else:
            fields = self.read_form(SUPPORT_MAX_BODY_BYTES, len(SUPPORT_FIELDS))
            if fields is None:
                return
        if set(fields) - SUPPORT_FIELDS or any(len(value) != 1 for value in fields.values()):
            self.support_error({}, "Please submit one value for each field.")
            return
        values = {key: value[0].strip() for key, value in fields.items()}
        if values.get("website"):
            self.redirect("/support/thanks/")
            return
        email = normalize_email(values.get("email", ""))
        if email is None:
            self.support_error(values, "Enter a valid email address so we can reply.")
            return
        if values.get("category") not in CATEGORIES or values.get("platform") not in PLATFORMS:
            self.support_error(values, "Choose a category and platform.")
            return
        for key, maximum, required in (("subject", 160, True), ("message", 5000, True),
                                       ("app_version", 40, False), ("device", 120, False)):
            if (required and not values.get(key)) or len(values.get(key, "")) > maximum:
                self.support_error(values, f"{key.replace('_', ' ').capitalize()} must contain {'1' if required else '0'}–{maximum} characters.")
                return
        attachment = None
        if len(attachments) > 1:
            self.support_error(values, "Only one attachment is allowed.")
            return
        if attachments and attachments[0] != ("", b""):
            filename, content = attachments[0]
            if len(content) > MAX_ATTACHMENT_BYTES:
                self.support_error(values, "The attachment must be 5 MB or smaller.", 413)
                return
            extension = attachment_extension(content)
            if extension is None:
                self.support_error(values, "Choose a PDF, JPEG, PNG, WebP, GIF, or HEIC image.")
                return
            filename = "".join(c for c in filename.replace("\\", "/").split("/")[-1] if c.isprintable())[:160]
            attachment = (filename or "attachment." + extension, extension, content)
        try:
            with connect(self.db_path) as connection:
                cursor = connection.execute(
                    "INSERT INTO support_requests (email, category, platform, subject, message, app_version, device, created_at) "
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    (email, values["category"], values["platform"], values["subject"], values["message"],
                     values.get("app_version", ""), values.get("device", ""), int(time.time())),
                )
                if attachment:
                    connection.execute(
                        "INSERT INTO support_attachments (request_id, filename, extension, content) VALUES (?, ?, ?, ?)",
                        (cursor.lastrowid, *attachment),
                    )
        except sqlite3.Error:
            self.support_error(values, "We could not save your message. Please try again shortly.", 503)
            return
        self.redirect("/support/thanks/")

    def read_form(self, max_bytes, max_fields):
        if self.headers.get_content_type() != "application/x-www-form-urlencoded":
            self.send_error(415)
            return None
        try:
            size = int(self.headers.get("Content-Length", ""))
        except ValueError:
            self.send_error(411)
            return None
        if size < 1 or size > max_bytes:
            self.send_error(413)
            return None
        try:
            return parse_qs(self.rfile.read(size).decode("utf-8"), keep_blank_values=True,
                            strict_parsing=True, max_num_fields=max_fields)
        except (UnicodeDecodeError, ValueError):
            self.send_error(400)
            return None

    def support_error(self, values, message, status=400):
        fields = []
        for key, title in (("email", "Email"), ("category", "Category"), ("platform", "Platform"),
                           ("subject", "Subject"), ("message", "Message"),
                           ("app_version", "App version (optional)"), ("device", "Device (optional)")):
            value = escape(values.get(key, ""))
            if key in ("category", "platform"):
                options = CATEGORIES if key == "category" else PLATFORMS
                control = '<select name="' + key + '" required><option value="">Select…</option>'
                control += "".join(f'<option value="{item}" {"selected" if values.get(key) == item else ""}>{item.title()}</option>' for item in options)
                control += '</select>'
            elif key == "message":
                control = f'<textarea name="message" maxlength="5000" rows="8" required>{value}</textarea>'
            else:
                maximum = {"email": 254, "subject": 160, "app_version": 40, "device": 120}[key]
                control = f'<input name="{key}" type="{"email" if key == "email" else "text"}" maxlength="{maximum}" value="{value}" {"required" if key in ("email", "subject") else ""}>'
            fields.append(f'<label>{title}{control}</label>')
        self.html_response(status, page("Contact support", f'<p role="alert">{escape(message)}</p>'
                           '<form method="post" action="/api/support-requests" enctype="multipart/form-data">' + "".join(fields) +
                           '<input name="website" hidden tabindex="-1" autocomplete="off">'
                           '<label>Attachment (optional)<input type="file" name="attachment" accept=".pdf,.jpg,.jpeg,.png,.webp,.gif,.heic,.heif"></label>'
                           '<p>One PDF or image, up to 5 MB. Please select your attachment again before resubmitting.</p>'
                           '<button>Send message</button></form><p><a href="/support/">Back to support</a></p>', public=True))

    def html_response(self, status, html):
        body = html.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "same-origin")
        self.send_header("Content-Security-Policy", "default-src 'none'; style-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'")
        self.end_headers()
        self.wfile.write(body)

    def redirect(self, destination):
        self.send_response(303)
        self.send_header("Location", destination)
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, format, *args):
        pass


def page(title, content, public=False):
    style = '<link rel="stylesheet" href="/support/error.css">' if public else (
        '<style>'
        'body{font:16px/1.5 system-ui,sans-serif;margin:40px auto;max-width:960px;padding:0 20px;color:#211b30;background:#faf8ff}'
        'h1,h2{line-height:1.2}a{color:#6538a3}label{display:block;margin:18px 0}'
        'input,select,textarea,button{box-sizing:border-box;font:inherit;padding:10px;border:1px solid #aaa;border-radius:6px}'
        'input,select,textarea{display:block;width:100%;max-width:640px}button{cursor:pointer;background:#60349d;color:white}'
        'article{border:1px solid #c9bfd5;border-radius:10px;padding:20px;margin:20px 0;background:white}'
        '.message{white-space:pre-wrap;overflow-wrap:anywhere}small{color:#625969}'
        'nav{display:flex;gap:18px;flex-wrap:wrap}'
        '</style>'
    )
    return ('<!doctype html><html lang="en"><head><meta charset="utf-8">'
            '<meta name="viewport" content="width=device-width, initial-scale=1">'
            f'<title>{escape(title)} · Val Esports</title>{style}</head><body><header><p>Val Esports</p>'
            f'<h1>{escape(title)}</h1></header><main>{content}</main></body></html>')


def timestamp(value):
    return time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime(value))


class AdminHandler(SignupHandler):
    """Private inbox served only on the separate admin listener, behind Tailscale."""

    csrf_token = secrets.token_urlsafe(32)
    origin = ADMIN_ORIGIN
    page_size = 25

    def trusted_host(self):
        return self.headers.get("Host") == urlsplit(self.origin).netloc

    def do_GET(self):
        if not self.trusted_host():
            self.send_error(403)
            return
        url = urlsplit(self.path)
        attachment_match = re.fullmatch(r"/support/([1-9][0-9]*)/attachment", url.path)
        if attachment_match:
            self.download_attachment(int(attachment_match[1]))
            return
        if url.path != "/":
            self.send_error(404)
            return
        try:
            query = parse_qs(url.query, max_num_fields=2)
            view = query.get("view", ["support"])[0]
            current_page = int(query.get("page", ["1"])[0])
            if view not in ("support", "signups") or current_page < 1 or current_page > 1000000:
                raise ValueError
        except ValueError:
            self.send_error(400)
            return
        try:
            with connect(self.db_path) as connection:
                connection.row_factory = sqlite3.Row
                rows = connection.execute(
                    ("SELECT support_requests.*, (SELECT filename FROM support_attachments WHERE request_id = support_requests.id) AS attachment_filename FROM support_requests"
                     if view == "support" else "SELECT * FROM signups") +
                    f" ORDER BY created_at DESC, {'id' if view == 'support' else 'email'} DESC LIMIT ? OFFSET ?",
                    (self.page_size + 1, (current_page - 1) * self.page_size),
                ).fetchall()
        except sqlite3.Error:
            self.send_error(503)
            return
        content = '<nav><a href="/?view=support">Support requests</a><a href="/?view=signups">TestFlight signups</a></nav>'
        if view == "signups":
            content += '<p>Marking a signup as invited records its status only. Send the actual invitation through App Store Connect.</p>'
        if not rows:
            content += '<p>No entries on this page.</p>'
        for row in rows[:self.page_size]:
            created = timestamp(row["created_at"])
            if view == "support":
                resolved = row["resolved_at"] is not None
                status = "Resolved" if resolved else "Open"
                body = (f'<h2>#{row["id"]} · {escape(row["subject"])}</h2><p>{status} · {created}</p>'
                        f'<p>{escape(row["email"])} · {escape(row["category"])} · {escape(row["platform"])}</p>'
                        f'<p>App version: {escape(row["app_version"]) or "—"} · Device: {escape(row["device"]) or "—"}</p>'
                        f'<div class="message">{escape(row["message"])}</div>')
                if row["attachment_filename"] is not None:
                    body += f'<p><a href="/support/{row["id"]}/attachment">Download attachment: {escape(row["attachment_filename"])}</a></p>'
                body += self.action("support", str(row["id"]), "open" if resolved else "resolved",
                                    "Reopen" if resolved else "Mark resolved", current_page)
            else:
                invited = row["invited_at"] is not None
                body = (f'<h2>{escape(row["email"])}</h2><p>Signed up: {created}</p>'
                        f'<p>{"Invited: " + timestamp(row["invited_at"]) if invited else "Pending invitation"}</p>')
                body += self.action("signups", row["email"], "pending" if invited else "invited",
                                    "Mark pending" if invited else "Mark invited", current_page)
            content += '<article>' + body + '</article>'
        content += '<nav>'
        if current_page > 1:
            content += f'<a href="/?view={view}&amp;page={current_page - 1}">Previous</a>'
        content += f'<span>Page {current_page}</span>'
        if len(rows) > self.page_size:
            content += f'<a href="/?view={view}&amp;page={current_page + 1}">Next</a>'
        content += '</nav>'
        self.html_response(200, page("Support inbox" if view == "support" else "TestFlight signups", content))

    def download_attachment(self, request_id):
        try:
            with connect(self.db_path) as connection:
                row = connection.execute("SELECT extension, content FROM support_attachments WHERE request_id = ?",
                                         (request_id,)).fetchone()
        except sqlite3.Error:
            self.send_error(503)
            return
        if row is None:
            self.send_error(404)
            return
        extension, content = row
        self.send_response(200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Disposition", f'attachment; filename="support-{request_id}.{extension}"')
        self.send_header("Content-Length", str(len(content)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Security-Policy", "default-src 'none'; sandbox")
        self.end_headers()
        self.wfile.write(content)

    def action(self, view, identifier, status, label, current_page):
        fields = {"csrf": self.csrf_token, "id": identifier, "status": status, "page": str(current_page)}
        hidden = "".join(f'<input type="hidden" name="{key}" value="{escape(value)}">' for key, value in fields.items())
        return f'<form method="post" action="/{view}/status">{hidden}<p><button>{label}</button></p></form>'

    def do_POST(self):
        if self.path not in ("/support/status", "/signups/status"):
            self.send_error(404)
            return
        if not self.trusted_host() or self.headers.get("Origin") != self.origin:
            self.send_error(403)
            return
        fields = self.read_form(2048, 4)
        if fields is None:
            return
        if set(fields) != {"csrf", "id", "status", "page"} or any(len(value) != 1 for value in fields.values()):
            self.send_error(400)
            return
        values = {key: value[0] for key, value in fields.items()}
        if not secrets.compare_digest(values["csrf"].encode(), self.csrf_token.encode()):
            self.send_error(403)
            return
        view = "support" if self.path == "/support/status" else "signups"
        statuses = ("open", "resolved") if view == "support" else ("pending", "invited")
        try:
            current_page = int(values["page"])
            if values["status"] not in statuses or current_page < 1 or current_page > 1000000:
                raise ValueError
            identifier = int(values["id"]) if view == "support" else normalize_email(values["id"])
            if identifier is None:
                raise ValueError
        except ValueError:
            self.send_error(400)
            return
        updated_at = int(time.time()) if values["status"] == statuses[1] else None
        try:
            with connect(self.db_path) as connection:
                if view == "support":
                    cursor = connection.execute("UPDATE support_requests SET resolved_at = ? WHERE id = ?", (updated_at, identifier))
                else:
                    cursor = connection.execute("UPDATE signups SET invited_at = ? WHERE email = ?", (updated_at, identifier))
                if cursor.rowcount != 1:
                    self.send_error(404)
                    return
        except sqlite3.Error:
            self.send_error(503)
            return
        self.redirect("/?" + urlencode({"view": view, "page": current_page}))


if __name__ == "__main__":
    initialize()
    admin = ThreadingHTTPServer((os.environ.get("ADMIN_BIND", "127.0.0.1"), 8081), AdminHandler)
    threading.Thread(target=admin.serve_forever, daemon=True).start()
    ThreadingHTTPServer(("0.0.0.0", 8080), SignupHandler).serve_forever()
