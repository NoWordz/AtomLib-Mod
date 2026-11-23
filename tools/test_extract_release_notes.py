import unittest
from extract_release_notes import extract


class ReleaseNotesTests(unittest.TestCase):
    def test_extracts_the_selected_complete_bilingual_section(self):
        text = '# Releases\n\n## v0.7.2\n\n- 修复资源。\n\n----\n\n- Fix resources.\n\n## v0.7.1\n\n- Older.\n'
        self.assertEqual('- 修复资源。\n\n----\n\n- Fix resources.\n', extract(text, '0.7.2'))

    def test_rejects_missing_duplicate_or_incomplete_versions(self):
        for text in ['## v0.7.1\n- Other.', '## v0.7.2\n- 中文。',
                     '## v0.7.2\n- 中文。\n----\n',
                     '## v0.7.2\n- 中文。\n----\n- English.\n## v0.7.2\n- Again.']:
            with self.assertRaises(ValueError):
                extract(text, '0.7.2')


if __name__ == '__main__':
    unittest.main()
