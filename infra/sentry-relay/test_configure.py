import re
import unittest

from configure import render, validate


class RelayConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.values = {
            "SENTRY_UPSTREAM_HOST": "o123.ingest.us.sentry.io",
            "SENTRY_PROJECT_KEYS": "123:" + "a" * 32 + ",456:" + "b" * 32,
            "PUBLIC_HOST": "events.example.com",
            "INGRESS_NETWORK": "appsnet",
        }

    def test_rejects_destination_and_config_injection(self):
        for field, value in [
            ("SENTRY_UPSTREAM_HOST", "127.0.0.1"),
            ("SENTRY_UPSTREAM_HOST", "o123.ingest.sentry.io.evil.example"),
            ("SENTRY_UPSTREAM_HOST", "o123.ingest.sentry.io/path"),
            ("PUBLIC_HOST", "events.example.com; return 200;"),
            ("SENTRY_PROJECT_KEYS", "123:" + "a" * 32 + "\nserver {}"),
            ("SENTRY_PROJECT_KEYS", "123:" + "a" * 32 + ",123:" + "b" * 32),
            ("INGRESS_NETWORK", "$(whoami)"),
        ]:
            with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                validate({**self.values, field: value})

    def test_auth_matches_native_sdk_header_orders_and_exact_key(self):
        nginx, _ = render(*validate(self.values))
        pattern = re.search(r'"~(.*?)" 1;', nginx)[1]
        key = "a" * 32
        for header in [
            f"Sentry sentry_key={key}, sentry_version=7, sentry_client=sentry.java/8.0",
            f"Sentry sentry_version=7,sentry_client=sentry.cocoa/8.58.2,sentry_key={key}",
            f"Sentry sentry_version=7, sentry_key={key}, sentry_client=sentry.java/8.0",
        ]:
            self.assertIsNotNone(re.search(pattern, header), header)
        for header in [
            f"Sentry sentry_key={key}0",
            f"Sentry sentry_key=0{key}",
            f"Sentry not_sentry_key={key}",
            f"Bearer sentry_key={key}",
            "Sentry sentry_key=" + "b" * 32,
        ]:
            self.assertIsNone(re.search(pattern, header), header)


if __name__ == "__main__":
    unittest.main()
