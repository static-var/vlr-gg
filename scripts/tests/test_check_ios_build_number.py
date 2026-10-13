import importlib.util
from pathlib import Path
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "check-ios-build-number.py"
SPEC = importlib.util.spec_from_file_location("check_ios_build_number", SCRIPT)
build_number = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(build_number)


class IosBuildNumberTest(unittest.TestCase):
    def check(self, environ, code="90"):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "version.xcconfig"
            path.write_text(f"MARKETING_VERSION = 1.1.1\nCURRENT_PROJECT_VERSION = {code}\n")
            return build_number.check(environ, path)

    def test_release_with_matching_build_passes(self):
        self.assertIsNone(self.check({"CI_WORKFLOW": "iOS TestFlight Release", "CI_BUILD_NUMBER": "90"}))

    def test_release_with_different_build_fails(self):
        error = self.check({"CI_WORKFLOW": "iOS TestFlight Release", "CI_BUILD_NUMBER": "91"})
        self.assertIn("Xcode Cloud build 91", error)
        self.assertIn("CURRENT_PROJECT_VERSION 90", error)

    def test_release_without_build_number_fails(self):
        self.assertIn("(unset)", self.check({"CI_WORKFLOW": "iOS TestFlight Release"}))

    def test_other_workflows_are_not_checked(self):
        for environ in ({}, {"CI_WORKFLOW": "iOS CI Validation", "CI_BUILD_NUMBER": "91"}):
            with self.subTest(environ=environ):
                self.assertIsNone(self.check(environ))


if __name__ == "__main__":
    unittest.main()
