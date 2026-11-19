package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.ContractPage;
import com.atom.lib.frontend.contract.ContractDesign;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ContractPageBoundaryTest {
    @TempDir Path root;
    BoundaryReport inspect(String body)throws Exception{
        Path source=root.resolve("Page.java");Files.writeString(source,"""
            package consumer;
            import com.atom.lib.frontend.*;
            import com.atom.lib.frontend.contract.ContractRegistry;
            import java.util.*;
            class Ink {static void rect(int n){}}
            class Page extends ContractPage {
              public StrictPage build(ContractRegistry.PageHandle page){
            """+body+"""
              }
            }
            """);
        List<Path> cp=List.of(Path.of(ContractPage.class.getProtectionDomain().getCodeSource().getLocation().toURI()),Path.of(ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        return BoundaryCheck.inspect(List.of(source),cp,17,new BoundaryPolicy(Set.of(),Set.of(),Set.of("consumer.Ink"),Set.of(),Set.of(),Map.of(),Map.of(),true));
    }
    @Test void newPageClassIsStrictWithoutEditingAnEnrollmentList()throws Exception{
        var legal=inspect("return StrictPage.builder(page).component(\"row\",page.recipe(\"card\"),new StrictPage.Content(Map.of(\"title\",new StrictPage.Text(\"Business\")),true)).build();");
        assertTrue(legal.valid(),legal.toString());
        assertTrue(legal.categories().getOrDefault("consumer.Page#build(com.atom.lib.frontend.contract.ContractRegistry.PageHandle)",Set.of()).contains("CONTRACT_PAGE"),legal.toString());
        var illegal=inspect("Ink.rect(25); return null;");
        assertTrue(illegal.diagnostics().stream().anyMatch(x->x.startsWith("PAGE_DRAWING consumer.Page#build")),illegal.toString());
        assertFalse(illegal.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")),illegal.toString());
        var callbackEscape=inspect("new com.atom.lib.controls.HostedControl(new com.atom.lib.layout.NodeId(\"unapproved\"),null);return null;");
        assertTrue(callbackEscape.diagnostics().stream().anyMatch(x->x.startsWith("PAGE_DRAWING ")&&x.contains("HostedControl")),callbackEscape.toString());
        assertFalse(callbackEscape.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")),callbackEscape.toString());
    }

    @Test void nestedBusinessContentIsAllowedButDrawingInGroupCallbackIsRejected()throws Exception{
        String page="""
            return StrictPage.builder(page).group("group",page.recipe("group"),group->{
                %s
                group.component("row",page.recipe("card"),
                    new StrictPage.Content(Map.of("title",new StrictPage.Text("Business")),true));
            }).build();
            """;
        var legal=inspect(page.formatted(""));
        assertTrue(legal.valid(),legal.toString());
        var illegal=inspect(page.formatted("Ink.rect(25);"));
        assertTrue(illegal.diagnostics().stream().anyMatch(x->x.startsWith("PAGE_DRAWING ")&&x.contains("consumer.Ink")),illegal.toString());
        assertFalse(illegal.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")),illegal.toString());
    }

    @Test void sourceRecordReadsArePureButExplicitAccessorsStillHaveTheirBodiesChecked()throws Exception{
        String build="""
            var value=new Caption("Settings");
            return StrictPage.builder(page).component("row",page.recipe("card"),
                new StrictPage.Content(Map.of("title",new StrictPage.Text(value.label())),true)).build();
            """;
        var legal=inspect("record Caption(String label) {}\n"+build);
        assertTrue(legal.valid(),legal.diagnostics().toString());
        var illegal=inspect("record Caption(String label) { public String label(){Ink.rect(25);return label;} }\n"+build);
        assertTrue(illegal.diagnostics().stream().anyMatch(x->x.startsWith("PAGE_DRAWING ")&&x.contains("consumer.Ink")),
                illegal.diagnostics().toString());
        assertFalse(illegal.diagnostics().stream().anyMatch(x->x.startsWith("SOURCE_ANALYSIS_FAILED")),
                illegal.diagnostics().toString());
    }
}
