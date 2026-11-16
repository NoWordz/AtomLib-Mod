// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.motion;

/** Finite-duration, interruptible scalar transition, sampled against the caller's frame clock. */
public final class Transition {
    private float from,value,target;
    private long started,lastFrame=Long.MIN_VALUE,lastNow=Long.MIN_VALUE;
    private double duration;
    public Transition(float initial){if(!Float.isFinite(initial))throw new IllegalArgumentException("Nonfinite value");from=value=target=initial;}
    public float value(){return value;}
    public float target(){return target;}
    public boolean complete(){return value==target;}
    public void target(float next,FrameTime frame,double seconds,boolean enabled){
        if(!Float.isFinite(next)||!Double.isFinite(seconds)||seconds<=0)throw new IllegalArgumentException("Invalid transition");
        sample(frame,enabled);
        if(next!=target){from=value;target=next;started=frame.nowNanos();duration=seconds;}
        if(!enabled)from=value=target;
    }
    public float sample(FrameTime frame,boolean enabled){
        if(frame.frameId()<lastFrame||frame.nowNanos()<lastNow)throw new IllegalArgumentException("Nonmonotonic frame");
        if(!enabled){from=value=target;lastFrame=frame.frameId();lastNow=frame.nowNanos();return value;}
        if(frame.frameId()==lastFrame)return value;
        lastFrame=frame.frameId();lastNow=frame.nowNanos();
        if(value!=target){double t=Math.max(0,Math.min(1,((double)frame.nowNanos()-started)/(duration*1e9)));double ease=t*t*(3-2*t);value=t>=1?target:(float)(from+(target-from)*ease);}
        return value;
    }
}
