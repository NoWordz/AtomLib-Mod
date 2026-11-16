package com.atom.lib.controls;
import com.atom.lib.geometry.*;
import com.atom.lib.input.*;
import com.atom.lib.motion.*;
import com.atom.lib.layout.*;
import io.github.humbleui.skija.Surface;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VirtualCollectionTest {
    @Test void failedLargerViewportDoesNotReplaceDisplayedScrollLimits(){
        var values=new ArrayList<String>();for(int i=0;i<10;i++)values.add("row"+i);
        try(var view=VirtualCollection.grid(new NodeId("grid"),data(values),320,40,0);var surface=Surface.makeRasterN32Premul(320,600)){
            var original=view.layout(new Viewport(320,600,1),new Rect(0,0,320,220));view.paint(surface.getCanvas(),original,(c,item,r)->{});
            var larger=view.layout(new Viewport(320,600,1),new Rect(0,0,320,500));assertThrows(IllegalStateException.class,()->view.paint(surface.getCanvas(),larger,(c,item,r)->{throw new IllegalStateException();}));
            view.wheel(20,20,-1);assertTrue(view.layout(VIEW,new Rect(0,0,320,220)).offset()>0);
        }
    }
    @Test void replacingDataCancelsScrollbarCapture(){
        var values=new ArrayList<String>();for(int i=0;i<30;i++)values.add("row"+i);
        try(var view=VirtualCollection.grid(new NodeId("grid"),data(values),320,40,0);var surface=Surface.makeRasterN32Premul(320,220)){
            var frame=view.layout(VIEW,new Rect(0,0,320,220));view.paint(surface.getCanvas(),frame,(c,item,r)->{});var thumb=frame.thumb();
            view.pointer(new PointerEvent(PointerEvent.Type.PRESS,thumb.x()+1,thumb.y()+1,0));view.data(data(values));float before=view.offset();
            view.pointer(new PointerEvent(PointerEvent.Type.MOVE,thumb.x()+1,thumb.y()+30,0));assertEquals(before,view.offset());
        }
    }
    private static final Viewport VIEW=new Viewport(320,220,1);
    private static final FrameTime NOW=new FrameTime(1,0,0);
    private VirtualData<String,String> data(List<String> values){return new VirtualData<>(){public int count(){return values.size();}public String itemAt(int i){return values.get(i);}public String key(String value){return value;}};}
    @Test void onlyVisibleItemsAreBuiltAndGapsNeverHit(){
        int[] reads={0};var source=new VirtualData<Integer,Integer>(){public int count(){return 100_000;}public Integer itemAt(int i){reads[0]++;return i;}public Integer key(Integer value){return value;}};
        try(var f=new ControlFixture();var view=VirtualCollection.grid(new NodeId("grid"),source,100,60,10);var surface=Surface.makeRasterN32Premul(320,220)){
            var frame=view.layout(VIEW,new Rect(0,0,320,220));assertTrue(reads[0]<=15); // four visible rows plus one prefetch row
            view.paint(surface.getCanvas(),frame,(c,item,box)->{});
            assertEquals(3,frame.columns());assertNull(view.hit(105,20));assertNotNull(view.hit(10,20));
            assertNull(view.hit(10,221));
        }
    }
    @Test void childActionWinsAndReplacementCancelsCapturedItem(){
        var events=new ArrayList<CollectionEvent<String>>();
        try(var view=VirtualCollection.grid(new NodeId("grid"),data(List.of("a","b")),100,60,10);var surface=Surface.makeRasterN32Premul(320,220)){
            view.events(events::add);view.actions((key,box)->Map.of("buy",new Rect(box.right()-25,box.y(),25,20)));
            var frame=view.layout(VIEW,new Rect(0,0,320,220));view.paint(surface.getCanvas(),frame,(c,item,box)->{});
            view.pointer(new PointerEvent(PointerEvent.Type.PRESS,90,10,0));view.pointer(new PointerEvent(PointerEvent.Type.RELEASE,90,10,0));
            assertEquals("a",events.get(0).key());assertEquals("buy",events.get(0).action());
            view.pointer(new PointerEvent(PointerEvent.Type.PRESS,10,30,0));view.data(data(List.of("b","a")));
            view.pointer(new PointerEvent(PointerEvent.Type.RELEASE,10,30,0));assertEquals(1,events.size());
            frame=view.layout(VIEW,new Rect(0,0,320,220));view.paint(surface.getCanvas(),frame,(c,item,box)->{});assertEquals("b",view.hit(10,30).key());
        }
    }
    @Test void failedPaintKeepsLastSuccessfulHitFrameAndSortIsOnlyIntent(){
        var events=new ArrayList<CollectionEvent<String>>();
        try(var view=VirtualCollection.table(new NodeId("table"),data(List.of("a","b")),40,List.of(new TableColumn("name","Name",150,true),new TableColumn("action","Action",170,false)));var surface=Surface.makeRasterN32Premul(320,220)){
            view.events(events::add);var frame=view.layout(VIEW,new Rect(0,0,320,220));view.paint(surface.getCanvas(),frame,(c,item,box)->{});
            view.pointer(new PointerEvent(PointerEvent.Type.PRESS,10,10,0));view.pointer(new PointerEvent(PointerEvent.Type.RELEASE,10,10,0));
            assertEquals(CollectionEvent.Kind.SORT,events.get(0).kind());assertEquals("name",events.get(0).column());
            view.data(data(List.of("b","a")));var changed=view.layout(VIEW,new Rect(0,0,320,220));
            assertThrows(IllegalStateException.class,()->view.paint(surface.getCanvas(),changed,(c,item,box)->{throw new IllegalStateException("Painter failed");}));
            assertEquals("a",view.hit(10,50).key());assertEquals("name",view.hit(10,50).column());
        }
    }
    @Test void narrowEmptyAndSameKeysAcrossViewsRemainIndependent(){
        try(var a=VirtualCollection.grid(new NodeId("same"),data(List.of("same")),100,60,10);var b=VirtualCollection.grid(new NodeId("same"),data(List.of()),100,60,10);var surface=Surface.makeRasterN32Premul(320,220)){
            var af=a.layout(VIEW,new Rect(0,0,40,100));var bf=b.layout(VIEW,new Rect(0,0,40,100));
            assertEquals(1,af.columns());assertTrue(bf.cells().isEmpty());a.paint(surface.getCanvas(),af,(c,item,r)->{});b.paint(surface.getCanvas(),bf,(c,item,r)->{});
            assertNotNull(a.hit(5,5));assertNull(b.hit(5,5));a.close();assertDoesNotThrow(()->b.layout(VIEW,new Rect(0,0,40,100)));
        }
    }
    @Test void replacementRetainsTheVisibleAnchorByKeyAndWheelUsesDisplayedFrame(){
        var values=new ArrayList<String>();for(int i=0;i<30;i++)values.add("key"+i);
        try(var view=VirtualCollection.table(new NodeId("rows"),data(values),40,List.of(new TableColumn("name","Name",320,true)));var surface=Surface.makeRasterN32Premul(320,220)){
            var frame=view.layout(VIEW,new Rect(0,0,320,220));view.paint(surface.getCanvas(),frame,(c,item,r)->{});view.scrollBy(400);
            frame=view.layout(VIEW,new Rect(0,0,320,220));view.paint(surface.getCanvas(),frame,(c,item,r)->{});var anchor=frame.cells().get(0).key();
            Collections.rotate(values,3);view.data(data(values));frame=view.layout(VIEW,new Rect(0,0,320,220));assertEquals(anchor,frame.cells().get(0).key());
            assertEquals(InputResult.PASS,view.wheel(350,20,1));assertEquals(InputResult.CONSUMED,view.wheel(10,80,1));
        }
    }
}
