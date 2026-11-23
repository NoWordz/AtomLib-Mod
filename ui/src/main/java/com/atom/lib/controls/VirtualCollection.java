// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;

import com.atom.lib.geometry.*;
import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import io.github.humbleui.skija.Canvas;
import java.util.*;
import java.util.function.*;

/** Fixed-height adaptive grid/table. Own one per view; only visible items plus one row are requested. */
public final class VirtualCollection<K,T> implements AutoCloseable {
    @FunctionalInterface public interface ItemPainter<K,T>{void paint(Canvas canvas,CollectionFrame.Cell<K,T> item,Rect bounds);}
    private final NodeId id;private final Object identity=new Object();
    private VirtualData<K,T> data;
    private final float minWidth,rowHeight,gap;
    private final List<TableColumn> columns;
    private CollectionFrame<K,T> displayed;
    private CollectionEvent<K> pressed;
    private K selected,hovered;
    private Consumer<CollectionEvent<K>> events=e->{};
    private BiFunction<K,Rect,Map<String,Rect>> actions=(k,r)->Map.of();
    private float offset,extent,viewportHeight;
    private boolean closed,ascending=true;
    private String sortedColumn="";
    private long revision;
    private final SmoothScroll smoothing=new SmoothScroll();
    private com.atom.lib.motion.FrameTime time=new com.atom.lib.motion.FrameTime(0,0,0);
    private com.atom.lib.motion.MotionSpec motion=com.atom.lib.motion.MotionSpec.OFF;
    private boolean dragging;
    private float dragY;
    /** Advance before layout. Without this opt-in, scrolling remains immediate. */
    public void advance(com.atom.lib.motion.FrameTime time,com.atom.lib.motion.MotionSpec motion){requireOpen();this.time=Objects.requireNonNull(time);this.motion=Objects.requireNonNull(motion);smoothing.bounds(extent,viewportHeight);offset=smoothing.sample(time,motion);}
    private VirtualCollection(NodeId id,VirtualData<K,T> data,float width,float height,float gap,List<TableColumn> columns){
        this.id=Objects.requireNonNull(id);this.data=Objects.requireNonNull(data);this.columns=List.copyOf(columns);
        if(!Float.isFinite(width)||width<=0||!Float.isFinite(height)||height<=0||!Float.isFinite(gap)||gap<0)throw new IllegalArgumentException("Invalid collection geometry");
        if(new HashSet<>(columns.stream().map(TableColumn::id).toList()).size()!=columns.size())throw new IllegalArgumentException("Duplicate column ID");
        minWidth=width;rowHeight=height;this.gap=gap;
    }
    public static <K,T> VirtualCollection<K,T> grid(NodeId id,VirtualData<K,T> data,float minimumCardWidth,float cardHeight,float gap){return new VirtualCollection<>(id,data,minimumCardWidth,cardHeight,gap,List.of());}
    public static <K,T> VirtualCollection<K,T> table(NodeId id,VirtualData<K,T> data,float rowHeight,List<TableColumn> columns){if(columns.isEmpty())throw new IllegalArgumentException("Table needs columns");return new VirtualCollection<>(id,data,1,rowHeight,0,columns);}
    public void events(Consumer<CollectionEvent<K>> events){requireOpen();this.events=Objects.requireNonNull(events);}
    /** Rectangles are clipped to their row/card and reused verbatim for painting and hit testing. */
    public void actions(BiFunction<K,Rect,Map<String,Rect>> actions){requireOpen();this.actions=Objects.requireNonNull(actions);pressed=null;}
    public K selected(){return selected;}
    public K hovered(){return hovered;}
    public float offset(){return offset;}
    public void data(VirtualData<K,T> next){
        requireOpen();Objects.requireNonNull(next);cancel();
        if(displayed!=null&&!displayed.cells().isEmpty()){
            var anchor=displayed.cells().get(0);int index=next.indexOf(anchor.key());
            if(index>=0){float bodyTop=displayed.bounds().y()+(columns.isEmpty()?0:rowHeight);offset=Math.max(0,(index/displayed.columns())*(rowHeight+gap)+(bodyTop-anchor.bounds().y()));}
        }
        if(selected!=null&&next.indexOf(selected)<0)selected=null;
        data=next;
        smoothing.bounds(Math.max(extent,offset+viewportHeight),viewportHeight);smoothing.jump(offset,time);
    }
    public void scrollBy(float delta){requireOpen();if(!Float.isFinite(delta))throw new IllegalArgumentException("Nonfinite scroll");offset=Math.max(0,Math.min(Math.max(0,extent-viewportHeight),offset+delta));smoothing.bounds(extent,viewportHeight);smoothing.jump(offset,time);pressed=null;}
    public InputResult wheel(float x,float y,float delta){requireOpen();if(displayed==null||!displayed.bounds().contains(x,y)||delta==0)return InputResult.PASS;if(!Float.isFinite(delta))throw new IllegalArgumentException("Nonfinite scroll");smoothing.bounds(extent,viewportHeight);smoothing.wheel(-delta*40,time,motion);if(!motion.enabled())offset=smoothing.sample(time,motion);pressed=null;return InputResult.CONSUMED;}
    public CollectionFrame<K,T> layout(Viewport viewport,Rect bounds){
        requireOpen();Objects.requireNonNull(viewport);Objects.requireNonNull(bounds);
        int count=data.count();if(count<0)throw new IllegalArgumentException("Negative item count");
        int n=columns.isEmpty()?Math.max(1,(int)Math.floor((bounds.width()+gap)/(minWidth+gap))):1;
        float width=Math.max(0,(bounds.width()-(n-1)*gap)/n),header=columns.isEmpty()?0:rowHeight;
        var body=new Rect(bounds.x(),Math.min(bounds.bottom(),bounds.y()+header),bounds.width(),Math.max(0,bounds.height()-header));
        long rows=((long)count+n-1)/n;float extent=(float)Math.max(0,rows*(rowHeight+(double)gap)-gap);
        float offset=Math.max(0,Math.min(this.offset,Math.max(0,extent-body.height())));
        int first=(int)Math.min(count,(long)Math.floor(offset/(rowHeight+gap))*n);
        int end=(int)Math.min(count,(long)first+((long)Math.ceil(body.height()/(rowHeight+gap))+1)*n);
        var boxes=new LinkedHashMap<NodeId,LayoutBox>();var rootChildren=new ArrayList<LayoutNode>();var cells=new ArrayList<CollectionFrame.Cell<K,T>>();var hits=new LinkedHashMap<NodeId,CollectionEvent<K>>();var headers=new LinkedHashMap<String,Rect>();
        boxes.put(id,box(bounds,null));var bodyId=new NodeId(id.value()+"/body");boxes.put(bodyId,box(body,id));var children=new ArrayList<LayoutNode>();var keys=new HashSet<K>();
        for(int i=first;i<end;i++){
            T value=Objects.requireNonNull(data.itemAt(i));K key=Objects.requireNonNull(data.key(value));if(!keys.add(key))throw new IllegalArgumentException("Duplicate visible item key: "+key);
            var rect=new Rect(bounds.x()+(i%n)*(width+gap),body.y()+(i/n)*(rowHeight+gap)-offset,width,rowHeight);
            var itemId=new NodeId(id.value()+"/item/"+i);boxes.put(itemId,box(rect,bodyId));hits.put(itemId,new CollectionEvent<>(CollectionEvent.Kind.ITEM,key,"","",true));
            var childActions=new ArrayList<LayoutNode>();var areas=new LinkedHashMap<String,Rect>();int actionIndex=0;
            for(var entry:Map.copyOf(actions.apply(key,rect)).entrySet()){
                if(entry.getKey().isBlank())throw new IllegalArgumentException("Empty action ID");
                var area=entry.getValue().intersect(rect);areas.put(entry.getKey(),area);var actionId=new NodeId(itemId.value()+"/action/"+actionIndex++);
                boxes.put(actionId,box(area,itemId));hits.put(actionId,new CollectionEvent<>(CollectionEvent.Kind.ACTION,key,"",entry.getKey(),true));childActions.add(node(actionId,List.of(),false));
            }
            children.add(node(itemId,childActions,true));cells.add(new CollectionFrame.Cell<>(key,value,itemId,rect,areas));
        }
        rootChildren.add(node(bodyId,children,true));float columnX=bounds.x();
        for(var col:columns){var rect=new Rect(columnX,bounds.y(),col.width(),Math.min(header,bounds.height()));var headerId=new NodeId(id.value()+"/header/"+col.id());boxes.put(headerId,box(rect,id));rootChildren.add(node(headerId,List.of(),true));headers.put(col.id(),rect.intersect(bounds));if(col.sortable())hits.put(headerId,new CollectionEvent<>(CollectionEvent.Kind.SORT,null,col.id(),"",!col.id().equals(sortedColumn)||!ascending));columnX+=col.width();}
        var tree=new LayoutTree(node(id,rootChildren,true));long version=++revision;var order=List.copyOf(boxes.keySet());
        LayoutSnapshot snapshot=new LayoutSnapshot(){public long revision(){return version;}public LayoutBox box(NodeId id){return boxes.get(id);}public List<NodeId> paintOrder(){return order;}};
        return new CollectionFrame<>(identity,PresentationFrame.create(tree,snapshot,viewport,Map.of(),new PresentationFrame.Offset(0,0)),cells,hits,n,bounds,headers,extent,offset,header);
    }
    private static LayoutBox box(Rect r,NodeId parent){return new LayoutBox(r,r,r,r,parent,null);}
    private static LayoutNode node(NodeId id,List<LayoutNode> children,boolean clip){return new LayoutNode(id,LayoutStyle.column().behavior(false,false,clip,false),children,null,0);}
    /** Header/body defaults use the supplied instance context. The renderer owns item content and draws child actions at Cell.actions(). */
    public void paint(Canvas canvas,CollectionFrame<K,T> frame,ControlContext context,ItemPainter<K,T> painter){
        Objects.requireNonNull(context);paintInternal(canvas,frame,painter,context);
    }
    public void paint(Canvas canvas,CollectionFrame<K,T> frame,ItemPainter<K,T> painter){paintInternal(canvas,frame,painter,null);}
    private void paintInternal(Canvas canvas,CollectionFrame<K,T> frame,ItemPainter<K,T> painter,ControlContext context){
        requireOpen();Objects.requireNonNull(canvas);Objects.requireNonNull(painter);if(frame.owner!=identity)throw new IllegalArgumentException("Foreign collection frame");
        var chrome=context==null?CollectionSkin.defaults():Objects.requireNonNull(context.skin().collectionSkin());
        int saved=canvas.getSaveCount();canvas.save();
        try{
            clip(canvas,frame.bounds());
            for(var cell:frame.cells()){
                var clip=frame.presentation().box(cell.id()).clip();int count=canvas.getSaveCount();canvas.save();
                try{clip(canvas,clip);if(context!=null)chrome.paintItem(canvas,cell.bounds(),columns.isEmpty(),Objects.equals(hovered,cell.key()));painter.paint(canvas,cell,cell.bounds());}finally{canvas.restoreToCount(count);}
            }
            for(var column:columns){var r=frame.headers().get(column.id());chrome.paintHeader(canvas,r);
                if(context!=null){String label=column.label()+(column.id().equals(sortedColumn)?ascending?" ↑":" ↓":"");chrome.paintHeaderText(canvas,r,label,context.text());}
            }
            var thumb=frame.thumb();if(thumb.height()>0){float opacity=smoothing.scrollbarOpacity(time,hovered!=null,dragging,motion);chrome.paintScrollbar(canvas,thumb,opacity);}
        }finally{canvas.restoreToCount(saved);}
        displayed=frame;extent=frame.extent();viewportHeight=frame.body().height();offset=frame.offset();smoothing.bounds(extent,viewportHeight);
    }
    private static void clip(Canvas canvas,Rect r){canvas.clipRect(io.github.humbleui.types.Rect.makeXYWH(r.x(),r.y(),r.width(),r.height()));}
    public CollectionEvent<K> hit(float x,float y){
        requireOpen();if(displayed==null)return null;var id=displayed.presentation().topAt(displayed.hits.keySet(),x,y);var event=id==null?null:displayed.hits.get(id);
        if(event!=null&&event.kind()!=CollectionEvent.Kind.SORT)for(var column:columns){var r=displayed.headers().get(column.id());if(x>=r.x()&&x<r.right())return new CollectionEvent<>(event.kind(),event.key(),column.id(),event.action(),event.ascending());}
        return event;
    }
    public void hover(float x,float y){var event=hit(x,y);hovered=event==null?null:event.key();}
    public InputResult pointer(PointerEvent event){
        requireOpen();if(event.type()==PointerEvent.Type.CANCEL){cancel();return InputResult.CONSUMED;}
        if(dragging){if(event.type()==PointerEvent.Type.MOVE&&displayed!=null){float travel=displayed.body().height()-displayed.thumb().height();if(travel>0)scrollBy((event.y()-dragY)*(displayed.extent()-displayed.body().height())/travel);dragY=event.y();}if(event.type()==PointerEvent.Type.RELEASE&&event.button()==0)dragging=false;return InputResult.CONSUMED;}
        if(event.button()!=0)return InputResult.PASS;
        if(event.type()==PointerEvent.Type.PRESS&&displayed!=null&&displayed.thumb().height()>0&&displayed.thumb().contains(event.x(),event.y())){pressed=null;dragging=true;dragY=event.y();return InputResult.CONSUMED;}
        if(event.type()==PointerEvent.Type.PRESS){pressed=hit(event.x(),event.y());return pressed==null?InputResult.PASS:InputResult.CONSUMED;}
        if(event.type()==PointerEvent.Type.RELEASE){var before=pressed;pressed=null;if(before==null)return InputResult.PASS;var current=hit(event.x(),event.y());if(before.equals(current)){if(before.kind()==CollectionEvent.Kind.SORT){sortedColumn=before.column();ascending=before.ascending();}else selected=before.key();events.accept(before);}return InputResult.CONSUMED;}
        return pressed==null?InputResult.PASS:InputResult.CONSUMED;
    }
    public void cancel(){pressed=null;hovered=null;dragging=false;}
    private void requireOpen(){if(closed)throw new IllegalStateException("Collection closed");}
    @Override public void close(){if(closed)return;closed=true;displayed=null;pressed=null;selected=hovered=null;events=e->{};actions=(k,r)->Map.of();data=null;}
}
