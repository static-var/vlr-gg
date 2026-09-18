import importlib.util
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location("localizations", Path(__file__).parents[1] / "check-localizations.py")
localizations = importlib.util.module_from_spec(spec)
spec.loader.exec_module(localizations)


class LocalizationChecksTest(unittest.TestCase):
    def test_argument_reordering_is_valid_but_type_changes_are_not(self):
        expected = localizations.arguments("%1$s has %2$d matches (100%%)")
        self.assertEqual(expected, localizations.arguments("%2$d Spiele für %1$s (100%%)"))
        self.assertNotEqual(expected, localizations.arguments("%2$s Spiele für %1$s"))

    def test_missing_translation_and_changed_arguments_are_reported(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            resources = root / "androidApp/src/main/res"
            (resources / "values").mkdir(parents=True)
            (resources / "values/strings.xml").write_text(
                '<resources><string name="match">%1$s vs %2$s</string>'
                '<string name="title">Matches</string></resources>'
            )
            for language in localizations.LANGUAGES:
                target = resources / f"values-{language}"
                target.mkdir()
                (target / "strings.xml").write_text(
                    '<resources><string name="match">%1$s</string></resources>'
                )
            errors, _ = localizations.check_xml(root)
            self.assertEqual(8, sum("missing title" in error for error in errors))
            self.assertEqual(8, sum("format arguments differ" in error for error in errors))

    def test_duplicate_keys_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "strings.xml"
            path.write_text('<resources><string name="a">A</string><string name="a">B</string></resources>')
            with self.assertRaisesRegex(ValueError, "duplicate key a"):
                localizations.xml_strings(path)


if __name__ == "__main__":
    unittest.main()
