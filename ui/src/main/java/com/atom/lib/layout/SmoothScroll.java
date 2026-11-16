// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.layout;
import com.atom.lib.motion.*;
/** Explicit-frame wheel smoothing and scrollbar fade; drag/key positioning is immediate. */
public final class SmoothScroll {
    private Transition position=new Transition(0);
    private float target,maximum;
    private long lastActivity=Long.MIN_VALUE;
    public void bounds(float content,float viewport){if(!Float.isFinite(content)||!Float.isFinite(viewport)||content<0||viewport<0)throw new IllegalArgumentException("Invalid scroll extent");maximum=Math.max(0,content-viewport);target=clamp(target);if(position.value()>maximum)position=new Transition(maximum);}
    private float clamp(float value){return Math.max(0,Math.min(maximum,value));}
    public void wheel(float delta,FrameTime time,MotionSpec motion){if(!Float.isFinite(delta))throw new IllegalArgumentException("Nonfinite scroll");target=clamp(target+delta);position.target(target,time,motion.transitionSeconds(),motion.enabled());lastActivity=time.nowNanos();}
    public void jump(float offset,FrameTime time){if(!Float.isFinite(offset))throw new IllegalArgumentException("Nonfinite scroll");target=clamp(offset);position.target(target,time,1,false);lastActivity=time.nowNanos();}
    public float sample(FrameTime time,MotionSpec motion){if(target!=position.target())position.target(target,time,motion.transitionSeconds(),motion.enabled());return position.sample(time,motion.enabled());}
    public float scrollbarOpacity(FrameTime time,boolean hovered,boolean dragging,MotionSpec motion){if(hovered||dragging){lastActivity=time.nowNanos();return 1;}if(lastActivity==Long.MIN_VALUE)return 0;if(!motion.enabled())return 1;return (float)Math.max(0,1-Math.max(0,((double)time.nowNanos()-lastActivity)/1e9-.65)/.3);}
}
