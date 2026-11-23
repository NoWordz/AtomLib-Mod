package com.atom.lib.frontend.contract;

import java.util.*;
import org.junit.jupiter.api.Test;
import static com.atom.lib.frontend.contract.ContractDesign.*;
import static org.junit.jupiter.api.Assertions.*;

class ContractRegistryTest {
    @Test void admitsTheTwoReviewedLegacyMechanismsWithoutRewritingTheManifest() {
        for (String fingerprint : List.of(
                "595aed94fa5c0b076682d6555df481cf7f065da672801e9d841399d317fb57c7",
                "685cf4bd479a4fbf89559a529b9f4f168ae1617a7f01eef8c2e3ea3a8a735418")) {
            byte[] design = withMechanism(fingerprint);
            var registry = load(design);
            assertEquals("settings", registry.page("settings").id());
        }
    }

    @Test void stableEpochIsAcceptedAndBuildFingerprintsCannotSubstituteForIt() {
        byte[] stable = withMechanism("9cc8c318f8494c7ca101d606b5dca41805fbce4fc8bac9965f88c44b99b5854e");
        assertEquals("settings", load(stable).page("settings").id());

        for (String binding : List.of("302f90e" + "0".repeat(57), MechanismIdentity.current())) {
            byte[] incompatible = withMechanism(binding);
            var failure = assertThrowsExactly(ContractCompatibilityException.class, () -> load(incompatible));
            assertEquals(ContractCompatibilityException.Reason.MECHANISM_MISMATCH, failure.reason());
            assertTrue(failure.getMessage().startsWith("INCOMPATIBLE_PROFILE"), failure.toString());
        }
    }

    @Test void partiallyBoundDesignCannotOmitTheMechanism() {
        byte[] partial = withBindings(Map.of("config/protocol", "1".repeat(64)));
        var failure = assertThrowsExactly(ContractCompatibilityException.class, () -> load(partial));
        assertEquals(ContractCompatibilityException.Reason.MISSING_MECHANISM_BINDING, failure.reason());
    }

    private static ContractRegistry load(byte[] bytes) {
        return ContractRegistry.load(bytes, ContractRegistry.manifest(bytes, bytes), Set.of("example/1"));
    }

    private static byte[] withMechanism(String binding) {
        return withBindings(Map.of(MechanismIdentity.BINDING, binding));
    }

    private static byte[] withBindings(Map<String, String> bindings) {
        var d = DesignResolverTest.fixture();
        return ContractCodec.encode(new ContractDesign(d.format(), d.namespace(), d.version(), d.profile(),
                d.scopes(), d.states(), d.parameters(), d.families(), d.roles(), d.recipes(), d.layouts(),
                d.assets(), d.pages(), bindings));
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
