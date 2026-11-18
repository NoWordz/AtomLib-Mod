package com.atom.lib.frontend.contract;

import java.util.*;

/** Platform-independent author/result data. Runtime code consumes decoded data, never author callbacks. */
public record ContractDesign(int format, String namespace, String version, String profile,
        Map<String,String> scopes, Set<String> states, Map<String,Parameter> parameters,
        Map<String,Family> families, Map<String,Role> roles, Map<String,Recipe> recipes,
        Map<String,Layout> layouts, Map<String,Asset> assets, Map<String,Page> pages,Map<String,String> buildBindings) {
    public ContractDesign(int format,String namespace,String version,String profile,Map<String,String> scopes,Set<String> states,Map<String,Parameter> parameters,Map<String,Family> families,Map<String,Role> roles,Map<String,Recipe> recipes,Map<String,Layout> layouts,Map<String,Asset> assets,Map<String,Page> pages){this(format,namespace,version,profile,scopes,states,parameters,families,roles,recipes,layouts,assets,pages,Map.of());}
    public ContractDesign {
        scopes=Map.copyOf(scopes);states=Set.copyOf(states);parameters=Map.copyOf(parameters);
        families=Map.copyOf(families);roles=Map.copyOf(roles);recipes=Map.copyOf(recipes);
        layouts=Map.copyOf(layouts);assets=Map.copyOf(assets);pages=Map.copyOf(pages);
        buildBindings=Map.copyOf(buildBindings);
    }
    public enum Aspect { MATERIAL,COLOR,TYPOGRAPHY,SIZE,SPACING,ICON,HOVER,PRESS,RIPPLE,HIGHLIGHT,BOUNCE,
        LAYOUT,SOUND,FOCUS,EDITING,SCROLL,RESTORATION,TRANSITION,STATES,DEGRADATION,TOOLTIP,POINTER,PREFERENCES }
    public enum Unit { U,MS,RATIO,ARGB,COUNT }
    public enum Rule { REQUIRED,CONDITIONAL,NOT_APPLICABLE,UNSUPPORTED }
    public enum ContentKind { TEXT,BUSINESS_IMAGE,ACTION,NUMBER,BOOLEAN }
    public enum AssetKind { FONT,ICON,TEXTURE,SOUND }
    public enum Axis { ROW,COLUMN,WRAP }
    public record Ref(String family,String member) {}
    public sealed interface Value permits NumberValue,TextValue,Alias,ParameterValue,AssetValue,SoundValue {}
    public record NumberValue(double value,Unit unit) implements Value {}
    /** An implementation-defined operation name, not executable code. */
    public record TextValue(String value) implements Value {}
    public record Alias(Ref target) implements Value {}
    public record ParameterValue(String parameter) implements Value {}
    public record AssetValue(String asset) implements Value {}
    /** Empty asset plus a reason means deliberate silence; nonempty asset is one committed UI cue. */
    public record SoundValue(String event,String asset,String channel,String volumeParameter,double pitch,int priority,
                             NamedCondition when,String silenceReason) implements Value {}
    public record Parameter(double minimum,double maximum,double initial,Unit unit) {}
    /** Empty parent means isolated root. Other parents name one explicit family to derive from. */
    public record Family(String scope,Aspect aspect,String parent,Map<String,Value> members) {
        public Family {members=Map.copyOf(members);}
    }
    public record Requirement(Rule rule,NamedCondition condition,String reason) {
        public static Requirement required(){return new Requirement(Rule.REQUIRED,null,"");}
        public static Requirement conditional(NamedCondition condition){return new Requirement(Rule.CONDITIONAL,condition,"");}
        public static Requirement notApplicable(String reason){return new Requirement(Rule.NOT_APPLICABLE,null,reason);}
        public static Requirement unsupported(String reason){return new Requirement(Rule.UNSUPPORTED,null,reason);}
    }
    /** Every aspect receives an explicit decision; the recipe cannot weaken this role. */
    public record Role(boolean interactive,Map<Aspect,Requirement> requirements) {
        public Role {requirements=Map.copyOf(requirements);}
    }
    public record Binding(String implementation,String profile) {}
    public record Slot(ContentKind kind,boolean required) {}
    public record Recipe(String scope,String role,Map<Aspect,Ref> bindings,Binding implementation,
                         Map<String,Slot> slots,Set<String> scenarios) {
        public Recipe {bindings=Map.copyOf(bindings);slots=Map.copyOf(slots);scenarios=Set.copyOf(scenarios);}
    }
    /** Geometry is a design rule. Pages never supply numeric layout parameters. */
    public record Layout(String scope,Axis axis,Ref gap,Ref padding,Ref minimumWidth,Ref maximumWidth) {}
    public record Asset(AssetKind kind,String resource,String provenance) {}
    public record Page(String scope,Set<String> recipes,String layout,Set<String> scenarios) {
        public Page {recipes=Set.copyOf(recipes);scenarios=Set.copyOf(scenarios);}
    }
}
