"""Recompile reference source and replay original settings in isolated JVMs, without consumer writes."""
from pathlib import Path
import argparse, subprocess, os, json, re, hashlib


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--reference',type=Path,required=True)
    p.add_argument('--java-home',type=Path,required=True)
    p.add_argument('--atomchat',type=Path,default=Path('D:/AtomChat-Main'))
    a=p.parse_args();base=a.reference.resolve();platform='1.21.1-fabric'
    inputs=base/platform
    runtime=inputs.joinpath('runtime-classpath.txt').read_text(encoding='utf-8-sig').splitlines()
    compile_cp=inputs.joinpath('compile-classpath.txt').read_text(encoding='utf-8-sig').splitlines()
    outputs=inputs.joinpath('outputs.txt').read_text(encoding='utf-8-sig').splitlines()
    results=[]
    def run(name,cmd):
        r=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,encoding='utf-8',errors='replace',cwd=base)
        (base/(name+'.log')).write_text(r.stdout+r.stderr,encoding='utf-8')
        results.append(dict(name=name,exit=r.returncode,command=[str(x) for x in cmd]))
        if r.returncode:raise RuntimeError(f'{name} failed: {base/(name+".log")}')
        print(name+': PASS',flush=True)
        return r.stdout
    classes=base/'recompiled';classes.mkdir(exist_ok=True)
    source_files=inputs.joinpath('sources.txt').read_text(encoding='utf-8-sig').splitlines()
    chosen=[x for x in source_files if Path(x).stem in {'SettingsSectionPage','SettingsGroupFrontend','SettingsGroupPaint','SettingsChipBar','SettingsRowText','SettingsFormField','FeedbackSoundController','FeedbackSoundPolicy','FeedbackCue','PageTransitionComposite','HeaderChrome','ScreenMetrics','UiStagger','UiMotion','PressScale','FontManager','SkiaFontRenderer'}]
    args=base/'recompile.args'
    args.write_text('\n'.join('"'+x.replace('\\','/')+'"' for x in ['-encoding','UTF-8','-g','-proc:none','--release','21','-classpath',os.pathsep.join(outputs+compile_cp),'-d',str(classes)]+chosen),encoding='utf-8')
    run('source-recompile',[a.java_home/'bin/javac.exe','@'+str(args)])
    # Constant-pool layout changes during loader remapping; compare symbolic instructions, not raw pool offsets.
    def normalize(s):return re.sub(r'\s+',' ',re.sub(r'#\d+','CP',s)).strip()
    verified=[]
    for f in classes.rglob('*.class'):
        name=f.relative_to(classes).with_suffix('').as_posix().replace('/','.')
        first=run('source-'+name,[a.java_home/'bin/javap.exe','-p','-c','-s','-classpath',str(classes),name])
        second=run('build-'+name,[a.java_home/'bin/javap.exe','-p','-c','-s','-classpath',os.pathsep.join(outputs),name])
        if normalize(first)!=normalize(second):raise AssertionError('SOURCE_BUILD_MISMATCH '+name)
        verified.append(name)
    launcher=Path(__file__).resolve().with_name('ReferenceReplay.java');probe=base/'replay-classes';probe.mkdir(exist_ok=True)
    run('replay-compile',[a.java_home/'bin/javac.exe','-encoding','UTF-8','-proc:none','-classpath',os.pathsep.join(runtime),'-d',probe,launcher])
    for attempt in ['replay-a','replay-b']:
        run(attempt,[a.java_home/'bin/java.exe','-Djava.awt.headless=true','-cp',os.pathsep.join([str(probe)]+runtime),'com.atom.chat.config.ReferenceReplay',base/attempt])
    first=(base/'replay-a/frames.tsv').read_bytes();second=(base/'replay-b/frames.tsv').read_bytes()
    if first!=second:raise AssertionError('NONDETERMINISTIC_REFERENCE')
    report=dict(scope='STAGE_ONE_ORIGINAL_SETTINGS',source_build_symbolic_classes=verified,replay_frames=14,
                independent_jvms_equal=True,backend='Skija software raster; production SettingsDetailPage/SettingsSectionPage/HeaderChrome/PageTransitionComposite fixture',
                limits=['Fixture Host is not actual Minecraft window input or raw GL','No audio device output','No redesigned server page selected'],runs=results)
    (base/'replay-results.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('SOURCE_BUILD_MATCH and TWO_JVM_REFERENCE_STABLE')


if __name__=='__main__':main()
