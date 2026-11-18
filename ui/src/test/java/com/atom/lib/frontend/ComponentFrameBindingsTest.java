package com.atom.lib.frontend;

import com.atom.lib.controls.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.layout.*;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComponentFrameBindingsTest {
    static final Binding BINDING=new Binding(Component.class.getName(),"editable-test/1");
    static class Component implements StrictPageSession.Implementation {
        StrictPageSession.FrameBindings created,updated;
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView content){throw new AssertionError("new host adapter requires its declared frame inputs");}
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView content,StrictPageSession.FrameBindings frame){
            created=frame;return new HostedControl(id,new HostedControl.Adapter(){
                public LayoutNode node(NodeId id,LayoutStyle outer){return new LayoutNode(id,outer.size(Length.points(100),Length.points(30)),List.of(),null,0);}
                public boolean enabled(){return true;}public boolean press(com.atom.lib.input.PointerEvent e,PresentationFrame f){return false;}
                public ControlObservation observation(){return new ControlObservation(true,false,false,0,0,0);}
                public void paint(Canvas canvas,PresentationFrame frame,ControlVisual visual){}
            });
        }
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView content){throw new AssertionError("new host adapter requires its declared frame inputs");}
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView content,StrictPageSession.FrameBindings frame){updated=frame;}
    }
    @Test void defaultsActualStatesAndSnapshotsReachTheBoundComponentWithoutGlobalMutableStyle(){
        var d=EditableContentTest.design();var recipe=d.recipes().get("field");
        var model=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),Set.of("enabled","light"),Map.of("accent",new Parameter(0,1,.6,Unit.RATIO)),d.families(),d.roles(),Map.of("field",new Recipe(recipe.scope(),recipe.role(),recipe.bindings(),BINDING,recipe.slots(),recipe.scenarios())),d.layouts(),d.assets(),d.pages());
        byte[] bytes=ContractCodec.encode(model);var handle=ContractRegistry.load(bytes,ContractRegistry.manifest(bytes,bytes),Set.of(d.profile())).page("page");
        var data=new StrictPage.Content(Map.of("label",new StrictPage.Text("value"),"number",new StrictPage.Number(7),"checked",new StrictPage.BooleanValue(false)),true);var definition=StrictPage.builder(handle).component("one",handle.recipe("field"),data).build();var component=new Component();
        try(var canvas=Surface.makeRasterN32Premul(240,120);var session=new StrictPageSession(definition,Map.of(BINDING,component))){
            assertEquals(.6,component.created.parameters().get("accent"));assertTrue(component.created.states().get("enabled"));assertFalse(component.created.states().get("light"));
            var parameters=new HashMap<>(Map.of("accent",.25));var states=new HashMap<>(Map.of("light",true));
            session.render(canvas.getCanvas(),new Viewport(240,120,1),new FrameTime(1,0,0),parameters,states);var first=component.updated;
            parameters.put("accent",.9);states.put("light",false);assertEquals(.25,first.parameters().get("accent"));assertTrue(first.states().get("light"));
            assertThrows(UnsupportedOperationException.class,()->first.parameters().put("accent",.1));
            session.render(canvas.getCanvas(),new Viewport(240,120,1),new FrameTime(2,16_000_000,.016),parameters,states);
            assertEquals(.9,component.updated.parameters().get("accent"));assertFalse(component.updated.states().get("light"));assertEquals(.25,first.parameters().get("accent"));
        }
    }
}
