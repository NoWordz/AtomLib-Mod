"""Execute independent legacy/strict native raster paths and missing-hover/press mutants. No game/device."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import tempfile
import time
import uuid


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--java-home',default='C:/Program Files/Java/jdk-17.0.18')
    parser.add_argument('--font',default='C:/Windows/Fonts/msyh.ttc')
    args=parser.parse_args()
    root=Path(__file__).resolve().parents[2]
    output=Path(tempfile.mkdtemp(prefix=time.strftime('native-scene-%Y%m%d-%H%M%S-'),dir=root/'build/frontend-contract-lock'))
    cp=(root/'build/frontend-contract-lock/native-scene-classpath.txt').read_text(encoding='utf-8')
    classes=output/'classes';classes.mkdir()
    results=[]
    env=dict(os.environ,JAVA_HOME=args.java_home)

    def run(name,command,expected=True,rule=None):
        started=time.time();result=subprocess.run([str(x) for x in command],cwd=root,env=env,capture_output=True,text=True,encoding='utf-8',errors='replace')
        text=result.stdout+result.stderr;(output/f'{name}.log').write_text(text,encoding='utf-8')
        passed=(result.returncode==0)==expected and (rule is None or rule in text)
        results.append(dict(name=name,command=[str(x) for x in command],started=time.strftime('%Y-%m-%dT%H:%M:%S%z',time.localtime(started)),seconds=time.time()-started,exit=result.returncode,expected_rule=rule,passed=passed))
        (output/'results.json').write_text(json.dumps(dict(scope='SYNTHETIC_NATIVE_RASTER_NOT_ATOMCHAT_OR_DEVICE',results=results),indent=2),encoding='utf-8')
        print(f'{name}: {"PASS" if passed else "FAIL"} exit={result.returncode}',flush=True)
        if not passed:raise RuntimeError(str(output/f'{name}.log'))

    run('compile',[Path(args.java_home)/'bin/javac.exe','-encoding','UTF-8','-cp',cp,'-d',classes,root/'tools/frontend-contract-lock/StrictScenarioProbe.java'])
    if not Path(args.font).is_file():raise RuntimeError('MISSING_FONT '+args.font)
    java=[Path(args.java_home)/'bin/java.exe',f'-Datomlib.testFont={args.font}','-cp',str(classes)+os.pathsep+cp,'com.atom.lib.frontend.StrictScenarioProbe']
    run('legacy-reference',java+['legacy',output/'legacy',str(uuid.uuid4())],True,'SCENE_CAPTURE_VALID')
    reference=output/'legacy/capture.fcs'
    run('strict-current',java+['strict',output/'strict',str(uuid.uuid4()),reference],True,'SCENE_CAPTURE_VALID')
    run('missing-hover-mutant',java+['mutant-hover',output/'mutant-hover',str(uuid.uuid4()),reference],False,'VISUAL_DRIFT middle')
    run('missing-press-mutant',java+['mutant-press',output/'mutant-press',str(uuid.uuid4()),reference],False,'DYNAMIC_SAMPLE_MISSING ACTUAL page/button/PRESS')
    print(f'Evidence: {output}',flush=True)


if __name__=='__main__':main()
