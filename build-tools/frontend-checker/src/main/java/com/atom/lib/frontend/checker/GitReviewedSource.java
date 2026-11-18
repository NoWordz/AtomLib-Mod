package com.atom.lib.frontend.checker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Reads a caller-selected reviewed Git version without checkout or worktree fallback. */
public final class GitReviewedSource {
    private GitReviewedSource() {}
    public record Snapshot(Path config,String revision) {}
    public static Snapshot materialize(Path repository,String revision,String configPath,Path cache) throws Exception {
        if(revision==null || !(revision.matches("[0-9a-fA-F]{40}|[0-9a-fA-F]{64}") || revision.matches("refs/(tags|remotes)/[A-Za-z0-9_./-]+"))) throw new IOException("MISSING_EVIDENCE explicit reviewed version required, HEAD/worktree/latest is not a source");
        String commit=new String(git(repository,"rev-parse","--verify","--end-of-options",revision+"^{commit}"),StandardCharsets.UTF_8).trim();
        String config=path(configPath);
        byte[] configBytes=read(repository,commit,config);
        Properties properties=new Properties();properties.load(new StringReader(new String(configBytes,StandardCharsets.UTF_8)));
        String design=properties.getProperty("design");
        if(design==null) throw new IOException("MISSING_EVIDENCE reviewed config has no design package");
        String prefix=config.contains("/")?config.substring(0,config.lastIndexOf('/')+1):"";
        Map<String,byte[]> content=new TreeMap<>();content.put(config,configBytes);
        content.put(path(prefix+path(design)),read(repository,commit,path(prefix+path(design))));
        String protectedFiles=properties.getProperty("protectedFiles","");
        for(String file:protectedFiles.split(";")) if(!file.isBlank()) {
            String key=path(prefix+path(file.trim()));content.put(key,read(repository,commit,key));
        }
        Files.createDirectories(cache);
        Path staging=Files.createTempDirectory(cache,"reviewed-");
        for(var entry:content.entrySet()) {
            Path file=staging.resolve(entry.getKey());Files.createDirectories(file.getParent());Files.write(file,entry.getValue());
        }
        Files.writeString(staging.resolve("reviewed-source.txt"),"gitRevision="+commit+"\nconfig="+config+"\n");
        return new Snapshot(staging.resolve(config),commit);
    }
    private static String path(String input) throws IOException {
        if(input==null || input.isBlank() || input.contains("\\") || input.contains(":") || input.startsWith("/")
            || Arrays.stream(input.split("/",-1)).anyMatch(p->p.isBlank() || p.equals(".") || p.equals(".."))) throw new IOException("INVALID_CONFIGURATION repository-relative Git path required");
        return input;
    }
    private static byte[] read(Path repo,String commit,String path) throws Exception {
        String mode=new String(git(repo,"ls-tree",commit,"--",path),StandardCharsets.UTF_8);
        if(!(mode.startsWith("100644 blob ") || mode.startsWith("100755 blob "))) throw new IOException("MISSING_EVIDENCE regular tracked input required: "+path);
        return git(repo,"show","--no-textconv",commit+":"+path);
    }
    private static byte[] git(Path repo,String... arguments) throws Exception {
        List<String> command=new ArrayList<>(List.of("git","--no-replace-objects","-C",repo.toAbsolutePath().toString()));command.addAll(List.of(arguments));
        Process process=new ProcessBuilder(command).start();
        byte[] data=process.getInputStream().readAllBytes(),error=process.getErrorStream().readAllBytes();
        if(process.waitFor()!=0) throw new IOException("MISSING_EVIDENCE Git reviewed source: "+new String(error,StandardCharsets.UTF_8).trim());
        return data;
    }
}
