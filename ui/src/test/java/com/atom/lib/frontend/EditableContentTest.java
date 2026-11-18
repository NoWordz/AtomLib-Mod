package com.atom.lib.frontend;

import com.atom.lib.*;
import com.atom.lib.controls.*;
import com.atom.lib.design.atomchat.*;
import com.atom.lib.font.*;
import com.atom.lib.text.*;
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

class EditableContentTest {
    static final Binding BINDING=new Binding(Implementation.class.getName(),"editable-test/1");
    static ContractDesign design(){
        var role=new EnumMap<Aspect,Requirement>(Aspect.class);for(var a:Aspect.values())role.put(a,Requirement.notApplicable("data-commit fixture only"));role.put(Aspect.EDITING,Requirement.required());
        return new ContractDesign(1,"editable","1","editable-test/1",Map.of("root",""),Set.of(),Map.of(),Map.of("space",new Family("root",Aspect.SPACING,"",Map.of("normal",new NumberValue(12,Unit.U),"max",new NumberValue(800,Unit.U))),"editing",new Family("root",Aspect.EDITING,"",Map.of("field",new TextValue("typed-business-commit/1")))),Map.of("field",new Role(true,role)),Map.of("field",new Recipe("root","field",Map.of(Aspect.EDITING,new Ref("editing","field")),BINDING,Map.of("label",new Slot(ContentKind.TEXT,true),"number",new Slot(ContentKind.NUMBER,true),"checked",new Slot(ContentKind.BOOLEAN,true)),Set.of("commit"))),Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("space","normal"),new Ref("space","normal"),new Ref("space","normal"),new Ref("space","max"))),Map.of(),Map.of("page",new Page("root",Set.of("field"),"stack",Set.of("commit"))));
    }
    static ContractRegistry.PageHandle page(){return page(false);}
    static ContractRegistry.PageHandle page(boolean audible){
        var d=design();if(audible){var families=new HashMap<>(d.families());families.put("sound",new Family("root",Aspect.SOUND,"",Map.of("focus",new SoundValue("BUTTON","button","MASTER","volume",1,0,new NamedCondition.State("enabled"),""))));var requirements=new EnumMap<Aspect,Requirement>(d.roles().get("field").requirements());requirements.put(Aspect.SOUND,Requirement.required());var recipe=d.recipes().get("field");var bindings=new EnumMap<Aspect,Ref>(recipe.bindings());bindings.put(Aspect.SOUND,new Ref("sound","focus"));d=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),Set.of("enabled"),Map.of("volume",new Parameter(0,1,.6,Unit.RATIO)),families,Map.of("field",new Role(true,requirements)),Map.of("field",new Recipe(recipe.scope(),recipe.role(),bindings,recipe.implementation(),recipe.slots(),recipe.scenarios())),d.layouts(),Map.of("button",new Asset(AssetKind.SOUND,"example:feedback_button","fixture sink, not audio device")),d.pages());}
        byte[] b=ContractCodec.encode(d);return ContractRegistry.load(b,ContractRegistry.manifest(b,b),Set.of("editable-test/1")).page("page");
    }
    static final class Implementation implements StrictPageSession.Implementation {
        final ControlContext context;StrictPageSession.ContentView view;Runnable input=()->{};
        Implementation(ControlContext context){this.context=context;}
        ActionModel model(){var value=view.snapshot().slots().get("label");String text=value instanceof StrictPage.Text t?t.value():((StrictPage.TextInput)value).value();return new ActionModel(text,view.snapshot().enabled(),ActionModel.Role.NORMAL);}
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView content){view=content;return new ActionButton(id,context,model(),()->input.run());}
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView content){((ActionButton)control).update(model());}
    }
    @Test void actualInputCommitsTypedBusinessValuesAndRejectsRollbackWithoutAddingSound(){
        try(var runtime=AtomLibRuntime.open("editable-content");var fonts=new FontRegistry(runtime);var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)){
            fonts.registerDefaults();AtomChatFonts.register(fonts);var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));var p=page();int[] callbacks={0};
            var content=new StrictPage.Content(Map.of("label",new StrictPage.TextInput("Old",value->{callbacks[0]++;return !value.equals("reject");}),"number",new StrictPage.NumberInput(7,value->{callbacks[0]++;return value>=0;}),"checked",new StrictPage.BooleanInput(false,value->{callbacks[0]++;return true;})),true);
            var impl=new Implementation(context);var descriptor=StrictPage.builder(p).component("field",p.recipe("field"),content).build();
            try(var session=new StrictPageSession(descriptor,Map.of(BINDING,impl))){
                var frame=session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(1,0,0),Map.of());var r=frame.box(new NodeId("editable/page/field")).border();float x=r.x()+r.width()/2,y=r.y()+r.height()/2;
                impl.input=()->{assertTrue(impl.view.commitText("label","New"));assertTrue(impl.view.commitNumber("number",12));assertTrue(impl.view.commitBoolean("checked",true));assertFalse(impl.view.commitText("label","reject"));assertFalse(impl.view.commitNumber("number",-1));};
                session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                assertEquals(5,callbacks[0]);var now=impl.view.snapshot();assertEquals("New",((StrictPage.TextInput)now.slots().get("label")).value());assertEquals(12,((StrictPage.NumberInput)now.slots().get("number")).value());assertTrue(((StrictPage.BooleanInput)now.slots().get("checked")).value());assertEquals(ContractSoundSession.Result.SILENT,session.lastSoundResult());
                assertThrows(IllegalStateException.class,()->impl.view.commitText("label","outside-event"));
                session.update("field",new StrictPage.Content(now.slots(),false));session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(2,16_000_000,.016),Map.of());session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(5,callbacks[0]);
            }
        }
    }
    @Test void focusFeedbackDoesNotRequireABusinessCallbackAndUnknownCuesAreRejected(){
        try(var runtime=AtomLibRuntime.open("editable-feedback");var fonts=new FontRegistry(runtime);var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)){
            fonts.registerDefaults();AtomChatFonts.register(fonts);var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));var p=page(true);int[] commits={0};var impl=new Implementation(context);
            var content=new StrictPage.Content(Map.of("label",new StrictPage.TextInput("Focus",value->{commits[0]++;return true;}),"number",new StrictPage.NumberInput(7,value->{commits[0]++;return true;}),"checked",new StrictPage.BooleanInput(false,value->{commits[0]++;return true;})),true);
            var outputs=new ArrayList<UiSoundIntent>();var sink=new UiSoundSink(){public boolean supports(String channel){return channel.equals("MASTER");}public boolean available(String resource){return resource.equals("example:feedback_button");}public void play(UiSoundIntent intent){outputs.add(intent);}};
            try(var session=new StrictPageSession(StrictPage.builder(p).component("field",p.recipe("field"),content).build(),Map.of(BINDING,impl),sink)){
                var frame=session.render(surface.getCanvas(),new Viewport(480,320,1),new FrameTime(1,0,0),Map.of("volume",.35));var r=frame.box(new NodeId("editable/page/field")).border();float x=r.x()+r.width()/2,y=r.y()+r.height()/2;
                impl.input=()->{impl.view.feedback("BUTTON");impl.view.feedback("BUTTON");};session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));assertEquals(1,outputs.size());assertEquals(.35f,outputs.get(0).volume());assertEquals(0,commits[0]);
                assertThrows(IllegalStateException.class,()->impl.view.feedback("BUTTON"));impl.input=()->impl.view.feedback("unregistered");session.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));assertThrows(IllegalArgumentException.class,()->session.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0)));assertEquals(1,outputs.size());
            }
        }
    }
}
