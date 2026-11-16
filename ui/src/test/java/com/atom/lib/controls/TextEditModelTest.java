package com.atom.lib.controls;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextEditModelTest {
    @Test void deletingAtCaretAndUndoRestoresCollapsedCaretRatherThanSyntheticSelection(){
        var m=new TextEditModel("Ae\u0301😀",false);
        assertTrue(m.backspace());assertTrue(m.undo());assertEquals("Ae\u0301😀",m.text());
        assertEquals(5,m.anchor());assertEquals(5,m.caret());
        m.select(1,1);assertTrue(m.deleteForward());assertTrue(m.undo());
        assertEquals(1,m.anchor());assertEquals(1,m.caret());
    }
    @Test void basicEditorWorksInAnIsolatedRuntimeWithoutIcu() throws Exception {
        var locations=new java.net.URL[]{TextEditModel.class.getProtectionDomain().getCodeSource().getLocation(),
            com.atom.lib.text.UnicodeBoundaries.class.getProtectionDomain().getCodeSource().getLocation()};
        try(var loader=new java.net.URLClassLoader(locations,ClassLoader.getPlatformClassLoader())){
            assertThrows(ClassNotFoundException.class,()->loader.loadClass("com.ibm.icu.text.BreakIterator"));
            var boundaries=loader.loadClass("com.atom.lib.text.UnicodeBoundaries");
            assertEquals(java.util.List.of(0,2,7),boundaries.getMethod("graphemes",String.class).invoke(null,"e\u0301👩‍💻"));
            var type=loader.loadClass("com.atom.lib.controls.TextEditModel");
            var editor=type.getConstructor(String.class,boolean.class).newInstance("中👩🏽‍💻e\u0301",false);
            type.getMethod("backspace").invoke(editor);assertEquals("中👩🏽‍💻",type.getMethod("text").invoke(editor));
            type.getMethod("backspace").invoke(editor);assertEquals("中",type.getMethod("text").invoke(editor));
        }
    }
    @Test void backspaceDeletesWholeEmojiAndCombiningClusters(){
        var m=new TextEditModel("中👩🏽‍💻e\u0301🇨🇳",false);
        assertTrue(m.backspace());assertEquals("中👩🏽‍💻e\u0301",m.text());
        assertTrue(m.backspace());assertEquals("中👩🏽‍💻",m.text());
        assertTrue(m.backspace());assertEquals("中",m.text());
        assertTrue(m.backspace());assertEquals("",m.text());assertFalse(m.backspace());
    }
    @Test void navigationSelectionAndForwardDeleteUseWholeGraphemes(){
        var m=new TextEditModel("A👨‍👩‍👧‍👦e\u0301Z",false);
        m.select(0,0);m.move(1,false);m.move(1,true);
        assertEquals("👨‍👩‍👧‍👦",m.selectedText());
        assertTrue(m.deleteForward());assertEquals("Ae\u0301Z",m.text());
        assertTrue(m.deleteForward());assertEquals("AZ",m.text());
    }
    @Test void replacementUndoAndRedoRestoreTextAndSelection(){
        var m=new TextEditModel("中文",false);m.select(0,1);
        assertTrue(m.replaceSelection("👩🏽‍💻"));assertEquals("👩🏽‍💻文",m.text());
        assertTrue(m.undo());assertEquals("中文",m.text());assertEquals("中",m.selectedText());
        assertTrue(m.redo());assertEquals("👩🏽‍💻文",m.text());assertEquals(7,m.caret());
        assertTrue(m.undo());m.replaceSelection("新");assertFalse(m.redo());
    }
    @Test void partialSelectionExpandsInsteadOfSplittingSurrogatesOrAccents(){
        var m=new TextEditModel("A😀e\u0301Z",false);m.select(2,4);
        assertEquals("😀e\u0301",m.selectedText());m.replaceSelection("中");assertEquals("A中Z",m.text());
    }
    @Test void singleLinePasteNormalizesLineBreaksWhileMultilinePreservesThem(){
        var single=new TextEditModel("",false);single.replaceSelection("甲\r\n乙\r丙\n丁");
        assertEquals("甲 乙 丙 丁",single.text());
        var multi=new TextEditModel("",true);multi.replaceSelection("甲\r\n乙\r丙\n丁");
        assertEquals("甲\n乙\n丙\n丁",multi.text());assertTrue(multi.multiline());
    }
    @Test void editingOneModelDoesNotShareHistoryOrSelectionWithAnother(){
        var a=new TextEditModel("同名",false);var b=new TextEditModel("同名",false);
        a.replaceSelection("A");b.replaceSelection("B");a.undo();
        assertEquals("同名",a.text());assertEquals("同名B",b.text());assertTrue(b.undo());
    }
    @Test void unsupportedTextFailsAtomicallyBeforeChangingHistory(){
        var m=new TextEditModel("中文",false);
        assertThrows(IllegalArgumentException.class,()->m.replaceSelection("\ud83d"));
        assertEquals("中文",m.text());assertFalse(m.undo());
        assertThrows(IllegalArgumentException.class,()->m.replaceSelection("x".repeat(32769)));
        assertEquals("中文",m.text());
    }
}
