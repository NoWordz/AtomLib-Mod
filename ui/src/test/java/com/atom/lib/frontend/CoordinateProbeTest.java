package com.atom.lib.frontend;

import com.atom.lib.geometry.*;
import com.atom.lib.frontend.contract.GuiCoordinates;
import com.atom.lib.layout.*;
import com.atom.lib.layout.internal.YogaLayoutEngine;
import io.github.humbleui.skija.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Native geometry feasibility probe, not a Minecraft-host acceptance test. */
class CoordinateProbeTest {
    private record Observed(Rect pixels, NodeId hit) {}
    private Observed render(float contentScale,float pixelsPerUnit,double guiScale,boolean duplicateScale) {
        var child=LayoutNode.leaf("card",LayoutStyle.column().size(Length.points(20),Length.points(10)),null,0);
        var root=new LayoutNode(new NodeId("root"),LayoutStyle.column().size(Length.points(100),Length.points(100)).padding(10),List.of(child),null,0);
        var tree=new LayoutTree(root);var viewport=new Viewport(150,150,pixelsPerUnit);
        try(var engine=new YogaLayoutEngine();var surface=Surface.makeRasterN32Premul(220,220);var paint=new Paint().setColor(0xffffffff).setAntiAlias(false)) {
            var frame=PresentationFrame.create(tree,engine.layout(tree,viewport),viewport,
                Map.of(root.id(),new PresentationFrame.Offset(0,5)),new PresentationFrame.Offset(4,6),
                Map.of(child.id(),new EntrancePose(2,3,1))).scaled(contentScale);
            var canvas=surface.getCanvas();canvas.clear(0xff000000);
            canvas.scale(pixelsPerUnit*frame.renderScale(child.id()),pixelsPerUnit*frame.renderScale(child.id()));
            if(duplicateScale) canvas.scale(contentScale,contentScale);
            Rect box=frame.forPainting(child.id()).box(child.id()).border();
            canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(box.x(),box.y(),box.width(),box.height()),paint);
            int left=220,top=220,right=-1,bottom=-1;
            try(var image=surface.makeImageSnapshot();var bitmap=Bitmap.makeFromImage(image)) {
                for(int y=0;y<220;y++) for(int x=0;x<220;x++) if(bitmap.getColor(x,y)==0xffffffff) {
                    left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
                }
            }
            // The device target is derived by hand: local (10,10), page (4,6),
            // entrance (2,3), scroll (0,-5) => displayed unscaled (16,14).
            double deviceX=26*contentScale*pixelsPerUnit,deviceY=19*contentScale*pixelsPerUnit;
            double guiX=deviceX/guiScale,guiY=deviceY/guiScale;
            var point=new GuiCoordinates(guiScale,pixelsPerUnit).toLogical(new GuiCoordinates.GuiPoint(guiX,guiY));
            NodeId hit=frame.topAt(Set.of(child.id()),point.x(),point.y());
            return new Observed(new Rect(left,top,right-left+1,bottom-top+1),hit);
        }
    }
    @Test void actualRasterAndHitAgreeAcrossContentGuiAndDeviceScales() {
        for(float content:new float[]{.5f,1,1.5f}) for(float device:new float[]{1,2,3}) for(double gui:new double[]{1,2,3}) {
            Observed actual=render(content,device,gui,false);
            float factor=content*device;
            assertEquals(new Rect(16*factor,14*factor,20*factor,10*factor),actual.pixels());
            assertEquals(new NodeId("card"),actual.hit());
        }
    }
    @Test void duplicatedContentScaleIsVisibleInActualPixelsEvenWhenHitDataIsUnchanged() {
        Observed bad=render(1.5f,2,3,true);
        assertNotEquals(new Rect(48,42,60,30),bad.pixels());
        assertEquals(new NodeId("card"),bad.hit(),"same geometry alone would miss the bad render transform");
    }
}
