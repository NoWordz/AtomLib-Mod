package com.atom.lib.controls;
import com.atom.lib.*;
import com.atom.lib.design.atomdefault.*;
import com.atom.lib.design.atomchat.*;
import com.atom.lib.geometry.*;
import com.atom.lib.layout.*;
import com.atom.lib.layout.internal.YogaLayoutEngine;
import com.atom.lib.motion.*;
import com.atom.lib.text.*;
import io.github.humbleui.skija.*;
import java.util.*;
import java.nio.file.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AppearanceShowcaseRenderTest {
    @Test void lightDarkEditorsAndVirtualTableUseRealRendering() throws Exception {
        try(var runtime=AtomLibRuntime.open("appearance-render");var a=AtomDefault.createSession(runtime);var b=AtomDefault.createSession(runtime);var surface=Surface.makeRasterN32Premul(1200,760);var layout=new YogaLayoutEngine()){
            a.selectMotion("off");b.selectMotion("off");b.selectTheme("dusk");
            var canvas=surface.getCanvas();canvas.clear(0xff131722);int saved=canvas.getSaveCount();
            for(int index=0;index<2;index++){
                var ui=index==0?a:b;float x=24+index*588;
                var name=new TextField(new NodeId("name"),ui.context(),"AtomLib / 中文输入",new TextFieldEvents(){});
                var notes=new TextArea(new NodeId("notes"),ui.context(),"Independent window state\n多行文本 · clipboard · undo",new TextFieldEvents(){});
                var numeric=new NumberField(new NodeId("number"),ui.context(),BigDecimal.TEN,BigDecimal.ZERO,new BigDecimal("100"),new NumberFieldEvents(){});numeric.value("999");assertEquals(NumberField.Validation.OUT_OF_RANGE,numeric.validation());
                ui.input().setControls(List.of(name,notes,numeric));
                try(var frame=ui.beginFrame(new FrameTime(1,0,0),new Viewport(1200,760,1))){
                    frame.panel(canvas,new Rect(x,20,564,720),16,()->false);
                    var text=ui.context().text();var title=text.layout(new TextRequest(index==0?"MINIMAL / independent host A":"DUSK / independent host B",AtomChatFonts.HEADING,20,frame.theme().palette().textColor()),new Constraints(Constraints.Axis.atMost(520),Constraints.Axis.undefined()));text.draw(canvas,title,x+18,38);
                    var tree=new LayoutTree(new LayoutNode(new NodeId("root"),LayoutStyle.column().size(Length.points(528),Length.auto()).spacing(0,12),ui.input().controls().stream().map(c->c.node(LayoutStyle.column())).toList(),null,0));
                    var viewport=new Viewport(1200,760,1);var local=new Viewport(528,760,1);var shown=PresentationFrame.create(tree,layout.layout(tree,local),viewport,Map.of(),new PresentationFrame.Offset(x+18,82));frame.paint(canvas,shown);
                    var source=new VirtualData<String,String>(){public int count(){return 10000;}public String itemAt(int i){return "Item "+i;}public String key(String v){return v;}};
                    try(var table=VirtualCollection.table(new NodeId("table"),source,42,List.of(new TableColumn("name","Name / 10000 rows",330,true),new TableColumn("action","Action",198,false)))){
                        table.actions((key,r)->Map.of("open",new Rect(r.right()-72,r.y()+7,60,28)));
                        table.paint(canvas,table.layout(viewport,new Rect(x+18,394,528,324)),ui.context(),(c,item,r)->{
                            var label=text.layout(new TextRequest(item.item(),AtomChatFonts.BODY,14,frame.theme().palette().textColor()),new Constraints(Constraints.Axis.atMost(290),Constraints.Axis.undefined()));text.draw(c,label,r.x()+10,r.y()+12);
                            var action=item.actions().get("open");SkiaDraw.drawRoundedRect(c,action.x(),action.y(),action.width(),action.height(),6,frame.theme().palette().accentColor());
                        });
                    }
                }
            }
            assertEquals(saved,canvas.getSaveCount());assertFalse(runtime.isClosed());
            var output=java.nio.file.Path.of("build/v050-appearance-showcase.png");Files.createDirectories(output.getParent());try(var image=surface.makeImageSnapshot();var png=image.encodeToData()){Files.write(output,png.getBytes());assertTrue(png.getBytes().length>10000);}
        }
    }
}
