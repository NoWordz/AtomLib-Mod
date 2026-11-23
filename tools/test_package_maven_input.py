"""Exercise release archive selection through the real command-line packager."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
VERSION = next(line.split('=', 1)[1] for line in (ROOT/'gradle.properties').read_text().splitlines()
               if line.startswith('atomlib_version='))
MODULES = ['core', 'runtime', 'ui', 'frontend-checker', '1.20.1-forge', '1.21.1-fabric', '1.21.1-neoforge']


class MavenArchiveTests(unittest.TestCase):
    def fixture(self, root):
        for module in MODULES:
            artifact = f'atomlib-{module}'
            folder = root/'com/atom/lib'/artifact/VERSION
            folder.mkdir(parents=True)
            for suffix in ['jar', 'pom', 'module']:
                (folder/f'{artifact}-{VERSION}.{suffix}').write_bytes(b'release fixture')

    def run_packager(self, root, output):
        return subprocess.run([sys.executable, str(ROOT/'tools/package_maven_input.py'),
                               '--maven', str(root), '--output', str(output)], capture_output=True, text=True)

    def test_archive_contains_only_the_current_seven_module_versions(self):
        with tempfile.TemporaryDirectory() as temporary:
            temp = Path(temporary)
            source = temp/'maven'
            self.fixture(source)
            old = source/'com/atom/lib/atomlib-ui/0.0.1/atomlib-ui-0.0.1.jar'
            old.parent.mkdir()
            old.write_bytes(b'unrelated old build')
            result = self.run_packager(source, temp/'input.zip')
            self.assertEqual(0, result.returncode, result.stderr)
            with ZipFile(temp/'input.zip') as archive:
                entries = archive.namelist()
                self.assertEqual(21, len(entries))
                self.assertNotIn(old.relative_to(source).as_posix(), entries)
            self.assertTrue((temp/'input.zip.sha256').is_file())

    def test_missing_required_module_does_not_produce_an_archive(self):
        with tempfile.TemporaryDirectory() as temporary:
            temp = Path(temporary)
            source = temp/'maven'
            self.fixture(source)
            (source/f'com/atom/lib/atomlib-frontend-checker/{VERSION}/atomlib-frontend-checker-{VERSION}.jar').unlink()
            result = self.run_packager(source, temp/'input.zip')
            self.assertNotEqual(0, result.returncode)
            self.assertIn('atomlib-frontend-checker', result.stderr)
            self.assertFalse((temp/'input.zip').exists())


if __name__ == '__main__':
    unittest.main()
