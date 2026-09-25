import http.client
from contextlib import closing
import sqlite3
import tempfile
import threading
import unittest
from pathlib import Path
from urllib.parse import urlencode

import server


class SupportAndInboxTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.database = Path(self.directory.name) / "signups.sqlite3"
        server.initialize(self.database)
        self.servers = []
        self.public = self.start(server.SignupHandler)
        self.admin = self.start(server.AdminHandler)
        self.origin = f"http://127.0.0.1:{self.admin.server_port}"
        self.admin.RequestHandlerClass.origin = self.origin

    def start(self, handler):
        isolated = type("TestHandler", (handler,), {"db_path": self.database, "limiter": server.RateLimiter()})
        http = server.ThreadingHTTPServer(("127.0.0.1", 0), isolated)
        thread = threading.Thread(target=http.serve_forever, daemon=True)
        thread.start()
        self.servers.append((http, thread))
        return http

    def tearDown(self):
        for http, thread in self.servers:
            http.shutdown()
            http.server_close()
            thread.join()
        self.directory.cleanup()

    def request(self, listener, method, path, fields=None, headers=None):
        connection = http.client.HTTPConnection("127.0.0.1", listener.server_port)
        connection.request(method, path, None if fields is None else urlencode(fields),
                           headers={"Content-Type": "application/x-www-form-urlencoded", **(headers or {})})
        response = connection.getresponse()
        result = response.status, response.getheader("Location"), response.read().decode()
        connection.close()
        return result

    def support(self, **changes):
        fields = {"email": "Fan@Example.com", "category": "bug", "platform": "ios", "subject": "Scores stuck",
                  "message": "Scores do not update", "app_version": "1.0.5", "device": "iPhone", "website": ""}
        fields.update(changes)
        return self.request(self.public, "POST", "/api/support-requests", fields)

    def rows(self):
        with closing(sqlite3.connect(self.database)) as connection, connection:
            return connection.execute("SELECT email, subject, message, resolved_at FROM support_requests").fetchall()

    def action(self, path, **changes):
        fields = {"csrf": self.admin.RequestHandlerClass.csrf_token, "id": "1", "status": "resolved", "page": "1"}
        fields.update(changes)
        return self.request(self.admin, "POST", path, fields, {"Origin": self.origin})

    def test_support_stored_and_additive_initialization_preserves_both_tables(self):
        self.request(self.public, "POST", "/api/testflight-signups", {"email": "existing@example.com"})
        self.assertEqual(self.support()[:2], (303, "/support/thanks/"))
        server.initialize(self.database)
        self.assertEqual(self.rows(), [("fan@example.com", "Scores stuck", "Scores do not update", None)])
        with closing(sqlite3.connect(self.database)) as connection, connection:
            self.assertEqual(connection.execute("SELECT email FROM signups").fetchall(), [("existing@example.com",)])

    def test_public_listener_does_not_expose_inbox_or_status_actions(self):
        for path in ("/", "/?view=signups", "/admin", "/support/status", "/signups/status"):
            self.assertEqual(self.request(self.public, "GET", path)[0], 404)
            self.assertEqual(self.request(self.public, "POST", path, {"id": "1"})[0], 404)

    def test_invalid_fields_rejected_and_form_preserves_escaped_content(self):
        for changes in ({"email": "bad"}, {"category": "invalid"}, {"platform": "invalid"},
                        {"subject": ""}, {"subject": "a" * 161}, {"message": "a" * 5001},
                        {"app_version": "a" * 41}, {"device": "a" * 121}):
            self.assertEqual(self.support(**changes)[0], 400)
        response = self.support(email="bad", message='</textarea><script>alert(1)</script>', subject='" autofocus onfocus="alert(1)')
        self.assertIn('&lt;/textarea&gt;&lt;script&gt;', response[2])
        self.assertNotIn('<script>', response[2])
        self.assertIn('&quot; autofocus onfocus=&quot;', response[2])
        self.assertEqual(self.rows(), [])

    def test_honeypot_cross_origin_large_body_and_duplicates_are_rejected(self):
        self.assertEqual(self.support(website="bot")[:2], (303, "/support/thanks/"))
        self.assertEqual(self.support(message="a" * server.SUPPORT_MAX_BODY_BYTES)[0], 413)
        self.assertEqual(self.request(self.public, "POST", "/api/support-requests", {"email": "ok@example.com"},
                                      {"Origin": "https://evil.example"})[0], 403)
        self.assertEqual(self.request(self.public, "POST", "/api/support-requests", [("email", "a@b.com"), ("email", "b@c.com")])[0], 400)
        self.assertEqual(self.rows(), [])

    def test_support_rate_limited(self):
        for _ in range(server.RATE_MAX_REQUESTS):
            self.assertEqual(self.support()[0], 303)
        self.assertEqual(self.support()[0], 429)
        self.assertEqual(len(self.rows()), server.RATE_MAX_REQUESTS)

    def test_admin_escapes_stored_content_and_get_does_not_mutate(self):
        self.support(subject='<img src=x onerror=alert(1)>', message='<script>bad()</script>')
        response = self.request(self.admin, "GET", "/")
        self.assertEqual(response[0], 200)
        self.assertIn('&lt;img src=x onerror=alert(1)&gt;', response[2])
        self.assertIn('&lt;script&gt;bad()&lt;/script&gt;', response[2])
        self.assertNotIn('<script>', response[2])
        self.assertEqual(self.rows()[0][3], None)
        self.assertEqual(self.request(self.admin, "GET", "/support/status?id=1&status=resolved")[0], 404)

    def test_admin_host_origin_and_csrf_protection(self):
        self.support()
        self.assertEqual(self.request(self.admin, "GET", "/", headers={"Host": "evil.example"})[0], 403)
        fields = {"csrf": self.admin.RequestHandlerClass.csrf_token, "id": "1", "status": "resolved", "page": "1"}
        for origin in (None, "https://evil.example"):
            self.assertEqual(self.request(self.admin, "POST", "/support/status", fields,
                                           {} if origin is None else {"Origin": origin})[0], 403)
        self.assertEqual(self.action("/support/status", csrf="wrong")[0], 403)
        self.assertEqual(self.rows()[0][3], None)

    def test_admin_status_actions_resolve_reopen_and_track_invites(self):
        self.support()
        self.assertEqual(self.action("/support/status")[:2], (303, "/?view=support&page=1"))
        self.assertIsNotNone(self.rows()[0][3])
        self.assertEqual(self.action("/support/status", status="open")[0], 303)
        self.assertIsNone(self.rows()[0][3])
        self.request(self.public, "POST", "/api/testflight-signups", {"email": "fan@example.com"})
        self.assertEqual(self.action("/signups/status", id="fan@example.com", status="invited")[0], 303)
        listing = self.request(self.admin, "GET", "/?view=signups")[2]
        self.assertIn("fan@example.com", listing)
        self.assertIn("Invited:", listing)
        self.assertEqual(self.action("/signups/status", id="fan@example.com", status="pending")[0], 303)
        self.assertIn("Pending invitation", self.request(self.admin, "GET", "/?view=signups")[2])
        self.assertEqual(self.action("/support/status", status="deleted")[0], 400)
        self.assertEqual(self.action("/support/status", id="999")[0], 404)

    def test_inbox_pagination_is_bounded(self):
        with closing(sqlite3.connect(self.database)) as connection, connection:
            connection.executemany("INSERT INTO signups (email, created_at) VALUES (?, ?)",
                                   [(f"fan{index:02}@example.com", index) for index in range(26)])
        first = self.request(self.admin, "GET", "/?view=signups")[2]
        second = self.request(self.admin, "GET", "/?view=signups&page=2")[2]
        self.assertIn("fan25@example.com", first)
        self.assertNotIn("fan00@example.com", first)
        self.assertIn("fan00@example.com", second)
        self.assertNotIn("fan25@example.com", second)
        self.assertEqual(self.request(self.admin, "GET", "/?view=signups&page=-1")[0], 400)


if __name__ == "__main__":
    unittest.main()
