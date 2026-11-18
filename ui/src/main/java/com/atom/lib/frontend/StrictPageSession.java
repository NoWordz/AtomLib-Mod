package com.atom.lib.frontend;

import com.atom.lib.controls.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import com.atom.lib.layout.internal.YogaLayoutEngine;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Canvas;
import java.util.*;

/** Host-owned bridge to existing controls, Yoga and displayed-frame input. Never a page factory callback. */
public final class StrictPageSession implements AutoCloseable {
    public interface Implementation {
        Control create(NodeId id,Map<Aspect,Value> values,ContentView content);
        void update(Control control,Map<Aspect,Value> values,ContentView content);
        default Control create(NodeId id,Map<Aspect,Value> values,ContentView content,FrameBindings frame){return create(id,values,content);}
        default void update(Control control,Map<Aspect,Value> values,ContentView content,FrameBindings frame){update(control,values,content);}
    }
    /** Host components receive immutable declared inputs, never a mutable process-wide current appearance. */
    public record FrameBindings(Map<String,Double> parameters,Map<String,Boolean> states){public FrameBindings{parameters=Map.copyOf(parameters);states=Map.copyOf(states);}}
    /** Stable business binding; callbacks read current content rather than a previous frame's closure. */
    public static final class ContentView {
        private final StrictPageSession owner;private final ContractRegistry.RecipeHandle recipe;private StrictPage.Content current;
        private ContentView(StrictPageSession owner,ContractRegistry.RecipeHandle recipe,StrictPage.Content content){this.owner=owner;this.recipe=recipe;current=content;}
        public StrictPage.Content snapshot(){return current;}
        public boolean activate(String slot){return owner.activate(this,slot);}
        public boolean commitText(String slot,String value){Objects.requireNonNull(value);return owner.commit(this,slot,TextEdit.of(value));}
        public boolean commitNumber(String slot,double value){if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid business number");return owner.commit(this,slot,NumberEdit.of(value));}
        public boolean commitBoolean(String slot,boolean value){return owner.commit(this,slot,BooleanEdit.of(value));}
        public void feedback(String event){owner.feedback(this,event);}
    }
    private record Mounted(StrictPage.Entry entry,Implementation implementation,Control control,ContentView content){}
    private final StrictPage page;private final Map<String,Mounted> mounted;private final UiSession session;private final YogaLayoutEngine layout;
    private final ContractSoundSession sounds;
    private boolean closed,dispatching;private long lastFrame=Long.MIN_VALUE,lastMillis,actionSequence;private float displayedDensity;
    private Map<String,Double> frameParameters=Map.of();private Map<String,Boolean> frameStates=Map.of();
    private Map<String,ControlObservation> frameObservations=Map.of();
    private ContractSoundSession.Result lastSound=ContractSoundSession.Result.SILENT;
    public StrictPageSession(StrictPage page,Map<Binding,Implementation> implementations){this(page,implementations,null);}
    public StrictPageSession(StrictPage page,Map<Binding,Implementation> implementations,UiSoundSink output){
        this.page=Objects.requireNonNull(page);Map<Binding,Implementation> fixed=Map.copyOf(implementations);
        if(output==null&&page.entries().stream().anyMatch(e->e.recipe().bindings(Map.of()).get(Aspect.SOUND) instanceof SoundValue sound&&!sound.asset().isEmpty()))throw new IllegalArgumentException("MISSING_SOUND_OUTPUT audible contract needs a host adapter");
        UiSoundSink sink=output!=null?output:new UiSoundSink(){public boolean supports(String channel){return false;}public boolean available(String resource){return false;}public void play(UiSoundIntent intent){throw new IllegalStateException("MISSING_SOUND_OUTPUT");}};
        sounds=new ContractSoundSession(page.contract(),sink);
        for(var entry:page.entries()){
            Binding b=entry.recipe().implementation();Implementation impl=fixed.get(b);
            if(impl==null)throw new IllegalArgumentException("MISSING_IMPLEMENTATION page="+page.contract().id()+" recipe="+entry.recipe().id()+" expected="+b);
            if(!impl.getClass().getName().equals(b.implementation()))throw new IllegalArgumentException("IMPLEMENTATION_MISMATCH expected="+b.implementation()+" actual="+impl.getClass().getName());
        }
        Map<String,Mounted> instances=new LinkedHashMap<>();
        for(var entry:page.entries()){
            var impl=fixed.get(entry.recipe().implementation());var content=new ContentView(this,entry.recipe(),entry.content());
            NodeId id=new NodeId(page.contract().namespace()+"/"+page.contract().id()+"/"+entry.id());
            var initialStates=statesFor(content,Map.of());
            Control control=Objects.requireNonNull(impl.create(id,entry.recipe().bindings(Map.of(),initialStates),content,new FrameBindings(page.contract().parameters(Map.of()),initialStates)));if(!id.equals(control.id()))throw new IllegalArgumentException("COMPONENT_ID_MISMATCH "+id);
            instances.put(entry.id(),new Mounted(entry,impl,control,content));
        }
        mounted=Collections.unmodifiableMap(instances);session=new UiSession(instances.values().stream().map(Mounted::control).toList());layout=new YogaLayoutEngine();
    }
    /** Canvas is already in logical UI units. The host owns the single UI-to-device conversion. */
    public PresentationFrame render(Canvas logicalCanvas,Viewport viewport,FrameTime time,Map<String,Double> parameters){
        return render(logicalCanvas,viewport,time,parameters,Map.of());
    }
    public PresentationFrame render(Canvas logicalCanvas,Viewport viewport,FrameTime time,Map<String,Double> parameters,Map<String,Boolean> states){
        requireOpen();if(time.frameId()<=lastFrame)throw new IllegalArgumentException("FRAME_CLOCK repeated or reversed frame id");
        if(time.nowNanos()<0)throw new IllegalArgumentException("FRAME_CLOCK negative time");
        Map<String,Double> snapshot=page.contract().parameters(parameters);Map<String,Boolean> stateSnapshot=Map.copyOf(states);
        if(!page.contract().namedStates().containsAll(stateSnapshot.keySet()))throw new IllegalArgumentException("UNKNOWN_STATE "+stateSnapshot.keySet());
        var rule=page.contract().layout(snapshot);session.prepare(viewport);
        var nodes=new ArrayList<LayoutNode>();
        for(var m:mounted.values()){var componentStates=statesFor(m.content(),stateSnapshot);m.implementation().update(m.control(),m.entry().recipe().bindings(snapshot,componentStates),m.content(),new FrameBindings(snapshot,componentStates));nodes.add(m.control().node(LayoutStyle.column().flex(0,0,Length.auto())));}
        float width=Math.min(viewport.width(),Math.max(rule.minimumWidth(),Math.min(rule.maximumWidth(),viewport.width())));
        LayoutStyle style=(rule.axis()==Axis.COLUMN?LayoutStyle.column():LayoutStyle.row()).size(Length.points(width),Length.auto()).spacing(rule.padding(),rule.gap()).behavior(rule.axis()==Axis.WRAP,false,false,false);
        var tree=new LayoutTree(new LayoutNode(new NodeId(page.contract().namespace()+"/"+page.contract().id()+"/root"),style,nodes,null,0));
        var frame=PresentationFrame.create(tree,layout.layout(tree,viewport),viewport,Map.of(),new PresentationFrame.Offset(0,0));
        session.paint(logicalCanvas,frame,viewport,time,true);
        Map<String,ControlObservation> observations=new LinkedHashMap<>();mounted.forEach((id,m)->observations.put(id,m.control().observation()));frameObservations=Map.copyOf(observations);
        lastFrame=time.frameId();lastMillis=time.nowNanos()/1_000_000;displayedDensity=viewport.pixelsPerUnit();frameParameters=snapshot;frameStates=stateSnapshot;return frame;
    }
    private boolean activate(ContentView view,String slot){
        if(!dispatching)throw new IllegalStateException("NO_INPUT_TRANSACTION component activation outside host input");
        if(!view.current.enabled())return false;boolean committed=((StrictPage.Action)view.current.slots().get(slot)).commit().getAsBoolean();
        if(committed){var states=statesFor(view,frameStates);Value sound=view.recipe.bindings(frameParameters,states).get(Aspect.SOUND);if(sound instanceof SoundValue spec)sounds.request(view.recipe,spec.event(),frameParameters,states);}
        return committed;
    }
    private void feedback(ContentView view,String event){
        if(!dispatching)throw new IllegalStateException("NO_INPUT_TRANSACTION feedback outside host input");
        if(!view.current.enabled())return;
        var rule=view.recipe.role().requirements().get(Aspect.SOUND);
        if(rule.rule()==Rule.NOT_APPLICABLE||rule.rule()==Rule.UNSUPPORTED)throw new IllegalArgumentException("ROLE_CAPABILITY sound not declared for recipe="+view.recipe.id());
        sounds.request(view.recipe,event,frameParameters,statesFor(view,frameStates));
    }
    private interface Edit {boolean accept(StrictPage.Item previous);StrictPage.Item updated(StrictPage.Item previous);}
    private record TextEdit(String value) implements Edit {
        static TextEdit of(String value){return new TextEdit(value);}
        public boolean accept(StrictPage.Item item){if(!(item instanceof StrictPage.TextInput input))throw new IllegalArgumentException("CONTENT_CAPABILITY_MISSING editable text");return input.commit().test(value);}
        public StrictPage.Item updated(StrictPage.Item item){return new StrictPage.TextInput(value,((StrictPage.TextInput)item).commit());}
    }
    private record NumberEdit(double value) implements Edit {
        static NumberEdit of(double value){return new NumberEdit(value);}
        public boolean accept(StrictPage.Item item){if(!(item instanceof StrictPage.NumberInput input))throw new IllegalArgumentException("CONTENT_CAPABILITY_MISSING editable number");return input.commit().test(value);}
        public StrictPage.Item updated(StrictPage.Item item){return new StrictPage.NumberInput(value,((StrictPage.NumberInput)item).commit());}
    }
    private record BooleanEdit(boolean value) implements Edit {
        static BooleanEdit of(boolean value){return new BooleanEdit(value);}
        public boolean accept(StrictPage.Item item){if(!(item instanceof StrictPage.BooleanInput input))throw new IllegalArgumentException("CONTENT_CAPABILITY_MISSING editable boolean");return input.commit().test(value);}
        public StrictPage.Item updated(StrictPage.Item item){return new StrictPage.BooleanInput(value,((StrictPage.BooleanInput)item).commit());}
    }
    /** Data commits stay silent; the approved component owns any separately declared focus/commit feedback. */
    private boolean commit(ContentView view,String slot,Edit edit){
        if(!dispatching)throw new IllegalStateException("NO_INPUT_TRANSACTION data commit outside host input");
        if(!view.current.enabled())return false;
        var rule=view.recipe.role().requirements().get(Aspect.EDITING);
        if(rule.rule()==Rule.NOT_APPLICABLE||rule.rule()==Rule.UNSUPPORTED)throw new IllegalArgumentException("ROLE_CAPABILITY editing not declared for recipe="+view.recipe.id());
        if(rule.rule()==Rule.CONDITIONAL&&!DesignResolver.evaluate(rule.condition(),statesFor(view,frameStates)))return false;
        StrictPage.Item previous=view.current.slots().get(slot);
        if(previous==null)throw new IllegalArgumentException("CONTENT_CAPABILITY_MISSING slot="+slot);
        if(!edit.accept(previous))return false;
        // A synchronous backend may already have supplied normalized content. Preserve that authoritative update.
        if(view.current.slots().get(slot)==previous){Map<String,StrictPage.Item> next=new HashMap<>(view.current.slots());next.put(slot,edit.updated(previous));view.current=new StrictPage.Content(next,view.current.enabled());}
        return true;
    }
    private InputResult dispatch(java.util.function.Supplier<InputResult> input){
        requireOpen();if(dispatching)throw new IllegalStateException("REENTRANT_INPUT");sounds.begin(++actionSequence,lastMillis);dispatching=true;
        try{InputResult result=input.get();lastSound=sounds.end();return result;}catch(RuntimeException|Error e){sounds.cancel();throw e;}finally{dispatching=false;}
    }
    private Map<String,Boolean> statesFor(ContentView view,Map<String,Boolean> frame){Map<String,Boolean> values=new HashMap<>();for(String name:page.contract().namedStates())values.put(name,frame.getOrDefault(name,false));if(values.containsKey("enabled"))values.put("enabled",view.current.enabled());return Map.copyOf(values);}
    public ContractSoundSession.Result lastSoundResult(){return lastSound;}
    public Map<String,ControlObservation> observations(){requireOpen();return frameObservations;}
    public InputResult pointer(PointerEvent event){return dispatch(()->{if(event.type()==PointerEvent.Type.MOVE)session.hover(event.x(),event.y());return session.pointer(event);});}
    /** Raw MC GUI coordinates enter here; never apply contentScale again in the host. */
    public InputResult pointerGui(PointerEvent.Type type,GuiCoordinates.GuiPoint point,int button,double guiScale){requireOpen();if(lastFrame==Long.MIN_VALUE)return InputResult.PASS;var logical=new GuiCoordinates(guiScale,displayedDensity).toLogical(point);return pointer(new PointerEvent(type,logical.x(),logical.y(),button));}
    public InputResult key(KeyEvent event){return dispatch(()->session.key(event));}
    public InputResult text(String value){return dispatch(()->session.text(value));}
    public void cancel(CancelReason reason){requireOpen();session.cancel(reason);}
    public void update(String id,StrictPage.Content content){requireOpen();Mounted m=mounted.get(id);if(m==null)throw new IllegalArgumentException("UNKNOWN_COMPONENT "+id);StrictPage.validate(m.entry().recipe(),content);m.content().current=content;}
    private void requireOpen(){if(closed)throw new IllegalStateException("Strict page session closed");}
    public void close(){if(closed)return;closed=true;if(!dispatching)sounds.cancel();try{session.close();}finally{layout.close();}}
}
