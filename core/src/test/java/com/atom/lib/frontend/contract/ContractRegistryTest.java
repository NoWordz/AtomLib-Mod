package com.atom.lib.frontend.contract;

import java.util.*;
import org.junit.jupiter.api.Test;
import static com.atom.lib.frontend.contract.ContractDesign.*;
import static org.junit.jupiter.api.Assertions.*;

class ContractRegistryTest {
    @Test void approvedBytesFromAnotherMechanismCannotSilentlyUseTheCurrentInterpreter(){
        var d=DesignResolverTest.fixture();
        var incompatible=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),d.states(),d.parameters(),d.families(),d.roles(),d.recipes(),d.layouts(),d.assets(),d.pages(),Map.of("mechanism/atomlib","0".repeat(64)));
        byte[] bytes=ContractCodec.encode(incompatible);
        var failure=assertThrows(IllegalArgumentException.class,()->ContractRegistry.load(bytes,ContractRegistry.manifest(bytes,bytes),Set.of("example/1")));
        assertTrue(failure.getMessage().startsWith("INCOMPATIBLE_PROFILE"),failure.toString());
        var compatible=new ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),d.states(),d.parameters(),d.families(),d.roles(),d.recipes(),d.layouts(),d.assets(),d.pages(),Map.of(MechanismIdentity.BINDING,MechanismIdentity.current()));
        byte[] current=ContractCodec.encode(compatible);
        assertDoesNotThrow(()->ContractRegistry.load(current,ContractRegistry.manifest(current,current),Set.of("example/1")));
    }
    @org.junit.jupiter.api.Test void conditionalCapabilitiesAreResolvedFromActualNamedState(){
        var d=DesignResolverTest.fixture();var roles=new java.util.HashMap<>(d.roles());var required=new java.util.EnumMap<ContractDesign.Aspect,ContractDesign.Requirement>(roles.get("action").requirements());required.put(ContractDesign.Aspect.HOVER,ContractDesign.Requirement.conditional(new NamedCondition.State("hover")));roles.put("action",new ContractDesign.Role(true,required));
        var conditional=DesignResolverTest.copy(d,d.families(),roles,d.recipes(),d.pages());byte[] bytes=ContractCodec.encode(conditional);var r=ContractRegistry.load(bytes,ContractRegistry.manifest(bytes,bytes),java.util.Set.of("example/1")).page("settings").recipe("card");
        try{var method=r.getClass().getMethod("bindings",java.util.Map.class,java.util.Map.class);var off=(java.util.Map<?,?>)method.invoke(r,java.util.Map.of(),java.util.Map.of("hover",false));var on=(java.util.Map<?,?>)method.invoke(r,java.util.Map.of(),java.util.Map.of("hover",true));org.junit.jupiter.api.Assertions.assertFalse(off.containsKey(ContractDesign.Aspect.HOVER));org.junit.jupiter.api.Assertions.assertTrue(on.containsKey(ContractDesign.Aspect.HOVER));}
        catch(ReflectiveOperationException e){org.junit.jupiter.api.Assertions.fail("conditional runtime binding is missing",e);}
    }
    @Test void missingManifestPendingDesignAndIncompatibleProfilesAreBlocked(){var bytes=ContractCodec.encode(DesignResolverTest.fixture());
        assertThrows(IllegalArgumentException.class,()->ContractRegistry.load(bytes,new byte[0],Set.of("example/1")));
        var other=DesignResolverTest.fixture();var f=new HashMap<>(other.families());f.put("surface",new Family("root",Aspect.MATERIAL,"",Map.of("card",new TextValue("other-card/1"))));
        byte[] changed=ContractCodec.encode(DesignResolverTest.copy(other,f,other.roles(),other.recipes(),other.pages()));
        var e=assertThrows(IllegalArgumentException.class,()->ContractRegistry.manifest(changed,bytes));assertTrue(e.getMessage().contains("PENDING_DESIGN_REVIEW"),e.getMessage());
        byte[] manifest=ContractRegistry.manifest(bytes,bytes);assertThrows(IllegalArgumentException.class,()->ContractRegistry.load(bytes,manifest,Set.of("example/2")));
        assertThrows(IllegalArgumentException.class,()->ContractRegistry.load(changed,manifest,Set.of("example/1")));}
    @Test void equalIdsInDifferentRegistriesCannotExchangeHandles(){byte[] bytes=ContractCodec.encode(DesignResolverTest.fixture());byte[] manifest=ContractRegistry.manifest(bytes,bytes);
        var a=ContractRegistry.load(bytes,manifest,Set.of("example/1"));var b=ContractRegistry.load(bytes,manifest,Set.of("example/1"));
        var page=a.page("settings");assertDoesNotThrow(()->page.require(page.recipe("card")));
        var e=assertThrows(IllegalArgumentException.class,()->page.require(b.page("settings").recipe("card")));assertTrue(e.getMessage().contains("FOREIGN_HANDLE"),e.getMessage());
        assertThrows(IllegalArgumentException.class,()->page.recipe("invented"));}
}
