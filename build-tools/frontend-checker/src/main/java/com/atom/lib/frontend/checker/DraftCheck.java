package com.atom.lib.frontend.checker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.zip.ZipFile;
import java.util.*;

/** Explicit development source/resource ownership and final archive exclusion. */
public final class DraftCheck {
    private DraftCheck(){}
    public static List<String> sources(Collection<Path> production,Collection<Path> draft){
        Set<Path> isolated=new HashSet<>();draft.forEach(p->isolated.add(canonical(p)));List<String> errors=new ArrayList<>();
        for(Path file:production)if(isolated.contains(canonical(file)))errors.add("DRAFT_IN_PRODUCTION source/resource="+file+" move the reference to the isolated frontendDraft source set");return List.copyOf(errors);
    }
    private static Path canonical(Path path){try{return Files.exists(path)?path.toRealPath():path.toAbsolutePath().normalize();}catch(IOException e){throw new IllegalArgumentException("UNSUPPORTED_SOURCE "+path,e);}}
    public static List<String> archive(Path archive,Set<String> draftEntries)throws IOException{
        List<String> errors=new ArrayList<>();try(var zip=new ZipFile(archive.toFile())){var entries=zip.entries();while(entries.hasMoreElements()){
            String name=entries.nextElement().getName();if(draftEntries.contains(name)||name.startsWith("frontend-draft/")||name.startsWith("META-INF/atomlib/frontend-draft/"))errors.add("DRAFT_IN_PRODUCTION archive="+archive+" entry="+name+" remove isolated development content from production output");
        }}return List.copyOf(errors);
    }
}
