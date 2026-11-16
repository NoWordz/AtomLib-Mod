// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;
import com.atom.lib.layout.*;
import com.atom.lib.geometry.*;
import java.util.*;
/** One immutable geometry snapshot, published for input only after the entire painter succeeds. */
public final class CollectionFrame<K,T> {
    public record Cell<K,T>(K key,T item,NodeId id,Rect bounds,Map<String,Rect> actions){public Cell{actions=Map.copyOf(actions);}}
    final Object owner;
    final Map<NodeId,CollectionEvent<K>> hits;
    private final PresentationFrame presentation;
    private final List<Cell<K,T>> cells;
    private final int columns;
    private final Rect bounds;
    private final Map<String,Rect> headers;
    private final float extent,offset,header;
    CollectionFrame(Object owner,PresentationFrame presentation,List<Cell<K,T>> cells,Map<NodeId,CollectionEvent<K>> hits,int columns,Rect bounds,Map<String,Rect> headers,float extent,float offset,float header){this.owner=owner;this.presentation=presentation;this.cells=List.copyOf(cells);this.hits=Map.copyOf(hits);this.columns=columns;this.bounds=bounds;this.headers=Map.copyOf(headers);this.extent=extent;this.offset=offset;this.header=header;}
    public PresentationFrame presentation(){return presentation;}
    public List<Cell<K,T>> cells(){return cells;}
    public int columns(){return columns;}
    public Rect bounds(){return bounds;}
    public Map<String,Rect> headers(){return headers;}
    public Rect body(){return new Rect(bounds.x(),Math.min(bounds.bottom(),bounds.y()+header),bounds.width(),Math.max(0,bounds.height()-header));}
    public float offset(){return offset;}
    public float extent(){return extent;}
    public Rect thumb(){var b=body();float h=extent<=b.height()?0:Math.min(b.height(),Math.max(24,b.height()*b.height()/extent));return new Rect(Math.max(b.x(),b.right()-6),b.y()+(extent<=b.height()?0:offset/(extent-b.height())*(b.height()-h)),Math.min(4,b.width()),h);}
}
