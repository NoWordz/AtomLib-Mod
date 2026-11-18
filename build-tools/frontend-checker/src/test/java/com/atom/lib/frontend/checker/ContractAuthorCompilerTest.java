package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.contract.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ContractAuthorCompilerTest {
    @TempDir Path temp;
    static String source(){return """
        import com.atom.lib.frontend.contract.*;
        import com.atom.lib.frontend.contract.ContractDesign.*;
        import java.util.*;
        public final class Design {
          public static final ContractDesign DESIGN = new ContractDesign(1,"sample","1","sample/1",
            Map.of("root",""),Set.of(),Map.of(),Map.of(),Map.of(),Map.of(),
            Map.of("stack",new Layout("root",Axis.COLUMN,new Ref("gap","normal"),new Ref("gap","normal"),new Ref("gap","normal"),new Ref("gap","normal"))),
            Map.of(),Map.of("page",new Page("root",Set.of("card"),"stack",Set.of("normal"))));
        }
        """;}
    // A complete literal author uses explicit role decisions; no runtime factory reads the environment.
    static String valid(){String req=java.util.Arrays.stream(ContractDesign.Aspect.values()).map(a->"Map.entry(Aspect."+a+",Requirement.notApplicable(\"no such capability in approved display role\"))").collect(java.util.stream.Collectors.joining(","));
        return source().replace("Set.of(),Map.of(),Map.of(),Map.of(),Map.of(),", "Set.of(),Map.of(),Map.of(\"gap\",new Family(\"root\",Aspect.SPACING,\"\",Map.of(\"normal\",new NumberValue(8,Unit.U)))),Map.of(\"display\",new Role(false,Map.ofEntries("+req+"))),Map.of(\"card\",new Recipe(\"root\",\"display\",Map.of(),new Binding(\"display/1\",\"sample/1\"),Map.of(\"text\",new Slot(ContentKind.TEXT,true)),Set.of(\"normal\"))),");}
    Path author(String text)throws Exception{Path p=temp.resolve("Design.java");Files.writeString(p,text);return p;}
    @Test void typedLiteralAuthorProducesRuntimeReadableDeterministicPackage()throws Exception{Path p=author(valid());byte[] a=ContractAuthorCompiler.compile(p);byte[] b=ContractAuthorCompiler.compile(p);assertArrayEquals(a,b);assertEquals("sample",ContractCodec.decode(a).namespace());}
    @Test void environmentCallsAndInitializerSideEffectsAreRejectedWithoutExecuting()throws Exception{
        for(String text:java.util.List.of(valid().replace("\"sample\",\"1\"","System.getenv(\"FCL_TEST\"),\"1\""),valid().replace("public static final ContractDesign","static { System.setProperty(\"fcl.sideEffect\",\"executed\"); } public static final ContractDesign"))) {
            var e=assertThrows(IllegalArgumentException.class,()->ContractAuthorCompiler.compile(author(text)));assertTrue(e.getMessage().contains("INVALID_AUTHOR"),e.getMessage());assertNull(System.getProperty("fcl.sideEffect"));}}
    @Test void missingRoleRequirementsFailsDuringAuthorCompilation()throws Exception{var e=assertThrows(IllegalArgumentException.class,()->ContractAuthorCompiler.compile(author(valid().replaceFirst("Requirement.notApplicable","Requirement.unsupported"))));assertTrue(e.getMessage().contains("UNSUPPORTED_CAPABILITY"),e.getMessage());}
}
