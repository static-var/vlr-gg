import importlib.util
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile


spec = importlib.util.spec_from_file_location(
    "check_compose_resources", Path(__file__).resolve().parents[1] / "check-compose-resources.py"
)
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


class ComposeResourcePackagingTest(unittest.TestCase):
    def test_apk_and_aab_require_every_referenced_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "feature/build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/custom/String0.kt"
            source.parent.mkdir(parents=True)
            source.write_text('''private const val MD: String = "composeResources/custom.package/"
ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 0, 10)
ResourceItem(setOf(), "${MD}drawable/icon.svg", -1, -1)
''')
            for suffix, prefix in ((".apk", "assets/"), (".aab", "base/assets/")):
                with self.subTest(suffix=suffix):
                    artifact = root / f"app{suffix}"
                    with ZipFile(artifact, "w") as archive:
                        archive.writestr(prefix + "composeResources/custom.package/values/strings.commonMain.cvr", b"strings")
                    with self.assertRaisesRegex(ValueError, "drawable/icon.svg"):
                        checker.check_artifact(root, artifact)
                    with ZipFile(artifact, "a") as archive:
                        archive.writestr(prefix + "composeResources/custom.package/drawable/icon.svg", b"image")
                    self.assertEqual(checker.check_artifact(root, artifact), 2)

    def test_missing_generated_accessors_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "No generated Compose"):
                checker.expected_resources(Path(directory))

    def test_unrecognized_generated_format_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "feature/build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/String0.kt"
            source.parent.mkdir(parents=True)
            source.write_text('ResourceItem(setOf(), generatedPath, 0, 10)')
            with self.assertRaisesRegex(ValueError, "Cannot read resource paths"):
                checker.expected_resources(root)


if __name__ == "__main__":
    unittest.main()
