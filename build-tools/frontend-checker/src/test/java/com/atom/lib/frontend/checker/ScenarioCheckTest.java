package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioCheckTest {
    @TempDir Path temp;
    ContractDesign design()throws Exception{Path author=temp.resolve("Design.java");Files.writeString(author,ContractAuthorCompilerTest.valid());return ContractCodec.decode(ContractAuthorCompiler.compile(author));}
    static BufferedImage image(int color){var image=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);for(int x=0;x<4;x++)for(int y=0;y<4;y++)image.setRGB(x,y,color);return image;}
    static ScenarioCheck.Sample sample(String key,long time,ScenarioCheck.Phase phase,int color,boolean enabled,Map<String,Boolean> states){return new ScenarioCheck.Sample(key,"page","card","normal",time,phase,enabled,states,image(color),Map.of("paintedWidth",4d,"motion.HOVER",phase==ScenarioCheck.Phase.INTERMEDIATE?.5:phase==ScenarioCheck.Phase.SETTLED?1d:0d),List.of(),Set.of());}
    static ScenarioCheck.Baseline baseline(ScenarioCheck.Sample sample){Map<String,Double> tolerances=new HashMap<>();sample.observations().keySet().forEach(k->tolerances.put(k,0d));return new ScenarioCheck.Baseline(sample,0,tolerances);}
    @Test void emptyPartialAndStaleOrUnexpectedCapturesCannotPass()throws Exception {
        var d=design();var a=sample("initial",0,ScenarioCheck.Phase.INITIAL,0xff112233,true,Map.of());var b=sample("end",100,ScenarioCheck.Phase.SETTLED,0xff334455,true,Map.of());
        assertTrue(ScenarioCheck.compare(d,List.of(baseline(a),baseline(b)),List.of(a,b)).isEmpty());
        for(var captures:List.of(List.<ScenarioCheck.Sample>of(),List.of(a),List.of(a,a)))assertFalse(ScenarioCheck.compare(d,List.of(baseline(a),baseline(b)),captures).isEmpty());
        var bad=sample("end",100,ScenarioCheck.Phase.SETTLED,0xff998877,true,Map.of());assertTrue(ScenarioCheck.compare(d,List.of(baseline(a),baseline(b)),List.of(a,bad)).stream().anyMatch(x->x.startsWith("VISUAL_DRIFT ")));
    }
    @Test void requiredMotionNeedsAnActualEnabledIntermediateAndConditionalWitnesses()throws Exception {
        var d=design();var req=new EnumMap<Aspect,Requirement>(d.roles().get("display").requirements());req.put(Aspect.HOVER,Requirement.conditional(new NamedCondition.State("hover")));
        var role=new Role(false,req);var old=d.recipes().get("card");var families=new HashMap<>(d.families());families.put("hover",new Family("root",Aspect.HOVER,"",Map.of("normal",new NumberValue(100,Unit.MS))));
        d=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),Set.of("hover"),d.parameters(),families,Map.of("display",role),Map.of("card",new Recipe(old.scope(),old.role(),Map.of(Aspect.HOVER,new Ref("hover","normal")),old.implementation(),old.slots(),old.scenarios())),d.layouts(),d.assets(),d.pages());
        var a=sample("initial",0,ScenarioCheck.Phase.INITIAL,0xff000000,true,Map.of("hover",false));var b=sample("middle",50,ScenarioCheck.Phase.INTERMEDIATE,0xff777777,true,Map.of("hover",true));var c=sample("end",100,ScenarioCheck.Phase.SETTLED,0xffffffff,true,Map.of("hover",true));var expected=List.of(baseline(a),baseline(b),baseline(c));
        assertTrue(ScenarioCheck.compare(d,expected,List.of(a,b,c)).isEmpty());
        var endOnly=ScenarioCheck.compare(d,List.of(baseline(c)),List.of(c));assertTrue(endOnly.stream().anyMatch(x->x.startsWith("DYNAMIC_SAMPLE_MISSING ")),endOnly.toString());assertTrue(endOnly.stream().anyMatch(x->x.startsWith("CONDITION_WITNESS_MISSING ")),endOnly.toString());
        var disabled=sample("middle",50,ScenarioCheck.Phase.INTERMEDIATE,0xff777777,false,Map.of("hover",true));assertFalse(ScenarioCheck.compare(d,List.of(baseline(a),baseline(disabled),baseline(c)),List.of(a,disabled,c)).isEmpty());
    }
    @Test void soundCountParametersAndUnresolvedResourcesAreRealOutputFailures()throws Exception {
        var a=sample("commit",100,ScenarioCheck.Phase.SETTLED,0xff112233,true,Map.of());
        var sound=new SoundObservation("BUTTON","sample:click","MASTER",.6f,1,100);
        var audible=new ScenarioCheck.Sample(a.key(),a.page(),a.recipe(),a.scenario(),a.nanos(),a.phase(),a.enabled(),a.states(),a.image(),a.observations(),List.of(sound),Set.of("sample:click"));
        assertTrue(ScenarioCheck.compare(design(),List.of(baseline(audible)),List.of(a)).stream().anyMatch(x->x.startsWith("SOUND_EVENT_COUNT ")));
    }
    @Test void enabledConditionCanUseARealDisabledFalseWitnessWithoutWeakeningPositiveMotion()throws Exception {
        var d=design();var req=new EnumMap<Aspect,Requirement>(d.roles().get("display").requirements());req.put(Aspect.HOVER,Requirement.conditional(new NamedCondition.State("enabled")));
        var old=d.recipes().get("card");var families=new HashMap<>(d.families());families.put("hover",new Family("root",Aspect.HOVER,"",Map.of("normal",new NumberValue(100,Unit.MS))));
        d=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),Set.of("enabled"),d.parameters(),families,Map.of("display",new Role(false,req)),Map.of("card",new Recipe(old.scope(),old.role(),Map.of(Aspect.HOVER,new Ref("hover","normal")),old.implementation(),old.slots(),old.scenarios())),d.layouts(),d.assets(),d.pages());
        var a=sample("initial",0,ScenarioCheck.Phase.INITIAL,0xff000000,true,Map.of("enabled",true));var b=sample("middle",50,ScenarioCheck.Phase.INTERMEDIATE,0xff777777,true,Map.of("enabled",true));var c=sample("end",100,ScenarioCheck.Phase.SETTLED,0xffffffff,true,Map.of("enabled",true));var off=sample("disabled",150,ScenarioCheck.Phase.DISABLED,0xff000000,false,Map.of("enabled",false));
        var samples=List.of(a,b,c,off);var errors=ScenarioCheck.compare(d,samples.stream().map(ScenarioCheckTest::baseline).toList(),samples);assertTrue(errors.isEmpty(),errors.toString());
    }
    @Test void hoverSamplesCannotStandInForAnOmittedPressAnimation()throws Exception {
        var d=design();var requirements=new EnumMap<Aspect,Requirement>(d.roles().get("display").requirements());requirements.put(Aspect.HOVER,Requirement.required());requirements.put(Aspect.PRESS,Requirement.required());
        var families=new HashMap<>(d.families());families.put("hover",new Family("root",Aspect.HOVER,"",Map.of("normal",new NumberValue(100,Unit.MS))));families.put("press",new Family("root",Aspect.PRESS,"",Map.of("normal",new NumberValue(100,Unit.MS))));var old=d.recipes().get("card");
        d=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),Set.of(),d.parameters(),families,Map.of("display",new Role(false,requirements)),Map.of("card",new Recipe(old.scope(),old.role(),Map.of(Aspect.HOVER,new Ref("hover","normal"),Aspect.PRESS,new Ref("press","normal")),old.implementation(),old.slots(),old.scenarios())),d.layouts(),d.assets(),d.pages());
        var samples=List.of(sample("initial",0,ScenarioCheck.Phase.INITIAL,0xff000000,true,Map.of()),sample("hover-middle",50,ScenarioCheck.Phase.INTERMEDIATE,0xff777777,true,Map.of()),sample("hover-end",100,ScenarioCheck.Phase.SETTLED,0xffffffff,true,Map.of()));
        var errors=ScenarioCheck.compare(d,samples.stream().map(ScenarioCheckTest::baseline).toList(),samples);assertTrue(errors.stream().anyMatch(x->x.startsWith("DYNAMIC_SAMPLE_MISSING ")&&x.contains("/PRESS")),errors.toString());
    }
}
