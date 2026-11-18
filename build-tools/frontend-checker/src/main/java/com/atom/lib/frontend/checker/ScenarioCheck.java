package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Compares independently reviewed production captures; declarations alone never count as observations. */
public final class ScenarioCheck {
    private ScenarioCheck() {}
    public enum Phase { INITIAL,INTERMEDIATE,SETTLED,INTERRUPTED,REVERSED,EXIT,CANCELLED,DISABLED }
    public record Sample(String key,String page,String recipe,String scenario,long nanos,Phase phase,
                         boolean enabled,Map<String,Boolean> states,BufferedImage image,
                         Map<String,Double> observations,List<SoundObservation> sounds,Set<String> resolvedSounds){
        public Sample {states=Map.copyOf(states);observations=Map.copyOf(observations);sounds=List.copyOf(sounds);resolvedSounds=Set.copyOf(resolvedSounds);}
    }
    public record Baseline(Sample sample,int channelTolerance,Map<String,Double> observationTolerances){
        public Baseline{if(channelTolerance<0||channelTolerance>255)throw new IllegalArgumentException("INVALID_SCENARIO_TOLERANCE");observationTolerances=Map.copyOf(observationTolerances);}
    }
    private static final Set<Aspect> MOTION=Set.of(Aspect.HOVER,Aspect.PRESS,Aspect.RIPPLE,Aspect.HIGHLIGHT,Aspect.BOUNCE,Aspect.TRANSITION);
    public static List<String> compare(ContractDesign design,List<Baseline> reviewed,List<Sample> actual){
        DesignResolver.validate(design);List<String> errors=new ArrayList<>();Map<String,Baseline> expected=new TreeMap<>();Map<String,Sample> observed=new TreeMap<>();
        if(reviewed.isEmpty())errors.add("MISSING_EVIDENCE no reviewed production scene captures");
        for(Baseline entry:reviewed){if(expected.put(entry.sample().key(),entry)!=null)errors.add("INVALID_REFERENCE duplicate sample "+entry.sample().key());validate(design,entry.sample(),"REFERENCE",errors);}
        for(Sample entry:actual){if(observed.put(entry.key(),entry)!=null)errors.add("DUPLICATE_SAMPLE "+entry.key());validate(design,entry,"ACTUAL",errors);}
        Map<String,Long> times=new HashMap<>();for(Sample sample:actual){String group=sample.page()+"/"+sample.recipe()+"/"+sample.scenario();Long prior=times.put(group,sample.nanos());if(prior!=null&&sample.nanos()<=prior)errors.add("SCENE_CLOCK "+sample.key()+" repeated or reversed time");}
        for(var entry:expected.entrySet()){
            Baseline baseline=entry.getValue();Sample old=baseline.sample(),next=observed.get(entry.getKey());if(next==null){errors.add("MISSING_SAMPLE "+entry.getKey());continue;}
            if(!Objects.equals(old.page(),next.page())||!Objects.equals(old.recipe(),next.recipe())||!Objects.equals(old.scenario(),next.scenario())||old.nanos()!=next.nanos()||old.phase()!=next.phase()||old.enabled()!=next.enabled()||!old.states().equals(next.states()))errors.add("SCENE_METADATA_DRIFT "+entry.getKey());
            if(!pixelsEqual(old.image(),next.image(),baseline.channelTolerance()))errors.add("VISUAL_DRIFT "+entry.getKey()+" tolerance="+baseline.channelTolerance());
            Map<String,ObservationCheck.Expected> observations=new TreeMap<>();
            for(var measurement:old.observations().entrySet()){
                Double tolerance=baseline.observationTolerances().get(measurement.getKey());if(tolerance==null||!Double.isFinite(tolerance)||tolerance<0){errors.add("INVALID_REFERENCE missing finite observation tolerance "+entry.getKey()+"/"+measurement.getKey());continue;}
                observations.put(measurement.getKey(),new ObservationCheck.Expected(measurement.getValue(),tolerance));
            }
            for(String error:ObservationCheck.compare(observations,next.observations(),Set.of(),Map.of()))errors.add(error+" sample="+entry.getKey());
            for(String error:SoundCheck.compare(old.sounds().isEmpty(),old.sounds(),next.sounds(),next.resolvedSounds()))errors.add(error+" sample="+entry.getKey());
        }
        for(String key:observed.keySet())if(!expected.containsKey(key))errors.add("UNCOVERED_SAMPLE "+key);
        coverage(design,reviewed.stream().map(Baseline::sample).toList(),"REFERENCE",errors);
        coverage(design,actual,"ACTUAL",errors);return List.copyOf(errors);
    }
    private static void validate(ContractDesign design,Sample sample,String origin,List<String> errors){
        Page page=design.pages().get(sample.page());Recipe recipe=design.recipes().get(sample.recipe());
        if(sample.key()==null||sample.key().isBlank()||page==null||recipe==null||!page.recipes().contains(sample.recipe())||!page.scenarios().contains(sample.scenario())||!recipe.scenarios().contains(sample.scenario()))errors.add("UNCOVERED_SCENE "+origin+" "+sample.key());
        if(sample.nanos()<0||sample.phase()==null)errors.add("SCENE_CLOCK "+origin+" "+sample.key());
        if(!design.states().containsAll(sample.states().keySet()))errors.add("UNKNOWN_STATE "+origin+" "+sample.key());
        if(sample.states().containsKey("enabled")&&sample.states().get("enabled")!=sample.enabled())errors.add("SCENE_ENABLED_MISMATCH "+origin+" "+sample.key());
        if(sample.image()==null||sample.image().getWidth()<1||sample.image().getHeight()<1)errors.add("MISSING_IMAGE "+origin+" "+sample.key());
        if(sample.observations().isEmpty()||sample.observations().values().stream().anyMatch(v->!Double.isFinite(v)))errors.add("MISSING_OBSERVATION "+origin+" "+sample.key());
        if((!sample.enabled()||sample.phase()==Phase.CANCELLED||sample.phase()==Phase.DISABLED)&&!sample.sounds().isEmpty())errors.add("SOUND_ON_CANCELLED_ACTION "+origin+" "+sample.key());
    }
    private static void coverage(ContractDesign design,List<Sample> samples,String origin,List<String> errors){
        for(var entry:new TreeMap<>(design.pages()).entrySet())for(String recipeId:new TreeSet<>(entry.getValue().recipes())){
            Recipe recipe=design.recipes().get(recipeId);Role role=design.roles().get(recipe.role());String path=origin+" "+entry.getKey()+"/"+recipeId;
            List<Sample> captures=samples.stream().filter(s->s.page().equals(entry.getKey())&&s.recipe().equals(recipeId)).toList();
            for(String scenario:recipe.scenarios())if(captures.stream().noneMatch(s->s.scenario().equals(scenario)))errors.add("MISSING_SCENARIO "+path+"/"+scenario);
            for(String scenario:entry.getValue().scenarios())if(samples.stream().noneMatch(s->s.page().equals(entry.getKey())&&s.scenario().equals(scenario)))errors.add("MISSING_SCENARIO "+origin+" "+entry.getKey()+"/"+scenario);
            for(var requirement:role.requirements().entrySet()){
                if(requirement.getValue().rule()==Rule.CONDITIONAL)witness(requirement.getValue().condition(),captures,path+"/"+requirement.getKey(),errors);
                if((requirement.getValue().rule()==Rule.REQUIRED||requirement.getValue().rule()==Rule.CONDITIONAL)&&MOTION.contains(requirement.getKey())){
                    List<Sample> enabled=captures.stream().filter(Sample::enabled).toList();
                    String metric="motion."+requirement.getKey().name();
                    Optional<Sample> initial=enabled.stream().filter(s->s.phase()==Phase.INITIAL&&s.observations().getOrDefault(metric,Double.NaN)==0).findFirst(),middle=enabled.stream().filter(s->{double progress=s.observations().getOrDefault(metric,Double.NaN);return s.phase()==Phase.INTERMEDIATE&&progress>0&&progress<1&&active(requirement.getValue(),s,errors,path);}).findFirst(),end=enabled.stream().filter(s->s.phase()==Phase.SETTLED&&s.observations().getOrDefault(metric,Double.NaN)==1&&active(requirement.getValue(),s,errors,path)).findFirst();
                    if(initial.isEmpty()||middle.isEmpty()||end.isEmpty()||!(initial.get().nanos()<middle.get().nanos()&&middle.get().nanos()<end.get().nanos())||pixelsEqual(initial.get().image(),middle.get().image(),0)||pixelsEqual(middle.get().image(),end.get().image(),0))errors.add("DYNAMIC_SAMPLE_MISSING "+path+"/"+requirement.getKey()+" enabled initial/intermediate/settled pixels and measured "+metric+" required");
                }
            }
            if(role.interactive()){
                if(captures.stream().noneMatch(s->s.enabled()&&s.observations().getOrDefault("actions.committed",0d)>0))errors.add("INTERACTIVE_WITNESS_MISSING "+path+" committed enabled action");
                if(captures.stream().noneMatch(s->(s.phase()==Phase.CANCELLED||s.phase()==Phase.DISABLED)&&s.observations().containsKey("actions.committed")&&s.observations().get("actions.committed")==0d))errors.add("INTERACTIVE_WITNESS_MISSING "+path+" cancelled/disabled action must not commit");
            }
            Ref sound=recipe.bindings().get(Aspect.SOUND);
            if(sound!=null&&DesignResolver.value(design,sound) instanceof SoundValue value&&!value.asset().isEmpty()){
                if(value.when()!=null)witness(value.when(),captures,path+"/SOUND_OUTPUT",errors);
                if(captures.stream().noneMatch(s->s.enabled()&&!s.sounds().isEmpty()))errors.add("SOUND_WITNESS_MISSING "+path+" audible output required");
            }
        }
    }
    private static boolean active(Requirement rule,Sample sample,List<String> errors,String path){try{return rule.rule()!=Rule.CONDITIONAL||DesignResolver.evaluate(rule.condition(),sample.states());}catch(IllegalArgumentException e){errors.add("CONDITION_WITNESS_MISSING "+path+" "+e.getMessage());return false;}}
    private static void witness(NamedCondition condition,List<Sample> samples,String path,List<String> errors){
        Set<Boolean> outcomes=new HashSet<>();boolean enabledCondition=references(condition,"enabled");
        for(Sample sample:samples)try{
            boolean value=DesignResolver.evaluate(condition,sample.states());
            // A true capability witness must be enabled. A declared enabled guard's false branch is genuinely disabled.
            if(sample.enabled()||(!value&&enabledCondition&&Boolean.FALSE.equals(sample.states().get("enabled"))))outcomes.add(value);
        }catch(IllegalArgumentException e){errors.add("CONDITION_WITNESS_MISSING "+path+" "+e.getMessage());}
        if(!outcomes.equals(Set.of(true,false)))errors.add("CONDITION_WITNESS_MISSING "+path+" enabled true witness and reachable false branch required");
    }
    private static boolean references(NamedCondition condition,String name){if(condition instanceof NamedCondition.State state)return state.name().equals(name);if(condition instanceof NamedCondition.Not not)return references(not.operand(),name);return (condition instanceof NamedCondition.All all?all.operands():((NamedCondition.Any)condition).operands()).stream().anyMatch(item->references(item,name));}
    private static boolean pixelsEqual(BufferedImage a,BufferedImage b,int tolerance){
        if(a==null||b==null||a.getWidth()!=b.getWidth()||a.getHeight()!=b.getHeight())return false;
        for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++){int left=a.getRGB(x,y),right=b.getRGB(x,y);for(int shift=0;shift<=24;shift+=8)if(Math.abs((left>>>shift&255)-(right>>>shift&255))>tolerance)return false;}return true;
    }
}
