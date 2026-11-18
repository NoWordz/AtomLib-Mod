"""Read installed settings baseline and its build provenance; never write to AtomChat/instances."""
from pathlib import Path
import hashlib, json, zipfile, argparse, datetime


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--atomchat', type=Path, default=Path('D:/AtomChat-Main'))
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    evidence = a.atomchat / 'build/v090-shared-frontend'
    archive = Path('D:/AtomChat-Backups/2026-11-17-closeout/final-candidate-evidence.zip')
    def evidence_bytes(name):
        if evidence.joinpath(name).is_file():return evidence.joinpath(name).read_bytes()
        with zipfile.ZipFile(archive) as z:return z.read(name)
    receipt = json.loads(evidence_bytes('deployment-receipt.json').decode('utf-8-sig'))
    artifacts = json.loads(evidence_bytes('results.json').decode('utf-8-sig'))
    out = a.out
    out.mkdir(parents=True, exist_ok=True)
    results = []
    for entry in receipt:
        installed = Path(entry['target'])
        artifact = next(x for x in artifacts if x['sha256'] == entry['sha256'])
        archived = Path(artifact['artifact'])
        if archived.is_file(): archived_bytes=archived.read_bytes()
        else:
            with zipfile.ZipFile(archive) as z:
                names=[n for n in z.namelist() if n.endswith('/'+archived.name) or n==archived.name]
                assert len(names)==1,names
                archived_bytes=z.read(names[0])
        assert sha(installed.read_bytes()) == sha(archived_bytes) == entry['sha256'], installed
        platform = artifact['platform']
        root = a.atomchat/'platforms'/platform
        outputs = root/'build/atomlib-consumer'
        source_list = out/platform/'sources.txt'
        assert source_list.is_file(), 'export reference inputs first'
        sources = [Path(x) for x in source_list.read_text(encoding='utf-8-sig').splitlines() if x]
        selected = [x for x in sources if any(part in x.as_posix() for part in (
            '/settings/', '/ui/', '/audio/', '/font/', '/nav/PageTransitionComposite.java', '/screen/ScreenMetrics.java',
            '/render/HeaderChrome.java', '/render/ScreenMetrics.java', '/render/ClickRipples.java',
            '/render/SkiaFontRenderer.java'))]
        # Record source identity as provenance, not a new approval or trust mechanism.
        snapshot = out/platform/'source-snapshot.zip'
        with zipfile.ZipFile(snapshot, 'w', zipfile.ZIP_DEFLATED) as z:
            for source in selected:
                z.write(source, source.relative_to(a.atomchat).as_posix())
        matches, remapped, missing = [], [], []
        with zipfile.ZipFile(installed) as z:
            for f in sorted((outputs/'classes/java/main/com/atom/chat').rglob('*.class')):
                name = f.relative_to(outputs/'classes/java/main').as_posix()
                if not any('/'+part+'/' in name for part in ('settings','ui','audio','font')) and not any(s in name for s in ('PageTransitionComposite','HeaderChrome','ScreenMetrics','ClickRipples','SkiaFontRenderer')):
                    continue
                if name not in z.namelist(): missing.append(name)
                elif z.read(name) == f.read_bytes(): matches.append(name)
                else: remapped.append(name)
            resources = {n: sha(z.read(n)) for n in z.namelist() if n.startswith('assets/atomchat/') and (n.endswith(('.ogg', '.json', '.svg', '.png')))}
        assert matches or remapped, 'no reference classes found'
        assert not missing, (platform, missing)
        libs = list(Path(entry['instance']).glob('mods/atomlib-*.jar'))
        assert len(libs) == 1, libs
        with zipfile.ZipFile(libs[0]) as z:
            fonts = {n: sha(z.read(n)) for n in z.namelist() if n.endswith(('bundled-default.otf','bundled-default-bold.otf'))}
            assert len(fonts) == 2, 'fixed packaged fonts required; no system fallback'
            for name in fonts:
                target = out/'fonts'/Path(name).name
                target.parent.mkdir(parents=True, exist_ok=True)
                data = z.read(name)
                assert not target.exists() or target.read_bytes() == data, 'three hosts have different fonts'
                target.write_bytes(data)
        results.append(dict(platform=platform, installed=str(installed), sha256=entry['sha256'],
            archived_artifact=str(archived), atomlib=str(libs[0]), atomlib_sha256=sha(libs[0].read_bytes()),
            byte_identical_classes=matches, remapped_classes_require_source_compile_comparison=remapped,
            source_files={str(x.relative_to(a.atomchat)):sha(x.read_bytes()) for x in selected},
            fonts=fonts, resources=resources))
    report = dict(observed_at=datetime.datetime.now().astimezone().isoformat(),
        approval='User approved current original settings language including interaction and sound; not server redesign',
        scope='ORIGINAL_SETTINGS_REFERENCE_PROVENANCE', platforms=results,
            limits=['Source hashes are provenance, not authorization', 'Remapped classes need independent source/build comparison',
                'No game process/device/GPU observations; no instance writes'])
    (out/'installed-reference.json').write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding='utf-8')
    print(json.dumps({x['platform']:{'identical_classes':len(x['byte_identical_classes']),
         'remapped_classes':len(x['remapped_classes_require_source_compile_comparison']),
         'sources':len(x['source_files']), 'sha256':x['sha256']} for x in results}, indent=2))


if __name__ == '__main__':
    main()
