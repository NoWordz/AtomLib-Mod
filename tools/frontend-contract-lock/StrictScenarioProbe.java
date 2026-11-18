package com.atom.lib.frontend;

import com.atom.lib.*;
import com.atom.lib.controls.*;
import com.atom.lib.design.atomchat.*;
import com.atom.lib.font.*;
import com.atom.lib.text.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.checker.*;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.input.*;
import com.atom.lib.layout.*;
import com.atom.lib.layout.internal.YogaLayoutEngine;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import static com.atom.lib.frontend.contract.ContractDesign.*;

/** Synthetic complete scene: independent legacy UiSession path versus strict mount, never the AtomChat reference. */
public final class StrictScenarioProbe {
    static final Binding BINDING=new Binding(ToggleImplementation.class.getName(),"native-probe/1");
    static ContractDesign design(){
        var role=new EnumMap<Aspect,Requirement>(Aspect.class);for(var a:Aspect.values())role.put(a,Requirement.notApplicable("synthetic action has no such capability"));role.put(Aspect.HOVER,Requirement.required());role.put(Aspect.PRESS,Requirement.required());role.put(Aspect.SOUND,Requirement.required());
        return new ContractDesign(1,"probe","2","native-probe/1",Map.of("root",""),Set.of("enabled"),Map.of("volume",new Parameter(0,1,.4,Unit.RATIO)),
            Map.of("space",new Family("root",Aspect.SPACING,"",Map.of("gap",new NumberValue(12,Unit.U),"max",new NumberValue(800,Unit.U))),"hover",new Family("root",Aspect.HOVER,"",Map.of("normal",new NumberValue(90,Unit.MS))),"press",new Family("root",Aspect.PRESS,"",Map.of("normal",new NumberValue(90,Unit.MS))),"sound",new Family("root",Aspect.SOUND,"",Map.of("button",new SoundValue("BUTTON","click","MASTER","volume",1,0,new NamedCondition.State("enabled"),"")))),
            Map.of("action",new Role(true,role)),Map.of("button",new Recipe("root","action",Map.of(Aspect.HOVER,new Ref("hover","normal"),Aspect.PRESS,new Ref("press","normal"),Aspect.SOUND,new Ref("sound","button")),BINDING,Map.of("label",new Slot(ContentKind.TEXT,true),"activate",new Slot(ContentKind.ACTION,true),"checked",new Slot(ContentKind.NUMBER,true)),Set.of("normal"))),
            Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("space","gap"),new Ref("space","gap"),new Ref("space","gap"),new Ref("space","max"))),
            Map.of("click",new Asset(AssetKind.SOUND,"example:feedback_button","typed sink probe; codec/device separately tested")),Map.of("page",new Page("root",Set.of("button"),"stack",Set.of("normal"))));
    }
    static final class ToggleImplementation implements StrictPageSession.Implementation {
        final ControlContext context;ToggleImplementation(ControlContext context){this.context=context;}
        ToggleModel model(StrictPage.Content c){return new ToggleModel(((StrictPage.Text)c.slots().get("label")).value(),"Native toggle",((StrictPage.Number)c.slots().get("checked")).value()!=0,c.enabled());}
        public Control create(NodeId id,Map<Aspect,Value> values,StrictPageSession.ContentView content){return new Toggle(id,context,model(content.snapshot()),value->content.activate("activate"));}
        public void update(Control control,Map<Aspect,Value> values,StrictPageSession.ContentView content){((Toggle)control).update(model(content.snapshot()));}
    }
    interface Driver extends AutoCloseable {PresentationFrame paint(Canvas canvas,long millis);void pointer(PointerEvent event);void enabled(boolean enabled);void volume(double volume);ControlObservation observation();void close();}
    static final class Counters {boolean checked;int committed;long millis;double volume=.4;final List<UiSoundIntent> outputs=new ArrayList<>();}
    static StrictPage.Content content(Counters counter,boolean enabled){return new StrictPage.Content(Map.of("label",new StrictPage.Text("Apply"),"activate",new StrictPage.Action(()->{counter.committed++;counter.checked=!counter.checked;}),"checked",new StrictPage.Number(counter.checked?1:0)),enabled);}
    static Driver strict(ControlContext context,Counters counters,boolean ignoreMoves,boolean ignorePress){
        byte[] bytes=ContractCodec.encode(design());var p=ContractRegistry.load(bytes,ContractRegistry.manifest(bytes,bytes),Set.of("native-probe/1")).page("page");
        var page=StrictPage.builder(p).component("apply",p.recipe("button"),content(counters,true)).build();
        var sink=new UiSoundSink(){public boolean supports(String channel){return channel.equals("MASTER");}public boolean available(String resource){return resource.equals("example:feedback_button");}public void play(UiSoundIntent intent){counters.outputs.add(intent);}};
        var session=new StrictPageSession(page,Map.of(BINDING,new ToggleImplementation(context)),sink);
        return new Driver(){long frame,previous;boolean enabled=true;public PresentationFrame paint(Canvas canvas,long ms){session.update("apply",content(counters,enabled));var time=new FrameTime(++frame,ms*1_000_000,(ms-previous)/1000d);previous=ms;return session.render(canvas,new Viewport(480,320,1),time,Map.of("volume",counters.volume));}public void pointer(PointerEvent event){if(!(ignoreMoves&&event.type()==PointerEvent.Type.MOVE)&&!(ignorePress&&(event.type()==PointerEvent.Type.PRESS||event.type()==PointerEvent.Type.RELEASE)))session.pointer(event);}public void enabled(boolean next){enabled=next;session.update("apply",content(counters,enabled));}public void volume(double volume){counters.volume=volume;}public ControlObservation observation(){return session.observations().get("apply");}public void close(){session.close();}};
    }
    static Driver legacy(ControlContext context,Counters counters){
        var id=new NodeId("probe/page/apply");var button=new Toggle(id,context,new ToggleModel("Apply","Native toggle",false,true),value->{counters.checked=value;counters.committed++;if(counters.volume>0)counters.outputs.add(new UiSoundIntent("BUTTON","example:feedback_button","MASTER",(float)counters.volume,1,0,counters.millis));});
        var session=new UiSession(List.of(button));var yoga=new YogaLayoutEngine();
        return new Driver(){long frame,previous;boolean enabled=true;public PresentationFrame paint(Canvas canvas,long ms){button.update(new ToggleModel("Apply","Native toggle",counters.checked,enabled));var viewport=new Viewport(480,320,1);session.prepare(viewport);var node=button.node(LayoutStyle.column().flex(0,0,Length.auto()));var tree=new LayoutTree(new LayoutNode(new NodeId("probe/page/root"),LayoutStyle.column().size(Length.points(480),Length.auto()).spacing(12,12),List.of(node),null,0));var presented=PresentationFrame.create(tree,yoga.layout(tree,viewport),viewport,Map.of(),new PresentationFrame.Offset(0,0));session.paint(canvas,presented,viewport,new FrameTime(++frame,ms*1_000_000,(ms-previous)/1000d),true);previous=ms;counters.millis=ms;return presented;}public void pointer(PointerEvent event){if(event.type()==PointerEvent.Type.MOVE)session.hover(event.x(),event.y());session.pointer(event);}public void enabled(boolean next){enabled=next;button.update(new ToggleModel("Apply","Native toggle",counters.checked,enabled));}public void volume(double volume){counters.volume=volume;}public ControlObservation observation(){return button.observation();}public void close(){session.close();yoga.close();}};
    }
    static BufferedImage snapshot(Surface surface)throws Exception{try(var image=surface.makeImageSnapshot();var data=image.encodeToData(EncodedImageFormat.PNG)){return ImageIO.read(new java.io.ByteArrayInputStream(data.getBytes()));}}
    static ScenarioCheck.Sample capture(Driver driver,Surface surface,Counters counters,String key,long ms,ScenarioCheck.Phase phase,boolean enabled,Path directory)throws Exception{
        surface.getCanvas().clear(0);var frame=driver.paint(surface.getCanvas(),ms);var image=snapshot(surface);ImageIO.write(image,"PNG",directory.resolve(key+".png").toFile());var box=frame.box(new NodeId("probe/page/apply")).border();
        var sounds=counters.outputs.stream().map(s->new SoundObservation(s.event(),s.resource(),s.channel(),s.volume(),s.pitch(),s.frameMillis())).toList();
        var observed=driver.observation();if(observed.enabled()!=enabled)throw new IllegalStateException("CONTROL_ENABLED_MISMATCH actual rendered control");
        var sample=new ScenarioCheck.Sample(key,"page","button","normal",ms*1_000_000,phase,observed.enabled(),Map.of("enabled",observed.enabled()),image,Map.of("paintedWidth",(double)box.width(),"actions.committed",(double)counters.committed,"motion.HOVER",(double)observed.hover(),"motion.PRESS",(double)observed.press()),sounds,Set.of("example:feedback_button"));counters.committed=0;counters.outputs.clear();return sample;
    }
    public static void main(String[] args)throws Exception{
        String mode=args[0],invocation=args[2];Path directory=Path.of(args[1]);Files.createDirectories(directory);byte[] design=ContractCodec.encode(design());String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(design));
        List<ScenarioCheck.Sample> samples=new ArrayList<>();
        try(var runtime=AtomLibRuntime.open("fcl-native-scene");var fonts=new FontRegistry(runtime);var text=new TextService(fonts,TextBreakPolicy.CJK_UI);var surface=Surface.makeRasterN32Premul(480,320)){
            fonts.registerDefaults();AtomChatFonts.register(fonts);var context=new ControlContext(text,new AtomChatControlSkin(AtomChatControlStyle.defaults()));var counters=new Counters();
            try(var driver=mode.equals("legacy")?legacy(context,counters):strict(context,counters,mode.equals("mutant-hover"),mode.equals("mutant-press"))){
                driver.paint(surface.getCanvas(),0);driver.paint(surface.getCanvas(),160);
                samples.add(capture(driver,surface,counters,"initial",300,ScenarioCheck.Phase.INITIAL,true,directory));
                var rect=driver.paint(surface.getCanvas(),301).box(new NodeId("probe/page/apply")).border();float x=rect.x()+rect.width()/2,y=rect.y()+rect.height()/2;
                driver.pointer(new PointerEvent(PointerEvent.Type.MOVE,x,y,0));driver.paint(surface.getCanvas(),316);
                samples.add(capture(driver,surface,counters,"middle",350,ScenarioCheck.Phase.INTERMEDIATE,true,directory));
                samples.add(capture(driver,surface,counters,"settled",500,ScenarioCheck.Phase.SETTLED,true,directory));
                driver.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));driver.paint(surface.getCanvas(),516);
                samples.add(capture(driver,surface,counters,"press-middle",550,ScenarioCheck.Phase.INTERMEDIATE,true,directory));
                samples.add(capture(driver,surface,counters,"press-settled",650,ScenarioCheck.Phase.SETTLED,true,directory));driver.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                samples.add(capture(driver,surface,counters,"commit",660,ScenarioCheck.Phase.SETTLED,true,directory));
                driver.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));driver.pointer(new PointerEvent(PointerEvent.Type.MOVE,0,310,0));driver.pointer(new PointerEvent(PointerEvent.Type.RELEASE,0,310,0));
                samples.add(capture(driver,surface,counters,"cancel",750,ScenarioCheck.Phase.CANCELLED,true,directory));
                driver.enabled(false);driver.paint(surface.getCanvas(),800);driver.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));driver.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));samples.add(capture(driver,surface,counters,"disabled",850,ScenarioCheck.Phase.DISABLED,false,directory));
                driver.enabled(true);driver.volume(0);driver.paint(surface.getCanvas(),900);driver.pointer(new PointerEvent(PointerEvent.Type.PRESS,x,y,0));driver.pointer(new PointerEvent(PointerEvent.Type.RELEASE,x,y,0));
                samples.add(capture(driver,surface,counters,"muted",950,ScenarioCheck.Phase.SETTLED,true,directory));
            }
        }
        Path file=directory.resolve("capture.fcs");SceneCaptureIO.write(file,new SceneCaptureIO.Capture(invocation,digest,samples));
        if(!mode.equals("legacy")){
            var actual=SceneCaptureIO.read(file);SceneProcessCheck.requireCurrent(actual,invocation,digest);var old=SceneCaptureIO.read(Path.of(args[3]));if(!old.designDigest().equals(digest))throw new IllegalArgumentException("SCENE_DESIGN_MISMATCH reference");
            var baselines=old.samples().stream().map(s->{Map<String,Double> tolerances=new HashMap<>();s.observations().keySet().forEach(key->tolerances.put(key,0d));return new ScenarioCheck.Baseline(s,0,tolerances);}).toList();var errors=ScenarioCheck.compare(design(),baselines,actual.samples());Files.write(directory.resolve("comparison.txt"),errors);if(!errors.isEmpty())throw new IllegalStateException(String.join("\n",errors));
        }
        System.out.println("SCENE_CAPTURE_VALID "+mode+" samples="+samples.size()+" digest="+digest);
    }
}
