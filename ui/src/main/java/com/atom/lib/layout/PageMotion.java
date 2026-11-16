// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.layout;
import com.atom.lib.geometry.*;
import com.atom.lib.motion.*;
/** Optional slide/zoom with interruption from its currently displayed pose. Host owns routing and lifecycle. */
public final class PageMotion {
    private final Transition x=new Transition(0),y=new Transition(0),scale=new Transition(1);
    public void target(float dx,float dy,float zoom,FrameTime time,MotionSpec spec){if(!Float.isFinite(zoom)||zoom<=0)throw new IllegalArgumentException("Invalid page scale");x.target(dx,time,spec.transitionSeconds(),spec.enabled());y.target(dy,time,spec.transitionSeconds(),spec.enabled());scale.target(zoom,time,spec.transitionSeconds(),spec.enabled());}
    public PresentationFrame sample(PresentationFrame frame,Viewport viewport,FrameTime time,MotionSpec spec){return frame.transformed(scale.sample(time,spec.enabled()),viewport.width()/2,viewport.height()/2,x.sample(time,spec.enabled()),y.sample(time,spec.enabled())).clipped(viewport.bounds());}
    public boolean complete(){return x.complete()&&y.complete()&&scale.complete();}
    /** Cancel keeps the current pose; animate back to (0,0,1) or close the owning view. */
    public void cancel(FrameTime time){x.target(x.value(),time,1,false);y.target(y.value(),time,1,false);scale.target(scale.value(),time,1,false);}
}
