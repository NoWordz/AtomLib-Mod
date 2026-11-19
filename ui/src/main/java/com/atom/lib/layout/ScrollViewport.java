// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.layout;
import com.atom.lib.geometry.*;
import java.util.*;
/** Single vertical viewport. Natural measurement is kept separate from its displayed clip. */
public final class ScrollViewport {
    private Rect bounds=new Rect(0,0,0,0);private float offset,extent;private Set<NodeId> descendants=Set.of();
    record State(Rect bounds,float extent,float offset,Set<NodeId> descendants){}
    /** UiSession publishes the state carried by exactly the frame that painted successfully. */
    public void publish(PresentationFrame frame){var s=frame.scrollState(this);if(s!=null){bounds=s.bounds();extent=s.extent();offset=s.offset();descendants=s.descendants();}}
    public Rect thumb(PresentationFrame frame){var s=frame.scrollState(this);return s==null?thumb():thumb(s.bounds(),s.extent(),s.offset());}
    private boolean hovered;
    private SmoothScroll smoothing;
    private com.atom.lib.motion.FrameTime frameTime;
    private com.atom.lib.motion.MotionSpec motion;
    /** Safe-session path calls this before layout. Legacy scrollBy remains immediate. */
    public void advance(com.atom.lib.motion.FrameTime time,com.atom.lib.motion.MotionSpec motion){
        if(smoothing==null){smoothing=new SmoothScroll();smoothing.bounds(extent,bounds.height());smoothing.jump(offset,time);}
        frameTime=Objects.requireNonNull(time);this.motion=Objects.requireNonNull(motion);smoothing.bounds(extent,bounds.height());offset=smoothing.sample(time,motion);
    }
    public boolean wheelBy(float delta){
        if(smoothing==null)return scrollBy(delta);
        if(!Float.isFinite(delta))throw new IllegalArgumentException("Nonfinite scroll");
        smoothing.wheel(delta,frameTime,motion);if(!motion.enabled())offset=smoothing.sample(frameTime,motion);return true;
    }
    public float scrollbarOpacity(){return smoothing==null?1:smoothing.scrollbarOpacity(frameTime,hovered,false,motion);}
    public void hover(float x,float y){var t=thumb();hovered=t.height()>0&&new Rect(t.x()-3,bounds.y(),t.width()+6,bounds.height()).contains(x,y);}
    public boolean scrollbarVisible(){return smoothing==null?offset>0||hovered:scrollbarOpacity()>0;}
    public float offset(){return offset;}
    public float extent(){return extent;}
    public Rect bounds(){return bounds;}
    public boolean contains(NodeId id){return descendants.contains(id);}
    public boolean contains(PresentationFrame frame,NodeId id){var state=frame.scrollState(this);return state!=null&&state.descendants().contains(id);}
    public boolean scrollBy(float delta){if(!Float.isFinite(delta))throw new IllegalArgumentException("Nonfinite scroll");float next=Math.max(0,Math.min(Math.max(0,extent-bounds.height()),offset+delta));boolean changed=next!=offset;offset=next;if(smoothing!=null)smoothing.jump(offset,frameTime);return changed;}
    public boolean ensureVisible(Rect row){if(row.y()<bounds.y())return scrollBy(row.y()-bounds.y());if(row.bottom()>bounds.bottom())return scrollBy(row.bottom()-bounds.bottom());return false;}
    public PresentationFrame present(LayoutTree tree,LayoutSnapshot natural,Viewport screen,Rect bounds,float scale){
        return present(tree,natural,screen,bounds,scale,Map.of());
    }
    public PresentationFrame present(LayoutTree tree,LayoutSnapshot natural,Viewport screen,Rect bounds,float scale,Map<NodeId,EntrancePose> entrance){
        Objects.requireNonNull(bounds);var rootPose=entrance.get(tree.root().id());
        var displayedBounds=rootPose==null?bounds:bounds.translate(rootPose.x()*scale,rootPose.y()*scale).intersect(screen.bounds());
        float extent=natural.box(tree.root().id()).border().height()*scale;float offset=Math.max(0,Math.min(this.offset,Math.max(0,extent-displayedBounds.height())));var state=new State(displayedBounds,extent,offset,Set.copyOf(natural.paintOrder()));
        var limited=PresentationFrame.constrainRootHeight(natural,tree.root().id(),bounds.height()/scale);
        var frame=PresentationFrame.create(tree,limited,new Viewport(screen.width()/scale,screen.height()/scale,screen.pixelsPerUnit()*scale),Map.of(tree.root().id(),new PresentationFrame.Offset(0,offset/scale)),new PresentationFrame.Offset(bounds.x()/scale,bounds.y()/scale),entrance);
        return frame.scaled(scale).withScroll(this,state);
    }
    /** Scrolls only the descendants of an embedded, measured viewport. Siblings stay fixed. */
    public PresentationFrame presentGroup(LayoutTree tree,LayoutSnapshot natural,Viewport screen,NodeId id){
        LayoutNode group=tree.node(id);
        if(!group.style().clip()||!group.style().scroll())throw new IllegalArgumentException("Expected a clipped scroll group");
        LayoutBox groupBox=natural.box(id);
        Rect bounds=groupBox.clip();
        Set<NodeId> descendants=new HashSet<>();
        float bottom=collectExtent(group,natural,descendants);
        float extent=Math.max(bounds.height(),bottom+group.style().padding().bottom()-bounds.y());
        float shownOffset=Math.max(0,Math.min(offset,extent-bounds.height()));
        return PresentationFrame.create(tree,natural,screen,Map.of(id,new PresentationFrame.Offset(0,shownOffset)),
                new PresentationFrame.Offset(0,0)).withScroll(this,new State(bounds,extent,shownOffset,Set.copyOf(descendants)));
    }
    private static float collectExtent(LayoutNode group,LayoutSnapshot natural,Set<NodeId> descendants){
        float bottom=natural.box(group.id()).border().y();
        for(LayoutNode child:group.children()){
            if(child.style().hidden())continue;
            descendants.add(child.id());
            bottom=Math.max(bottom,Math.max(natural.box(child.id()).border().bottom(),collectExtent(child,natural,descendants)));
        }
        return bottom;
    }
    public Rect thumb(){return thumb(bounds,extent,offset);}
    private Rect thumb(Rect bounds,float extent,float offset){float h=extent<=bounds.height()?0:Math.min(bounds.height(),Math.max(24,bounds.height()*bounds.height()/extent));float y=extent<=bounds.height()?bounds.y():bounds.y()+offset/(extent-bounds.height())*(bounds.height()-h);return new Rect(bounds.right()-6,y,4,h);}
    public boolean dragThumb(float delta){
        if(!Float.isFinite(delta))throw new IllegalArgumentException("Nonfinite thumb drag");
        // The minimum thumb size changes the ratio between its travel and content travel.
        float travel=bounds.height()-thumb().height();
        return travel>0&&scrollBy(delta*Math.max(0,extent-bounds.height())/travel);
    }
}
