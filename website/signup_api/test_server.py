import http.client
from contextlib import closing
import os
import sqlite3
import subprocess
import sys
import tempfile
import threading
import unittest
from pathlib import Path
from urllib.parse import urlencode

import server


class SignupHandlerTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.database = Path(self.directory.name) / "signups.sqlite3"
        server.initialize(self.database)
        handler = type(
            "TestSignupHandler",
            (server.SignupHandler,),
            {"db_path": self.database, "limiter": server.RateLimiter()},
        )
        self.http = server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
        self.thread = threading.Thread(target=self.http.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.http.shutdown()
        self.http.server_close()
        self.thread.join()
        self.directory.cleanup()

    def post(self, fields, headers=None):
        connection = http.client.HTTPConnection("127.0.0.1", self.http.server_port)
        connection.request(
            "POST",
            "/api/testflight-signups",
            urlencode(fields),
            headers={"Content-Type": "application/x-www-form-urlencoded", **(headers or {})},
        )
        response = connection.getresponse()
        result = response.status, response.getheader("Location")
        response.read()
        connection.close()
        return result

    def emails(self):
        with closing(sqlite3.connect(self.database)) as connection, connection:
            return connection.execute("SELECT email FROM signups").fetchall()

    def test_valid_signup_persists_once_and_redirects(self):
        for email in ["  Fan+Beta@Example.com  ", "fan+beta@example.com"]:
            self.assertEqual(
                self.post({"email": email, "website": ""}),
                (303, "/testflight/thanks/"),
            )
        self.assertEqual(self.emails(), [("fan+beta@example.com",)])

    def test_invalid_and_honeypot_submissions_do_not_store_addresses(self):
        for email in ["not-an-email", "=1+1@example.com"]:
            self.assertEqual(
                self.post({"email": email, "website": ""}),
                (303, "/testflight/error/"),
            )
        self.assertEqual(
            self.post({"email": "fan@example.com", "website": "spam.example"}),
            (303, "/testflight/thanks/"),
        )
        self.assertEqual(self.emails(), [])

    def test_cross_origin_submission_is_rejected(self):
        self.assertEqual(
            self.post({"email": "fan@example.com"}, {"Origin": "https://other.example"})[0],
            403,
        )
        self.assertEqual(self.emails(), [])

    def test_export_includes_only_uninvited_email_addresses(self):
        self.post({"email": "first@example.com"})
        self.post({"email": "second@example.com"})
        with closing(sqlite3.connect(self.database)) as connection, connection:
            connection.execute(
                "UPDATE signups SET invited_at = 1 WHERE email = ?",
                ("first@example.com",),
            )
        result = subprocess.run(
            [sys.executable, str(Path(__file__).with_name("export_csv.py"))],
            env={**os.environ, "SIGNUP_DB_PATH": str(self.database)},
            capture_output=True,
            text=True,
            check=True,
        )
        self.assertEqual(result.stdout, "email\nsecond@example.com\n")


if __name__ == "__main__":
    unittest.main()
