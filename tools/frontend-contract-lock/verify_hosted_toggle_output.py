"""Real AC hosting row input -> strict feedback -> real Fabric adapter -> typed SDK play boundary.

Never starts Minecraft or an audio device. Mutants live in separate JVM classpaths,
and neither production source nor the frozen reference is edited by this probe.
"""
from pathlib import Path
import argparse,json,os,subprocess,tempfile,time


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--java-home',type=Path,default=Path('C:/Program Files/Java/jdk-21.0.11'));parser.add_argument('--atomchat',type=Path,default=Path('D:/AtomChat-Main'));args=parser.parse_args()
    root=Path(__file__).resolve().parents[2];base=root/'build/frontend-contract-lock';out=Path(tempfile.mkdtemp(prefix='hosting-output-',dir=base));src=out/'sdk-source';classes=out/'sdk-classes';classes.mkdir()
    cp=(base/'stage-three-current-inputs/runtime-classpath.txt').read_text(encoding='utf-8-sig').splitlines();results=[]
    def save():
        (out/'results.json').write_text(json.dumps(dict(scope='REAL_HOSTING_ROW_INPUT_TO_REAL_FABRIC_ADAPTER_TYPED_SDK_PLAY_BOUNDARY',device=False,whole_page=False,limits=['SDK sound manager is a typed output spy; not a game mixer or resource-pack reload test','Independent installed resource closure and native audio decoding have separate evidence','Only the hosting toggle component; full two-page routing remains pending'],results=results),ensure_ascii=False,indent=2),encoding='utf-8')
    def run(label,executable,arguments,expected=True,marker=None):
        argfile=out/(label+'.args');argfile.write_text('\n'.join('"'+str(x).replace('\\','/').replace('"','\\"')+'"' for x in arguments),encoding='utf-8');command=[str(args.java_home/'bin'/executable),'@'+str(argfile)];started=time.time()
        result=subprocess.run(command,cwd=root,capture_output=True,text=True,encoding='utf-8',errors='replace');text=result.stdout+result.stderr;(out/(label+'.log')).write_text(text,encoding='utf-8')
        passed=(result.returncode==0)==expected and (marker is None or marker in text)
        results.append(dict(name=label,command=command,arguments_file=str(argfile),started=time.strftime('%Y-%m-%dT%H:%M:%S%z',time.localtime(started)),seconds=time.time()-started,exit=result.returncode,expected_marker=marker,passed=passed));save();print(label+': '+('PASS' if passed else 'FAIL'),flush=True)
        if not passed:raise RuntimeError(str(out/(label+'.log')))
    def write(name,body):
        path=src/(name.replace('.','/')+'.java');path.parent.mkdir(parents=True,exist_ok=True);path.write_text('package '+name.rsplit('.',1)[0]+'; '+body,encoding='utf-8')
    write('probe.Output','public final class Output {public record Event(String resource,float pitch,float volume,String channel){} public static final java.util.List<Event> events=new java.util.ArrayList<>();}')
    write('net.minecraft.util.Identifier','public class Identifier {public final String id; private Identifier(String id){this.id=id;}public static Identifier tryParse(String id){return id.matches("[a-z]+:[a-z_/]+")?new Identifier(id):null;}}')
    write('net.minecraft.sound.SoundEvent','public class SoundEvent {public net.minecraft.util.Identifier id;public static SoundEvent of(net.minecraft.util.Identifier id){var event=new SoundEvent();event.id=id;return event;}}')
    write('net.minecraft.client.sound.WeightedSoundSet','public class WeightedSoundSet {public int getWeight(){return 1;}}')
    write('net.minecraft.client.sound.SoundInstance','public interface SoundInstance {}')
    write('net.minecraft.client.sound.PositionedSoundInstance','public class PositionedSoundInstance implements SoundInstance {public probe.Output.Event output;public static PositionedSoundInstance master(net.minecraft.sound.SoundEvent event,float pitch,float volume){var instance=new PositionedSoundInstance();instance.output=new probe.Output.Event(event.id.id,pitch,volume,"MASTER");return instance;}}')
    write('net.minecraft.client.sound.SoundManager','public class SoundManager {public WeightedSoundSet get(net.minecraft.util.Identifier id){return id.id.equals("atomchat:feedback_button")?new WeightedSoundSet():null;}public void play(SoundInstance sound){probe.Output.events.add(((PositionedSoundInstance)sound).output);}}')
    write('net.minecraft.client.MinecraftClient','public class MinecraftClient {private static final MinecraftClient INSTANCE=new MinecraftClient();public static MinecraftClient getInstance(){return INSTANCE;}public net.minecraft.client.sound.SoundManager getSoundManager(){return new net.minecraft.client.sound.SoundManager();}}')
    write('com.atom.chat.settings.contract.HostedOutputProbe','''public final class HostedOutputProbe {
      public static void main(String[] args)throws Exception {
        var fixture=new HostingToggleComponentTest();fixture.setup();fixture.hostOutput=new com.atom.lib.client.input.ContractSoundOutput();
        try {fixture.originalPressCommitReachesControlledSoundAndDisabledOrMutedActionsDoNotPlay();
          var expected=java.util.List.of(new probe.Output.Event("atomchat:feedback_button",1f,.45f,"MASTER"));
          if(!probe.Output.events.equals(expected))throw new AssertionError("SOUND_OUTPUT_PARAMETERS "+probe.Output.events);
          System.out.println("ACTUAL_HOSTING_PRESS_TO_SDK_PLAY count=1 volume=.45 pitch=1 channel=MASTER; no device");
        }finally{fixture.restore();}
      }
    }''')
    main='com.atom.chat.settings.contract.HostedOutputProbe';classpath=os.pathsep.join(cp)
    run('compile-sdk-boundary','javac.exe',['-encoding','UTF-8','-proc:none','--release','21','-cp',classpath,'-d',classes,*src.rglob('*.java')])
    run('real-component-and-real-adapter','java.exe',['-Djava.awt.headless=true','-cp',os.pathsep.join([str(classes)]+cp),main],marker='ACTUAL_HOSTING_PRESS_TO_SDK_PLAY count=1')
    original=(args.atomchat/'shared/src/main/java/com/atom/chat/settings/contract/HostingToggleComponent.java').read_text(encoding='utf-8')
    missing=original.replace('if(values.containsKey(Aspect.SOUND))content.feedback("BUTTON");','')
    if missing==original:raise RuntimeError('component sound mutation matched zero sites')
    mutant=out/'missing-feedback-source/HostingToggleComponent.java';mutant.parent.mkdir();mutant.write_text(missing,encoding='utf-8');mutant_classes=out/'missing-feedback-classes';mutant_classes.mkdir()
    run('compile-missing-feedback','javac.exe',['-encoding','UTF-8','-proc:none','--release','21','-cp',classpath,'-d',mutant_classes,mutant])
    run('missing-feedback-rejected','java.exe',['-Djava.awt.headless=true','-cp',os.pathsep.join([str(mutant_classes),str(classes)]+cp),main],False,'SOUND_OUTPUT_COUNT')
    adapter=(root/'platforms/1.21.1-fabric/src/main/java/com/atom/lib/client/input/ContractSoundOutput.java').read_text(encoding='utf-8');swapped=adapter.replace('intent.pitch(),intent.volume()','intent.volume(),intent.pitch()')
    if swapped==adapter:raise RuntimeError('adapter parameter mutation matched zero sites')
    changed=out/'swapped-source/ContractSoundOutput.java';changed.parent.mkdir();changed.write_text(swapped,encoding='utf-8');changed_classes=out/'swapped-classes';changed_classes.mkdir()
    run('compile-swapped-adapter','javac.exe',['-encoding','UTF-8','-proc:none','--release','21','-cp',os.pathsep.join([str(classes)]+cp),'-d',changed_classes,changed])
    run('swapped-output-rejected','java.exe',['-Djava.awt.headless=true','-cp',os.pathsep.join([str(changed_classes),str(classes)]+cp),main],False,'SOUND_OUTPUT_PARAMETERS')
    print('Evidence: '+str(out),flush=True)


if __name__=='__main__':main()
