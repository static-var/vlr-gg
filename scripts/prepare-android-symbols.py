#!/usr/bin/env python3
"""Collect matching native debug symbols from resolved release AARs for Google Play."""

import argparse
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
import struct
import sys
from zipfile import BadZipFile, ZIP_DEFLATED, ZipFile


@dataclass(frozen=True)
class ElfInfo:
    bits: int
    machine: int
    build_id: bytes
    has_symbols: bool


def elf_info(data: bytes) -> ElfInfo:
    if len(data) < 16 or data[:4] != b"\x7fELF" or data[4] not in (1, 2) or data[5] not in (1, 2):
        raise ValueError("Expected a 32-bit or 64-bit ELF file")
    endian = "<" if data[5] == 1 else ">"
    bits = 32 if data[4] == 1 else 64

    def unpack(fmt: str, offset: int):
        return struct.unpack_from(endian + fmt, data, offset)

    try:
        machine = unpack("H", 18)[0]
        if bits == 64:
            offset = unpack("Q", 40)[0]
            entry_size, count = unpack("HH", 58)
            section_format = "IIQQQQIIQQ"
        else:
            offset = unpack("I", 32)[0]
            entry_size, count = unpack("HH", 46)
            section_format = "IIIIIIIIII"
        if not count or entry_size < struct.calcsize(endian + section_format):
            raise ValueError("ELF has no usable section headers")
        build_id = b""
        has_symbols = False
        for index in range(count):
            section = unpack(section_format, offset + index * entry_size)
            kind, start, size, symbol_size = section[1], section[4], section[5], section[9]
            if kind == 2:  # SHT_SYMTAB
                if start + size > len(data) or not symbol_size or size % symbol_size:
                    raise ValueError("ELF has an invalid symbol table")
                has_symbols = size > symbol_size
            if kind != 7:  # SHT_NOTE
                continue
            if start + size > len(data):
                raise ValueError("ELF note section exceeds the file size")
            cursor = start
            while cursor < start + size:
                name_size, value_size, note_kind = unpack("III", cursor)
                name_start = cursor + 12
                value_start = name_start + (name_size + 3) // 4 * 4
                next_note = value_start + (value_size + 3) // 4 * 4
                if next_note > start + size:
                    raise ValueError("ELF note exceeds its section size")
                if note_kind == 3 and data[name_start:name_start + name_size] == b"GNU\0":
                    build_id = data[value_start:value_start + value_size]
                cursor = next_note
    except struct.error as error:
        raise ValueError("Truncated ELF file") from error
    if not build_id:
        raise ValueError("ELF has no GNU build ID")
    return ElfInfo(bits, machine, build_id, has_symbols)


def prepare_symbols(bundle: Path, aars: list[Path], output: Path, allow_missing: set[str]) -> tuple[int, list[str]]:
    shipped = {}
    with ZipFile(bundle) as archive:
        for name in archive.namelist():
            parts = PurePosixPath(name).parts
            if len(parts) == 4 and parts[:2] == ("base", "lib") and name.endswith(".so"):
                shipped[(parts[2], parts[3])] = elf_info(archive.read(name))
    if not shipped:
        raise ValueError("The bundle contains no native libraries")

    matches = {}
    mismatches = set()
    for aar in aars:
        with ZipFile(aar) as archive:
            for name in archive.namelist():
                library = PurePosixPath(name).name
                targets = {key: value for key, value in shipped.items() if key[1] == library}
                if not targets:
                    continue
                data = archive.read(name)
                candidate = elf_info(data)
                if not candidate.has_symbols:
                    continue
                for key, target in targets.items():
                    if (candidate.bits, candidate.machine) != (target.bits, target.machine):
                        continue
                    if candidate.build_id != target.build_id:
                        mismatches.add(key)
                        continue
                    matches[key] = data

    missing = []
    errors = []
    for key, target in sorted(shipped.items()):
        if key in matches:
            continue
        label = f"{key[0]}/{key[1]} (build ID {target.build_id.hex()})"
        missing.append(label)
        if key in mismatches:
            errors.append(f"Debug symbols have the wrong build ID: {label}")
        elif key[1] not in allow_missing:
            errors.append(f"No debug symbols found: {label}")
    if not matches:
        errors.append("No matching native debug symbols found; refusing to create an empty archive")
    if errors:
        raise ValueError("\n".join(errors))

    output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(output, "w", compression=ZIP_DEFLATED) as archive:
        for (abi, library), data in sorted(matches.items()):
            archive.writestr(f"{abi}/{library}", data)
    return len(matches), missing


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bundle", required=True, type=Path)
    parser.add_argument("--aar", action="append", type=Path, default=[])
    parser.add_argument("--aar-list", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--allow-missing", action="append", default=[])
    args = parser.parse_args()
    try:
        aars = args.aar
        if args.aar_list:
            aars += [Path(line) for line in args.aar_list.read_text().splitlines() if line]
        if not aars:
            raise ValueError("Pass the resolved release AARs with --aar or --aar-list")
        count, missing = prepare_symbols(args.bundle, aars, args.output, set(args.allow_missing))
    except (OSError, ValueError, BadZipFile) as error:
        print(error, file=sys.stderr)
        return 1
    print(f"Prepared {count} native debug symbol files in {args.output}")
    for library in missing:
        print(f"WARNING: Upstream debug symbols unavailable: {library}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
