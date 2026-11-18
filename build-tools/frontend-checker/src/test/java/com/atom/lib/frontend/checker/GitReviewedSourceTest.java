package com.atom.lib.frontend.checker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class GitReviewedSourceTest {
    @TempDir Path temporary;
    // Hand-written historical objects in an isolated fixture, no commits or Git writes
    // are performed against the user's repository (or through a shell).
    private Path repository() throws Exception {
        return repository(new byte[]{0,1,2,3});
    }
    private Path repository(byte[] design) throws Exception {
        Path repo=temporary.resolve("repo"),git=repo.resolve(".git");
        Files.createDirectories(git.resolve("objects"));Files.createDirectories(git.resolve("refs/tags"));
        Files.writeString(git.resolve("HEAD"),"ref: refs/heads/unused\n");
        Files.writeString(git.resolve("config"),"[core]\nrepositoryformatversion=0\nbare=false\n");
        Map<String,byte[]> entries=new TreeMap<>();
        entries.put("contract.properties","protocol=1\nprofiles=probe-v1\nstrict=consumer.Page#build()\ndesign=design.bin\nprotectedFiles=build.gradle\n".getBytes(StandardCharsets.UTF_8));
        entries.put("design.bin",new byte[]{0,1,2,3});
        entries.put("build.gradle","apply plugin: 'reviewed'\n".getBytes(StandardCharsets.UTF_8));
        var tree=new ByteArrayOutputStream();
        entries.put("design.bin",design);
        for(var entry:entries.entrySet()) {
            String blob=object(git,"blob",entry.getValue());
            tree.write(("100644 "+entry.getKey()+"\0").getBytes(StandardCharsets.UTF_8));
            tree.write(HexFormat.of().parseHex(blob));
        }
        String treeId=object(git,"tree",tree.toByteArray());
        String commit=object(git,"commit",("tree "+treeId+"\nauthor Fixture <fixture@example.invalid> 0 +0000\ncommitter Fixture <fixture@example.invalid> 0 +0000\n\nreviewed fixture\n").getBytes(StandardCharsets.UTF_8));
        Files.writeString(git.resolve("refs/tags/design-v1"),commit+"\n");
        Files.writeString(repo.resolve("contract.properties"),"design=unreviewed.bin\n");
        Files.writeString(repo.resolve("build.gradle"),"worktree poison\n");
        return repo;
    }
    private String object(Path git,String type,byte[] data) throws Exception {
        var raw=new ByteArrayOutputStream();raw.write((type+" "+data.length+"\0").getBytes(StandardCharsets.UTF_8));raw.write(data);
        String id=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(raw.toByteArray()));
        Path file=git.resolve("objects/"+id.substring(0,2)+"/"+id.substring(2));Files.createDirectories(file.getParent());
        try(var out=new DeflaterOutputStream(Files.newOutputStream(file))) {out.write(raw.toByteArray());}
        return id;
    }
    @Test void usesHistoricalBytesInsteadOfCurrentWorktreeAndRecordsVersion() throws Exception {
        var snapshot=GitReviewedSource.materialize(repository(),"refs/tags/design-v1","contract.properties",temporary.resolve("cache"));
        assertTrue(Files.readString(snapshot.config()).contains("design=design.bin"));
        assertEquals("apply plugin: 'reviewed'\n",Files.readString(snapshot.config().getParent().resolve("build.gradle")));
        assertArrayEquals(new byte[]{0,1,2,3},Files.readAllBytes(snapshot.config().getParent().resolve("design.bin")));
        assertTrue(snapshot.revision().matches("[0-9a-f]{40}"));
    }
    @Test void neverFallsBackToHeadMissingRefMissingFileOrEscapingPath() throws Exception {
        Path repo=repository(),cache=temporary.resolve("cache");
        assertThrows(Exception.class,()->GitReviewedSource.materialize(repo,"HEAD","contract.properties",cache));
        assertThrows(Exception.class,()->GitReviewedSource.materialize(repo,"refs/tags/missing","contract.properties",cache));
        assertThrows(Exception.class,()->GitReviewedSource.materialize(repo,"refs/tags/design-v1","missing.properties",cache));
        assertThrows(Exception.class,()->GitReviewedSource.materialize(repo,"refs/tags/design-v1","../contract.properties",cache));
    }
    @Test void historicalBundleFeedsOrdinaryCheckWithoutWorktreeApproval() throws Exception {
        Path author=temporary.resolve("Theme.java");
	actualAuthor(author);
        byte[] design=DesignPackage.compile(author);
        var snapshot=GitReviewedSource.materialize(repository(design),"refs/tags/design-v1","contract.properties",temporary.resolve("cache"));
        Path current=temporary.resolve("candidate");Files.createDirectories(current);
        for(String file:List.of("contract.properties","design.bin","build.gradle")) Files.copy(snapshot.config().getParent().resolve(file),current.resolve(file));
        Path page=current.resolve("Page.java");Files.writeString(page,"package consumer; class Page {void build(){int volume=125;}} ");
        var result=ProjectCheck.run(current.resolve("contract.properties"),snapshot.config(),List.of(page),List.of(),17);
        assertTrue(result.ordinarySuccess(),result.toString());
        Files.writeString(current.resolve("build.gradle"),"apply plugin: 'unchecked'\n");
        var changed=ProjectCheck.run(current.resolve("contract.properties"),snapshot.config(),List.of(page),List.of(),17);
        assertFalse(changed.ordinarySuccess());
        assertTrue(changed.diagnostics().stream().anyMatch(s->s.startsWith("INTEGRATION_CHANGED ")));
    }
    private void actualAuthor(Path path) throws Exception {
        Files.writeString(path,"class Theme {static final String namespace=\"fixture\",version=\"1\",profile=\"probe-v1\"; static final int gap=8;}");
    }
}
