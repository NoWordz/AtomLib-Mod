#!/usr/bin/env python3
"""Build a deterministic AtomLib test-Maven archive for AC CI."""
from __future__ import annotations

import argparse, hashlib, json, zipfile
from pathlib import Path

root = Path(__file__).resolve().parent.parent
version = next(line.split("=", 1)[1].strip() for line in (root / "gradle.properties").read_text().splitlines()
               if line.startswith("atomlib_version="))
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--maven', type=Path, default=root / 'build/test-maven')
parser.add_argument('--output', type=Path, default=root / 'build' / f'atomlib-input-{version}.zip')
args = parser.parse_args()
source = args.maven.resolve()
output = args.output.resolve()
if not source.is_dir() or not any(source.rglob('*.jar')):
    parser.error(f'Maven input has no jars: {source}')
if output.is_relative_to(source):
    parser.error('Output must be outside the input Maven tree')
modules = ['core', 'runtime', 'ui', 'frontend-checker',
           *json.loads((root/'versions/targets.json').read_text())['targets']]
selected = []
for module in modules:
    artifact = f'atomlib-{module}'
    directory = source/'com/atom/lib'/artifact/version
    for extension in ('jar', 'pom', 'module'):
        required = directory/f'{artifact}-{version}.{extension}'
        if not required.is_file():
            parser.error(f'Missing current release artifact: {required}')
    selected.extend(path for path in directory.rglob('*') if path.is_file())
output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for path in sorted(selected):
        info = zipfile.ZipInfo(path.relative_to(source).as_posix(), (2020, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o644 << 16
        archive.writestr(info, path.read_bytes())
digest = hashlib.sha256(output.read_bytes()).hexdigest()
(output.with_suffix(output.suffix + ".sha256")).write_text(f"{digest}  {output.name}\n", encoding="ascii")
print(output)
print(digest)
