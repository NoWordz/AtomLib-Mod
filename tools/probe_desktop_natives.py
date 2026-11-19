"""Run the desktop native probe against each packaged mod, using the target JVM."""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
from urllib.request import urlopen
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
TARGETS = json.loads((ROOT / 'versions/targets.json').read_text(encoding='utf-8'))['targets']
LOADER_LABELS = {'forge': 'Forge', 'fabric': 'Fabric', 'neoforge': 'NeoForge'}
MAVEN_CENTRAL = 'https://repo.maven.apache.org/maven2'
PLATFORMS = {
    'macos-x64': 'macos', 'macos-arm64': 'macos-arm64',
    'linux-x64': 'linux', 'linux-arm64': 'linux-arm64',
    'windows-x64': 'windows', 'windows-arm64': 'windows-arm64',
}


def embedded_classpath(jar, directory):
    # Forge and NeoForge use Jar-in-Jar. Fabric exposes the same dependencies
    # from META-INF/jars. Use the bytes that actually shipped in the candidate.
    classpath = [jar]
    with ZipFile(jar) as archive:
        for entry in archive.namelist():
            if entry.startswith(('META-INF/jarjar/', 'META-INF/jars/')) and entry.endswith('.jar'):
                embedded = directory / Path(entry).name
                with embedded.open('xb') as output:
                    output.write(archive.read(entry))
                classpath.append(embedded)
    return classpath


def lwjgl_classpath(version, classifier, directory):
    # Minecraft supplies LWJGL core, which is intentionally absent from the mod.
    for name in (f'lwjgl-{version}.jar', f'lwjgl-{version}-natives-{classifier}.jar'):
        jar = directory / name
        url = f'{MAVEN_CENTRAL}/org/lwjgl/lwjgl/{version}/{name}'
        with urlopen(url, timeout=30) as response:
            jar.write_bytes(response.read())
        yield jar


def run_probe(java, classpath, game, platform, java_version, directory, mode):
    directory.mkdir()
    source = 'offline' if mode == 'cold-offline' else mode
    result = subprocess.run(
        [java, f'-Djava.io.tmpdir={directory}', f'-Djava.library.path={directory}',
         f'-Datomlib.natives.source={source}', '-Xlog:library=info', '-cp', classpath,
         'PackagedNativeProbe', str(game), platform, java_version, mode],
        cwd=game, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        encoding='utf-8', errors='replace', timeout=100,
    )
    windows = platform.startswith('windows-')
    extension = 'dll' if windows else 'dylib' if platform.startswith('macos-') else 'so'
    prefix = '' if windows else 'lib'
    libraries = {f'{prefix}{library}.{extension}' for library in ('skija', 'lwjgl_yoga')}
    load_root = (directory if windows else game / 'atomlib/natives' / platform).resolve()
    loaded = set()
    for line in result.stdout.splitlines():
        if '[library]' not in line:
            print(line, flush=True)
        if '[library] Loaded library ' in line:
            path = Path(line.split('Loaded library ', 1)[1].rsplit(', handle ', 1)[0])
            if path.name in libraries:
                print(line, flush=True)
                if path.resolve().is_relative_to(load_root):
                    loaded.add(path.name)
    result.check_returncode()
    expected = set() if mode == 'cold-offline' else libraries
    if loaded != expected:
        raise ValueError(f'{mode}: expected {sorted(expected)} loaded from {load_root}, got {sorted(loaded)}')


def probe(target, jars, platform, directory):
    label = LOADER_LABELS[target['loader']]
    java_version = str(target['java'])
    (jar,) = jars.rglob(f"atomlib-{label}-{target['minecraft']}-v*.jar")
    jdk = Path(os.environ[f'JAVA{java_version}_HOME']) / 'bin'
    suffix = '.exe' if os.name == 'nt' else ''
    game = directory / 'instance'
    game.mkdir()
    classpath = embedded_classpath(jar, directory)
    classpath.extend(lwjgl_classpath(target['yoga'], PLATFORMS[platform], directory))
    classpath_arg = os.pathsep.join(map(str, classpath))
    print(f'=== {jar.name} on {platform}, Java {java_version} ===', flush=True)
    subprocess.run([jdk / f'javac{suffix}', '-cp', classpath_arg, '-d', directory,
                    ROOT / 'tools/native-probe/PackagedNativeProbe.java'], check=True, timeout=90)

    # A fresh JVM and native extraction directory for each run. Only the mod's
    # verified instance cache survives the transition from auto to offline.
    modes = ('offline',) if platform.startswith('windows-') else ('cold-offline', 'auto', 'offline')
    for mode in modes:
        run_probe(jdk / f'java{suffix}', os.pathsep.join((str(directory), classpath_arg)),
                  game, platform, java_version, directory / mode, mode)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jars', type=Path)
    parser.add_argument('platform', choices=PLATFORMS)
    args = parser.parse_args()
    failed = []
    for name, target in TARGETS.items():
        try:
            with tempfile.TemporaryDirectory(prefix=f'atomlib-{target["loader"]}-') as scratch:
                probe(target, args.jars.resolve(), args.platform, Path(scratch))
        except (OSError, ValueError, KeyError, subprocess.SubprocessError) as error:
            print(f'FAIL {name}: {error}', file=sys.stderr, flush=True)
            failed.append(name)
    if failed:
        print(f'Failed on {args.platform}: {", ".join(failed)}', file=sys.stderr)
    return bool(failed)


if __name__ == '__main__':
    sys.exit(main())
