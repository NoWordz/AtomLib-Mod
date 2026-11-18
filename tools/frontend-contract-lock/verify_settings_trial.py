"""Compare current real settings/reuse components against independently frozen production classes and fonts."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import tempfile
import time
import zipfile
from PIL import Image, ImageChops


def compare(reference, candidate, output):
    expected=sorted(reference.glob('frame-*.png'))
    if len(expected)!=14 or {p.name for p in expected}!={p.name for p in candidate.glob('frame-*.png')}:
        raise RuntimeError('MISSING_PRODUCTION_FRAMES')
    results=[]
    for file in expected:
        with Image.open(file) as left,Image.open(candidate/file.name) as right:
            left=left.convert('RGBA');right=right.convert('RGBA')
            equal=left.size==right.size and left.tobytes()==right.tobytes()
            record=dict(frame=file.name,equal=equal,reference_size=left.size,candidate_size=right.size)
            if not equal and left.size==right.size:
                channels=ImageChops.difference(left,right).split()
                maximum=ImageChops.lighter(ImageChops.lighter(channels[0],channels[1]),ImageChops.lighter(channels[2],channels[3]))
                record.update(changed_pixels=sum(v!=0 for v in maximum.tobytes()),bounds=maximum.getbbox(),maximum_channel_delta=maximum.getextrema()[1])
                output.mkdir(exist_ok=True);Image.merge('RGB',(maximum,Image.new('L',left.size),Image.new('L',left.size))).save(output/file.name)
            results.append(record)
    return results


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--java-home',default='C:/Program Files/Java/jdk-21.0.11');parser.add_argument('--matrix',choices=('basic','all'),default='basic');args=parser.parse_args()
    root=Path(__file__).resolve().parents[2];base=root/'build/frontend-contract-lock';old=base/'original-settings-reference'
    output=Path(tempfile.mkdtemp(prefix=time.strftime('production-replay-%Y%m%d-%H%M%S-'),dir=base));classes=output/'launcher';classes.mkdir()
    current=(base/'stage-three-current-inputs/runtime-classpath.txt').read_text(encoding='utf-8-sig').splitlines();frozen=(old/'frozen-runtime/runtime-classpath.txt').read_text(encoding='utf-8-sig').splitlines()
    for path in current+frozen:
        if not Path(path).exists():raise RuntimeError('MISSING_PRODUCTION_INPUT '+path)
    results=[]
    def run(name,command,rule=None):
        started=time.time();result=subprocess.run([str(x) for x in command],cwd=root,env=dict(os.environ,JAVA_HOME=args.java_home),capture_output=True,text=True,encoding='utf-8',errors='replace');(output/f'{name}.log').write_text(result.stdout+result.stderr,encoding='utf-8')
        passed=result.returncode==0 and (rule is None or rule in result.stdout+result.stderr)
        results.append(dict(name=name,command=[str(x) for x in command],started=time.strftime('%Y-%m-%dT%H:%M:%S%z',time.localtime(started)),seconds=time.time()-started,exit=result.returncode,expected_rule=rule,passed=passed));save();print(f'{name}: exit={result.returncode}',flush=True)
        if not passed:raise RuntimeError(str(output/f'{name}.log'))
    comparisons=[]
    def save():
        (output/'results.json').write_text(json.dumps(dict(scope='REAL_PRODUCTION_COMPONENTS_SOFTWARE_NOT_GAME_OR_DEVICE',runs=results,comparisons=comparisons),ensure_ascii=False,indent=2),encoding='utf-8')
    run('compile',[Path(args.java_home)/'bin/javac.exe','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(current),'-d',classes,root/'tools/frontend-contract-lock/SettingsTrialReplay.java'])
    agent_classes=output/'agent-classes';agent_classes.mkdir()
    run('compile-clock-port',[Path(args.java_home)/'bin/javac.exe','-encoding','UTF-8','-proc:none','-cp',os.pathsep.join(current),'-d',agent_classes,root/'tools/frontend-contract-lock/FixedHeaderClockAgent.java'])
    agent=output/'fixed-header-clock-agent.jar'
    with zipfile.ZipFile(agent,'w',zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: com.atom.lib.frontend.probe.FixedHeaderClockAgent\n\n')
        for p in agent_classes.rglob('*.class'):archive.write(p,p.relative_to(agent_classes).as_posix())
    def replay(label,classpath,page,locale,width,theme):
        target=output/label;run(label,[Path(args.java_home)/'bin/java.exe','-Djava.awt.headless=true','-javaagent:'+str(agent),'-cp',os.pathsep.join([str(classes),str(agent)]+classpath),'com.atom.chat.config.SettingsTrialReplay',target,page,locale,str(width),theme],rule='FCL_HEADER_CLOCK_BOUND count=1 value=17:05');return target
    anchor=replay('frozen-anchor',frozen,'history','zh_cn',420,'dark')
    identity=compare(old/'replay-a',anchor,output/'anchor-diff');comparisons.append(dict(name='frozen-versus-original-approved-anchor',frames=identity));save()
    if not all(x['equal'] for x in identity):raise RuntimeError('REFERENCE_PREMISE_CONFLICT original fourteen frames differ; do not regenerate approval')
    conditions=[('zh_cn',420,'dark')] if args.matrix=='basic' else [(locale,width,theme) for locale in ('zh_cn','en_us') for width in (280,420) for theme in ('dark','light')]
    for page in ('history','hosting'):
        for locale,width,theme in conditions:
            label=f'{page}-{locale}-{width}-{theme}'
            reference=anchor if label=='history-zh_cn-420-dark' else replay('old-'+label,frozen,page,locale,width,theme)
            actual=replay('current-'+label,current,page,locale,width,theme);comparison=compare(reference,actual,output/('diff-'+label));comparisons.append(dict(name=label,frames=comparison));save()
            print(label+': '+('PIXELS_EQUAL' if all(x['equal'] for x in comparison) else 'VISUAL_DRIFT'),flush=True)
    if any(not frame['equal'] for entry in comparisons for frame in entry['frames']):raise RuntimeError('PRODUCTION_VISUAL_DRIFT '+str(output/'results.json'))
    print('PRODUCTION_PIXELS_EQUAL '+str(output),flush=True)


if __name__=='__main__':main()
