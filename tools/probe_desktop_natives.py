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
    modes = ('offline',) if platform.startswith('windows-') else ('auto', 'offline')
    for source in modes:
        native_tmp = directory / source
        native_tmp.mkdir()
        subprocess.run([jdk / f'java{suffix}', f'-Djava.io.tmpdir={native_tmp}',
                        f'-Djava.library.path={native_tmp}', f'-Datomlib.natives.source={source}',
                        '-cp', os.pathsep.join((str(directory), classpath_arg)),
                        'PackagedNativeProbe', str(game), platform, java_version],
                       cwd=game, check=True, timeout=100)


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
