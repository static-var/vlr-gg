"""Exercise generated Swift configuration with synthetic settings only."""

import os
from pathlib import Path
import runpy
import stat
import tempfile
import unittest
from unittest.mock import patch


GENERATOR = runpy.run_path(str(Path(__file__).resolve().parents[1] / "generate-ios-build-config.py"))


class GenerateIosBuildConfigTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.repo = Path(self.directory.name)
        self.environment = patch.dict(os.environ, {}, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def test_literal_escaping_prevents_swift_interpolation_and_source_injection(self):
        literal = GENERATOR["swift_string"]
        self.assertEqual(literal(r"\(fatalError())"), r'"\\(fatalError())"')
        self.assertEqual(literal('a"b\nc\rd\te'), r'"a\"b\nc\rd\te"')
        self.assertEqual(literal("valorant 🐈"), '"valorant 🐈"')

    def test_normalization_only_removes_matched_outer_quotes(self):
        normalize = GENERATOR["normalize"]
        self.assertEqual(normalize('  "quoted"  '), "quoted")
        self.assertEqual(normalize("'quoted'"), "quoted")
        self.assertEqual(normalize('"literal-prefix'), '"literal-prefix')
        self.assertEqual(normalize("literal-suffix'"), "literal-suffix'")
        self.assertEqual(normalize("replace-with-your-token"), "")

    def test_settings_prefer_environment_then_local_properties_then_dotenv(self):
        self.repo.joinpath("local.properties").write_text("SENTRY_ENVIRONMENT=local\n")
        self.repo.joinpath(".env").write_text("SENTRY_ENVIRONMENT=dotenv\n")
        setting = GENERATOR["setting"]

        os.environ["SENTRY_ENVIRONMENT"] = "environment"
        self.assertEqual(setting(self.repo, "SENTRY_ENVIRONMENT", "fallback"), "environment")
        os.environ["SENTRY_ENVIRONMENT"] = "  "
        self.assertEqual(setting(self.repo, "SENTRY_ENVIRONMENT", "fallback"), "local")
        self.repo.joinpath("local.properties").write_text("SENTRY_ENVIRONMENT=\n")
        self.assertEqual(setting(self.repo, "SENTRY_ENVIRONMENT", "fallback"), "dotenv")
        self.repo.joinpath(".env").write_text("SENTRY_ENVIRONMENT=\n")
        self.assertEqual(setting(self.repo, "SENTRY_ENVIRONMENT", "fallback"), "fallback")

    def test_platform_dsn_and_legacy_token_precedence_without_upload_credentials(self):
        self.repo.joinpath("local.properties").write_text(
            "TOKEN=legacy-token\nVLR_AUTH_TOKEN=named-token\n"
            "SENTRY_DSN_IOS=https://ios-key@example.invalid/1\n"
        )
        os.environ.update({
            "SENTRY_DSN": "https://generic-key@example.invalid/2",
            "SENTRY_AUTH_TOKEN": "upload-secret-must-never-be-embedded",
            "SENTRY_ENABLED": "FALSE",
        })
        destination = self.repo / "iosApp" / "GeneratedBuildConfig.swift"
        GENERATOR["generate"](self.repo, destination)
        content = destination.read_text()
        self.assertIn('authToken: String? = "legacy-token"', content)
        self.assertIn('sentryDsn: String = "https://ios-key@example.invalid/1"', content)
        self.assertIn("sentryEnabled: Bool = false", content)
        self.assertNotIn("SENTRY_AUTH_TOKEN", content)
        self.assertNotIn("upload-secret", content)
        self.assertEqual(stat.S_IMODE(destination.stat().st_mode), 0o600)

        os.environ["VLR_AUTH_TOKEN"] = "environment-token"
        GENERATOR["generate"](self.repo, destination)
        self.assertIn('authToken: String? = "environment-token"', destination.read_text())

    def test_missing_optional_settings_generate_valid_defaults(self):
        destination = self.repo / "GeneratedBuildConfig.swift"
        GENERATOR["generate"](self.repo, destination)
        self.assertEqual(
            destination.read_text(),
            "enum GeneratedBuildConfig {\n"
            "    static let authToken: String? = nil\n"
            '    static let sentryDsn: String = ""\n'
            '    static let sentryEnvironment: String = ""\n'
            "    static let sentryEnabled: Bool = true\n"
            "}\n",
        )


if __name__ == "__main__":
    unittest.main()
