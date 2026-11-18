"""Isolated source/contract/resource Maven probe. Synthetic inputs, never real-project acceptance."""
from pathlib import Path
import argparse
import json
import os
import shutil
import subprocess
import tempfile
import time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--language-level", type=int, choices=(17,21), default=17)
    parser.add_argument("--protocol",type=int,choices=(1,2),default=1)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    version = next(line.split("=", 1)[1] for line in (root / "gradle.properties").read_text().splitlines() if line.startswith("atomlib_version="))
    output = Path(tempfile.mkdtemp(prefix=time.strftime(f"consumer-%Y%m%d-%H%M%S-jdk{args.language_level}-"), dir=root / "build/frontend-contract-lock"))
    consumer, reviewed = output / "consumer", output / "reviewed-fixture"
    consumer.mkdir(parents=True)
    reviewed.mkdir()
    repo = output / "maven"
    artifact = Path("com/atom/lib/atomlib-frontend-checker") / version
    shutil.copytree(root / "build/test-maven" / artifact, repo / artifact)
    core_artifact=Path('com/atom/lib/atomlib-core')/version
    shutil.copytree(root/'build/test-maven'/core_artifact,repo/core_artifact)
    jar = repo / artifact / f"atomlib-frontend-checker-{version}.jar"
    core_jar=repo/core_artifact/f'atomlib-core-{version}.jar'
    tool_cp=os.pathsep.join([str(jar),str(core_jar)])
    shutil.copy(root / "gradlew.bat", consumer)
    shutil.copytree(root / "gradle/wrapper", consumer / "gradle/wrapper")
    (consumer / "settings.gradle").write_text("rootProject.name='isolated-fcl-probe'\n", encoding="utf-8")
    build = f"""buildscript {{
    repositories {{ maven {{ url=uri('{repo.as_posix()}') }} }}
    dependencies {{ classpath 'com.atom.lib:atomlib-frontend-checker:{version}' }}
}}
apply plugin: com.atom.lib.frontend.checker.FrontendContractPlugin
java {{ toolchain.languageVersion=JavaLanguageVersion.of({args.language_level}) }}
frontendContract {{
    contract=file('contract.properties')
    reviewed=file('{(reviewed / 'contract.properties').as_posix()}')
}}
if (providers.gradleProperty('fclInjectDraft').isPresent()) {{ tasks.named('jar') {{ from sourceSets.frontendDraft.output }} }}
if (providers.gradleProperty('fclIncludeDraftSources').isPresent()) {{ sourceSets.main.java.srcDir('src/frontendDraft/java') }}
"""
    (consumer / "build.gradle").write_text(build, encoding="utf-8")
    shutil.copy(consumer / "build.gradle", reviewed)
    config = "protocol=1\nprofiles=probe-v1\ndesign=design.bin\nstrict=consumer.Page#build()\ndrawingTypes=consumer.Ink\nprotectedFiles=build.gradle\n"
    if args.protocol==2:config=config.replace('protocol=1','protocol=2').replace('profiles=probe-v1','profiles=probe/1')+'author=Theme.java\nbusinessMethods=consumer.Business#save(int)\n'
    for directory in (consumer, reviewed):
        (directory / "contract.properties").write_text(config, encoding="utf-8")
    source = consumer / "src/main/java/consumer/Page.java"
    source.parent.mkdir(parents=True)
    source.write_text("package consumer; class Ink {static void rect(int x){}} public class Page {void build(){int volume=125;}}", encoding="utf-8")
    if args.protocol==2:
        source.write_text('package consumer; interface Business{void save(int count);} class Backend implements Business{public void save(int count){}} class Ink{static void rect(int x){}} public class Page{private final Business backend=new Backend();void build(){int volume=125;backend.save(volume);}}',encoding='utf-8')
    draft_source=consumer/'src/frontendDraft/java/consumer/DraftOnly.java';draft_source.parent.mkdir(parents=True)
    draft_source.write_text('package consumer; public class DraftOnly {public static final int EXPLORATORY_COLOR=123;}',encoding='utf-8')
    draft_resource=consumer/'src/frontendDraft/resources/frontend-draft/unapproved.material';draft_resource.parent.mkdir(parents=True)
    draft_resource.write_text('isolated exploratory material',encoding='utf-8')
    if args.language_level==21:
        source.write_text(source.read_text().replace("int volume=125;", "int volume=125; Object v=volume; int n=switch(v){case Integer i -> i; default -> 0;};"),encoding="utf-8")
    author = output / "Theme.java"
    java = Path(args.java_home) / "bin/java.exe"
    env = dict(os.environ, JAVA_HOME=args.java_home)
    results = []

    def run(label, command, expected, rule=None, cwd=consumer, extra_env=None):
        started = time.time()
        process_env=dict(env,FCL_PROBE_SCENE_MODE='normal');process_env.update(extra_env or {})
        result = subprocess.run([str(x) for x in command], cwd=cwd, env=process_env, capture_output=True, text=True, encoding="utf-8", errors="replace")
        text = result.stdout + result.stderr
        (output / f"{label}.log").write_text(text, encoding="utf-8")
        passed = (result.returncode == 0) == expected and (rule is None or rule in text)
        results.append(dict(name=label, command=[str(x) for x in command], started=time.strftime("%Y-%m-%dT%H:%M:%S%z", time.localtime(started)), seconds=time.time()-started, exit=result.returncode, expected_rule=rule, passed=passed))
        (output / "results.json").write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"{label}: {'PASS' if passed else 'FAIL'} exit={result.returncode}", flush=True)
        if not passed:
            raise RuntimeError(f"{label} failed; see {output / (label+'.log')}")

    def design(version):
        if args.protocol==1:
            author.write_text(f'class Theme {{static final String namespace="fixture",version="{version}",profile="probe-v1";}}', encoding="utf-8")
            operation='compile-design'
        else:
            aspects='MATERIAL COLOR TYPOGRAPHY SIZE SPACING ICON HOVER PRESS RIPPLE HIGHLIGHT BOUNCE LAYOUT SOUND FOCUS EDITING SCROLL RESTORATION TRANSITION STATES DEGRADATION TOOLTIP POINTER PREFERENCES'.split()
            decisions=','.join(f'Map.entry(Aspect.{a},Requirement.notApplicable("synthetic distribution fixture"))' for a in aspects)
            author.write_text('import java.util.*; import com.atom.lib.frontend.contract.*; import com.atom.lib.frontend.contract.ContractDesign.*; class Theme {static final ContractDesign DESIGN=new ContractDesign(1,"fixture","'+version+'","probe/1",Map.of("root",""),Set.of(),Map.of(),Map.of("space",new Family("root",Aspect.SPACING,"",Map.of("gap",new NumberValue(8,Unit.U),"max",new NumberValue(800,Unit.U)))),Map.of("display",new Role(false,Map.ofEntries('+decisions+'))),Map.of("card",new Recipe("root","display",Map.of(),new Binding("fixture/1","probe/1"),Map.of("text",new Slot(ContentKind.TEXT,true)),Set.of("normal"))),Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("space","gap"),new Ref("space","gap"),new Ref("space","gap"),new Ref("space","max"))),Map.of(),Map.of("page",new Page("root",Set.of("card"),"stack",Set.of("normal"))));}',encoding='utf-8')
            shutil.copy(author,consumer/'Theme.java');operation='compile-project'
        design_input=consumer/'contract.properties' if args.protocol==2 else author
        run(f"design-{version}", [java,"-cp",tool_cp,"com.atom.lib.frontend.checker.CheckerMain",operation,design_input,consumer/"design.bin"], True)

    gradle = [consumer/"gradlew.bat", "--offline", "--no-daemon", "--console=plain"]
    if args.protocol==2:
        scene=source.with_name('Scenes.java')
        scene.write_text('''package consumer;
import java.util.*; import java.nio.file.*; import java.io.*; import java.awt.image.BufferedImage; import javax.imageio.ImageIO;
public class Scenes {
 public static void main(String[] args)throws Exception {
  String mode=System.getenv("FCL_PROBE_SCENE_MODE");
  try(var out=new DataOutputStream(Files.newOutputStream(Path.of(args[0])))) {
   out.writeInt(0x46435301);out.writeUTF("stale".equals(mode)?"previous-invocation":args[1]);out.writeUTF(args[2]);
   out.writeInt("empty".equals(mode)?0:1);if("empty".equals(mode))return;
   out.writeUTF("normal");out.writeUTF("page");out.writeUTF("card");out.writeUTF("normal");out.writeLong(0);out.writeUTF("INITIAL");out.writeBoolean(true);out.writeInt(0);
   var image=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);for(int x=0;x<4;x++)for(int y=0;y<4;y++)image.setRGB(x,y,"drift".equals(mode)?0xff8899aa:0xff112233);
   var png=new ByteArrayOutputStream();ImageIO.write(image,"PNG",png);out.writeInt(png.size());out.write(png.toByteArray());
   out.writeInt(1);out.writeUTF("paintedWidth");out.writeDouble(image.getWidth());out.writeInt(0);out.writeInt(0);
  }
 }
}''',encoding='utf-8')
        scene_classes=output/'scene-reference-classes';scene_classes.mkdir()
        run('scene-fixture-compile',[Path(args.java_home)/'bin/javac.exe','-d',scene_classes,scene],True)
        run('scene-reference-export',[java,'-cp',scene_classes,'consumer.Scenes',consumer/'reference.fcs','independent-fixture-reference','synthetic-original-reference'],True)
        shutil.copy(consumer/'reference.fcs',reviewed)
        source_list=output/'sources.txt';source_list.write_text(str(source)+'\n'+str(scene)+'\n',encoding='utf-8')
        cp_list=output/'classpath.txt';cp_list.write_text('',encoding='utf-8')
        run('scene-implementation-inventory',[java,'-cp',tool_cp,'com.atom.lib.frontend.checker.InventoryMain',source_list,cp_list,output/'observed'],True,'OBSERVED_ONLY_NOT_APPROVED')
        ast=next(line.split('=',1)[1] for line in (output/'observed/observed-implementations.properties').read_text(encoding='utf-8').splitlines() if line.startswith('consumer.Scenes='))
        for directory in (consumer,reviewed):
            target=directory/'contract.properties'
            text=target.read_text(encoding='utf-8').replace('protectedFiles=build.gradle','protectedFiles=build.gradle;reference.fcs')
            target.write_text(text+'scene.main=consumer.Scenes\nscene.reference=reference.fcs\nscene.referenceDesignDigest=synthetic-original-reference\nscene.channelTolerance=0\nscene.observationTolerance=0\nimplementation.consumer.Scenes='+ast+'\n',encoding='utf-8')
    design("1")
    shutil.copy(consumer / "design.bin", reviewed)
    run("approved-build", gradle+["build"], True,"status=APPROVED_VALID")
    if args.protocol==2:
        assert 'status=SCENARIOS_VALID' in (consumer/'build/frontend-contract-lock/scenes.txt').read_text(encoding='utf-8')
        import zipfile
        consumer_jar=next((consumer/'build/libs').glob('*.jar'))
        with zipfile.ZipFile(consumer_jar) as package:
            base='META-INF/atomlib/frontend/fixture/1/'
            assert package.read(base+'design.fcl')==(consumer/'design.bin').read_bytes()
            assert package.read(base+'manifest.fcl')[:4]==bytes.fromhex('46434d01')
            assert not any('.generated-resources' in n for n in package.namelist())
            assert 'consumer/DraftOnly.class' not in package.namelist()
            assert 'frontend-draft/unapproved.material' not in package.namelist()
        print('packaged-runtime-resources: PASS',flush=True)
        run('isolated-draft-compiles',gradle+['frontendDraftClasses'],True)
        run('draft-archive-blocked',gradle+['build','-PfclInjectDraft=true'],False,'DRAFT_IN_PRODUCTION')
        run('draft-source-overlap-blocked',gradle+['build','-PfclIncludeDraftSources=true'],False,'DRAFT_IN_PRODUCTION')
        run('actual-visual-drift-blocked',gradle+['build'],False,'VISUAL_DRIFT normal',extra_env={'FCL_PROBE_SCENE_MODE':'drift'})
        run('stale-capture-blocked',gradle+['build'],False,'STALE_SCENE_CAPTURE',extra_env={'FCL_PROBE_SCENE_MODE':'stale'})
        run('empty-scene-blocked',gradle+['build'],False,'MISSING_SAMPLE normal',extra_env={'FCL_PROBE_SCENE_MODE':'empty'})
        business_legal=source.read_text(encoding='utf-8')
        source.write_text(business_legal.replace('public void save(int count){}','public void save(int count){Ink.rect(count);}'),encoding='utf-8')
        run('business-port-cannot-hide-drawing',gradle+['build'],False,'UNCOVERED_ENTRY consumer.Backend#save(int)')
        source.write_text(business_legal,encoding='utf-8')
    legal = source.read_text()
    source.write_text(legal.replace("int volume=125;", "int volume=125; Ink.rect(12);"), encoding="utf-8")
    run("illegal-build", gradle+["build"], False,"PAGE_DRAWING ")
    source.write_text(legal, encoding="utf-8")
    design("2")
    run("candidate-blocks-build", gradle+["build"], False,"status=CANDIDATE_VALID")
    run("candidate-preview", gradle+["frontendContractCandidate"], True,"status=CANDIDATE_VALID")
    design("1")
    (consumer / "build.gradle").write_text("plugins { id 'java' }\n", encoding="utf-8")
    run("missing-plugin-preflight", [java,"-cp",tool_cp,"com.atom.lib.frontend.checker.CheckerMain","--config",consumer/"contract.properties","--reviewed",reviewed/"contract.properties","--source",source,"--release",str(args.language_level),"--report",output/"preflight.txt"], False,"INTEGRATION_CHANGED ")
    (consumer / "build.gradle").write_text(build, encoding="utf-8")
    run("restored-build", gradle+["build"], True,"status=APPROVED_VALID")
    print(f"Evidence: {output}", flush=True)


if __name__ == "__main__":
    main()
