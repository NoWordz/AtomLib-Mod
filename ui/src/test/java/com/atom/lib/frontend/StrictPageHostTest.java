package com.atom.lib.frontend;

import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StrictPageHostTest {
    static StrictPage data(ContractRegistry.PageHandle handle){
        return StrictPage.builder(handle).component("apply",handle.recipe("button"),
            new StrictPage.Content(Map.of("label",new StrictPage.Text("Apply"),"activate",new StrictPage.Action(()->{})),true)).build();
    }
    @Test void hostRejectsAFactoryThatReturnsAnotherRegistryBeforeItCanMountAnything(){
        var expected=StrictPageSessionTest.registry().page("page");var foreign=StrictPageSessionTest.registry().page("page");
        var host=new StrictPageHost(expected,Map.of(),null);
        var rejection=assertThrows(IllegalArgumentException.class,()->host.create(new ContractPage(){public StrictPage build(ContractRegistry.PageHandle supplied){assertSame(expected,supplied);return data(foreign);}}));
        assertTrue(rejection.getMessage().contains("FOREIGN_PAGE_HANDLE"),rejection.toString());
        var accepted=assertThrows(IllegalArgumentException.class,()->host.mount(data(expected)));
        assertTrue(accepted.getMessage().contains("MISSING_IMPLEMENTATION"),"valid admission must proceed to the actual implementation check");
    }
}
