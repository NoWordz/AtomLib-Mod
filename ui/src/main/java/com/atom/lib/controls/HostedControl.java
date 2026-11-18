package com.atom.lib.controls;

import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Canvas;
import java.util.*;

/** Approved host component adapter. Page definitions never receive this capability or its paint callbacks. */
public final class HostedControl extends Control {
    public interface Adapter {
        LayoutNode node(NodeId id,LayoutStyle outer);
        boolean enabled();
        boolean press(PointerEvent event,PresentationFrame displayed);
        default void hover(float x,float y,PresentationFrame displayed){}
        default void drag(PointerEvent event,PresentationFrame displayed){}
        default void release(PointerEvent event,PresentationFrame displayed){}
        default void cancel(CancelReason reason){}
        default boolean key(KeyEvent event){return false;}
        default boolean text(String text){return false;}
        default boolean interacting(){return false;}
        default void advance(FrameTime time,boolean motion,boolean hovered,boolean pressed){}
        /** Actual rendered progress; the checker does not accept declared durations in its place. */
        ControlObservation observation();
        void paint(Canvas canvas,PresentationFrame displayed,ControlVisual visual);
        default Set<NodeId> slots(NodeId id){return Set.of(id);}
    }
    private final Adapter adapter;private boolean pointer;
    public HostedControl(NodeId id,Adapter adapter){super(id);this.adapter=Objects.requireNonNull(adapter);}
    /** Only the approved host implementation updates its own component state. */
    public <T extends Adapter> T adapter(Class<T> type){return type.cast(adapter);}
    public LayoutNode node(LayoutStyle outer){var node=Objects.requireNonNull(adapter.node(id(),outer));if(!node.id().equals(id()))throw new IllegalArgumentException("COMPONENT_ID_MISMATCH "+id());return node;}
    Set<NodeId> slots(){var ids=Set.copyOf(adapter.slots(id()));if(!ids.contains(id()))throw new IllegalArgumentException("COMPONENT_ID_MISMATCH missing root "+id());return ids;}
    boolean enabled(){return adapter.enabled();}
    boolean interacting(){return pointer||adapter.interacting();}
    /** UiSession paints this component through the matching Canvas scale, so input uses its inverse once. */
    private PointerEvent paintingEvent(PointerEvent event,PresentationFrame displayed){float scale=displayed.renderScale(id());return new PointerEvent(event.type(),event.x()/scale,event.y()/scale,event.button());}
    void hoverPointer(float x,float y,PresentationFrame displayed){if(displayed==null||!displayed.contains(id()))return;float scale=displayed.renderScale(id());adapter.hover(x/scale,y/scale,displayed.forPainting(id()));}
    boolean press(PointerEvent event,PresentationFrame displayed){boolean accepted=adapter.press(paintingEvent(event,displayed),displayed.forPainting(id()));pointer=accepted;pressed=accepted;return accepted;}
    void drag(PointerEvent event,PresentationFrame displayed){if(pointer)adapter.drag(paintingEvent(event,displayed),displayed.forPainting(id()));}
    void release(PointerEvent event,PresentationFrame displayed){try{if(pointer)adapter.release(paintingEvent(event,displayed),displayed.forPainting(id()));}finally{pointer=false;pressed=false;}}
    void cancel(CancelReason reason){try{adapter.cancel(reason);}finally{pointer=false;pressed=false;}}
    boolean key(KeyEvent event){return adapter.key(event);}
    boolean text(String text){return adapter.text(text);}
    void paint(Canvas canvas,PresentationFrame frame,ControlVisual visual){adapter.paint(canvas,frame,visual);}
    void advance(FrameTime time,boolean motion){
        adapter.advance(time,motion,hovered,pressed);var observed=Objects.requireNonNull(adapter.observation());
        hover=weight(observed.hover());press=weight(observed.press());position=weight(observed.position());
    }
    private static float weight(float value){if(!Float.isFinite(value))throw new IllegalArgumentException("Invalid host component observation");return Math.max(0,Math.min(1,value));}
    @Override protected ControlObservation snapshotObservation(){var actual=Objects.requireNonNull(adapter.observation());return new ControlObservation(available(),actual.hovered(),actual.pressed(),actual.hover(),actual.press(),actual.position());}
    /** Called by the bound implementation after a business update; disabled capture is cancelled immediately. */
    public void refresh(){if(!adapter.enabled())terminate(CancelReason.DISABLED);}
}
