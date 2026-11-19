package com.atom.lib.frontend;

import com.atom.lib.controls.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import com.atom.lib.geometry.Rect;
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
    /** Host implementation for a structural recipe that declares a group surface. */
    public interface GroupImplementation extends Implementation {
        void paintGroup(Canvas canvas, PresentationFrame frame, NodeId group, List<NodeId> children,
                        Value material, FrameBindings bindings);

        @Override default Control create(NodeId id, Map<Aspect,Value> values, ContentView content) {
            throw new IllegalStateException("STRUCTURAL_RECIPE " + id);
        }

        @Override default void update(Control control, Map<Aspect,Value> values, ContentView content) {
            throw new IllegalStateException("STRUCTURAL_RECIPE " + control.id());
        }
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
    private final ScrollViewport scroll;
    private final Map<String, GroupImplementation> groupImplementations;
    private List<Control> displayedControls;
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
        var groups = new LinkedHashMap<String, GroupImplementation>();
        bindGroupImplementations(page.elements(), fixed, groups);
        groupImplementations = Map.copyOf(groups);
        Map<String,Mounted> instances=new LinkedHashMap<>();
        for(var entry:page.entries()){
            var impl=fixed.get(entry.recipe().implementation());var content=new ContentView(this,entry.recipe(),entry.content());
            NodeId id=nodeId(entry.id());
            var initialStates=statesFor(content,Map.of());
            Control control=Objects.requireNonNull(impl.create(id,entry.recipe().bindings(Map.of(),initialStates),content,new FrameBindings(page.contract().parameters(Map.of()),initialStates)));if(!id.equals(control.id()))throw new IllegalArgumentException("COMPONENT_ID_MISMATCH "+id);
            instances.put(entry.id(),new Mounted(entry,impl,control,content));
        }
        mounted=Collections.unmodifiableMap(instances);
        displayedControls=instances.values().stream().map(Mounted::control).toList();
        session=new UiSession(displayedControls);
        layout=new YogaLayoutEngine();
        scroll=page.scrollGroup()==null?null:new ScrollViewport();
        if(scroll!=null)session.scrollViewport(scroll);
    }
    private static void bindGroupImplementations(List<StrictPage.Element> elements, Map<Binding, Implementation> catalog,
                                                 Map<String, GroupImplementation> groups) {
        for (var element : elements) {
            if (!(element instanceof StrictPage.Group group)) continue;
            Rule material = group.recipe().role().requirements().get(Aspect.MATERIAL).rule();
            if (material == Rule.REQUIRED || material == Rule.CONDITIONAL) {
                Binding binding = group.recipe().implementation();
                var implementation = catalog.get(binding);
                if (!(implementation instanceof GroupImplementation painter)
                        || !painter.getClass().getName().equals(binding.implementation())) {
                    throw new IllegalArgumentException("MISSING_GROUP_IMPLEMENTATION " + group.id() + " expected=" + binding);
                }
                groups.put(group.id(), painter);
            }
            bindGroupImplementations(group.children(), catalog, groups);
        }
    }

    /** Canvas is already in logical UI units. The host owns the single UI-to-device conversion. */
    public PresentationFrame render(Canvas logicalCanvas,Viewport viewport,FrameTime time,Map<String,Double> parameters){
        return render(logicalCanvas,viewport,time,parameters,Map.of());
    }
    public PresentationFrame render(Canvas logicalCanvas,Viewport viewport,FrameTime time,Map<String,Double> parameters,Map<String,Boolean> states){
        return renderIn(logicalCanvas, viewport, viewport.bounds(), time, parameters, states);
    }

    /** Composes the body in the host's logical coordinates so pointer, clipping and paint share the same origin. */
    public PresentationFrame renderIn(Canvas logicalCanvas, Viewport screen, Rect region, FrameTime time,
                                      Map<String,Double> parameters, Map<String,Boolean> states) {
        requireOpen();
        if (!region.intersect(screen.bounds()).equals(region)) {
            throw new IllegalArgumentException("LAYOUT_REGION outside host viewport");
        }
        var viewport = new Viewport(region.width(), region.height(), screen.pixelsPerUnit());
        if(time.frameId()<=lastFrame)throw new IllegalArgumentException("FRAME_CLOCK repeated or reversed frame id");
        if(time.nowNanos()<0)throw new IllegalArgumentException("FRAME_CLOCK negative time");
        Map<String,Double> snapshot=page.contract().parameters(parameters);Map<String,Boolean> stateSnapshot=Map.copyOf(states);
        if(!page.contract().namedStates().containsAll(stateSnapshot.keySet()))throw new IllegalArgumentException("UNKNOWN_STATE "+stateSnapshot.keySet());
        var rule=page.contract().layout(snapshot);session.prepare(screen);
        List<Mounted> visible=new ArrayList<>();
        var nodes=nodes(page.elements(),snapshot,stateSnapshot,viewport.width(),visible);
        float width=Math.min(viewport.width(),Math.max(rule.minimumWidth(),Math.min(rule.maximumWidth(),viewport.width())));
        LayoutStyle style=style(rule,false,false).size(Length.points(width),scroll==null?Length.auto():Length.points(viewport.height()));
        var tree=new LayoutTree(new LayoutNode(nodeId("root"),style,nodes,null,0));
        var measured=layout.layout(tree,viewport);
        var frame=scroll==null?PresentationFrame.create(tree,measured,viewport,Map.of(),new PresentationFrame.Offset(0,0))
                :scroll.presentGroup(tree,measured,viewport,nodeId(page.scrollGroup()));
        if (region.x() != 0 || region.y() != 0) frame = frame.translated(region.x(), region.y());
        var nextControls=visible.stream().map(Mounted::control).toList();
        if(!displayedControls.equals(nextControls)){
            session.setControls(nextControls);
            displayedControls=nextControls;
        }
        if (groupImplementations.isEmpty()) {
            session.paint(logicalCanvas,frame,screen,time,true);
        } else {
            var groupPaint = new LinkedHashMap<NodeId, java.util.function.Consumer<Canvas>>();
            groupPaint(page.elements(), frame, new FrameBindings(snapshot, stateSnapshot), groupPaint);
            session.paintGroups(logicalCanvas, frame, screen, time, groupPaint);
        }
        Map<String,ControlObservation> observations=new LinkedHashMap<>();
        for(Mounted component:visible)observations.put(component.entry().id(),component.control().observation());
        frameObservations=Map.copyOf(observations);
        lastFrame=time.frameId();lastMillis=time.nowNanos()/1_000_000;displayedDensity=viewport.pixelsPerUnit();frameParameters=snapshot;frameStates=stateSnapshot;return frame;
    }
    private void groupPaint(List<StrictPage.Element> elements, PresentationFrame frame, FrameBindings bindings,
                            Map<NodeId, java.util.function.Consumer<Canvas>> painters) {
        for (var element : elements) {
            if (!(element instanceof StrictPage.Group group)) continue;
            NodeId id = nodeId(group.id());
            if (!frame.contains(id)) continue;
            var painter = groupImplementations.get(group.id());
            if (painter != null) {
                Value material = group.recipe().bindings(bindings.parameters(), bindings.states()).get(Aspect.MATERIAL);
                if (material != null) {
                    var children = group.children().stream()
                            .map(child -> nodeId(child instanceof StrictPage.Entry entry ? entry.id() : ((StrictPage.Group)child).id()))
                            .filter(frame::contains).toList();
                    painters.put(id, canvas -> painter.paintGroup(canvas, frame, id, children, material, bindings));
                }
            }
            groupPaint(group.children(), frame, bindings, painters);
        }
    }

    /**
     * A screen with an existing displayed layout can route its measured slots through
     * the same approved controls. The screen retains its pointer transforms and layer order.
     */
    public final class EmbeddedBody {
        private final Map<String, PresentationFrame> slots = new HashMap<>();
        private final Map<String, StrictPage.Group> groups = new HashMap<>();
        private final Map<String, String> owners = new HashMap<>();
        private final Set<String> paintedGroups = new HashSet<>();
        private FrameTime time;
        private String currentGroup;

        private EmbeddedBody() {
            if (scroll != null || mounted.values().stream().anyMatch(m -> !(m.control() instanceof HostedControl))) {
                throw new IllegalArgumentException("EMBEDDED_BODY requires hosted controls and host-owned scrolling");
            }
            for (var element : page.elements()) {
                if (!(element instanceof StrictPage.Group group) || !groupImplementations.containsKey(group.id())) {
                    throw new IllegalArgumentException("EMBEDDED_BODY requires a bound group for every hosted slot");
                }
                groups.put(group.id(), group);
                for (var child : group.children()) {
                    if (!(child instanceof StrictPage.Entry entry)) {
                        throw new IllegalArgumentException("EMBEDDED_BODY requires direct group children");
                    }
                    owners.put(entry.id(), group.id());
                }
            }
        }

        public void begin(FrameTime next, FrameBindings bindings) {
            requireOpen();
            if (dispatching || next.frameId() <= lastFrame || next.nowNanos() < 0) {
                throw new IllegalArgumentException("FRAME_CLOCK invalid embedded frame");
            }
            var snapshot = page.contract().parameters(bindings.parameters());
            if (!page.contract().namedStates().containsAll(bindings.states().keySet())) {
                throw new IllegalArgumentException("UNKNOWN_STATE " + bindings.states().keySet());
            }
            time = next;
            lastFrame = next.frameId();
            lastMillis = next.nowNanos() / 1_000_000;
            frameParameters = snapshot;
            frameStates = bindings.states();
            slots.clear();
            paintedGroups.clear();
            currentGroup = null;
            for (var entry : mounted.values()) {
                var states = statesFor(entry.content(), frameStates);
                entry.implementation().update(entry.control(), entry.entry().recipe().bindings(snapshot, states),
                        entry.content(), new FrameBindings(snapshot, states));
            }
        }

        public void paintGroup(Canvas canvas, String id, Rect bounds) {
            requireOpen();
            var group = groups.get(id);
            if (group == null || !paintedGroups.add(id)) throw new IllegalArgumentException("INVALID_HOSTED_GROUP " + id);
            var material = group.recipe().bindings(frameParameters, frameStates).get(Aspect.MATERIAL);
            if (material == null) throw new IllegalArgumentException("MISSING_HOSTED_GROUP_MATERIAL " + id);
            NodeId node = nodeId(id);
            groupImplementations.get(id).paintGroup(canvas, PresentationFrame.hosted(time.frameId(), node, bounds),
                    node, List.of(), material, new FrameBindings(frameParameters, frameStates));
            currentGroup = id;
        }

        public void paint(Canvas canvas, String id, Rect bounds, float x, float y, boolean focused, boolean pressed) {
            requireOpen();
            var adapter = adapter(id);
            if (!Objects.equals(owners.get(id), currentGroup)) throw new IllegalArgumentException("HOSTED_SLOT_GROUP " + id);
            var frame = PresentationFrame.hosted(time.frameId(), nodeId(id), bounds);
            if (slots.putIfAbsent(id, frame) != null) throw new IllegalStateException("DUPLICATE_HOSTED_SLOT " + id);
            adapter.hover(x, y, frame);
            adapter.advance(time, frameStates.getOrDefault("decorativeMotion", true), bounds.contains(x, y), pressed);
            adapter.paint(canvas, frame, new ControlVisual(focused, 0, pressed ? 1 : 0, 0, 1));
        }

        public boolean pointer(String id, PointerEvent event) {
            var frame = slots.get(id);
            if (frame == null) return false;
            return dispatch(() -> {
                var adapter = adapter(id);
                boolean accepted = switch (event.type()) {
                    case PRESS -> adapter.enabled() && adapter.acceptsPointer(event.x(), event.y(), frame)
                            && adapter.press(event, frame);
                    case MOVE -> { adapter.drag(event, frame); yield true; }
                    case RELEASE -> { adapter.release(event, frame); yield true; }
                    case CANCEL -> { adapter.cancel(CancelReason.FOCUS_LOST); yield true; }
                };
                return accepted ? InputResult.CONSUMED : InputResult.PASS;
            }) == InputResult.CONSUMED;
        }

        public boolean key(String id, KeyEvent event) {
            return dispatch(() -> adapter(id).key(event) ? InputResult.CONSUMED : InputResult.PASS) == InputResult.CONSUMED;
        }

        public boolean text(String id, String value) {
            return dispatch(() -> adapter(id).text(value) ? InputResult.CONSUMED : InputResult.PASS) == InputResult.CONSUMED;
        }

        public void focus(String id, boolean focused) { adapter(id).hostFocus(focused); }
        public boolean capturesEditing(String id) { return adapter(id).capturesEditingInput(); }
        public boolean interacting(String id) { return adapter(id).interacting(); }

        public void clear() {
            slots.clear();
            for (String id : mounted.keySet()) adapter(id).cancel(CancelReason.FOCUS_LOST);
        }

        private HostedControl.Adapter adapter(String id) {
            requireOpen();
            var entry = mounted.get(id);
            if (entry == null) throw new IllegalArgumentException("UNKNOWN_COMPONENT " + id);
            return ((HostedControl) entry.control()).adapter(HostedControl.Adapter.class);
        }
    }

    public EmbeddedBody embeddedBody() {
        requireOpen();
        return new EmbeddedBody();
    }

    private NodeId nodeId(String id){
        return new NodeId(page.contract().namespace()+"/"+page.contract().id()+"/"+id);
    }
    private static LayoutStyle style(ContractRegistry.ResolvedLayout rule,boolean clip,boolean scroll){
        return (rule.axis()==Axis.COLUMN?LayoutStyle.column():LayoutStyle.row())
                .spacing(rule.padding(),rule.gap())
                .behavior(rule.axis()==Axis.WRAP,false,clip,scroll);
    }
    private List<LayoutNode> nodes(List<StrictPage.Element> elements,Map<String,Double> parameters,
                                   Map<String,Boolean> states,float availableWidth,List<Mounted> visible){
        List<LayoutNode> nodes=new ArrayList<>();
        for(var element:elements){
            if(element instanceof StrictPage.Group group){
                var requirement=group.recipe().role().requirements().get(Aspect.LAYOUT);
                if(requirement.rule()==Rule.CONDITIONAL&&!DesignResolver.evaluate(requirement.condition(),states))continue;
                var binding=page.contract().groupLayout(group.recipe(),parameters);
                var rule=binding.layout();
                float width=Math.min(availableWidth,rule.maximumWidth());
                var style=style(rule,binding.clip(),binding.scroll())
                        .limits(Math.min(width,rule.minimumWidth()),0,width,Float.POSITIVE_INFINITY)
                        .flex(binding.scroll()?1:0,binding.scroll()?1:0,binding.scroll()?Length.points(0):Length.auto());
                nodes.add(new LayoutNode(nodeId(group.id()),style,nodes(group.children(),parameters,states,width,visible),null,0));
            }else{
                var mountedComponent=mounted.get(((StrictPage.Entry)element).id());
                var componentStates=statesFor(mountedComponent.content(),states);
                mountedComponent.implementation().update(mountedComponent.control(),
                        mountedComponent.entry().recipe().bindings(parameters,componentStates),
                        mountedComponent.content(),new FrameBindings(parameters,componentStates));
                nodes.add(mountedComponent.control().node(LayoutStyle.column().flex(0,0,Length.auto())));
                visible.add(mountedComponent);
            }
        }
        return nodes;
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
    public InputResult wheel(float x,float y,float dx,float dy){return dispatch(()->session.wheel(x,y,dx,dy));}
    /** Raw MC GUI coordinates enter here; never apply contentScale again in the host. */
    public InputResult pointerGui(PointerEvent.Type type,GuiCoordinates.GuiPoint point,int button,double guiScale){requireOpen();if(lastFrame==Long.MIN_VALUE)return InputResult.PASS;var logical=new GuiCoordinates(guiScale,displayedDensity).toLogical(point);return pointer(new PointerEvent(type,logical.x(),logical.y(),button));}
    public InputResult key(KeyEvent event){return dispatch(()->session.key(event));}
    public InputResult character(int codePoint){return dispatch(()->session.character(codePoint));}
    public InputResult text(String value){return dispatch(()->session.text(value));}
    public void cancel(CancelReason reason){requireOpen();session.cancel(reason);}
    public void update(String id,StrictPage.Content content){requireOpen();Mounted m=mounted.get(id);if(m==null)throw new IllegalArgumentException("UNKNOWN_COMPONENT "+id);StrictPage.validate(m.entry().recipe(),content);m.content().current=content;}
    /**
     * Rebinds business content without restarting motion or replacing controls.
     * The registry, recipe assignments, order and group structure remain fixed.
     */
    public void update(StrictPage next) {
        requireOpen();
        page.requireSameStructure(Objects.requireNonNull(next));
        for (var entry : next.entries()) mounted.get(entry.id()).content().current = entry.content();
    }
    private void requireOpen(){if(closed)throw new IllegalStateException("Strict page session closed");}
    public void close(){if(closed)return;closed=true;if(!dispatching)sounds.cancel();try{session.close();}finally{layout.close();}}
}
