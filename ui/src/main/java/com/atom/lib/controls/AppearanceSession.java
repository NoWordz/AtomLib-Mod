// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;

import com.atom.lib.design.atomchat.*;
import com.atom.lib.design.atomdefault.*;
import com.atom.lib.geometry.*;
import com.atom.lib.layout.*;
import com.atom.lib.motion.*;
import com.atom.lib.text.TextService;
import io.github.humbleui.skija.Canvas;
import java.util.*;

/** Window-local appearance, input and feedback. Borrowed text/runtime resources outlive this window. */
public final class AppearanceSession implements AutoCloseable {
    private final Map<String,AtomDefaultPalette> themes=new LinkedHashMap<>();
    private final Map<String,MotionSpec> motions=new LinkedHashMap<>();
    private final ControlSkin skin;
    private final ControlContext context;
    private final UiSession input=new UiSession(List.of());
    private final FeedbackLayer ripples=new FeedbackLayer();
    private final AutoCloseable owned;
    private String theme="minimal",motion="standard";
    private float radius=20,tint=1;private boolean halfScreen,closed;
    private long revision=1,lastFrame=Long.MIN_VALUE,lastNow=Long.MIN_VALUE;
    private Frame active;
    private ThemeSnapshot sampledTheme;
    private MotionSpec sampledMotion;
    private Design.RippleSink feedback=(x,y,w,h,r,s)->{};
    public AppearanceSession(TextService text){this(text,null);}
    public AppearanceSession(TextService text,AutoCloseable owned){this(text,owned,null);}
    public AppearanceSession(TextService text,AutoCloseable owned,ControlSkin customSkin){
        this.owned=owned;AtomDefault.presets().forEach(p->themes.put(p.id(),p));MotionSpec.presets().forEach(p->motions.put(p.id(),p));
        skin=customSkin==null?AtomDefault.skin(themes.get(theme)):customSkin;context=new ControlContext(text,skin);
    }
    public ControlContext context(){requireOpen();return context;}
    public UiSession input(){requireOpen();return input;}
    public boolean isClosed(){return closed;}
    public String themeId(){requireOpen();return theme;}
    public String motionId(){requireOpen();return motion;}
    public List<AtomDefaultPalette> themes(){requireOpen();return List.copyOf(themes.values());}
    public List<MotionSpec> motions(){requireOpen();return List.copyOf(motions.values());}
    public void themeCatalog(ThemeCatalog catalog){requireOpen();Objects.requireNonNull(catalog);catalog.find(theme);themes.clear();catalog.presets().forEach(p->themes.put(p.id(),p));revision++;}
    public FeedbackLayer feedbackLayer(){requireOpen();return ripples;}
    public void theme(AtomDefaultPalette value){requireOpen();Objects.requireNonNull(value);themes.put(value.id(),value);revision++;}
    public void motion(MotionSpec value){requireOpen();Objects.requireNonNull(value);motions.put(value.id(),value);revision++;}
    public void selectTheme(String id){requireOpen();if(!themes.containsKey(id))throw new IllegalArgumentException("Unknown theme: "+id);if(!theme.equals(id)){theme=id;revision++;}}
    public void selectMotion(String id){requireOpen();if(!motions.containsKey(id))throw new IllegalArgumentException("Unknown motion: "+id);if(!motion.equals(id)){motion=id;revision++;}}
    public void geometry(float radius,boolean halfScreen,float tint){requireOpen();new ThemeSnapshot(revision,themes.get(theme),radius,halfScreen,tint);this.radius=radius;this.halfScreen=halfScreen;this.tint=tint;revision++;}
    public void feedback(Design.RippleSink sink){requireOpen();feedback=Objects.requireNonNull(sink);}
    /** Independent preview; cancellation is just close(). The host explicitly applies a confirmed choice. */
    public AppearanceSession preview(){requireOpen();var p=new AppearanceSession(context.text());p.themes.clear();p.themes.putAll(themes);p.motions.clear();p.motions.putAll(motions);p.theme=theme;p.motion=motion;p.radius=radius;p.halfScreen=halfScreen;p.tint=tint;return p;}
    public Frame beginFrame(FrameTime time,Viewport viewport){
        requireOpen();Objects.requireNonNull(time);Objects.requireNonNull(viewport);
        if(active!=null)throw new IllegalStateException("Frame already open");
        if(time.frameId()<lastFrame||time.nowNanos()<lastNow)throw new IllegalArgumentException("Nonmonotonic frame");
        if(time.frameId()!=lastFrame||sampledTheme==null){sampledTheme=new ThemeSnapshot(revision,themes.get(theme),radius,halfScreen,tint);sampledMotion=motions.get(motion);}
        lastFrame=time.frameId();lastNow=time.nowNanos();
        var snapshot=sampledTheme;var policy=sampledMotion;
        skin.beginAppearance(snapshot,policy);
        input.prepare(viewport);input.advanceAppearance(time,policy);active=new Frame(time,viewport,snapshot,policy,feedback);return active;
    }
    private void requireOpen(){if(closed)throw new IllegalStateException("Appearance session closed");}
    @Override public void close(){
        if(closed)return;closed=true;if(active!=null)active.close();
        try{input.close();}finally{ripples.close();if(owned!=null)try{owned.close();}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Closing session resources",e);}}
    }
    /** Frame borrows Canvas and never starts a second rendering backend. Close in a finally/try-with-resources. */
    public final class Frame implements AutoCloseable {
        private final FrameTime time;private final Viewport viewport;private final ThemeSnapshot snapshot;private final MotionSpec policy;private final Design.RippleSink sink;
        private boolean ended;
        private Frame(FrameTime time,Viewport viewport,ThemeSnapshot snapshot,MotionSpec policy,Design.RippleSink sink){this.time=time;this.viewport=viewport;this.snapshot=snapshot;this.policy=policy;this.sink=sink;}
        private void check(){requireOpen();if(ended||active!=this)throw new IllegalStateException("Frame ended");}
        public ThemeSnapshot theme(){check();return snapshot;}
        public MotionSpec motion(){check();return policy;}
        public FrameTime time(){check();return time;}
        public void paint(Canvas canvas,PresentationFrame displayed){
            check();var factors=new HashMap<NodeId,Float>();
            for(var control:input.controls())if(displayed.contains(control.id())){var r=displayed.box(control.id()).border();float scale=policy.scale(control.hover,control.press,Math.max(r.width(),r.height()),2);if(scale!=1)factors.put(control.id(),scale);}
            input.paint(canvas,displayed.feedback(factors),viewport,time,policy.enabled());
            ripples.paint(canvas,time,policy);
        }
        public void card(Canvas canvas,Rect bounds,float radius,float hover){check();UiCards.drawCard(canvas,bounds.x(),bounds.y(),bounds.width(),bounds.height(),radius,hover,snapshot.controls(),sink);}
        /** Host supplies a real backdrop operation; false uses the readable solid fallback. */
        public void panel(Canvas canvas,Rect bounds,float radius,java.util.function.BooleanSupplier backdrop){
            check();boolean available=snapshot.palette().blurEnabled()&&Objects.requireNonNull(backdrop).getAsBoolean();
            SkiaDraw.drawRoundedRect(canvas,bounds.x(),bounds.y(),bounds.width(),bounds.height(),radius,snapshot.panelColor(available));
        }
        @Override public void close(){if(ended)return;ended=true;if(active==this)active=null;}
    }
}
