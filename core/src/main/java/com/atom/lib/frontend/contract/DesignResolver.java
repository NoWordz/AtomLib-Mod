package com.atom.lib.frontend.contract;

import java.util.*;
import static com.atom.lib.frontend.contract.ContractDesign.*;

/** Validates a complete design before any runtime registry can be created. */
public final class DesignResolver {
    private DesignResolver() {}
    public static void validate(ContractDesign d) {
        Objects.requireNonNull(d);
        if(d.format()!=1)fail("UNSUPPORTED_FORMAT","design","1",d.format());
        name(d.namespace());name(d.version());name(d.profile());
        d.buildBindings().forEach((key,hash)->{if(key==null||key.isBlank()||!hash.matches("[0-9a-f]{64}"))fail("INVALID_BUILD_BINDING",String.valueOf(key),"SHA-256 of reviewed build input",hash);});
        if(!d.namespace().matches("[a-zA-Z0-9_.-]+")||!d.version().matches("[a-zA-Z0-9_.-]+"))fail("INVALID_ID","namespace/version","resource path segments",d.namespace()+"/"+d.version());
        if(d.scopes().isEmpty()||d.pages().isEmpty())fail("MISSING_EVIDENCE","design","scopes and pages","empty");
        for(String scope:d.scopes().keySet()){name(scope);scopeAncestors(d,scope);}
        d.states().forEach(DesignResolver::name);
        d.parameters().forEach((id,p)->{name(id);if(p.unit()==null||!Double.isFinite(p.minimum())||!Double.isFinite(p.maximum())||!Double.isFinite(p.initial())||p.minimum()>p.maximum()||p.initial()<p.minimum()||p.initial()>p.maximum())fail("PARAMETER_RANGE",id,"finite min <= initial <= max",p);});
        d.assets().forEach((id,a)->{name(id);if(a.kind()==null||a.resource()==null||!a.resource().matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+")||a.resource().contains("..")||a.resource().contains(":/")||a.provenance()==null||a.provenance().isBlank())fail("INVALID_ASSET",id,"identified resource and source",a);});
        for(var entry:d.families().entrySet()){
            name(entry.getKey());Family f=entry.getValue();scopeAncestors(d,f.scope());Objects.requireNonNull(f.aspect());
            Map<String,Value> members=members(d,entry.getKey(),new HashSet<>());
            if(members.isEmpty())fail("EMPTY_FAMILY",entry.getKey(),"members","empty");
            for(String member:members.keySet()){name(member);Value v=value(d,new Ref(entry.getKey(),member),new HashSet<>());
                if(v instanceof NumberValue n && (n.unit()==null||!Double.isFinite(n.value())))fail("INVALID_VALUE",entry.getKey()+"/"+member,"finite typed number",n);
                if(v instanceof TextValue t && (t.value()==null||t.value().isBlank()))fail("INVALID_VALUE",entry.getKey()+"/"+member,"operation identity",t);
                if(v instanceof GroupLayoutValue group){
                    if(f.aspect()!=Aspect.LAYOUT)fail("ASPECT_MISMATCH",entry.getKey(),Aspect.LAYOUT,f.aspect());
                    Layout layout=d.layouts().get(group.layout());
                    if(layout==null)fail("UNKNOWN_LAYOUT",entry.getKey(),d.layouts().keySet(),group.layout());
                    accessible(d,f.scope(),layout.scope(),entry.getKey());
                    if(group.scroll()&&(!group.clip()||layout.axis()!=Axis.COLUMN)){
                        fail("INVALID_LAYOUT",entry.getKey(),"vertical scroll requires a clipped column",group);
                    }
                }
                if(v instanceof ParameterValue p && !d.parameters().containsKey(p.parameter()))fail("UNKNOWN_PARAMETER",entry.getKey(),"declared parameter",p);
                if(v instanceof AssetValue a){Asset asset=d.assets().get(a.asset());if(asset==null)fail("UNKNOWN_ASSET",entry.getKey(),"declared resource",a);
                    AssetKind expected=switch(f.aspect()){case ICON->AssetKind.ICON;case TYPOGRAPHY->AssetKind.FONT;case SOUND->AssetKind.SOUND;case MATERIAL->AssetKind.TEXTURE;default->null;};
                    if(expected!=asset.kind())fail("ASSET_PURPOSE",entry.getKey(),expected,asset.kind());}
                if(f.aspect()==Aspect.SOUND){
                    if(!(v instanceof SoundValue))fail("INVALID_SOUND_BINDING",entry.getKey(),"typed sound or explicit silence",v);
                    SoundValue sound=(SoundValue)v;name(sound.event());
                    if(sound.asset()==null||sound.channel()==null||sound.volumeParameter()==null||sound.silenceReason()==null||!Double.isFinite(sound.pitch())||sound.pitch()<=0||sound.priority()<0)fail("INVALID_SOUND_BINDING",entry.getKey(),"finite complete sound rule",sound);
                    if(sound.asset().isEmpty()) {if(sound.silenceReason().isBlank()||!sound.channel().isEmpty()||!sound.volumeParameter().isEmpty()||sound.when()!=null)fail("INVALID_SOUND_BINDING",entry.getKey(),"explicit unconditional silence with reason",sound);}
                    else {Asset asset=d.assets().get(sound.asset());if(asset==null||asset.kind()!=AssetKind.SOUND)fail("UNKNOWN_ASSET",entry.getKey(),"sound event resource",sound.asset());
                        Parameter volume=d.parameters().get(sound.volumeParameter());if(volume==null||volume.unit()!=Unit.RATIO||volume.minimum()<0||volume.maximum()>1||!sound.silenceReason().isEmpty()||sound.channel().isBlank())fail("INVALID_SOUND_BINDING",entry.getKey(),"bounded volume, channel and audible rule",sound);
                        if(sound.when()!=null){Set<String> names=new HashSet<>();conditionNames(sound.when(),names,0);if(!d.states().containsAll(names))fail("UNKNOWN_STATE",entry.getKey(),d.states(),names);}
                    }
                }
            }
        }
        d.roles().forEach((id,r)->{
            name(id);if(!r.requirements().keySet().equals(EnumSet.allOf(Aspect.class)))fail("INCOMPLETE_ROLE",id,"explicit decision for every aspect",r.requirements().keySet());
            r.requirements().forEach((aspect,req)->{
                if(req.rule()==null)fail("INCOMPLETE_ROLE",id+"/"+aspect,"rule",req);
                if(req.rule()==Rule.CONDITIONAL){if(req.condition()==null)fail("EMPTY_CONDITION",id+"/"+aspect,"named condition","null");Set<String> names=new HashSet<>();conditionNames(req.condition(),names,0);
                    if(!d.states().containsAll(names))fail("UNKNOWN_STATE",id+"/"+aspect,d.states(),names);
                    if(names.size()>16)fail("UNSUPPORTED_CONDITION",id,"at most 16 independent states",names.size());
                    boolean yes=false,no=false;List<String> list=new ArrayList<>(names);for(int mask=0;mask<(1<<list.size());mask++){Map<String,Boolean> state=new HashMap<>();for(int i=0;i<list.size();i++)state.put(list.get(i),(mask&(1<<i))!=0);if(evaluate(req.condition(),state))yes=true;else no=true;}
                    if(!yes||!no)fail("VACUOUS_CONDITION",id+"/"+aspect,"both truth outcomes possible",req.condition());
                }else if(req.condition()!=null)fail("INVALID_CONDITION",id+"/"+aspect,"condition only for conditional rule",req.condition());
                if((req.rule()==Rule.NOT_APPLICABLE||req.rule()==Rule.UNSUPPORTED)&&(req.reason()==null||req.reason().isBlank()))fail("MISSING_REASON",id+"/"+aspect,"explicit coverage reason",req.reason());
            });
        });
        d.recipes().forEach((id,r)->{
            name(id);scopeAncestors(d,r.scope());Role role=d.roles().get(r.role());if(role==null)fail("UNKNOWN_ROLE",id,d.roles().keySet(),r.role());
            if(r.implementation()==null||r.implementation().implementation()==null||r.implementation().implementation().isBlank()||!d.profile().equals(r.implementation().profile()))fail("INCOMPATIBLE_PROFILE",id,d.profile(),r.implementation());
            if(r.scenarios().isEmpty())fail("MISSING_SCENARIO",id,"production scenarios","empty");r.scenarios().forEach(DesignResolver::name);
            for(var e:role.requirements().entrySet())switch(e.getValue().rule()){
                case REQUIRED,CONDITIONAL->{if(!r.bindings().containsKey(e.getKey()))fail("MISSING_REQUIREMENT",id+"/"+e.getKey(),"role "+r.role(),"missing binding");}
                case NOT_APPLICABLE->{if(r.bindings().containsKey(e.getKey()))fail("ROLE_CAPABILITY",id+"/"+e.getKey(),"not applicable","binding exists");}
                case UNSUPPORTED->fail("UNSUPPORTED_CAPABILITY",id+"/"+e.getKey(),"supported role",e.getValue().reason());
            }
            r.bindings().forEach((aspect,ref)->{
                Family f=family(d,ref.family());accessible(d,r.scope(),f.scope(),id);
                if(f.aspect()!=aspect)fail("ASPECT_MISMATCH",id+"/"+aspect,aspect,f.aspect());
                if(value(d,ref,new HashSet<>()) instanceof GroupLayoutValue group){
                    Rule layoutRule=role.requirements().get(Aspect.LAYOUT).rule();
                    var aspects = group.scroll() ? EnumSet.of(Aspect.LAYOUT, Aspect.SCROLL) : EnumSet.of(Aspect.LAYOUT);
                    if (r.bindings().containsKey(Aspect.MATERIAL)) aspects.add(Aspect.MATERIAL);
                    if(!r.bindings().keySet().equals(aspects)||!r.slots().isEmpty()||role.interactive()){
                        fail("ROLE_CAPABILITY",id,"structural group requires "+aspects+" without content slots",r);
                    }
                    if(group.scroll()&&(layoutRule!=Rule.REQUIRED
                            ||role.requirements().get(Aspect.SCROLL).rule()!=Rule.REQUIRED
                            ||!new TextValue(GroupLayoutValue.VERTICAL_SCROLL).equals(value(d,r.bindings().get(Aspect.SCROLL))))){
                        fail("INVALID_LAYOUT",id,"always-present viewport with built-in vertical scrolling",r);
                    }
                }
            });
            r.slots().forEach((slot,s)->{name(slot);Objects.requireNonNull(s.kind());if(s.kind()==ContentKind.ACTION&&!role.interactive())fail("ROLE_CAPABILITY",id+"/"+slot,"interactive role","action on display role");});
        });
        d.layouts().forEach((id,l)->{name(id);scopeAncestors(d,l.scope());Objects.requireNonNull(l.axis());double min=geometry(d,l.scope(),l.minimumWidth(),id),max=geometry(d,l.scope(),l.maximumWidth(),id);geometry(d,l.scope(),l.gap(),id);geometry(d,l.scope(),l.padding(),id);if(max<min)fail("LAYOUT_RANGE",id,"min <= max",min+" > "+max);});
        d.pages().forEach((id,p)->{name(id);scopeAncestors(d,p.scope());Layout l=d.layouts().get(p.layout());if(l==null)fail("UNKNOWN_LAYOUT",id,d.layouts().keySet(),p.layout());accessible(d,p.scope(),l.scope(),id);
            if(p.recipes().isEmpty()||p.scenarios().isEmpty())fail("MISSING_EVIDENCE",id,"recipes and scenes","empty");
            for(String recipe:p.recipes()){Recipe r=d.recipes().get(recipe);if(r==null)fail("UNKNOWN_RECIPE",id,d.recipes().keySet(),recipe);accessible(d,p.scope(),r.scope(),id);if(!p.scenarios().containsAll(r.scenarios()))fail("MISSING_SCENARIO",id,r.scenarios(),p.scenarios());}
        });
    }
    public static Value value(ContractDesign d,Ref ref){return value(d,ref,new HashSet<>());}
    private static Value value(ContractDesign d,Ref ref,Set<Ref> seen){
        if(ref==null)throw new IllegalArgumentException("UNKNOWN_REFERENCE null");if(!seen.add(ref))fail("REFERENCE_CYCLE",ref.toString(),"acyclic references",seen);
        Family f=family(d,ref.family());Value v=members(d,ref.family(),new HashSet<>()).get(ref.member());if(v==null)fail("UNKNOWN_MEMBER",ref.toString(),"existing member","missing");
        if(v instanceof Alias a){Family target=family(d,a.target().family());accessible(d,f.scope(),target.scope(),ref.toString());if(f.aspect()!=target.aspect())fail("ASPECT_MISMATCH",ref.toString(),f.aspect(),target.aspect());return value(d,a.target(),seen);}return v;
    }
    public static NumberValue number(ContractDesign d,Ref ref,Map<String,Double> parameters){
        Value v=value(d,ref);if(v instanceof NumberValue n)return n;if(v instanceof ParameterValue p){Parameter definition=d.parameters().get(p.parameter());if(definition==null)fail("UNKNOWN_PARAMETER",ref.toString(),"defined parameter",p);
            double input=parameters.getOrDefault(p.parameter(),definition.initial());if(!Double.isFinite(input)||input<definition.minimum()||input>definition.maximum())fail("PARAMETER_RANGE",p.parameter(),definition,input);return new NumberValue(input,definition.unit());}
        throw new IllegalArgumentException("UNIT_MISMATCH "+ref+" expected typed number, actual "+v);
    }
    private static double geometry(ContractDesign d,String scope,Ref ref,String path){Family f=family(d,ref.family());accessible(d,scope,f.scope(),path);if(f.aspect()!=Aspect.SPACING&&f.aspect()!=Aspect.SIZE&&f.aspect()!=Aspect.LAYOUT)fail("ASPECT_MISMATCH",path,"geometry family",f.aspect());NumberValue n=number(d,ref,Map.of());if(n.unit()!=Unit.U||n.value()<0||n.value()>Float.MAX_VALUE)fail("UNIT_MISMATCH",path,"nonnegative logical u",n);return n.value();}
    private static Map<String,Value> members(ContractDesign d,String id,Set<String> seen){if(!seen.add(id))fail("REFERENCE_CYCLE",id,"acyclic family derivation",seen);Family f=family(d,id);Map<String,Value> values=new HashMap<>();if(f.parent()==null)fail("UNKNOWN_REFERENCE",id,"empty or named parent","null");if(!f.parent().isEmpty()){Family parent=family(d,f.parent());accessible(d,f.scope(),parent.scope(),id);if(f.aspect()!=parent.aspect())fail("ASPECT_MISMATCH",id,f.aspect(),parent.aspect());values.putAll(members(d,f.parent(),seen));}values.putAll(f.members());return values;}
    private static Family family(ContractDesign d,String id){Family f=d.families().get(id);if(f==null)fail("UNKNOWN_FAMILY",String.valueOf(id),d.families().keySet(),id);return f;}
    private static Set<String> scopeAncestors(ContractDesign d,String id){Set<String> seen=new HashSet<>();while(!"".equals(id)){if(id==null||!d.scopes().containsKey(id))fail("UNKNOWN_SCOPE",String.valueOf(id),d.scopes().keySet(),id);if(!seen.add(id))fail("REFERENCE_CYCLE",id,"acyclic scopes",seen);id=d.scopes().get(id);}return seen;}
    private static void accessible(ContractDesign d,String from,String target,String path){if(!scopeAncestors(d,from).contains(target))fail("SCOPE_VIOLATION",path,"explicit inherited scope of "+from,target);}
    private static void conditionNames(NamedCondition c,Set<String> names,int depth){if(c==null||depth>32)throw new IllegalArgumentException("UNSUPPORTED_CONDITION null or excessive depth");if(c instanceof NamedCondition.State s){name(s.name());names.add(s.name());}else if(c instanceof NamedCondition.Not n)conditionNames(n.operand(),names,depth+1);else {List<NamedCondition> items=c instanceof NamedCondition.All a?a.operands():((NamedCondition.Any)c).operands();if(items.isEmpty())throw new IllegalArgumentException("EMPTY_CONDITION operands required");for(var item:items)conditionNames(item,names,depth+1);}}
    public static boolean evaluate(NamedCondition c,Map<String,Boolean> states){if(c instanceof NamedCondition.State s){Boolean v=states.get(s.name());if(v==null)throw new IllegalArgumentException("MISSING_STATE "+s.name());return v;}if(c instanceof NamedCondition.Not n)return !evaluate(n.operand(),states);if(c instanceof NamedCondition.All a){boolean v=true;for(var item:a.operands())v&=evaluate(item,states);return v;}boolean v=false;for(var item:((NamedCondition.Any)c).operands())v|=evaluate(item,states);return v;}
    private static void name(String value){if(value==null||!value.matches("[a-zA-Z0-9][a-zA-Z0-9_.:/-]*")||value.contains("..")||Set.of("latest","HEAD").contains(value))throw new IllegalArgumentException("INVALID_ID "+value);}
    private static void fail(String code,String path,Object expected,Object actual){throw new IllegalArgumentException(code+" path="+path+" expected="+expected+" actual="+actual);}
}
