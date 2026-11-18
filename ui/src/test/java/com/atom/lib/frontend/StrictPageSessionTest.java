package com.atom.lib.frontend;

import com.atom.lib.*;
import com.atom.lib.controls.*;
import com.atom.lib.font.*;
import com.atom.lib.text.*;
import com.atom.lib.design.atomchat.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.atom.lib.frontend.contract.ContractDesign.*;
import static org.junit.jupiter.api.Assertions.*;

class StrictPageSessionTest {
    static final Binding BINDING=new Binding(ButtonImplementation.class.getName(),"controls-test/1");
    static ContractRegistry registry(){return registry(false);}
    static ContractRegistry registry(boolean audible){
        var role=new EnumMap<Aspect,Requirement>(Aspect.class);for(var a:Aspect.values())role.put(a,Requirement.notApplicable("fixture uses existing action control only"));
        if(audible)role.put(Aspect.SOUND,Requirement.required());
        var families=new HashMap<String,Family>();families.put("spacing",new Family("root",Aspect.SPACING,"",Map.of("normal",new NumberValue(12,Unit.U),"max",new NumberValue(800,Unit.U))));
        if(audible)families.put("sound",new Family("root",Aspect.SOUND,"",Map.of("button",new SoundValue("BUTTON","button","MASTER","volume",1,0,new NamedCondition.State("enabled"),""))));
        var d=new ContractDesign(1,"controltest","1","controls-test/1",Map.of("root",""),Set.of("enabled"),audible?Map.of("volume",new Parameter(0,1,.6,Unit.RATIO)):Map.of(),
            families,Map.of("button",new Role(true,role)),
            Map.of("button",new Recipe("root","button",audible?Map.of(Aspect.SOUND,new Ref("sound","button")):Map.of(),BINDING,Map.of("label",new Slot(ContentKind.TEXT,true),"activate",new Slot(ContentKind.ACTION,true)),Set.of("normal","activate","cancel"))),
            Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("spacing","normal"),new Ref("spacing","normal"),new Ref("spacing","normal"),new Ref("spacing","max"))),audible?Map.of("button",new Asset(AssetKind.SOUND,"example:feedback_button","fixture output resource")):Map.of(),
            Map.of("page",new Page("root",Set.of("button"),"stack",Set.of("normal","activate","cancel"))));byte[] b=ContractCodec.encode(d);return ContractRegistry.load(b,ContractRegistry.manifest(b,b),Set.of("controls-test/1"));
    }
    static final class ButtonImplementation implements StrictPageSession.Implementation {
        final ControlContext context;
        ButtonImplementation(ControlContext context){this.context=context;}
        ActionModel model(StrictPage.Content c){return new ActionModel(((StrictPage.Text)c.slots().get("label")).value(),c.enabled(),ActionModel.Role.NORMAL);}
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView c){return new ActionButton(id,context,model(c.snapshot()),()->c.activate("activate"));}
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView c){((ActionButton)control).update(model(c.snapshot()));}
    }
    @Test void yogaPaintAndInputUseSameFrameAndDisableCancelsPendingActivation(){
        try(var runtime=AtomLibRuntime.open("fcl-session");var fonts=new FontRegistry(runtime);var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)){
            fonts.registerDefaults();AtomChatFonts.register(fonts);var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));
            var p=registry().page("page");int[] activated={0};var content=new StrictPage.Content(Map.of("label",new StrictPage.Text("Apply"),"activate",new StrictPage.Action(()->activated[0]++)),true);
            var page=StrictPage.builder(p).component("apply",p.recipe("button"),content).build();
            try(var session=new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context)))){
                var viewport=new Viewport(480,320,1);var frame=session.render(surface.getCanvas(),viewport,new FrameTime(1,16_000_000,.016),Map.of());
                var id=new NodeId("controltest/page/apply");var rect=frame.box(id).border();assertTrue(rect.width()>0&&rect.height()>0);
                try(var image=surface.makeImageSnapshot();var bitmap=Bitmap.makeFromImage(image)){assertNotEquals(0,bitmap.getColor((int)(rect.x()+rect.width()/2),(int)(rect.y()+rect.height()/2)));}
                float x=rect.x()+rect.width()/2,y=rect.y()+rect.height()/2;
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,activated[0]);
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.update("apply",new StrictPage.Content(content.slots(),false));session.render(surface.getCanvas(),viewport,new FrameTime(2,32_000_000,.016),Map.of());
                session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,activated[0]);
            }
        }
    }
    @Test void approvedImplementationMustExistAndUnknownParametersCannotChangeLayout(){var p=registry().page("page");var page=StrictPage.builder(p).component("apply",p.recipe("button"),new StrictPage.Content(Map.of("label",new StrictPage.Text("x"),"activate",new StrictPage.Action(()->{})),true)).build();assertThrows(IllegalArgumentException.class,()->new StrictPageSession(page,Map.of()));}
    @Test void realControlCommitAutomaticallyReachesSoundOutputAndCancelStaysSilent(){
        try(var runtime=AtomLibRuntime.open("fcl-feedback");var fonts=new FontRegistry(runtime);var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)){
            fonts.registerDefaults();AtomChatFonts.register(fonts);var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));var p=registry(true).page("page");int[] actions={0};
            var page=StrictPage.builder(p).component("apply",p.recipe("button"),new StrictPage.Content(Map.of("label",new StrictPage.Text("Apply"),"activate",new StrictPage.Action(()->actions[0]++)),true)).build();
            var implementations=Map.<Binding,StrictPageSession.Implementation>of(BINDING,new ButtonImplementation(context));assertThrows(IllegalArgumentException.class,()->new StrictPageSession(page,implementations),"audible contract cannot silently omit its host output");
            var output=new ArrayList<UiSoundIntent>();var sink=new UiSoundSink(){public boolean supports(String c){return c.equals("MASTER");}public boolean available(String r){return r.equals("example:feedback_button");}public void play(UiSoundIntent intent){output.add(intent);}};
            try(var session=new StrictPageSession(page,implementations,sink)){
                var frame=session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(1,16_000_000,.016),Map.of("volume",.4));var rect=frame.box(new NodeId("controltest/page/apply")).border();float x=rect.x()+rect.width()/2,y=rect.y()+rect.height()/2;
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));assertTrue(output.isEmpty());session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,actions[0]);assertEquals(1,output.size());assertEquals(.4f,output.get(0).volume());assertEquals("MASTER",output.get(0).channel());
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.pointer(new PointerEvent(PointerEvent.Type.MOVE,0,310,0));session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,0,310,0));assertEquals(1,output.size());assertEquals(1,actions[0]);
                session.update("apply",new StrictPage.Content(Map.of("label",new StrictPage.Text("Rejected"),"activate",new StrictPage.Action(()->false)),true));
                session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(2,32_000_000,.016),Map.of("volume",.4));
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,output.size(),"business-rejected action stays silent and does not retain old callback");assertEquals(1,actions[0]);
            }
        }
    }
    @Test void observationsExposeImmutableRenderedMotionRatherThanDeclaredDurations()throws Exception{
        try(var runtime=AtomLibRuntime.open("fcl-observation");var fonts=new FontRegistry(runtime);var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)){
            fonts.registerDefaults();AtomChatFonts.register(fonts);var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));var p=registry().page("page");
            var page=StrictPage.builder(p).component("apply",p.recipe("button"),new StrictPage.Content(Map.of("label",new StrictPage.Text("Apply"),"activate",new StrictPage.Action(()->{})),true)).build();
            try(var session=new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context)))){
                var method=assertDoesNotThrow(()->session.getClass().getMethod("observations"),"must expose observations of the actual rendered control");
                var frame=session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(1,0,0),Map.of());
                var before=(Map<?,?>)method.invoke(session);Object initial=before.get("apply");var hover=initial.getClass().getMethod("hover");assertEquals(0f,hover.invoke(initial));
                var r=frame.box(new NodeId("controltest/page/apply")).border();session.pointer(new PointerEvent(PointerEvent.Type.MOVE,r.x()+r.width()/2,r.y()+r.height()/2,0));
                session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(2,40_000_000,.04),Map.of());var mid=(Map<?,?>)method.invoke(session);float value=(Float)hover.invoke(mid.get("apply"));assertTrue(value>0&&value<1);
                session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(3,200_000_000,.16),Map.of());var end=(Map<?,?>)method.invoke(session);assertEquals(1f,hover.invoke(end.get("apply")));assertEquals(0f,hover.invoke(initial));assertThrows(UnsupportedOperationException.class,()->end.clear());
            }
        }
    }
}
