package com.atom.lib.frontend;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.atom.lib.frontend.contract.*;
import static com.atom.lib.frontend.contract.ContractDesign.*;
import static org.junit.jupiter.api.Assertions.*;

class StrictPageTest {
    static ContractRegistry registry(){
        var req=new EnumMap<Aspect,Requirement>(Aspect.class);for(var a:Aspect.values())req.put(a,Requirement.notApplicable("isolated display fixture has no interaction"));
        var d=new ContractDesign(1,"test","1","test/1",Map.of("root",""),Set.of(),Map.of(),
            Map.of("space",new Family("root",Aspect.SPACING,"",Map.of("gap",new NumberValue(8,Unit.U),"max",new NumberValue(800,Unit.U)))),
            Map.of("display",new Role(false,req)),Map.of("card",new Recipe("root","display",Map.of(),new Binding("fixture/1","test/1"),
                Map.of("title",new Slot(ContentKind.TEXT,true),"avatar",new Slot(ContentKind.BUSINESS_IMAGE,false)),Set.of("normal"))),
            Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("space","gap"),new Ref("space","gap"),new Ref("space","gap"),new Ref("space","max"))),
            Map.of(),Map.of("page",new Page("root",Set.of("card"),"stack",Set.of("normal"))));
        byte[] bytes=ContractCodec.encode(d);return ContractRegistry.load(bytes,ContractRegistry.manifest(bytes,bytes),Set.of("test/1"));
    }
    @Test void businessImageCanContainUiScreenshotWithinApprovedContentSlot(){var p=registry().page("page");assertDoesNotThrow(()->StrictPage.builder(p).component("one",p.recipe("card"),new StrictPage.Content(Map.of("title",new StrictPage.Text("avatar"),"avatar",new StrictPage.Image("user-image-containing-ui-screenshot")),true)).build());}
    @Test void foreignHandlesAndImageMaterialPurposeAreRejected(){var a=registry().page("page");var b=registry().page("page");
        assertThrows(IllegalArgumentException.class,()->StrictPage.builder(a).component("one",b.recipe("card"),new StrictPage.Content(Map.of("title",new StrictPage.Text("test")),true)));
        assertThrows(IllegalArgumentException.class,()->StrictPage.builder(a).component("one",a.recipe("card"),new StrictPage.Content(Map.of("title",new StrictPage.Image("paint-the-whole-card")),true)));
        assertThrows(IllegalArgumentException.class,()->StrictPage.builder(a).component("one",a.recipe("card"),new StrictPage.Content(Map.of("title",new StrictPage.Text("x"),"material",new StrictPage.Image("custom-skin")),true)));}
    @Test void missingSlotsDuplicateIdentityAndEmptyPagesDoNotDisappearFromCoverage(){var p=registry().page("page");
        assertThrows(IllegalArgumentException.class,()->StrictPage.builder(p).build());
        assertThrows(IllegalArgumentException.class,()->StrictPage.builder(p).component("one",p.recipe("card"),new StrictPage.Content(Map.of(),true)));
        var content=new StrictPage.Content(Map.of("title",new StrictPage.Text("value")),true);
        assertThrows(IllegalArgumentException.class,()->StrictPage.builder(p).component("one",p.recipe("card"),content).component("one",p.recipe("card"),content));}
}
