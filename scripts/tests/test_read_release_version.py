import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


SCRIPT = Path(__file__).resolve().parents[1] / "read-release-version.py"
SPEC = importlib.util.spec_from_file_location("read_release_version", SCRIPT)
release_version = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release_version)


class ReleaseVersionTest(unittest.TestCase):
    def parse(self, text):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "version.xcconfig"
            path.write_text(text)
            return release_version.read_release_version(path)

    def test_accepts_comments_and_whitespace(self):
        self.assertEqual(
            self.parse("// Shared version\n\n MARKETING_VERSION = 1.2.3 // Release\nCURRENT_PROJECT_VERSION=42\n"),
            ("1.2.3", "42"),
        )

    def test_accepts_numeric_boundaries(self):
        for code in ("1", "2100000000"):
            with self.subTest(code=code):
                self.assertEqual(
                    self.parse(f"MARKETING_VERSION=0.0.0\nCURRENT_PROJECT_VERSION={code}"),
                    ("0.0.0", code),
                )

    def test_rejects_invalid_names(self):
        for name in ("", "1.2", "1.2.3.4", "1.2.3-beta", "01.2.3", "-1.2.3", "１.2.3", "$(VERSION)"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.parse(f"MARKETING_VERSION={name}\nCURRENT_PROJECT_VERSION=1")

    def test_rejects_invalid_codes(self):
        for code in ("", "0", "-1", "01", "1.2", "2100000001", "999999999999999999999", "one", "１"):
            with self.subTest(code=code), self.assertRaises(ValueError):
                self.parse(f"MARKETING_VERSION=1.2.3\nCURRENT_PROJECT_VERSION={code}")

    def test_rejects_missing_duplicate_and_unrecognized_assignments(self):
        valid = "MARKETING_VERSION=1.2.3\nCURRENT_PROJECT_VERSION=42\n"
        for text in ("", "MARKETING_VERSION=1.2.3", "CURRENT_PROJECT_VERSION=42", valid + "MARKETING_VERSION=1.2.4", valid + "CURRENT_PROJECT_VERSION=43", valid + "OTHER=1", valid + '#include "other.xcconfig"'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                self.parse(text)

    def test_main_emits_github_environment_values(self):
        stdout = io.StringIO()
        with patch.object(release_version, "read_release_version", return_value=("1.2.3", "42")), contextlib.redirect_stdout(stdout):
            self.assertEqual(release_version.main(), 0)
        self.assertEqual(stdout.getvalue(), "VERSION_NAME=1.2.3\nVERSION_CODE=42\n")

    def test_main_fails_without_emitting_partial_environment(self):
        for error in (ValueError("invalid"), FileNotFoundError("missing")):
            stdout, stderr = io.StringIO(), io.StringIO()
            with self.subTest(error=error), patch.object(release_version, "read_release_version", side_effect=error), contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
                self.assertEqual(release_version.main(), 1)
            self.assertEqual(stdout.getvalue(), "")
            self.assertIn("Invalid release version:", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
