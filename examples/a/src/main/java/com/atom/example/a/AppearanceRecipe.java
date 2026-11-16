package com.atom.example.a;

import com.atom.lib.RuntimeHandle;
import com.atom.lib.controls.*;
import com.atom.lib.design.atomdefault.AtomDefault;
import com.atom.lib.geometry.*;
import com.atom.lib.layout.*;
import com.atom.lib.layout.internal.YogaLayoutEngine;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Canvas;
import java.math.BigDecimal;
import java.util.*;

/** Two independent windows sharing a runtime. Caller owns Screen, bridge, input routing and persistence. */
public final class AppearanceRecipe implements AutoCloseable {
    private final AppearanceSession left,right;
    private final YogaLayoutEngine leftLayout=new YogaLayoutEngine(),rightLayout=new YogaLayoutEngine();
    public AppearanceRecipe(RuntimeHandle runtime){
        left=AtomDefault.createSession(runtime);right=AtomDefault.createSession(runtime);
        left.selectTheme("dusk");right.selectTheme("minimal");right.selectMotion("light");
        left.input().setControls(List.of(
            new TextField(new NodeId("name"),left.context(),"中文 / é / 👩‍💻",new TextFieldEvents(){}),
            new NumberField(new NodeId("count"),left.context(),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.TEN,new NumberFieldEvents(){})));
        // Identical NodeIds in the other window do not share drafts, focus, theme or feedback.
        right.input().setControls(List.of(new TextArea(new NodeId("name"),right.context(),"Independent multiline draft",new TextFieldEvents(){})));
    }
    public AppearanceSession left(){return left;}
    public AppearanceSession right(){return right;}
    public void paint(Canvas canvas,Viewport viewport,FrameTime time){
        float half=viewport.width()/2;
        paintWindow(canvas,left,leftLayout,new Rect(0,0,half,viewport.height()),viewport,time);
        paintWindow(canvas,right,rightLayout,new Rect(half,0,viewport.width()-half,viewport.height()),viewport,time);
    }
    private void paintWindow(Canvas canvas,AppearanceSession ui,YogaLayoutEngine layout,Rect bounds,Viewport viewport,FrameTime time){
        try(var frame=ui.beginFrame(time,viewport)){
            frame.panel(canvas,bounds,12,()->false);
            var root=new LayoutNode(new NodeId("root"),LayoutStyle.column().size(Length.points(bounds.width()),Length.auto()).padding(12).spacing(0,12),ui.input().controls().stream().map(c->c.node(LayoutStyle.column())).toList(),null,0);
            var tree=new LayoutTree(root);var local=new Viewport(bounds.width(),bounds.height(),viewport.pixelsPerUnit());
            var shown=PresentationFrame.create(tree,layout.layout(tree,local),viewport,Map.of(),new PresentationFrame.Offset(bounds.x(),bounds.y()));
            frame.paint(canvas,shown);
        }
    }
    @Override public void close(){try{left.close();}finally{try{right.close();}finally{leftLayout.close();rightLayout.close();}}}
}
