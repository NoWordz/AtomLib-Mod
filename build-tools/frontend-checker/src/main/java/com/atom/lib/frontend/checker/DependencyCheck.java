package com.atom.lib.frontend.checker;

import java.nio.file.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Binds the resolved external implementation closure; local source dependencies are checked by BoundaryCheck. */
final class DependencyCheck {
    private DependencyCheck() {}
    static List<String> inspect(BoundaryReport report,BoundaryPolicy policy,List<Path> classpath,Properties pins)throws Exception {
        Set<String> owners=new TreeSet<>(policy.implementations().keySet());
        for(String method:policy.trustedMethods()){int split=method.indexOf('#');if(split>0)owners.add(method.substring(0,split));}
        Deque<String> pending=new ArrayDeque<>(owners);while(!pending.isEmpty())for(String dependency:report.implementationDependencies().getOrDefault(pending.remove(),Set.of()))if(owners.add(dependency))pending.add(dependency);
        boolean external=false;List<String> errors=new ArrayList<>();
        for(String owner:owners){if(owner.startsWith("java.")||owner.startsWith("javax.")||report.implementationStructures().containsKey(owner))continue;
            String entry=owner.replace('.','/')+".class";boolean present=false;
            for(Path cp:classpath){if(Files.isDirectory(cp))present|=Files.isRegularFile(cp.resolve(entry));else if(Files.isRegularFile(cp))try(ZipFile zip=new ZipFile(cp.toFile())){present|=zip.getEntry(entry)!=null;}}
            if(!present)errors.add("MISSING_IMPLEMENTATION "+owner+" external trusted type not found in resolved dependencies");else external=true;
        }
        // A trusted jar can call another jar. Pin the complete supplied dependency catalog, not only its entry class.
        if(external){Set<String> names=new HashSet<>();for(Path path:classpath){String name=path.getFileName().toString();if(!names.add(name)){errors.add("DEPENDENCY_AMBIGUOUS "+name+" use a host adapter with unique resolved catalog identities");continue;}
            if(!Files.exists(path)){errors.add("DEPENDENCY_MISSING "+name);continue;}
            String actual=digest(path),expected=pins.getProperty("dependency."+name);
            if(expected==null)errors.add("DEPENDENCY_UNBOUND "+name+" sha256="+actual);else if(!actual.equals(expected))errors.add("DEPENDENCY_CHANGED "+name+" expected="+expected+" actual="+actual);
        }}
        return List.copyOf(errors);
    }
    static String digest(Path path)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        if(Files.isDirectory(path)){
            Path base=path.toRealPath();try(var files=Files.walk(path)){for(Path file:files.filter(Files::isRegularFile).sorted(Comparator.comparing(f->path.relativize(f).toString().replace('\\','/'))).toList()){
                if(!file.toRealPath().startsWith(base))throw new IOException("INVALID_DEPENDENCY_PATH symlink escapes root");
                byte[] name=path.relativize(file).toString().replace('\\','/').getBytes(java.nio.charset.StandardCharsets.UTF_8);digest.update(java.nio.ByteBuffer.allocate(4).putInt(name.length).array());digest.update(name);digest.update(java.nio.ByteBuffer.allocate(8).putLong(Files.size(file)).array());feed(digest,file);
            }}
        }else feed(digest,path);
        return HexFormat.of().formatHex(digest.digest());
    }
    private static void feed(MessageDigest digest,Path file)throws IOException{try(InputStream in=Files.newInputStream(file)){byte[] buffer=new byte[65536];int size;while((size=in.read(buffer))!=-1)digest.update(buffer,0,size);}}
}
