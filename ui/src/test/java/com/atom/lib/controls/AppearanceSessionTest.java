package com.atom.lib.controls;

import com.atom.lib.design.atomdefault.*;
import com.atom.lib.design.atomchat.*;
import com.atom.lib.geometry.*;
import com.atom.lib.motion.*;
import io.github.humbleui.skija.Surface;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppearanceSessionTest {
    @Test void composedClippedSlideCanPaintPartlyOutsideTheViewport(){
        try(var f=new ControlFixture();var surface=Surface.makeRasterN32Premul(480,860)){
            f.show(480);var moved=com.atom.lib.layout.PresentationFrame.compose(java.util.List.of(f.frame.transformed(1,0,0,-20,0).clipped(f.viewport.bounds())));
            assertDoesNotThrow(()->f.session.paint(surface.getCanvas(),moved,f.viewport,new FrameTime(++f.ticks,f.ticks*16_000_000L,.016),false));
        }
    }
    @Test void frameFreezesThemeAndMotionAndPreviewIsIndependent() {
        try(var f=new ControlFixture();var a=AtomDefault.createSession(f.text);var b=AtomDefault.createSession(f.text)) {
            a.selectTheme("dusk");
            try(var frame=a.beginFrame(new FrameTime(1,0,0),new Viewport(120,80,1))) {
                a.selectTheme("mint");a.selectMotion("off");
                assertEquals("dusk",frame.theme().palette().id());assertTrue(frame.motion().enabled());
                assertThrows(IllegalStateException.class,()->a.beginFrame(new FrameTime(2,1,0),new Viewport(120,80,1)));
            }
            try(var frame=a.beginFrame(new FrameTime(2,1,0),new Viewport(120,80,1))) {
                assertEquals("mint",frame.theme().palette().id());assertFalse(frame.motion().enabled());
            }
            try(var preview=a.preview()) {preview.selectTheme("raven");assertEquals("mint",a.themeId());}
            assertEquals("minimal",b.themeId());assertEquals("standard",b.motionId());
            a.close();assertFalse(b.isClosed());assertDoesNotThrow(()->f.text.layout(new com.atom.lib.text.TextRequest("ok",AtomChatFonts.BODY,12,-1),new com.atom.lib.layout.Constraints(com.atom.lib.layout.Constraints.Axis.undefined(),com.atom.lib.layout.Constraints.Axis.undefined())));
        }
    }
    @Test void directDrawingSurvivesLegacyGlobalOverwriteAndRoutesOnlyLocalFeedback() {
        var previous=Design.host();int[] calls={0,0};
        try(var f=new ControlFixture();var a=AtomDefault.createSession(f.text);var b=AtomDefault.createSession(f.text)) {
            a.feedback((x,y,w,h,r,s)->calls[0]++);b.feedback((x,y,w,h,r,s)->calls[1]++);
            byte[] first=draw(a,1);
            Design.host(AtomDefault.presets().get(3).controlStyle(0,false,1));
            Design.rippleSink((x,y,w,h,r,s)->fail("Global ripple used"));
            assertArrayEquals(first,draw(a,2));draw(b,1);assertArrayEquals(new int[]{2,1},calls);
        } finally {Design.host(previous);Design.rippleSink(null);}
    }
    @Test void customCatalogAndSameFrameSnapshotStayLocal(){
        try(var f=new ControlFixture();var a=AtomDefault.createSession(f.text);var b=AtomDefault.createSession(f.text)){
            var custom=new AtomDefaultPalette("custom",1,2,3,4,5,6,1,false);var original=AtomDefault.catalog();a.themeCatalog(original.with(custom));a.selectTheme("custom");
            try(var frame=a.beginFrame(new FrameTime(1,0,0),new Viewport(100,100,1))){assertEquals("custom",frame.theme().palette().id());a.selectTheme("dusk");assertEquals("custom",frame.theme().palette().id());}
            try(var frame=a.beginFrame(new FrameTime(1,0,0),new Viewport(100,100,1))){assertEquals("custom",frame.theme().palette().id());}
            try(var frame=a.beginFrame(new FrameTime(2,1,0),new Viewport(100,100,1))){assertEquals("dusk",frame.theme().palette().id());}
            assertThrows(IllegalArgumentException.class,()->original.find("custom"));assertThrows(IllegalArgumentException.class,()->b.selectTheme("custom"));
        }
    }
    private byte[] draw(AppearanceSession session,long frameId) {
        try(var surface=Surface.makeRasterN32Premul(120,80);var frame=session.beginFrame(new FrameTime(frameId,frameId,0),new Viewport(120,80,1))) {
            frame.card(surface.getCanvas(),new Rect(10,10,100,60),8,1);
            try(var image=surface.makeImageSnapshot();var data=image.encodeToData()) {return data.getBytes();}
        }
    }
}
