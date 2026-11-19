#!/usr/bin/env python3
"""Build the locked offline native bundles from upstream artifacts."""

import argparse
import hashlib
import json
import shutil
import tempfile
import urllib.error
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LICENSES = (
    "Skija-0.143.18-LICENSE.txt",
    "Skija-0.143.18-NOTICE.txt",
    "Skia-da51f0d60e-BSD-3-Clause.txt",
    "Yoga-upstream-MIT.txt",
    "LWJGL-3.3.1-BSD-3-Clause.txt",
    "LWJGL-3.3.3-BSD-3-Clause.txt",
)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def identity(contents):
    return {"size": len(contents), "sha256": hashlib.sha256(contents).hexdigest()}


def checked(contents, expected, label):
    if any(expected[key] != value for key, value in identity(contents).items()):
        raise ValueError(f"{label}: length or SHA-256 mismatch")
    return contents


def cached_archive(artifact, directory, offline):
    archive = directory / artifact["name"]
    if archive.is_file():
        try:
            checked(archive.read_bytes(), artifact, archive.name)
            return archive
        except ValueError:
            if offline:
                raise
    if offline:
        raise FileNotFoundError(archive)
    failures = []
    for url in reversed(artifact["urls"]):
        try:
            with urllib.request.build_opener(NoRedirect).open(url, timeout=20) as response:
                contents = checked(response.read(artifact["size"] + 1), artifact, url)
            temporary = archive.with_suffix(".download")
            temporary.write_bytes(contents)
            temporary.replace(archive)
            return archive
        except (OSError, ValueError) as failure:
            failures.append(f"{url}: {failure}")
    raise OSError("\n".join(failures))


def write_bundle(platform, specification, artifacts, output, offline):
    sources = [platform]
    payload = {}
    for artifact in (specification["skija"], *specification["yoga"].values()):
        archive = cached_archive(artifact, artifacts, offline)
        entry = artifact["file"]
        with zipfile.ZipFile(archive) as jar:
            payload[entry["packPath"]] = checked(jar.read(entry["path"]), entry, entry["path"])
        sources.append(f'{artifact["urls"][-1]}  SHA-256 {artifact["sha256"]}')
    payload["SOURCES.txt"] = ("\n".join(sources) + "\n").encode("utf-8")
    for name in LICENSES:
        payload["licenses/" + name] = (ROOT / "licenses" / name).read_bytes().replace(b"\r\n", b"\n")
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as bundle:
        for name, contents in sorted(payload.items()):
            member = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            member.create_system = 3
            member.external_attr = 0o100644 << 16
            bundle.writestr(member, contents)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifacts", type=Path, default=ROOT / "build/native-upstream")
    parser.add_argument("--output", type=Path, default=ROOT / "build/native-bundles")
    parser.add_argument("--offline", action="store_true", help="only use downloaded upstream archives")
    args = parser.parse_args()
    manifest = json.loads((ROOT / "versions/native-artifacts.json").read_text(encoding="utf-8"))
    args.artifacts.mkdir(parents=True, exist_ok=True)
    args.output.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".bundles-", dir=args.output) as stage:
        for platform, specification in manifest["platforms"].items():
            bundle = specification["bundle"]
            path = Path(stage) / bundle["name"]
            write_bundle(platform, specification, args.artifacts, path, args.offline)
            checked(path.read_bytes(), bundle, path.name)
            shutil.move(path, args.output / path.name)
            print(f'{path.name}  {bundle["sha256"]}')


if __name__ == "__main__":
    main()
