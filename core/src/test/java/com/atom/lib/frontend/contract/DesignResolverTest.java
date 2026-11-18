package com.atom.lib.frontend.contract;

import org.junit.jupiter.api.Test;
import java.util.*;
import static com.atom.lib.frontend.contract.ContractDesign.*;
import static org.junit.jupiter.api.Assertions.*;

class DesignResolverTest {
    static ContractDesign fixture(){
        var requirements=new EnumMap<Aspect,Requirement>(Aspect.class);
        for(var aspect:Aspect.values())requirements.put(aspect,Requirement.notApplicable("reference role does not use this capability"));
        for(var aspect:List.of(Aspect.MATERIAL,Aspect.SIZE,Aspect.HOVER,Aspect.SOUND))requirements.put(aspect,Requirement.required());
        return new ContractDesign(1,"example","1","example/1",Map.of("root","","private",""),Set.of("enabled","hover"),Map.of(),
            Map.of("surface",new Family("root",Aspect.MATERIAL,"",Map.of("card",new TextValue("reference-card/1"))),
                   "size",new Family("root",Aspect.SIZE,"",Map.of("normal",new NumberValue(48,Unit.U))),
                   "space",new Family("root",Aspect.SPACING,"",Map.of("gap",new NumberValue(8,Unit.U),"max",new NumberValue(800,Unit.U))),
                   "hover",new Family("root",Aspect.HOVER,"",Map.of("standard",new NumberValue(90,Unit.MS))),
                   "sound",new Family("root",Aspect.SOUND,"",Map.of("silent",new SoundValue("BUTTON","","","",1,0,null,"reference explicit silence")))),
            Map.of("action",new Role(true,requirements)),
            Map.of("card",new Recipe("root","action",Map.of(Aspect.MATERIAL,new Ref("surface","card"),Aspect.SIZE,new Ref("size","normal"),
                Aspect.HOVER,new Ref("hover","standard"),Aspect.SOUND,new Ref("sound","silent")),new Binding("reference-card/1","example/1"),Map.of("action",new Slot(ContentKind.ACTION,true)),Set.of("enter","activate","cancel"))),
            Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("space","gap"),new Ref("space","gap"),new Ref("space","gap"),new Ref("space","max"))),
            Map.of(),Map.of("settings",new Page("root",Set.of("card"),"stack",Set.of("enter","activate","cancel"))));
    }
    static ContractDesign copy(ContractDesign d,Map<String,Family> families,Map<String,Role> roles,Map<String,Recipe> recipes,Map<String,Page> pages){
        return new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),d.states(),d.parameters(),families,roles,recipes,d.layouts(),d.assets(),pages);
    }
    static void fails(String rule,ContractDesign d){var ex=assertThrows(IllegalArgumentException.class,()->DesignResolver.validate(d));assertTrue(ex.getMessage().contains(rule),ex.getMessage());}
    @Test void completeReferenceAllowsBusinessActionAndExplicitSilence(){assertDoesNotThrow(()->DesignResolver.validate(fixture()));}
    @Test void missingHoverCannotHideBehindDeclaredRole(){var d=fixture();var r=d.recipes().get("card");var b=new EnumMap<Aspect,Ref>(r.bindings());b.remove(Aspect.HOVER);
        fails("MISSING_REQUIREMENT",copy(d,d.families(),d.roles(),Map.of("card",new Recipe(r.scope(),r.role(),b,r.implementation(),r.slots(),r.scenarios())),d.pages()));}
    @Test void omittedCoverageAndDisplayRoleWithActionBothFail(){var d=fixture();var r=d.roles().get("action");var req=new EnumMap<Aspect,Requirement>(r.requirements());req.remove(Aspect.SOUND);
        fails("INCOMPLETE_ROLE",copy(d,d.families(),Map.of("action",new Role(true,req)),d.recipes(),d.pages()));
        fails("ROLE_CAPABILITY",copy(d,d.families(),Map.of("action",new Role(false,r.requirements())),d.recipes(),d.pages()));}
    @Test void familyCyclesScopeLeaksAndWrongUnitsFail(){var d=fixture();var f=new HashMap<>(d.families());f.put("surface",new Family("root",Aspect.MATERIAL,"surface",Map.of()));
        fails("REFERENCE_CYCLE",copy(d,f,d.roles(),d.recipes(),d.pages()));
        f=new HashMap<>(d.families());f.put("hover",new Family("private",Aspect.HOVER,"",Map.of("standard",new NumberValue(90,Unit.MS))));
        fails("SCOPE_VIOLATION",copy(d,f,d.roles(),d.recipes(),d.pages()));
        f=new HashMap<>(d.families());f.put("space",new Family("root",Aspect.SPACING,"",Map.of("gap",new NumberValue(8,Unit.MS),"max",new NumberValue(800,Unit.U))));
        fails("UNIT_MISMATCH",copy(d,f,d.roles(),d.recipes(),d.pages()));}
    @Test void profileDriftAndMissingScenariosCannotPass(){var d=fixture();var r=d.recipes().get("card");
        fails("INCOMPATIBLE_PROFILE",copy(d,d.families(),d.roles(),Map.of("card",new Recipe(r.scope(),r.role(),r.bindings(),new Binding("reference-card/1","example/2"),r.slots(),r.scenarios())),d.pages()));
        fails("MISSING_SCENARIO",copy(d,d.families(),d.roles(),d.recipes(),Map.of("settings",new Page("root",Set.of("card"),"stack",Set.of("enter")))));}
    @Test void conditionsUseKnownNonEmptyStatesAndNoArbitraryCallback(){var d=fixture();var req=new EnumMap<Aspect,Requirement>(d.roles().get("action").requirements());req.put(Aspect.HOVER,Requirement.conditional(new NamedCondition.State("invented")));
        fails("UNKNOWN_STATE",copy(d,d.families(),Map.of("action",new Role(true,req)),d.recipes(),d.pages()));
        req.put(Aspect.HOVER,Requirement.conditional(new NamedCondition.All(List.of())));
        fails("EMPTY_CONDITION",copy(d,d.families(),Map.of("action",new Role(true,req)),d.recipes(),d.pages()));}
}
