"""Read-only real controller/output-boundary and native resource feasibility probes."""
from pathlib import Path
import argparse, json, os, shutil, subprocess, tempfile


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--atomchat',type=Path,default=Path('D:/AtomChat-Main'))
    p.add_argument('--java-home',type=Path,required=True)
    a=p.parse_args()
    root=Path(__file__).resolve().parents[2]
    out=Path(tempfile.mkdtemp(prefix='sound-adapters-',dir=root/'build/frontend-contract-lock'))
    results=[]
    def run(name,cmd,success=True):
        r=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,encoding='utf-8',errors='replace')
        (out/(name+'.log')).write_text(r.stdout+r.stderr,encoding='utf-8')
        if (r.returncode==0)!=success: raise RuntimeError(f'{name}: unexpected exit {r.returncode}; {out}')
        results.append(dict(name=name,exit=r.returncode,expected_success=success,command=[str(x) for x in cmd]))
        print(name+': PASS',flush=True)
    def write(base,relative,source):
        f=base/relative;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(source,encoding='utf-8')
    for platform in ['1.20.1-forge','1.21.1-fabric','1.21.1-neoforge']:
        src=out/platform/'sources';classes=out/platform/'classes';classes.mkdir(parents=True)
        common={
          'probe/Output.java':'''package probe; import java.util.*; public class Output {
            public record Event(String id,float pitch,float volume,String channel){}
            public static final List<Event> events=new ArrayList<>();
            public static void emit(String id,float pitch,float volume){events.add(new Event(id,pitch,volume,"MASTER"));}
          }''',
          'com/atom/chat/config/AtomChatConfig.java':'''package com.atom.chat.config; public class AtomChatConfig {
            private static final AtomChatConfig VALUE=new AtomChatConfig(); public static AtomChatConfig get(){return VALUE;}
            public boolean feedbackSoundEnabled=true,feedbackButtonEnabled=true,feedbackSendEnabled=true,feedbackPanelEnabled=true,debug=false;
            public float feedbackVolume=.6f;
          }''',
          'com/atom/chat/AtomChat.java':'''package com.atom.chat; public class AtomChat {public static final Log LOGGER=new Log();public static class Log{public void info(String s,Object... args){}}}''',
          'com/atom/chat/diagnostics/AtomChatLog.java':'''package com.atom.chat.diagnostics; public class AtomChatLog {public static String message(String category,String text){return text;}}''',
        }
        fabric='fabric' in platform
        resource='net.minecraft.util.Identifier' if fabric else 'net.minecraft.resources.ResourceLocation'
        event='net.minecraft.sound.SoundEvent' if fabric else 'net.minecraft.sounds.SoundEvent'
        client='net.minecraft.client.MinecraftClient' if fabric else 'net.minecraft.client.Minecraft'
        instance='net.minecraft.client.sound.PositionedSoundInstance' if fabric else 'net.minecraft.client.resources.sounds.SimpleSoundInstance'
        manager='net.minecraft.client.sound.SoundManager' if fabric else 'net.minecraft.client.sounds.SoundManager'
        def cls(fqn,body): return 'package '+fqn.rsplit('.',1)[0]+'; public class '+fqn.rsplit('.',1)[1]+' {'+body+'}'
        common[resource.replace('.','/')+'.java']=cls(resource,'private final String id;private '+resource.split('.')[-1]+'(String id){this.id=id;} public static '+resource+' '+('of' if fabric else 'fromNamespaceAndPath')+'(String ns,String path){return new '+resource+'(ns+":"+path);} public String toString(){return id;}')
        common[event.replace('.','/')+'.java']=cls(event,'private '+resource+' id;public static '+event+' '+('of' if fabric else 'createVariableRangeEvent')+'('+resource+' id){var v=new '+event+'();v.id=id;return v;} public '+resource+' '+('getId' if fabric else 'getLocation')+'(){return id;}')
        common[instance.replace('.','/')+'.java']=cls(instance,'public String id; public float pitch,volume;public static '+instance+' '+('master' if fabric else 'forUI')+'('+event+' event,float pitch,float volume){var v=new '+instance+'();v.id=event.'+('getId' if fabric else 'getLocation')+'().toString();v.pitch=pitch;v.volume=volume;return v;}')
        common[manager.replace('.','/')+'.java']=cls(manager,'public void play('+instance+' value){probe.Output.emit(value.id,value.pitch,value.volume);}')
        common[client.replace('.','/')+'.java']=cls(client,'private static final '+client+' VALUE=new '+client+'();public static '+client+' getInstance(){return VALUE;}public '+manager+' getSoundManager(){return new '+manager+'();}')
        if fabric:
            common['net/minecraft/registry/Registries.java']='package net.minecraft.registry; public class Registries{public static final Object SOUND_EVENT=new Object();}'
            common['net/minecraft/registry/Registry.java']='package net.minecraft.registry; public class Registry{public static void register(Object r,Object id,Object value){}}'
        else:
            common['net/minecraft/core/registries/Registries.java']='package net.minecraft.core.registries; public class Registries{public static final Object SOUND_EVENT=new Object();}'
            register='net.neoforged.neoforge.registries.RegisterEvent' if 'neoforge' in platform else 'net.minecraftforge.registries.RegisterEvent'
            common[register.replace('.','/')+'.java']=cls(register,'public void register(Object r,Object id,java.util.function.Supplier<?> value){value.get();}')
        for relative,source in common.items(): write(src,relative,source)
        for f in [a.atomchat/'shared/src/main/java/com/atom/chat/audio/FeedbackSoundPolicy.java',a.atomchat/'shared/src/main/java/com/atom/chat/audio/FeedbackCue.java',a.atomchat/f'platforms/{platform}/src/main/java/com/atom/chat/audio/FeedbackSoundController.java']:
            target=src/'com/atom/chat/audio'/f.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy(f,target)
        write(src,'probe/ControllerProbe.java','''package probe; import com.atom.chat.audio.*;import com.atom.chat.config.AtomChatConfig;
          public class ControllerProbe {public static void main(String[] args){
            var c=AtomChatConfig.get(); FeedbackSoundController.play(FeedbackCue.BUTTON);
            check(1,"atomchat:feedback_button",.6f);Output.events.clear();
            FeedbackSoundController.beginAction();FeedbackSoundController.play(FeedbackCue.BUTTON);FeedbackSoundController.play(FeedbackCue.BUTTON);
            FeedbackSoundController.play(FeedbackCue.PANEL_OPEN);FeedbackSoundController.play(FeedbackCue.SEND);
            if(!Output.events.isEmpty())throw new AssertionError("premature output");FeedbackSoundController.endAction();check(1,"atomchat:feedback_send",.6f);Output.events.clear();
            FeedbackSoundController.beginAction();FeedbackSoundController.endAction();if(!Output.events.isEmpty())throw new AssertionError("empty action output");
            c.feedbackSoundEnabled=false;FeedbackSoundController.play(FeedbackCue.BUTTON);if(!Output.events.isEmpty())throw new AssertionError("global mute");
            c.feedbackSoundEnabled=true;c.feedbackButtonEnabled=false;FeedbackSoundController.play(FeedbackCue.BUTTON);if(!Output.events.isEmpty())throw new AssertionError("cue mute");
            c.feedbackButtonEnabled=true;c.feedbackVolume=Float.NaN;FeedbackSoundController.play(FeedbackCue.BUTTON);if(!Output.events.isEmpty())throw new AssertionError("nonfinite volume");
            c.feedbackVolume=3;FeedbackSoundController.play(FeedbackCue.BUTTON);check(1,"atomchat:feedback_button",1f);
            System.out.println("real controller -> typed SDK output boundary: PASS; no audio device");
          }static void check(int count,String id,float volume){if(Output.events.size()!=count)throw new AssertionError("event count");var e=Output.events.get(0);if(!e.id().equals(id)||e.pitch()!=1f||e.volume()!=volume||!e.channel().equals("MASTER"))throw new AssertionError("parameters");}}
        ''')
        run(platform+'-compile',[a.java_home/'bin/javac.exe','--release','17','-d',classes,*sorted(src.rglob('*.java'))])
        run(platform+'-controller',[a.java_home/'bin/java.exe','-cp',classes,'probe.ControllerProbe'])
    inputs=root/'build/frontend-contract-lock/atomchat-forge-observed/classpath.txt'
    cp=[Path(x) for x in inputs.read_text().splitlines() if x.strip()]
    cache=Path.home()/'.gradle/caches/modules-2/files-2.1/org.lwjgl'
    for module in ['lwjgl','lwjgl-stb']:
        cp.extend((cache/module/'3.3.1').rglob(f'{module}-3.3.1-natives-windows.jar'))
    native=out/'native';native.mkdir()
    run('native-sdk-compile',[a.java_home/'bin/javac.exe','-cp',os.pathsep.join(map(str,cp)),'-d',native,root/'tools/frontend-contract-lock/NativeSoundResourceProbe.java'])
    resources=[]
    for platform in ['1.20.1-forge','1.21.1-fabric','1.21.1-neoforge']:
        assets=a.atomchat/f'platforms/{platform}/src/main/resources/assets/atomchat'
        definitions=json.loads((assets/'sounds.json').read_text())
        for cue in ['feedback_button','feedback_panel_open','feedback_send']:
            if definitions[cue]['sounds']!=['atomchat:'+cue]: raise RuntimeError('unexpected reviewed resource mapping '+platform+'/'+cue)
            resources.append(assets/'sounds'/f'{cue}.ogg')
    run('native-resources',[a.java_home/'bin/java.exe','-cp',os.pathsep.join(map(str,[native,*cp])),'NativeSoundResourceProbe',*resources])
    broken=out/'feedback_button.ogg';broken.write_bytes(b'not an Ogg stream')
    run('broken-resource-rejected',[a.java_home/'bin/java.exe','-cp',os.pathsep.join(map(str,[native,*cp])),'NativeSoundResourceProbe',broken],False)
    failure=(out/'broken-resource-rejected.log').read_text(encoding='utf-8')
    if 'OggAudioStream' not in failure or not any(x in failure for x in ['java.io.IOException','java.io.EOFException']):
        raise RuntimeError('negative failed outside the expected Ogg decoder path: '+str(out))
    (out/'results.json').write_text(json.dumps(dict(scope='STAGE_ONE_PROBE',device_output=False,controller_boundary='typed SDK test doubles; real native decoder; factory semantics inspected separately in SDK bytecode',results=results),ensure_ascii=False,indent=2),encoding='utf-8')
    print('Evidence: '+str(out),flush=True)


if __name__=='__main__': main()
