package com.atom.lib.frontend.checker;

import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DraftCheckTest {
    @TempDir Path root;
    Path jar(String... names)throws Exception{Path p=root.resolve("candidate.jar");try(var z=new ZipOutputStream(Files.newOutputStream(p))){for(String name:names){z.putNextEntry(new ZipEntry(name));z.write(new byte[]{1,2,3});z.closeEntry();}}return p;}
    @Test void draftSourceCannotBeAddedToProductionEvenWhenJavaWouldCompile(){Path file=root.resolve("frontendDraft/Example.java");assertTrue(DraftCheck.sources(List.of(file),List.of(file)).stream().anyMatch(s->s.startsWith("DRAFT_IN_PRODUCTION")));assertTrue(DraftCheck.sources(List.of(root.resolve("main/Page.java")),List.of(file)).isEmpty());}
    @Test void resourceAndCompiledClassDraftsAreRejectedByActualJarEntries()throws Exception{
        assertTrue(DraftCheck.archive(jar("consumer/Page.class","assets/example/icon.png"),Set.of("consumer/Draft.class")).isEmpty());
        assertTrue(DraftCheck.archive(jar("consumer/Page.class","consumer/Draft.class"),Set.of("consumer/Draft.class")).stream().anyMatch(s->s.startsWith("DRAFT_IN_PRODUCTION")));
        assertTrue(DraftCheck.archive(jar("assets/example/icon.png","frontend-draft/new-style.png"),Set.of()).stream().anyMatch(s->s.startsWith("DRAFT_IN_PRODUCTION")));
    }
}
