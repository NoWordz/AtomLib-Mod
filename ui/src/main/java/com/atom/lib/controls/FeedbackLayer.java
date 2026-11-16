// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;
import com.atom.lib.geometry.Rect;
import com.atom.lib.motion.*;
import io.github.humbleui.skija.*;
import java.util.*;
/** Bounded window-local ripples. Pulse on activation; painting never emits business callbacks. */
public final class FeedbackLayer implements AutoCloseable {
    private record Pulse(Rect bounds,float radius,float x,float y,int color,long started){}
    private final ArrayDeque<Pulse> pulses=new ArrayDeque<>();private boolean closed;
    public void pulse(Rect bounds,float radius,float x,float y,int color,FrameTime time){
        if(closed)throw new IllegalStateException("Feedback closed");Objects.requireNonNull(bounds);Objects.requireNonNull(time);
        if(!Float.isFinite(radius)||radius<0||!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Invalid feedback");
        if(!bounds.contains(x,y))return;if(pulses.size()==32)pulses.removeFirst();pulses.addLast(new Pulse(bounds,radius,x,y,color,time.nowNanos()));
    }
    public int activeCount(){return pulses.size();}
    public void paint(Canvas canvas,FrameTime time,MotionSpec motion){
        if(closed)throw new IllegalStateException("Feedback closed");if(!motion.enabled()){pulses.clear();return;}
        double duration=motion.transitionSeconds()*2;
        pulses.removeIf(p->((double)time.nowNanos()-p.started)/1e9>=duration);
        for(var pulse:pulses){float t=(float)Math.max(0,Math.min(1,((double)time.nowNanos()-pulse.started)/(duration*1e9)));var r=pulse.bounds;int saved=canvas.getSaveCount();canvas.save();
            try(var paint=new Paint().setAntiAlias(true).setColor((pulse.color&0xffffff)|(Math.round(70*(1-t))<<24))){canvas.clipRRect(io.github.humbleui.types.RRect.makeXYWH(r.x(),r.y(),r.width(),r.height(),pulse.radius));canvas.drawCircle(pulse.x,pulse.y,(float)Math.hypot(r.width(),r.height())*(1-(1-t)*(1-t)),paint);}finally{canvas.restoreToCount(saved);}
        }
    }
    public void clear(){pulses.clear();}
    @Override public void close(){closed=true;pulses.clear();}
}
