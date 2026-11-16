package com.atom.lib.layout;
import com.atom.lib.geometry.*;
import com.atom.lib.motion.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PresentationMotionTest {
    private PresentationFrame frame(){
        var id=new NodeId("root");var rect=new Rect(10,20,100,40);var tree=new LayoutTree(LayoutNode.leaf("root",LayoutStyle.column(),null,0));
        LayoutSnapshot raw=new LayoutSnapshot(){public long revision(){return 1;}public LayoutBox box(NodeId ignored){return new LayoutBox(rect,rect,rect,new Rect(0,0,300,200),null,null);}public List<NodeId> paintOrder(){return List.of(id);}};
        return PresentationFrame.create(tree,raw,new Viewport(300,200,1),Map.of(),new PresentationFrame.Offset(0,0));
    }
    @Test void displayedHitAndPaintingCoordinatesShareScaleAndTranslation(){
        var id=new NodeId("root");var shown=frame().transformed(.5f,0,0,50,10);
        var box=shown.box(id).border();assertEquals(new Rect(55,20,50,20),box);
        assertEquals(id,shown.topAt(Set.of(id),60,25));assertNull(shown.topAt(Set.of(id),15,25));
        var paint=shown.forPainting(id).box(id).border();assertEquals(box.x(),paint.x()*shown.renderScale(id),.0001);assertEquals(100,paint.width());
        var feedback=shown.feedback(Map.of(id,1.04f));assertEquals(100,feedback.forPainting(id).box(id).border().width());
    }
    @Test void rectangleMorphReversesAndOffSnaps(){
        var r=new RegionMotion(new Rect(0,0,20,30));r.target(new Rect(100,80,80,60),new FrameTime(0,0,0),MotionSpec.STANDARD);
        var mid=r.sample(new FrameTime(1,110_000_000,.11),MotionSpec.STANDARD);assertEquals(50,mid.x(),.001);
        r.target(new Rect(0,0,20,30),new FrameTime(1,110_000_000,0),MotionSpec.STANDARD);assertEquals(mid,r.sample(new FrameTime(1,110_000_000,0),MotionSpec.STANDARD));
        assertEquals(new Rect(0,0,20,30),r.sample(new FrameTime(2,111_000_000,.001),MotionSpec.OFF));assertTrue(r.complete());
    }
    @Test void wheelSmoothsButDragAndMotionOffAreImmediate(){
        var s=new SmoothScroll();s.bounds(1000,200);s.wheel(300,new FrameTime(1,0,0),MotionSpec.STANDARD);
        assertEquals(0,s.sample(new FrameTime(1,0,0),MotionSpec.STANDARD));float mid=s.sample(new FrameTime(2,110_000_000,.11),MotionSpec.STANDARD);assertTrue(mid>0&&mid<300);
        assertEquals(mid,s.sample(new FrameTime(2,110_000_000,1),MotionSpec.STANDARD));s.jump(900,new FrameTime(3,120_000_000,.01));assertEquals(800,s.sample(new FrameTime(3,120_000_000,0),MotionSpec.STANDARD));
        s.wheel(-100,new FrameTime(4,130_000_000,.01),MotionSpec.OFF);assertEquals(700,s.sample(new FrameTime(4,130_000_000,0),MotionSpec.OFF));assertEquals(0,s.scrollbarOpacity(new FrameTime(5,2_000_000_000,1),false,false,MotionSpec.STANDARD));
    }
}
