// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.motion;

import java.util.List;
import java.util.Objects;

/** Immutable policy. Durations are seconds; mutable animation state belongs to each view. */
public record MotionSpec(String id, double enterSeconds, double exitSeconds, double transitionSeconds,
                         float hoverScale, float pressScale, boolean enabled) {
    public static final MotionSpec STANDARD=new MotionSpec("standard",.09,.14,.22,1.015f,.975f,true);
    public static final MotionSpec LIGHT=new MotionSpec("light",.06,.09,.12,1.005f,.99f,true);
    public static final MotionSpec OFF=new MotionSpec("off",.09,.14,.22,1,1,false);
    public MotionSpec {
        Objects.requireNonNull(id);
        if(id.isBlank())throw new IllegalArgumentException("Empty motion ID");
        for(double v:new double[]{enterSeconds,exitSeconds,transitionSeconds})
            if(!Double.isFinite(v)||v<=0)throw new IllegalArgumentException("Invalid duration");
        if(!Float.isFinite(hoverScale)||!Float.isFinite(pressScale)||hoverScale<=0||pressScale<=0)
            throw new IllegalArgumentException("Invalid scale");
    }
    public static List<MotionSpec> presets(){return List.of(STANDARD,LIGHT,OFF);}
    /** Bounds displacement of an edge, rather than scaling large cards without a limit. */
    public float scale(float hover,float press,float size,float maxEdgeDisplacement){
        if(!Float.isFinite(size)||size<0||!Float.isFinite(maxEdgeDisplacement)||maxEdgeDisplacement<0)
            throw new IllegalArgumentException("Invalid feedback geometry");
        if(!enabled||size==0)return 1;
        float desired=(hoverScale-1)*Math.max(0,Math.min(1,hover))+(pressScale-1)*Math.max(0,Math.min(1,press));
        float limit=2*maxEdgeDisplacement/size;
        return 1+Math.max(-limit,Math.min(limit,desired));
    }
}
