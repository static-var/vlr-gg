from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


REPO = Path(__file__).resolve().parents[2]
CI_SCRIPTS = REPO / "iosApp" / "ci_scripts"


class IosSymbolUploadHookTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.ci_scripts = self.root / "ci_scripts"
        self.ci_scripts.mkdir()
        for name in ("ci_post_xcodebuild.sh", "sentry-upload-symbols.sh"):
            shutil.copyfile(CI_SCRIPTS / name, self.ci_scripts / name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.archive = self.root / "Release build.xcarchive"
        self.symbols = []
        for name in ("VLR", "VLRWidget", "shared"):
            dwarf = self.archive / "dSYMs" / f"{name}.dSYM/Contents/Resources/DWARF/{name}"
            dwarf.parent.mkdir(parents=True)
            dwarf.write_text("fake DWARF")
            self.symbols.append(str(dwarf.resolve()))
        self.cli_template = self.root / "fake-sentry-cli"
        self.cli_template.write_text(
            '#!/bin/sh\nprintf "%s\\n" "$@" > "$CLI_LOG"\nexit "${CLI_EXIT:-0}"\n'
        )
        self.cli_template.chmod(0o755)
        self.cli = self.bin / "sentry-cli"
        shutil.copyfile(self.cli_template, self.cli)
        self.cli.chmod(0o755)
        self.brew = self.bin / "brew"
        self.brew.write_text(
            '#!/bin/sh\n'
            'printf "%s\\n" "$@" > "$BREW_LOG"\n'
            'if [ "${BREW_EXIT:-0}" != 0 ]; then exit "$BREW_EXIT"; fi\n'
            'cp "$CLI_TEMPLATE" "$CLI_DESTINATION"\n'
            'chmod +x "$CLI_DESTINATION"\n'
        )
        self.brew.chmod(0o755)
        self.env = {
            "PATH": f"{self.bin}:/usr/bin:/bin",
            "HOME": str(self.root),
            "CI_WORKFLOW": "iOS TestFlight Release",
            "CI_XCODEBUILD_ACTION": "archive",
            "CI_XCODEBUILD_EXIT_CODE": "0",
            "CI_ARCHIVE_PATH": str(self.archive),
            "SENTRY_AUTH_TOKEN": "test-token-must-not-be-logged",
            "SENTRY_ORG": "test-org",
            "SENTRY_PROJECT": "test-project",
            "CLI_LOG": str(self.root / "cli.log"),
            "BREW_LOG": str(self.root / "brew.log"),
            "CLI_TEMPLATE": str(self.cli_template),
            "CLI_DESTINATION": str(self.cli),
        }

    def run_hook(self):
        result = subprocess.run(
            ["/bin/sh", str(self.ci_scripts / "ci_post_xcodebuild.sh")],
            env=self.env,
            cwd=self.root,
            text=True,
            capture_output=True,
        )
        self.assertNotIn("test-token-must-not-be-logged", result.stdout + result.stderr)
        return result

    def assert_no_external_commands(self):
        self.assertFalse(Path(self.env["CLI_LOG"]).exists())
        self.assertFalse(Path(self.env["BREW_LOG"]).exists())

    def test_success_uploads_all_archive_symbols_without_checkout(self):
        self.assertTrue((CI_SCRIPTS / "sentry-upload-symbols.sh").is_symlink())
        self.assertEqual(
            (CI_SCRIPTS / "sentry-upload-symbols.sh").resolve(),
            REPO / "scripts/sentry-upload-symbols.sh",
        )
        result = self.run_hook()
        self.assertEqual(result.returncode, 0, result.stderr)
        args = Path(self.env["CLI_LOG"]).read_text().splitlines()
        self.assertEqual(args[:5], ["--log-level", "warn", "debug-files", "upload", "--wait"])
        self.assertCountEqual(args[5:], self.symbols)
        self.assertFalse(Path(self.env["BREW_LOG"]).exists())

    def test_other_actions_skip_without_credentials(self):
        for key in ("SENTRY_AUTH_TOKEN", "SENTRY_ORG", "SENTRY_PROJECT"):
            self.env.pop(key)
        for action in ("build", "build-for-testing", "test-without-building", "analyze", ""):
            with self.subTest(action=action):
                self.env["CI_XCODEBUILD_ACTION"] = action
                self.assertEqual(self.run_hook().returncode, 0)
                self.assert_no_external_commands()

    def test_failed_archive_skips_without_credentials(self):
        self.env["CI_XCODEBUILD_EXIT_CODE"] = "65"
        self.env.pop("SENTRY_AUTH_TOKEN")
        self.assertEqual(self.run_hook().returncode, 0)
        self.assert_no_external_commands()

    def test_other_archive_workflows_skip_without_credentials(self):
        for key in ("SENTRY_AUTH_TOKEN", "SENTRY_ORG", "SENTRY_PROJECT"):
            self.env.pop(key)
        for workflow in ("iOS CI Validation", ""):
            with self.subTest(workflow=workflow):
                self.env["CI_WORKFLOW"] = workflow
                self.assertEqual(self.run_hook().returncode, 0)
                self.assert_no_external_commands()

    def test_required_ci_configuration_fails_before_install_or_upload(self):
        for key in ("CI_XCODEBUILD_EXIT_CODE", "CI_ARCHIVE_PATH", "SENTRY_AUTH_TOKEN", "SENTRY_ORG", "SENTRY_PROJECT"):
            with self.subTest(key=key):
                original = self.env.pop(key)
                result = self.run_hook()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(key, result.stderr)
                self.assert_no_external_commands()
                self.env[key] = original

    def test_missing_or_empty_symbols_fail_without_upload(self):
        shutil.rmtree(self.archive)
        for create_empty in (False, True):
            with self.subTest(empty=create_empty):
                if create_empty:
                    (self.archive / "dSYMs").mkdir(parents=True)
                self.assertNotEqual(self.run_hook().returncode, 0)
                self.assert_no_external_commands()

    def test_upload_failure_fails_hook(self):
        self.env["CLI_EXIT"] = "17"
        self.assertEqual(self.run_hook().returncode, 17)

    def test_missing_cli_installs_official_formula_then_uploads(self):
        self.cli.unlink()
        result = self.run_hook()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(
            Path(self.env["BREW_LOG"]).read_text().splitlines(),
            ["install", "getsentry/tools/sentry-cli"],
        )
        self.assertTrue(Path(self.env["CLI_LOG"]).exists())

    def test_install_failure_fails_hook_without_upload(self):
        self.cli.unlink()
        self.env["BREW_EXIT"] = "23"
        self.assertEqual(self.run_hook().returncode, 23)
        self.assertFalse(Path(self.env["CLI_LOG"]).exists())

    def test_invalid_explicit_cli_fails_without_install(self):
        self.env["SENTRY_CLI"] = str(self.root / "missing-cli")
        self.assertNotEqual(self.run_hook().returncode, 0)
        self.assert_no_external_commands()


if __name__ == "__main__":
    unittest.main()
