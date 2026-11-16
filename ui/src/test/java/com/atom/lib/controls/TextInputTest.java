package com.atom.lib.controls;

import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import io.github.humbleui.skija.Surface;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextInputTest {
    @Test void unsupportedPasteIsRejectedWithoutChangingDraftSelectionOrHistory(){
        try(var f=new ControlFixture()){
            var field=new TextField(new NodeId("paste"),f.context,"seed",new TextFieldEvents(){});f.session.setControls(List.of(field));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);f.session.text("x");tap(f.session,KeyEvent.Key.A,false,true);
            int anchor=field.edit().anchor(),caret=field.edit().caret();
            for(String invalid:List.of("a\tb","مرحبا","x".repeat(40000))){f.session.clipboard().write(invalid);assertDoesNotThrow(()->tap(f.session,KeyEvent.Key.V,false,true));assertEquals("seedx",field.value());assertEquals(anchor,field.edit().anchor());assertEquals(caret,field.edit().caret());}
            tap(f.session,KeyEvent.Key.Z,false,true);assertEquals("seed",field.value());
        }
    }
    private static KeyEvent key(KeyEvent.Key key,boolean down,boolean shift,boolean ctrl){return new KeyEvent(key,down,shift,ctrl,false);}
    private static void tap(UiSession s,KeyEvent.Key k,boolean shift,boolean ctrl){s.key(key(k,true,shift,ctrl));s.key(key(k,false,shift,ctrl));}
    private static TextField field(ControlFixture f,String id,String value,List<String> changes,List<String> submits){
        return new TextField(new NodeId(id),f.context,value,new TextFieldEvents(){
            public void changed(String text){changes.add(text);}
            public void committed(String text){submits.add(text);}
        });
    }
    @Test void keyDoesNotInsertTextAndCharacterCommitsOnlyToFocusedDisplayedField(){
        try(var f=new ControlFixture()){
            var changes=new ArrayList<String>();var submits=new ArrayList<String>();
            var a=field(f,"a","",changes,submits);var b=field(f,"b","",changes,submits);
            f.session.setControls(List.of(a,b));assertEquals(InputResult.PASS,f.session.character('中'));
            f.show(480);assertEquals(InputResult.PASS,f.session.character('中'));
            tap(f.session,KeyEvent.Key.TAB,false,false);
            tap(f.session,KeyEvent.Key.SPACE,false,false);assertEquals("",a.value());
            assertEquals(InputResult.CONSUMED,f.session.character(' '));f.session.text("中文👩🏽‍💻");
            assertEquals(" 中文👩🏽‍💻",a.value());assertEquals(List.of(" "," 中文👩🏽‍💻"),changes);assertTrue(submits.isEmpty());
            tap(f.session,KeyEvent.Key.TAB,false,false);f.session.character('乙');
            assertEquals("乙",b.value());assertEquals(" 中文👩🏽‍💻",a.value());
            tap(f.session,KeyEvent.Key.ENTER,false,false);assertEquals(List.of("乙"),submits);
        }
    }
    @Test void clipboardSelectionCutPasteAndUndoRedoAreRealEdits(){
        try(var f=new ControlFixture()){
            var changes=new ArrayList<String>();var a=field(f,"a","中文👩🏽‍💻",changes,new ArrayList<>());
            f.session.setControls(List.of(a));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);
            tap(f.session,KeyEvent.Key.A,false,true);tap(f.session,KeyEvent.Key.C,false,true);
            assertEquals("中文👩🏽‍💻",f.session.clipboard().read());assertTrue(changes.isEmpty());
            tap(f.session,KeyEvent.Key.X,false,true);assertEquals("",a.value());
            tap(f.session,KeyEvent.Key.V,false,true);assertEquals("中文👩🏽‍💻",a.value());
            tap(f.session,KeyEvent.Key.Z,false,true);assertEquals("",a.value());
            tap(f.session,KeyEvent.Key.Z,true,true);assertEquals("中文👩🏽‍💻",a.value());
            assertEquals(4,changes.size());
        }
    }
    @Test void singleLineNormalizesPastedNewlinesAndMultilineEnterEditsUntilControlEnter(){
        try(var f=new ControlFixture()){
            var submits=new ArrayList<String>();var single=field(f,"a","",new ArrayList<>(),submits);
            var multi=new TextArea(new NodeId("b"),f.context,"",new TextFieldEvents(){public void committed(String text){submits.add(text);}});
            f.session.setControls(List.of(single,multi));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);
            f.session.text("一\r\n二\r三");assertEquals("一 二 三",single.value());
            tap(f.session,KeyEvent.Key.TAB,false,false);f.session.text("一");tap(f.session,KeyEvent.Key.ENTER,false,false);f.session.text("二");
            assertEquals("一\n二",multi.value());assertTrue(submits.isEmpty());
            tap(f.session,KeyEvent.Key.HOME,false,false);tap(f.session,KeyEvent.Key.UP,false,false);f.session.text("甲");
            assertEquals("甲一\n二",multi.value());tap(f.session,KeyEvent.Key.ENTER,false,true);assertEquals(List.of("甲一\n二"),submits);
        }
    }
    @Test void numberKeepsIntermediateAndRangeErrorsUntilAnExplicitValidCommit(){
        try(var f=new ControlFixture()){
            var changes=new ArrayList<String>();var commits=new ArrayList<BigDecimal>();
            var number=new NumberField(new NodeId("n"),f.context,new BigDecimal("2"),BigDecimal.ZERO,BigDecimal.TEN,new NumberFieldEvents(){
                public void changed(String text){changes.add(text);}
                public void committed(BigDecimal value){commits.add(value);}
            });
            f.session.setControls(List.of(number));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);
            for(String value:List.of("-","1e","xyz","11")){
                tap(f.session,KeyEvent.Key.A,false,true);f.session.text(value);tap(f.session,KeyEvent.Key.ENTER,false,false);
                assertEquals(value,number.value());assertTrue(commits.isEmpty());assertNotEquals(NumberField.Validation.VALID,number.validation());
                assertEquals(new BigDecimal("2"),number.committedValue());
            }
            assertEquals(NumberField.Validation.OUT_OF_RANGE,number.validation());f.show(480);
            tap(f.session,KeyEvent.Key.A,false,true);f.session.text("3.50");tap(f.session,KeyEvent.Key.ENTER,false,false);
            assertEquals(List.of(new BigDecimal("3.50")),commits);assertEquals("3.50",number.value());assertEquals(5,changes.size());
        }
    }
    @Test void modalTextReachesChildAndNeverLeaksIntoBaseOrClosingModal(){
        try(var f=new ControlFixture()){
            var base=field(f,"a","",new ArrayList<>(),new ArrayList<>());var child=field(f,"child","",new ArrayList<>(),new ArrayList<>());
            f.session.setControls(List.of(base));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);f.session.text("底");
            var dialog=new ModalDialog(new NodeId("dialog"),f.context,new ModalOptions("输入",300,10,true,child.id()));
            dialog.content(child.node(LayoutStyle.column()),List.of(child));f.session.open(dialog,r->{});
            f.session.text("未显示");assertEquals("",child.value());f.show(480);f.session.text("中文👩🏽‍💻");
            assertEquals("中文👩🏽‍💻",child.value());assertEquals("底",base.value());
            f.session.closeModal(ModalCloseReason.COMPLETED);f.session.text("关闭尾部");assertEquals("中文👩🏽‍💻",child.value());
            f.show(480);f.session.text("层");assertEquals("底层",base.value());
        }
    }
    @Test void adapterPairsSurrogatesAndDropsPendingHalfAcrossFocusChanges(){
        try(var f=new ControlFixture()){
            var a=field(f,"a","",new ArrayList<>(),new ArrayList<>());var b=field(f,"b","",new ArrayList<>(),new ArrayList<>());
            f.session.setControls(List.of(a,b));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);
            var input=new ControlScreenAdapter(f.session);assertTrue(input.character('\ud83d'));assertEquals("",a.value());assertTrue(input.character('\ude00'));assertEquals("😀",a.value());
            input.character('\ud83d');tap(f.session,KeyEvent.Key.TAB,false,false);input.character('\ude00');assertEquals("",b.value());
            input.windowFocused(false);assertFalse(input.character('错'));input.windowFocused(true);input.character('对');assertEquals("对",b.value());
        }
    }
    @Test void pendingSurrogateCannotSurviveAFocusRoundTrip(){
        try(var f=new ControlFixture()){
            var a=field(f,"a","",new ArrayList<>(),new ArrayList<>());var b=field(f,"b","",new ArrayList<>(),new ArrayList<>());
            f.session.setControls(List.of(a,b));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);
            var adapter=new ControlScreenAdapter(f.session);adapter.character('\ud83d');
            tap(f.session,KeyEvent.Key.TAB,false,false);tap(f.session,KeyEvent.Key.TAB,false,false);adapter.character('\ude00');
            assertEquals("",a.value());assertEquals("",b.value());
        }
    }
    @Test void disabledRemovedAndBlankPointerFocusDoNotReceiveText(){
        try(var f=new ControlFixture()){
            var a=field(f,"a","",new ArrayList<>(),new ArrayList<>());f.session.setControls(List.of(a));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);
            f.pointer(PointerEvent.Type.PRESS,470,840,0);f.pointer(PointerEvent.Type.RELEASE,470,840,0);assertEquals(InputResult.PASS,f.session.text("无焦点"));
            tap(f.session,KeyEvent.Key.TAB,false,false);a.enabled(false);assertEquals(InputResult.PASS,f.session.text("禁用"));assertEquals("",a.value());
            f.session.setControls(List.of());assertEquals(InputResult.PASS,f.session.text("移除"));
        }
    }
    @Test void sessionsWithSameShortIdHaveIndependentUndoAndClipboard(){
        try(var a=new ControlFixture();var b=new ControlFixture()){
            var x=field(a,"text","",new ArrayList<>(),new ArrayList<>());var y=field(b,"text","",new ArrayList<>(),new ArrayList<>());
            a.session.setControls(List.of(x));b.session.setControls(List.of(y));a.show(480);b.show(480);
            tap(a.session,KeyEvent.Key.TAB,false,false);tap(b.session,KeyEvent.Key.TAB,false,false);a.session.text("甲");b.session.text("乙");
            tap(a.session,KeyEvent.Key.A,false,true);tap(a.session,KeyEvent.Key.C,false,true);assertEquals("",b.session.clipboard().read());
            tap(a.session,KeyEvent.Key.Z,false,true);assertEquals("",x.value());assertEquals("乙",y.value());
        }
    }
    @Test void clickAndDragSelectDisplayedGlyphsAndLongTextKeepsCaretVisible(){
        try(var f=new ControlFixture()){
            var a=field(f,"a","abcdef",new ArrayList<>(),new ArrayList<>());f.session.setControls(List.of(a));f.show(240);
            var r=f.frame.box(a.id()).content();float y=r.y()+r.height()/2;
            f.pointer(PointerEvent.Type.PRESS,r.x()+8,y,0);f.pointer(PointerEvent.Type.MOVE,r.right()-8,y,0);f.pointer(PointerEvent.Type.RELEASE,r.right()-8,y,0);
            assertEquals("abcdef",a.edit().selectedText());
            f.session.text("中文".repeat(100));f.show(240);assertTrue(a.horizontalScroll()>0);
            tap(f.session,KeyEvent.Key.HOME,false,true);f.show(240);assertEquals(0,a.horizontalScroll());
        }
    }
    @Test void repeatedEnterDoesNotDuplicateSubmissionAndBlurNeverCommits(){
        try(var f=new ControlFixture()){
            var submits=new ArrayList<String>();var a=field(f,"a","",new ArrayList<>(),submits);var b=field(f,"b","",new ArrayList<>(),submits);
            f.session.setControls(List.of(a,b));f.show(480);tap(f.session,KeyEvent.Key.TAB,false,false);f.session.text("输入");
            f.session.key(key(KeyEvent.Key.ENTER,true,false,false));f.session.key(key(KeyEvent.Key.ENTER,true,false,false));
            assertEquals(List.of("输入"),submits);f.session.key(key(KeyEvent.Key.ENTER,false,false,false));
            tap(f.session,KeyEvent.Key.TAB,false,false);assertEquals(List.of("输入"),submits);
        }
    }
}
