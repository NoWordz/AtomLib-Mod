"""Executes real Lib sound adapter copies at a typed SDK boundary, never on an audio device."""
from pathlib import Path
import argparse, json, subprocess, tempfile


def main():
    p=argparse.ArgumentParser();p.add_argument('--java-home',type=Path,required=True);a=p.parse_args()
    root=Path(__file__).resolve().parents[2]
    out=Path(tempfile.mkdtemp(prefix='contract-sound-outputs-',dir=root/'build/frontend-contract-lock'))
    results=[]
    def run(name,args,expected=True,marker=None):
        command=[str(x) for x in args];r=subprocess.run(command,capture_output=True,text=True,encoding='utf-8',errors='replace')
        text=r.stdout+r.stderr;(out/(name+'.log')).write_text(text,encoding='utf-8')
        passed=((r.returncode==0)==expected and (marker is None or marker in text))
        results.append(dict(name=name,command=command,exit=r.returncode,passed=passed,expected_marker=marker))
        if not passed:raise RuntimeError(name+' failed: '+str(out))
        print(name+': PASS',flush=True)
    def source(base,name,text):
        f=base/(name.replace('.','/')+'.java');f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text,encoding='utf-8')
    def klass(name,body):return 'package '+name.rsplit('.',1)[0]+'; public class '+name.rsplit('.',1)[1]+' {'+body+'}'
    for platform in ['1.20.1-forge','1.21.1-fabric','1.21.1-neoforge']:
        base=out/platform;src=base/'src';classes=base/'classes';classes.mkdir(parents=True)
        fabric='fabric' in platform
        resource='net.minecraft.util.Identifier' if fabric else 'net.minecraft.resources.ResourceLocation'
        event='net.minecraft.sound.SoundEvent' if fabric else 'net.minecraft.sounds.SoundEvent'
        manager='net.minecraft.client.sound.SoundManager' if fabric else 'net.minecraft.client.sounds.SoundManager'
        weighted='net.minecraft.client.sound.WeightedSoundSet' if fabric else 'net.minecraft.client.sounds.WeighedSoundEvents'
        client='net.minecraft.client.MinecraftClient' if fabric else 'net.minecraft.client.Minecraft'
        instance='net.minecraft.client.sound.PositionedSoundInstance' if fabric else 'net.minecraft.client.resources.sounds.SimpleSoundInstance'
        source(src,'probe.Output','''package probe; import java.util.*; public final class Output {
            public record Event(String resource,float pitch,float volume,String channel){}
            public static final List<Event> events=new ArrayList<>();public static int weight=1;
        }''')
        source(src,resource,klass(resource,'public final String id; private '+resource.split('.')[-1]+'(String id){this.id=id;}public static '+resource+' tryParse(String id){return id.matches("[a-z]+:[a-z_/]+")?new '+resource+'(id):null;}'))
        source(src,event,klass(event,'public '+resource+' id;public static '+event+' '+('of' if fabric else 'createVariableRangeEvent')+'('+resource+' id){var e=new '+event+'();e.id=id;return e;}'))
        source(src,weighted,klass(weighted,'public int getWeight(){return probe.Output.weight;}'))
        source(src,instance,klass(instance,'public probe.Output.Event output;public static '+instance+' '+('master' if fabric else 'forUI')+'('+event+' event,float pitch,float volume){var s=new '+instance+'();s.output=new probe.Output.Event(event.id.id,pitch,volume,"MASTER");return s;}'))
        source(src,manager,klass(manager,'public '+weighted+' '+('get' if fabric else 'getSoundEvent')+'('+resource+' id){return id.id.equals("atomchat:feedback_button")?new '+weighted+'():null;}public void play('+instance+' sound){probe.Output.events.add(sound.output);}'))
        source(src,client,klass(client,'private static final '+client+' INSTANCE=new '+client+'();public static '+client+' getInstance(){return INSTANCE;}public '+manager+' getSoundManager(){return new '+manager+'();}'))
        for name in ['UiSoundSink','UiSoundIntent']:
            source(src,'com.atom.lib.frontend.contract.'+name,(root/f'core/src/main/java/com/atom/lib/frontend/contract/{name}.java').read_text(encoding='utf-8'))
        adapter=root/f'platforms/{platform}/src/main/java/com/atom/lib/client/input/ContractSoundOutput.java'
        original=adapter.read_text(encoding='utf-8');source(src,'com.atom.lib.client.input.ContractSoundOutput',original)
        source(src,'probe.Probe','''package probe;
            import com.atom.lib.frontend.contract.*;import com.atom.lib.client.input.ContractSoundOutput;import java.util.*;
            public final class Probe {
              public static void main(String[] args){var sink=new ContractSoundOutput();
                if(!sink.supports("MASTER")||sink.supports("BLOCKS"))throw new AssertionError("SOUND_CHANNEL_SUPPORT");
                if(!sink.available("atomchat:feedback_button")||sink.available("atomchat:missing")||sink.available("bad id"))throw new AssertionError("SOUND_RESOURCE_RESOLUTION");
                Output.weight=0;if(sink.available("atomchat:feedback_button"))throw new AssertionError("EMPTY_SOUND_EVENT");Output.weight=1;
                var intent=new UiSoundIntent("BUTTON","atomchat:feedback_button","MASTER",.4f,1.2f,7,1048);
                sink.play(intent);if(!Output.events.equals(List.of(new Output.Event("atomchat:feedback_button",1.2f,.4f,"MASTER"))))throw new AssertionError("SOUND_OUTPUT_PARAMETERS "+Output.events);
                try{sink.play(new UiSoundIntent("BUTTON","atomchat:feedback_button","BLOCKS",.4f,1.2f,8,1064));throw new AssertionError("UNSUPPORTED_CHANNEL_PLAYED");}catch(IllegalStateException expected){}
                try{sink.play(new UiSoundIntent("BUTTON","atomchat:missing","MASTER",.4f,1.2f,9,1080));throw new AssertionError("MISSING_RESOURCE_PLAYED");}catch(IllegalStateException expected){}
                if(Output.events.size()!=1)throw new AssertionError("SOUND_OUTPUT_COUNT");System.out.println("HOST_OUTPUT_BOUNDARY_PASS; no device or real Minecraft sound manager executed");
              }
            }''')
        def compile(label):
            args=base/(label+'.args');args.write_text('\n'.join('"'+str(x).replace('\\','/')+'"' for x in ['-encoding','UTF-8','-proc:none','--release','17','-d',classes]+list(src.rglob('*.java'))),encoding='utf-8')
            run(platform+'-'+label,[a.java_home/'bin/javac.exe','@'+str(args)])
        compile('compile');run(platform+'-output',[a.java_home/'bin/java.exe','-cp',classes,'probe.Probe'])
        swapped=original.replace('intent.pitch(),intent.volume()','intent.volume(),intent.pitch()');assert swapped!=original
        source(src,'com.atom.lib.client.input.ContractSoundOutput',swapped);compile('mutant-compile')
        run(platform+'-mutant-output',[a.java_home/'bin/java.exe','-cp',classes,'probe.Probe'],False,'SOUND_OUTPUT_PARAMETERS')
    report=dict(scope='REAL_ADAPTER_SOURCE_WITH_TYPED_SDK_DOUBLES',device_output=False,
                limits=['Real SDK compilation is a separate check','Normal SDK factory channel semantics verified separately', 'No actual SoundManager/engine/device, input event binding or listening acceptance'],results=results)
    (out/'results.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print('Evidence: '+str(out))


if __name__=='__main__':main()
