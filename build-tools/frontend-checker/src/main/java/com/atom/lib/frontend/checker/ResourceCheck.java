package com.atom.lib.frontend.checker;

import com.atom.lib.frontend.contract.ContractDesign;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Resolves actual resource bytes and transitive Minecraft sound/font descriptors, then verifies reviewed bindings. */
public final class ResourceCheck {
    private final List<Path> roots,classpath;
    private final Properties bindings;
    private final Map<String,String> actual=new TreeMap<>();
    private final List<String> errors=new ArrayList<>();
    private final Set<String> events=new HashSet<>(),activeEvents=new HashSet<>(),fonts=new HashSet<>(),activeFonts=new HashSet<>();
    private ResourceCheck(List<Path> roots,List<Path> classpath,Properties bindings){this.roots=List.copyOf(roots);this.classpath=List.copyOf(classpath);this.bindings=bindings;}
    public record Result(List<String> diagnostics,Map<String,String> resources){public Result{diagnostics=List.copyOf(diagnostics);resources=Map.copyOf(resources);}}
    public static Result inspect(ContractDesign design,List<Path> roots,List<Path> classpath,Properties bindings){
        var check=new ResourceCheck(roots,classpath,bindings);
        for(var item:new TreeMap<>(design.assets()).entrySet())try{
            var asset=item.getValue();String id=asset.resource();
            if(asset.kind()==ContractDesign.AssetKind.SOUND)check.event(id);
            else if(asset.kind()==ContractDesign.AssetKind.FONT&&id.endsWith(".json")&&!id.startsWith("classpath:"))check.font(id);
            else {String path=id.startsWith("classpath:")?id.substring("classpath:".length()):path(id);byte[] data=check.read(path,true);
                if(data!=null&&asset.kind()==ContractDesign.AssetKind.ICON&&id.endsWith(".svg")){
                    String svg=new String(data,StandardCharsets.UTF_8);
                    // SVG external references require a host-specific closure adapter; do not silently ignore them.
                    if(svg.matches("(?s).*(?:href\\s*=\\s*[\"'](?!#)|url\\(\\s*(?!#)|<!ENTITY|<!DOCTYPE).*"))check.errors.add("UNSUPPORTED_RESOURCE_DEPENDENCY "+path+" external SVG reference");
                }
                if(data!=null&&id.endsWith(".png")){String metadata=path+".mcmeta";byte[] meta=check.read(metadata,false);if(meta!=null)ResourceJson.parse(new String(meta,StandardCharsets.UTF_8));}
            }
        }catch(Exception e){check.errors.add("INVALID_RESOURCE "+item.getKey()+" "+e.getMessage());}
        // Explicit supplemental bindings also participate (e.g. generated/vector implementation assets).
        for(String key:new TreeSet<>(bindings.stringPropertyNames()))if(key.startsWith("resource.")&&!check.actual.containsKey(key.substring(9)))try{check.read(key.substring(9),true);}catch(Exception e){check.errors.add("INVALID_RESOURCE "+key+" "+e.getMessage());}
        return new Result(check.errors,check.actual);
    }
    private void event(String id)throws Exception {
        if(activeEvents.size()>128)throw new IllegalArgumentException("RESOURCE_REFERENCE_DEPTH");
        id=identifier(id,"minecraft");if(activeEvents.contains(id))throw new IllegalArgumentException("RESOURCE_REFERENCE_CYCLE "+id);if(!events.add(id))return;activeEvents.add(id);
        String ns=id.substring(0,id.indexOf(':')),name=id.substring(id.indexOf(':')+1);byte[] bytes=read("assets/"+ns+"/sounds.json",true);if(bytes==null){activeEvents.remove(id);return;}
        Map<?,?> all=object(ResourceJson.parse(new String(bytes,StandardCharsets.UTF_8)));Object entry=all.get(name);
        if(entry==null)throw new IllegalArgumentException("SOUND_EVENT_MISSING "+id);
        Map<?,?> descriptor=object(entry);if(!(descriptor.get("sounds") instanceof List<?> sounds)||sounds.isEmpty())throw new IllegalArgumentException("SOUND_EVENT_EMPTY "+id);
        for(Object sound:sounds){String target,type="file";
            if(sound instanceof String text)target=text;
            else {Map<?,?> option=object(sound);if(!(option.get("name") instanceof String text))throw new IllegalArgumentException("INVALID_SOUND_NAME "+id);target=text;if(option.containsKey("type"))type=String.valueOf(option.get("type"));}
            target=identifier(target,"minecraft");
            if(type.equals("event"))event(target);
            else if(type.equals("file")){int split=target.indexOf(':');read("assets/"+target.substring(0,split)+"/sounds/"+target.substring(split+1)+".ogg",true);}
            else throw new IllegalArgumentException("UNSUPPORTED_SOUND_TYPE "+type);
        }
        activeEvents.remove(id);
    }
    private void font(String id)throws Exception {
        if(activeFonts.size()>128)throw new IllegalArgumentException("RESOURCE_REFERENCE_DEPTH");
        id=identifier(id,"minecraft");if(activeFonts.contains(id))throw new IllegalArgumentException("RESOURCE_REFERENCE_CYCLE "+id);if(!fonts.add(id))return;activeFonts.add(id);
        byte[] bytes=read(path(id),true);if(bytes==null){activeFonts.remove(id);return;}
        Object input=object(ResourceJson.parse(new String(bytes,StandardCharsets.UTF_8))).get("providers");if(!(input instanceof List<?> providers)||providers.isEmpty())throw new IllegalArgumentException("FONT_PROVIDERS_MISSING "+id);
        for(Object raw:providers){Map<?,?> provider=object(raw);String type=String.valueOf(provider.get("type")).replace("minecraft:","");
            switch(type){case "space"->{}case "reference"->{String target=identifier(String.valueOf(provider.get("id")),"minecraft");int split=target.indexOf(':');font(target.substring(0,split)+":font/"+target.substring(split+1)+".json");}
                case "bitmap","ttf"->{String target=identifier(String.valueOf(provider.get("file")),"minecraft");int split=target.indexOf(':');String p=type.equals("bitmap")?"assets/"+target.substring(0,split)+"/textures/"+target.substring(split+1):path(target);read(p,true);}
                default->throw new IllegalArgumentException("UNSUPPORTED_FONT_PROVIDER "+type);}
        }activeFonts.remove(id);
    }
    private byte[] read(String entry,boolean required)throws Exception {
        if(!entry.matches("[a-zA-Z0-9_-][a-zA-Z0-9_./-]*")||entry.contains("..")||entry.contains("//")||entry.endsWith("/"))throw new IllegalArgumentException("INVALID_RESOURCE_PATH "+entry);
        List<byte[]> found=new ArrayList<>();
        for(Path root:roots)readDirectory(root,entry,found);
        for(Path file:classpath){if(Files.isDirectory(file))readDirectory(file,entry,found);else if(Files.isRegularFile(file))try(ZipFile zip=new ZipFile(file.toFile())){var member=zip.getEntry(entry);if(member!=null){if(member.getSize()>32L*1024*1024)throw new IllegalArgumentException("RESOURCE_SIZE_LIMIT "+entry);try(var stream=zip.getInputStream(member)){byte[] bytes=stream.readNBytes(32*1024*1024+1);if(bytes.length>32*1024*1024)throw new IllegalArgumentException("RESOURCE_SIZE_LIMIT "+entry);found.add(bytes);}}}}
        if(found.isEmpty()){if(required)errors.add("RESOURCE_MISSING "+entry);return null;}
        byte[] data=found.get(0);for(byte[] duplicate:found)if(!Arrays.equals(data,duplicate)){errors.add("RESOURCE_AMBIGUOUS "+entry+" conflicting roots require an explicit host adapter");return data;}
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));actual.put(entry,hash);String expected=bindings.getProperty("resource."+entry);
        if(expected==null)errors.add("RESOURCE_UNBOUND "+entry+" sha256="+hash);
        else if(!expected.equals(hash))errors.add("RESOURCE_CHANGED "+entry+" expected="+expected+" actual="+hash);
        return data;
    }
    private static void readDirectory(Path root,String entry,List<byte[]> found)throws Exception {
        if(!Files.isDirectory(root))return;Path file=root.toAbsolutePath().normalize().resolve(entry).normalize();if(!Files.isRegularFile(file))return;
        if(!file.toRealPath().startsWith(root.toRealPath()))throw new IllegalArgumentException("INVALID_RESOURCE_PATH symlink escapes root: "+entry);
        if(Files.size(file)>32L*1024*1024)throw new IllegalArgumentException("RESOURCE_SIZE_LIMIT "+entry);found.add(Files.readAllBytes(file));
    }
    private static Map<?,?> object(Object value){if(!(value instanceof Map<?,?> map))throw new IllegalArgumentException("INVALID_RESOURCE_DESCRIPTOR object required");return map;}
    private static String identifier(String id,String fallback){if(!id.contains(":"))id=fallback+":"+id;if(!id.matches("[a-z0-9_.-]+:[a-zA-Z0-9_./-]+")||id.contains("..")||id.contains("//")||id.contains(":/"))throw new IllegalArgumentException("INVALID_RESOURCE_ID "+id);return id;}
    private static String path(String id){id=identifier(id,"minecraft");return "assets/"+id.replace(':','/');}
}
