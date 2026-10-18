import importlib.util
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from zipfile import ZipFile


spec = importlib.util.spec_from_file_location(
    "prepare_android_symbols", Path(__file__).resolve().parents[1] / "prepare-android-symbols.py"
)
symbols = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = symbols
spec.loader.exec_module(symbols)


def elf(build_id=b"0123456789abcdef", *, stripped=False, bits=64, endian="<", machine=183):
    """Small ELF fixture with a GNU build-ID note and two symbol-table entries."""
    ident = b"\x7fELF" + bytes([2 if bits == 64 else 1, 1 if endian == "<" else 2, 1]) + bytes(9)
    note = struct.pack(endian + "III", 4, len(build_id), 3) + b"GNU\0" + build_id
    note += bytes(-len(note) % 4)
    symbol_size = 24 if bits == 64 else 16
    table = bytes(symbol_size * 2)
    if bits == 64:
        header_size, section_size = 64, 64
        header = ident + struct.pack(endian + "HHIQQQIHHHHHH", 3, machine, 1, 0, 0, header_size + len(note) + len(table), 0,
                                     header_size, 0, 0, section_size, 2 if stripped else 3, 0)
        section_format = "IIQQQQIIQQ"
    else:
        header_size, section_size = 52, 40
        header = ident + struct.pack(endian + "HHIIIIIHHHHHH", 3, machine, 1, 0, 0, header_size + len(note) + len(table), 0,
                                     header_size, 0, 0, section_size, 2 if stripped else 3, 0)
        section_format = "IIIIIIIIII"
    section_headers = bytes(section_size)
    section_headers += struct.pack(endian + section_format, 0, 7, 0, 0, header_size, len(note), 0, 0, 4, 0)
    if not stripped:
        section_headers += struct.pack(endian + section_format, 0, 2, 0, 0, header_size + len(note), len(table), 0, 0, 8,
                                       symbol_size)
    return header + note + table + section_headers


class NativeSymbolsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bundle = self.root / "app.aab"
        self.aar = self.root / "sdk.aar"
        self.output = self.root / "native-debug-symbols.zip"

    def write_archives(self, packaged, candidates):
        with ZipFile(self.bundle, "w") as archive:
            for path, data in packaged.items():
                archive.writestr("base/lib/" + path, data)
        with ZipFile(self.aar, "w") as archive:
            for path, data in candidates.items():
                archive.writestr(path, data)

    def test_collects_matching_prefab_symbols_and_reports_explicit_missing_libraries(self):
        for bits, endian, machine, abi in [(64, "<", 183, "arm64-v8a"), (32, "<", 40, "armeabi-v7a"),
                                            (64, ">", 62, "x86_64")]:
            with self.subTest(bits=bits, endian=endian):
                native = elf(bits=bits, endian=endian, machine=machine)
                stripped = elf(stripped=True, bits=bits, endian=endian, machine=machine)
                self.write_archives(
                    {f"{abi}/libsdk.so": stripped, f"{abi}/libupstream.so": stripped},
                    {f"prefab/modules/sdk/libs/android.{abi}/libsdk.so": native,
                     f"jni/{abi}/libsdk.so": stripped},
                )
                count, missing = symbols.prepare_symbols(self.bundle, [self.aar], self.output, {"libupstream.so"})
                self.assertEqual(count, 1)
                self.assertEqual(missing, [f"{abi}/libupstream.so (build ID {b'0123456789abcdef'.hex()})"])
                with ZipFile(self.output) as archive:
                    self.assertEqual(archive.namelist(), [f"{abi}/libsdk.so"])
                    self.assertEqual(archive.read(f"{abi}/libsdk.so"), native)

    def test_wrong_build_id_fails_even_when_library_is_allowed_missing(self):
        self.write_archives({"arm64-v8a/libsdk.so": elf(stripped=True)},
                            {"prefab/libsdk.so": elf(b"different-build!")})
        with self.assertRaisesRegex(ValueError, "wrong build ID"):
            symbols.prepare_symbols(self.bundle, [self.aar], self.output, {"libsdk.so"})
        self.assertFalse(self.output.exists())

    def test_stripped_input_cannot_produce_empty_symbol_archive(self):
        self.write_archives({"arm64-v8a/libsdk.so": elf(stripped=True)},
                            {"jni/arm64-v8a/libsdk.so": elf(stripped=True)})
        with self.assertRaisesRegex(ValueError, "empty archive"):
            symbols.prepare_symbols(self.bundle, [self.aar], self.output, {"libsdk.so"})
        self.assertFalse(self.output.exists())

    def test_unexpected_missing_library_fails(self):
        self.write_archives({"arm64-v8a/libsdk.so": elf(stripped=True),
                             "arm64-v8a/libnew.so": elf(stripped=True)}, {"prefab/libsdk.so": elf()})
        with self.assertRaisesRegex(ValueError, "No debug symbols found: arm64-v8a/libnew.so"):
            symbols.prepare_symbols(self.bundle, [self.aar], self.output, set())

    def test_wrong_architecture_does_not_match(self):
        self.write_archives({"arm64-v8a/libsdk.so": elf(stripped=True)},
                            {"prefab/libsdk.so": elf(machine=62)})
        with self.assertRaisesRegex(ValueError, "No debug symbols found"):
            symbols.prepare_symbols(self.bundle, [self.aar], self.output, set())

    def test_truncated_elf_is_rejected(self):
        for data in [b"not ELF", elf()[:50], elf()[:-10]]:
            with self.subTest(length=len(data)), self.assertRaises(ValueError):
                symbols.elf_info(data)


if __name__ == "__main__":
    unittest.main()
