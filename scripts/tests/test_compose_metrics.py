"""Regression coverage for compiler reports with overloaded composables."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("compose_metrics", Path(__file__).parents[1] / "compose-metrics.py")
metrics = importlib.util.module_from_spec(spec)
spec.loader.exec_module(metrics)


class ComposableReportsTest(unittest.TestCase):
    def test_overloads_keep_both_eligibility_records(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "composables.csv"
            report.write_text(
                "package,name,composable,skippable,restartable,readonly,inline,groups,calls\n"
                "sample.Title,Title,1,1,1,0,0,1,2\n"
                "sample.Title,Title,1,0,0,1,0,0,1\n"
                "sample.Other,Other,1,1,1,0,0,1,1\n"
            )
            result = metrics.parse_composables_csv(report)
        self.assertEqual(3, result["namedComposableCount"])
        self.assertEqual(2, result["eligibleForSkippingCount"])
        self.assertEqual(1, result["notEligibleForSkippingCount"])
        self.assertTrue(result["byQualifiedName"]["sample.Title#overload-1"]["restartable"])
        self.assertTrue(result["byQualifiedName"]["sample.Title#overload-2"]["readonly"])
        self.assertIn("sample.Other", result["byQualifiedName"])

    def test_overload_identity_changes_are_not_reported_as_additions(self):
        value = {"eligibleForSkipping": True, "restartable": True}
        before = {"byQualifiedName": {"sample.Title": value}}
        after = {"byQualifiedName": {"sample.Title#overload-1": value, "sample.Title#overload-2": value}}
        result = metrics.compare_named_composables(before, after)
        self.assertEqual([], result["added"])
        self.assertEqual([], result["removed"])
        self.assertEqual([], result["eligibilityTransitions"])
        self.assertEqual(["sample.Title"], result["overloadGroupsExcludedFromIdentityComparison"])


if __name__ == "__main__":
    unittest.main()
