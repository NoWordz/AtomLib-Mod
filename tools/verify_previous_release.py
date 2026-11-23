"""Run a binary compiled against released modules and compare its collection pixels and public API."""
from pathlib import Path
import argparse
import json
import os
import re
import subprocess
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
MODULES = ('core', 'runtime', 'ui')


def run(command, output, name):
    result = subprocess.run(list(map(str, command)), cwd=ROOT, capture_output=True, text=True,
                            encoding='utf-8', errors='replace', timeout=240)
    (output/f'{name}.log').write_text(result.stdout+result.stderr, encoding='utf-8')
    if result.returncode:
        raise RuntimeError(f'{name} failed ({result.returncode}): {result.stderr[-2000:]}')
    return result.stdout


def api_signatures(text):
    result = set()
    current = pending = None
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.endswith('{'):
            match = re.search(r'\b(?:class|interface) ([\w.$]+)', stripped)
            current = match.group(1) if match and stripped.startswith('public ') else None
            pending = None
        elif current and stripped.startswith(('public ', 'protected ')):
            pending = stripped.split(' throws ', 1)[0].rstrip(';')
        elif current and pending and stripped.startswith('descriptor:'):
            result.add((current, pending, stripped))
            pending = None
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline-archive', type=Path, required=True)
    parser.add_argument('--baseline-version', default='0.7.1')
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--classpath', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    released = output/'released-modules'
    released.mkdir(exist_ok=True)
    old_jars = []
    classes = set()
    with ZipFile(args.baseline_archive) as archive:
        for module in MODULES:
            name = f'atomlib-{module}-{args.baseline_version}.jar'
            entry = f'com/atom/lib/atomlib-{module}/{args.baseline_version}/{name}'
            jar = released/name
            jar.write_bytes(archive.read(entry))
            old_jars.append(jar)
            with ZipFile(jar) as library:
                classes.update(n[:-6].replace('/', '.') for n in library.namelist()
                               if n.startswith('com/atom/lib/') and n.endswith('.class'))
    current = list(dict.fromkeys(args.classpath.read_text(encoding='utf-8').split(os.pathsep)))
    legacy = []
    for path in current:
        normalized = path.replace('\\', '/')
        main_output = any(f'/standalone/_{module}/' in normalized for module in MODULES)
        test_output = '/classes/java/test' in normalized or '/resources/test' in normalized
        if not main_output or test_output:
            legacy.append(path)
    legacy.extend(map(str, old_jars))
    old_cp = os.pathsep.join(legacy)
    new_cp = os.pathsep.join(current)
    suffix = '.exe' if os.name == 'nt' else ''
    javap = args.java_home/'bin'/f'javap{suffix}'
    old_api = api_signatures(run([javap, '-protected', '-s', '-classpath', old_cp, *sorted(classes)], output, 'old-api'))
    new_api = api_signatures(run([javap, '-protected', '-s', '-classpath', new_cp, *sorted(classes)], output, 'new-api'))
    if not old_api:
        raise AssertionError('No released public symbols were inspected')
    missing = sorted(old_api-new_api)
    if missing:
        raise AssertionError(f'Released public descriptors changed: {missing[:10]}')

    binary = output/'released-binary'
    binary.mkdir(exist_ok=True)
    sources = ROOT/'ui/src/test/java/com/atom/lib/controls'
    run([args.java_home/'bin'/f'javac{suffix}', '--release', '17', '-encoding', 'UTF-8',
         '-cp', old_cp, '-d', binary, sources/'ControlFixture.java', sources/'CollectionCompatibilityProbe.java'], output, 'compile-released-binary')
    for name, cp in [('old', old_cp), ('new', new_cp)]:
        run([args.java_home/'bin'/f'java{suffix}', '-cp', os.pathsep.join((str(binary), cp)),
             'com.atom.lib.controls.CollectionCompatibilityProbe', output/name], output, f'{name}-collection')
    references = {p.name: p for p in (output/'old').glob('*.argb')}
    candidates = {p.name: p for p in (output/'new').glob('*.argb')}
    if len(references) != 36 or references.keys() != candidates.keys():
        raise AssertionError(f'Incomplete collection samples: {len(references)} old, {len(candidates)} new')
    changed = [name for name, path in references.items() if path.read_bytes() != candidates[name].read_bytes()]
    result = {'baseline': args.baseline_version, 'releasedPublicSymbols': len(old_api),
              'missingSymbols': missing, 'collectionScenes': len(references), 'changedScenes': changed}
    (output/'result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    if changed:
        raise AssertionError(f'Default collection pixels changed: {changed}')
    print(json.dumps(result))


if __name__ == '__main__':
    main()
