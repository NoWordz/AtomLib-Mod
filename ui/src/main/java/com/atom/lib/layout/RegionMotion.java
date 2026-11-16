// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.layout;
import com.atom.lib.geometry.Rect;
import com.atom.lib.motion.*;
/** Shared-region shape. The host remeasures content in the sampled rectangle; no business navigation lives here. */
public final class RegionMotion {
    private final Transition x,y,width,height;
    public RegionMotion(Rect initial){x=new Transition(initial.x());y=new Transition(initial.y());width=new Transition(initial.width());height=new Transition(initial.height());}
    public void target(Rect next,FrameTime time,MotionSpec motion){x.target(next.x(),time,motion.transitionSeconds(),motion.enabled());y.target(next.y(),time,motion.transitionSeconds(),motion.enabled());width.target(next.width(),time,motion.transitionSeconds(),motion.enabled());height.target(next.height(),time,motion.transitionSeconds(),motion.enabled());}
    public Rect sample(FrameTime time,MotionSpec motion){return new Rect(x.sample(time,motion.enabled()),y.sample(time,motion.enabled()),width.sample(time,motion.enabled()),height.sample(time,motion.enabled()));}
    public boolean complete(){return x.complete()&&y.complete()&&width.complete()&&height.complete();}
}
