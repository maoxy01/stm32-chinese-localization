from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

EXPECTED_SOURCE_SHA256 = "017305908641977b62dd786bd08f7687f19a2adeb7deb01843e6dc546a2607dd"
EXPECTED_OUTPUT_SHA256 = "87698b2fe57c7b2cff49904907a19d79ec40e66133cc1c5f2c5e73c8b4070cc3"
STRING_POOL_START = 0x2B0000
STRING_POOL_END = 0x2CC000


def main() -> None:
    parser = argparse.ArgumentParser(description="Build a version-pinned Chinese UI copy of STM32 Trusted Package Creator.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--dictionary", type=Path, default=Path(__file__).with_name("zh-CN.json"))
    args = parser.parse_args()

    data = args.source.read_bytes()
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED_SOURCE_SHA256:
        raise RuntimeError("Unsupported vendor executable: localization requires requalification")

    translations = json.loads(args.dictionary.read_text(encoding="utf-8"))
    patched = bytearray(data)
    changes: list[dict[str, object]] = []
    for match in re.finditer(rb"[\x20-\x7e]{3,}\x00", data):
        offset = match.start()
        if not STRING_POOL_START <= offset < STRING_POOL_END:
            continue
        original = match.group()[:-1].decode("ascii")
        key = original.rstrip()
        if key not in translations:
            continue
        translated = translations[key].encode("utf-8")
        if len(translated) > len(original):
            raise ValueError(f"Translation too long: {original}")
        patched[offset : offset + len(original)] = translated.ljust(len(original), b" ")
        changes.append({"offset": offset, "source": original, "translation": translations[key]})

    output_digest = hashlib.sha256(patched).hexdigest()
    if len(changes) != 304 or output_digest != EXPECTED_OUTPUT_SHA256:
        raise RuntimeError(f"Unexpected build result: changes={len(changes)}, sha256={output_digest}")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(patched)
    manifest = args.output.with_suffix(args.output.suffix + ".manifest.json")
    manifest.write_text(
        json.dumps(
            {
                "source_file": args.source.name,
                "source_sha256": digest,
                "output_file": args.output.name,
                "output_sha256": output_digest,
                "changes": changes,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print(json.dumps({"output": str(args.output), "sha256": output_digest, "changes": len(changes)}, ensure_ascii=False))


if __name__ == "__main__":
    main()

