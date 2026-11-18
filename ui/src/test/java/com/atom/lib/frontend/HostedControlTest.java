package com.atom.lib.frontend;

import com.atom.lib.controls.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HostedControlTest {
    static final Binding BINDING=new Binding(ExistingHostComponent.class.getName(),"controls-test/1");
    static final class ExistingHostComponent implements StrictPageSession.Implementation {
        int releases,cancels;boolean enabled=true;float position;
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView content){
            return new HostedControl(id,new HostedControl.Adapter(){
                public LayoutNode node(NodeId id,LayoutStyle outer){return new LayoutNode(id,outer.size(Length.points(160),Length.points(48)),List.of(),null,0);}
                public boolean enabled(){return enabled;}
                public boolean press(PointerEvent event,PresentationFrame frame){if(event.button()!=0)return false;return content.activate("activate");}
                public void release(PointerEvent event,PresentationFrame frame){releases++;}
                public void cancel(CancelReason reason){cancels++;}
                public ControlObservation observation(){return new ControlObservation(enabled,false,false,0,0,position);}
                public void paint(Canvas canvas,PresentationFrame frame,ControlVisual visual){var r=frame.box(id).border();try(var p=new Paint().setColor(0xff123456)){canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(r.x(),r.y(),r.width(),r.height()),p);}}
            });
        }
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView content){enabled=content.snapshot().enabled();((HostedControl)control).refresh();}
    }
    @Test void originalSubControlHoverAndPressUseTheSamePresentedPaintingCoordinates(){
        var id=new NodeId("hosted/part");int[] hovers={0},actions={0};
        var control=new HostedControl(id,new HostedControl.Adapter(){
            public LayoutNode node(NodeId id,LayoutStyle outer){return new LayoutNode(id,outer.size(Length.points(100),Length.points(40)),List.of(),null,0);}
            public boolean enabled(){return true;}
            boolean over(float x,float y,PresentationFrame frame){var box=frame.box(id).border();return x>=box.right()-20&&x<=box.right()&&y>=box.y()&&y<=box.bottom();}
            public void hover(float x,float y,PresentationFrame frame){if(over(x,y,frame))hovers[0]++;}
            public boolean press(PointerEvent event,PresentationFrame frame){if(over(event.x(),event.y(),frame)){actions[0]++;return true;}return false;}
            public ControlObservation observation(){return new ControlObservation(true,false,false,0,0,0);}
            public void paint(Canvas canvas,PresentationFrame frame,ControlVisual visual){}
        });
        try(var session=new UiSession(List.of(control));var layout=new com.atom.lib.layout.internal.YogaLayoutEngine();var surface=Surface.makeRasterN32Premul(300,160)){
            var viewport=new Viewport(300,160,1);session.prepare(viewport);var tree=new LayoutTree(control.node(LayoutStyle.column()));
            var plain=PresentationFrame.create(tree,layout.layout(tree,viewport),viewport,Map.of(),new PresentationFrame.Offset(0,0));
            var frame=plain.scaled(1.5f).translated(13,7).clipped(viewport.bounds());session.paint(surface.getCanvas(),frame,viewport,new FrameTime(1,0,0),true);
            var shown=frame.box(id).border();float x=shown.right()-15,y=shown.y()+shown.height()/2;
            session.hover(x,y);assertEquals(1,hovers[0],"host sub-control hover must receive the displayed pointer");
            session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));assertEquals(1,actions[0],"press must use the same coordinates as the painter");
        }
    }
    @Test void approvedOriginalCanCommitOnPressWithoutBeingReplacedByReleaseSemantics(){
        var requirements=new EnumMap<Aspect,Requirement>(Aspect.class);for(var a:Aspect.values())requirements.put(a,Requirement.notApplicable("host event adaptation fixture"));
        var d=new ContractDesign(1,"hosted","1","controls-test/1",Map.of("root",""),Set.of(),Map.of(),
            Map.of("space",new Family("root",Aspect.SPACING,"",Map.of("normal",new NumberValue(0,Unit.U),"max",new NumberValue(800,Unit.U)))),
            Map.of("action",new Role(true,requirements)),Map.of("original",new Recipe("root","action",Map.of(),BINDING,Map.of("activate",new Slot(ContentKind.ACTION,true)),Set.of("press"))),
            Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("space","normal"),new Ref("space","normal"),new Ref("space","normal"),new Ref("space","max"))),Map.of(),Map.of("page",new Page("root",Set.of("original"),"stack",Set.of("press"))));
        byte[] bytes=ContractCodec.encode(d);var page=ContractRegistry.load(bytes,ContractRegistry.manifest(bytes,bytes),Set.of("controls-test/1")).page("page");
        int[] actions={0};var content=new StrictPage.Content(Map.of("activate",new StrictPage.Action(()->actions[0]++)),true);
        var descriptor=StrictPage.builder(page).component("original",page.recipe("original"),content).build();var implementation=new ExistingHostComponent();
        try(var surface=Surface.makeRasterN32Premul(240,120);var session=new StrictPageSession(descriptor,Map.of(BINDING,implementation))){
            var viewport=new Viewport(240,120,1);var frame=session.render(surface.getCanvas(),viewport,new FrameTime(1,0,0),Map.of());var box=frame.box(new NodeId("hosted/page/original")).border();float x=box.x()+box.width()/2,y=box.y()+box.height()/2;
            session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));assertEquals(1,actions[0],"the approved host owns press-time commit");assertEquals(0,implementation.releases);
            session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,actions[0]);assertEquals(1,implementation.releases);
            implementation.position=1.08f;assertDoesNotThrow(()->session.render(surface.getCanvas(),viewport,new FrameTime(2,16_000_000,.016),Map.of()));
            assertEquals(1.08f,session.observations().get("original").position(),0,"actual spring overshoot must be observed without rejecting the rendered frame");
            session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));assertEquals(2,actions[0]);
            session.update("original",new StrictPage.Content(content.slots(),false));session.render(surface.getCanvas(),viewport,new FrameTime(3,32_000_000,.016),Map.of());
            session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,implementation.releases,"disable cancelled the captured host control");assertTrue(implementation.cancels>0);
            session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));assertEquals(2,actions[0]);
        }
    }
}
