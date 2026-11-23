"""Extract the selected bilingual version section for the existing draft-release workflow."""
from pathlib import Path
import argparse
import re

ROOT = Path(__file__).resolve().parents[1]


def extract(text, version):
    headers = list(re.finditer(r'^## v([^\s]+)\s*$', text, re.MULTILINE))
    matches = [(index, match) for index, match in enumerate(headers) if match.group(1) == version]
    if len(matches) != 1:
        raise ValueError(f'Expected one release section for v{version}')
    index, match = matches[0]
    end = headers[index+1].start() if index+1 < len(headers) else len(text)
    section = text[match.end():end].strip()
    parts = re.split(r'^----\s*$', section, flags=re.MULTILINE)
    if len(parts) != 2 or not all(part.strip() for part in parts):
        raise ValueError(f'v{version} needs nonempty bilingual release notes')
    return section+'\n'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', required=True)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    args.output.write_text(extract((ROOT/'RELEASE_NOTES.md').read_text(encoding='utf-8'), args.version), encoding='utf-8')
