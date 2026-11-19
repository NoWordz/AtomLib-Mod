package com.atom.lib.frontend;

import com.atom.lib.*;
import com.atom.lib.controls.*;
import com.atom.lib.font.*;
import com.atom.lib.text.*;
import com.atom.lib.design.atomchat.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.geometry.Rect;
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
    private static final Binding GROUP_BINDING = new Binding(ClippedGroup.class.getName(), "controls-test/1");
    static ContractRegistry registry(){return registry(false);}
    static ContractRegistry registry(boolean audible){
        var role=new EnumMap<Aspect,Requirement>(Aspect.class);for(var a:Aspect.values())role.put(a,Requirement.notApplicable("fixture uses existing action control only"));
        if(audible)role.put(Aspect.SOUND,Requirement.required());
        var families=new HashMap<String,Family>();families.put("spacing",new Family("root",Aspect.SPACING,"",Map.of("normal",new NumberValue(12,Unit.U),"max",new NumberValue(800,Unit.U))));
        if(audible)families.put("sound",new Family("root",Aspect.SOUND,"",Map.of("button",new SoundValue("BUTTON","button","MASTER","volume",1,0,new NamedCondition.State("enabled"),""))));
        var groupRole=new EnumMap<Aspect,Requirement>(Aspect.class);
        for(var aspect:Aspect.values())groupRole.put(aspect,Requirement.notApplicable("structural group"));
        groupRole.put(Aspect.LAYOUT,Requirement.required());
        var decoratedRole = new EnumMap<>(groupRole);
        decoratedRole.put(Aspect.MATERIAL, Requirement.required());
        families.put("material", new Family("root", Aspect.MATERIAL, "", Map.of("clipped", new TextValue("clipped"))));
        var foldingRole=new EnumMap<>(groupRole);
        foldingRole.put(Aspect.LAYOUT,Requirement.conditional(new NamedCondition.State("expanded")));
        var scrollingRole=new EnumMap<>(groupRole);
        scrollingRole.put(Aspect.SCROLL,Requirement.required());
        families.put("container",new Family("root",Aspect.LAYOUT,"",Map.of(
                "inline",new GroupLayoutValue("inline",true),
                "scroll",new GroupLayoutValue("stack",true,true))));
        families.put("scrolling",new Family("root",Aspect.SCROLL,"",Map.of(
                "vertical",new TextValue(GroupLayoutValue.VERTICAL_SCROLL))));
        var scenarios=Set.of("normal","activate","cancel");
        var padding=new Ref("spacing","normal");
        var maximum=new Ref("spacing","max");
        var d=new ContractDesign(1,"controltest","1","controls-test/1",Map.of("root",""),Set.of("enabled","expanded"),audible?Map.of("volume",new Parameter(0,1,.6,Unit.RATIO)):Map.of(),
            families,Map.of("button",new Role(true,role),"group",new Role(false,groupRole),                "fold",new Role(false,foldingRole),"scroll",new Role(false,scrollingRole),
                "decorated",new Role(false,decoratedRole)),
            Map.of("button",new Recipe("root","button",audible?Map.of(Aspect.SOUND,new Ref("sound","button")):Map.of(),BINDING,Map.of("label",new Slot(ContentKind.TEXT,true),"activate",new Slot(ContentKind.ACTION,true)),scenarios),
                "group",new Recipe("root","group",Map.of(Aspect.LAYOUT,new Ref("container","inline")),BINDING,Map.of(),scenarios),
                "decorated",new Recipe("root","decorated",
                        Map.of(Aspect.LAYOUT,new Ref("container","inline"), Aspect.MATERIAL,new Ref("material","clipped")),
                        GROUP_BINDING,Map.of(),scenarios),
                "fold",new Recipe("root","fold",Map.of(Aspect.LAYOUT,new Ref("container","inline")),BINDING,Map.of(),scenarios),
                "scroll",new Recipe("root","scroll",Map.of(Aspect.LAYOUT,new Ref("container","scroll"),
                        Aspect.SCROLL,new Ref("scrolling","vertical")),BINDING,Map.of(),scenarios)),
            Map.of("stack",new Layout("root",Axis.COLUMN,padding,padding,padding,maximum),
                "inline",new Layout("root",Axis.ROW,padding,padding,padding,maximum)),audible?Map.of("button",new Asset(AssetKind.SOUND,"example:feedback_button","fixture output resource")):Map.of(),
            Map.of("page",new Page("root",Set.of("button","group","scroll","fold","decorated"),"stack",scenarios)));byte[] b=ContractCodec.encode(d);return ContractRegistry.load(b,ContractRegistry.manifest(b,b),Set.of("controls-test/1"));
    }
    static final class ButtonImplementation implements StrictPageSession.Implementation {
        final ControlContext context;
        ButtonImplementation(ControlContext context){this.context=context;}
        ActionModel model(StrictPage.Content c){return new ActionModel(((StrictPage.Text)c.slots().get("label")).value(),c.enabled(),ActionModel.Role.NORMAL);}
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView c){return new ActionButton(id,context,model(c.snapshot()),()->c.activate("activate"));}
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView c){((ActionButton)control).update(model(c.snapshot()));}
    }
    static final class ClippedGroup implements StrictPageSession.GroupImplementation {
        int paints;
        boolean clip;

        @Override public void paintGroup(Canvas canvas, PresentationFrame frame, NodeId group, List<NodeId> children,
                                         Value material, StrictPageSession.FrameBindings bindings) {
            assertEquals(new TextValue("clipped"), material);
            assertEquals(List.of(new NodeId("controltest/page/inside")), children);
            paints++;
            if (clip) canvas.clipRect(io.github.humbleui.types.Rect.makeWH(0, 0));
        }
    }

    @Test void declaredGroupSurfaceOwnsItsClipAndDoesNotClipTheFollowingSibling() {
        try (var runtime=AtomLibRuntime.open("fcl-group-paint");var fonts=new FontRegistry(runtime);
             var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(320,240)) {
            fonts.registerDefaults();
            AtomChatFonts.register(fonts);
            var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));
            var contract=registry().page("page");
            var action=action("Settings",new ArrayList<>());
            var recipe=contract.recipe("button");
            var page=StrictPage.builder(contract)
                    .group("card",contract.recipe("decorated"), rows->rows.component("inside",recipe,action))
                    .component("outside",recipe,action).build();
            assertThrows(IllegalArgumentException.class,()->new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context))));
            var group=new ClippedGroup();
            try (var session=new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context),GROUP_BINDING,group))) {
                var view = new Viewport(320, 240, 1);
                var frame = session.render(surface.getCanvas(), view, new FrameTime(1, 0, 0), Map.of());
                var child = frame.box(new NodeId("controltest/page/inside")).border();
                var sibling = frame.box(new NodeId("controltest/page/outside")).border();
                int siblingInk;
                try (var image = surface.makeImageSnapshot(); var bitmap = Bitmap.makeFromImage(image)) {
                    assertTrue(ink(bitmap, child) > 0);
                    siblingInk = ink(bitmap, sibling);
                    assertTrue(siblingInk > 0);
                }
                group.clip = true;
                surface.getCanvas().clear(0);
                session.render(surface.getCanvas(), view, new FrameTime(2, 16_000_000, .016), Map.of());
                assertEquals(2, group.paints);
                try (var image = surface.makeImageSnapshot(); var bitmap = Bitmap.makeFromImage(image)) {
                    assertEquals(0, ink(bitmap, child));
                    assertEquals(siblingInk, ink(bitmap, sibling));
                }
            }
        }
    }

    private static int ink(Bitmap bitmap, Rect rect) {
        int count = 0;
        for (int y = (int) Math.ceil(rect.y()); y < rect.bottom(); y++) {
            for (int x = (int) Math.ceil(rect.x()); x < rect.right(); x++) {
                if (bitmap.getColor(x, y) != 0) count++;
            }
        }
        return count;
    }

    @Test void approvedGroupPlacesItsChildrenSideBySideAndUsesTheirDisplayedHits() {
        try (var runtime=AtomLibRuntime.open("fcl-group");var fonts=new FontRegistry(runtime);
             var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)) {
            fonts.registerDefaults();
            AtomChatFonts.register(fonts);
            var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));
            var contract=registry().page("page");
            List<String> actions=new ArrayList<>();
            var first=new StrictPage.Content(Map.of("label",new StrictPage.Text("First"),
                    "activate",new StrictPage.Action(()->{actions.add("first");})),true);
            var second=new StrictPage.Content(Map.of("label",new StrictPage.Text("Second"),
                    "activate",new StrictPage.Action(()->{actions.add("second");})),true);
            var builder=StrictPage.builder(contract);
            assertThrows(IllegalArgumentException.class,()->builder.group("invalid",contract.recipe("button"),
                    contents->contents.component("first",contract.recipe("button"),first)));
            var page=builder.group("toolbar",contract.recipe("group"),contents->contents
                    .component("first",contract.recipe("button"),first)
                    .component("second",contract.recipe("button"),second)).build();
            try (var session=new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context)))) {
                var frame=session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(1,0,0),Map.of());
                var group=new NodeId("controltest/page/toolbar");
                var left=frame.box(new NodeId("controltest/page/first"));
                var right=frame.box(new NodeId("controltest/page/second"));
                assertEquals(group,right.parent());
                assertEquals(left.border().y(),right.border().y());
                assertEquals(12F,right.border().x()-left.border().right());
                assertEquals(frame.box(group).border(),right.clip());
                float x=right.border().x()+right.border().width()/2,y=right.border().y()+right.border().height()/2;
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));
                session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                assertEquals(List.of("second"),actions);

                var reordered=StrictPage.builder(contract)
                        .group("toolbar",contract.recipe("group"),contents->contents
                                .component("second",contract.recipe("button"),action("wrong",actions))
                                .component("first",contract.recipe("button"),first)).build();
                assertThrows(IllegalArgumentException.class,()->session.update(reordered));
                var refreshed=StrictPage.builder(contract)
                        .group("toolbar",contract.recipe("group"),contents->contents
                                .component("first",contract.recipe("button"),first)
                                .component("second",contract.recipe("button"),action("Second",actions))).build();
                session.update(refreshed);
                var next=session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(2,16_000_000,.016),Map.of());
                assertEquals(right,next.box(new NodeId("controltest/page/second")));
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));
                session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                assertEquals(List.of("second","Second"),actions);
            }
        }
    }
    @Test void scrollGroupKeepsHeaderFixedAndOnlyDisplayedRowsCanActivate() {
        try (var runtime=AtomLibRuntime.open("fcl-scroll");var fonts=new FontRegistry(runtime);
             var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(360,260)) {
            fonts.registerDefaults();
            AtomChatFonts.register(fonts);
            var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));
            var contract=registry().page("page");
            List<String> actions=new ArrayList<>();
            var recipe=contract.recipe("button");
            var page=StrictPage.builder(contract)
                    .component("header",recipe,action("Header",actions))
                    .group("body",contract.recipe("scroll"),body->{
                        for(int i=0;i<10;i++)body.component("row"+i,recipe,action("Row "+i,actions));
                    }).build();
            try (var session=new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context)))) {
                var viewport=new Viewport(360,260,1);
                var bodyRegion = new Rect(24, 32, 290, 200);
                var before=session.renderIn(surface.getCanvas(),viewport,bodyRegion,new FrameTime(1,0,0),Map.of(),Map.of());
                var header=new NodeId("controltest/page/header");
                var last=new NodeId("controltest/page/row9");
                var body=before.box(new NodeId("controltest/page/body")).border();
                assertFalse(before.box(last).clip().contains(before.box(last).border().x()+2,before.box(last).border().y()+2));
                assertEquals(24 + 12, before.box(header).border().x());
                assertEquals(32 + 12, before.box(header).border().y());
                assertEquals(InputResult.PASS,session.wheel(body.x()+2,before.box(header).border().y()+2,0,-50));
                assertEquals(InputResult.PASS,session.wheel(bodyRegion.x()-1,body.y()+2,0,-50));
                assertEquals(InputResult.CONSUMED,session.wheel(body.x()+2,body.y()+2,0,-50));
                var after=session.renderIn(surface.getCanvas(),viewport,bodyRegion,new FrameTime(2,16_000_000,.016),Map.of(),Map.of());
                assertEquals(before.box(header),after.box(header));
                var rect=after.box(last).border();
                float x=rect.x()+rect.width()/2,y=rect.y()+rect.height()/2;
                assertTrue(after.box(last).clip().contains(x,y));
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));
                session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                assertEquals(List.of("Row 9"),actions);
            }
        }
    }
    private static StrictPage.Content action(String name,List<String> actions){
        return new StrictPage.Content(Map.of("label",new StrictPage.Text(name),
                "activate",new StrictPage.Action(()->{actions.add(name);})),true);
    }
    @Test void foldingRemovesDisplayedHitsAndCancelsTheHeldActionBeforeReopening() {
        try (var runtime=AtomLibRuntime.open("fcl-fold");var fonts=new FontRegistry(runtime);
             var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(300,200)) {
            fonts.registerDefaults();
            AtomChatFonts.register(fonts);
            var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));
            var contract=registry().page("page");
            List<String> actions=new ArrayList<>();
            var recipe=contract.recipe("button");
            var page=StrictPage.builder(contract)
                    .component("header",recipe,action("Header",actions))
                    .group("body",contract.recipe("fold"),body->body.component("row",recipe,action("Row",actions)))
                    .build();
            try (var session=new StrictPageSession(page,Map.of(BINDING,new ButtonImplementation(context)))) {
                var viewport=new Viewport(300,200,1);
                var open=session.render(surface.getCanvas(),viewport,new FrameTime(1,0,0),Map.of(),Map.of("expanded",true));
                var id=new NodeId("controltest/page/row");
                var box=open.box(id).border();
                float x=box.x()+box.width()/2,y=box.y()+box.height()/2;
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));
                var closed=session.render(surface.getCanvas(),viewport,new FrameTime(2,16_000_000,.016),Map.of(),Map.of("expanded",false));
                assertFalse(closed.contains(id));
                assertFalse(session.observations().containsKey("row"));
                session.render(surface.getCanvas(),viewport,new FrameTime(3,32_000_000,.016),Map.of(),Map.of("expanded",true));
                session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                assertTrue(actions.isEmpty());
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));
                session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                assertEquals(List.of("Row"),actions);
            }
        }
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
