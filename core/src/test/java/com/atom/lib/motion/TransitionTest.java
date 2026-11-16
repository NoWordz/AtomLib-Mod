package com.atom.lib.motion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TransitionTest {
    @Test void retargetUsesCurrentValueAndFinishesExactly(){
        var t=new Transition(0);t.target(1,new FrameTime(1,0,0),1,true);
        assertEquals(.5,t.sample(new FrameTime(2,500_000_000,.5),true),.0001);
        assertEquals(.5,t.sample(new FrameTime(2,600_000_000,.1),true),.0001);
        t.target(0,new FrameTime(3,600_000_000,.1),1,true);float atReversal=t.value();assertTrue(atReversal>0);
        assertEquals(atReversal,t.sample(new FrameTime(3,600_000_000,0),true));
        assertEquals(0,t.sample(new FrameTime(4,1_600_000_000,1),true));assertTrue(t.complete());
        t.target(1,new FrameTime(5,1_600_000_000,0),1,true);assertEquals(1,t.sample(new FrameTime(5,1_600_000_000,0),false));
    }
    @Test void sameTargetDoesNotRestartAndDifferentFrameRatesAgree(){
        var a=new Transition(0);var b=new Transition(0);var start=new FrameTime(0,0,0);a.target(1,start,.25,true);b.target(1,start,.25,true);
        for(int i=1;i<=30;i++)a.target(1,new FrameTime(i,i*16_666_667L,1d/60),.25,true);
        for(int i=1;i<=60;i++)b.target(1,new FrameTime(i,i*8_333_334L,1d/120),.25,true);
        assertEquals(1,a.value());assertEquals(a.value(),b.value());
        assertEquals(1,MotionSpec.OFF.scale(1,1,400,2));assertEquals(1.01,MotionSpec.STANDARD.scale(1,0,400,2),.00001);
    }
}
