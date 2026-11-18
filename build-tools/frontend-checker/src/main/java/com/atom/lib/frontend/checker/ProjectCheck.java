package com.atom.lib.frontend.checker;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

/** Composes prototype checks without writing approval inputs. */
public final class ProjectCheck {
    private ProjectCheck() {}
    public static Result run(Path current, Path reviewed, List<Path> sources, List<Path> classpath, int release) throws Exception {
        return run(current,reviewed,sources,classpath,release,current==null?List.of():List.of(current.toAbsolutePath().getParent().resolve("src/main/resources")));
    }
    public static Result run(Path current, Path reviewed, List<Path> sources,List<Path> classpath,int release,List<Path> resourceRoots)throws Exception {
        return run(current,reviewed,sources,classpath,release,resourceRoots,classpath);
    }
    public static Result run(Path current,Path reviewed,List<Path> sources,List<Path> classpath,int release,List<Path> resourceRoots,List<Path> dependencyInputs)throws Exception {
        if(current==null || reviewed==null || !Files.isRegularFile(current) || !Files.isRegularFile(reviewed)
            || Files.isSameFile(current,reviewed)) return new Result("MISSING_EVIDENCE",List.of("MISSING_EVIDENCE separate reviewed configuration required; no worktree self-approval"));
        Properties c=load(current), r=load(reviewed);
        List<String> integrationChanges=new ArrayList<>();
        for(String relative:values(r,"protectedFiles")) {
            Path candidateFile=current.toAbsolutePath().getParent().resolve(relative).normalize();
            Path reviewedFile=reviewed.toAbsolutePath().getParent().resolve(relative).normalize();
            if(!candidateFile.startsWith(current.toAbsolutePath().getParent()) || !reviewedFile.startsWith(reviewed.toAbsolutePath().getParent())) return new Result("INVALID",List.of("INVALID_CONFIGURATION protected path escapes input root"));
            if(!Files.isRegularFile(candidateFile) || !Files.isRegularFile(reviewedFile) || !Arrays.equals(Files.readAllBytes(candidateFile),Files.readAllBytes(reviewedFile))) integrationChanges.add("INTEGRATION_CHANGED "+relative+" reviewed build wiring differs or is missing");
        }
        if(!Set.of("1","2").contains(c.getProperty("protocol","")) || !c.getProperty("protocol").equals(r.getProperty("protocol"))) return new Result("UNSUPPORTED",List.of("UNSUPPORTED checker protocol"));
        if(c.getProperty("design")==null || r.getProperty("design")==null) return new Result("MISSING_EVIDENCE",List.of("MISSING_EVIDENCE design package reference"));
        Path candidate=current.toAbsolutePath().getParent().resolve(c.getProperty("design"));
        Path approved=reviewed.toAbsolutePath().getParent().resolve(r.getProperty("design"));
        if(!Files.isRegularFile(approved) || ("1".equals(c.getProperty("protocol"))&&!Files.isRegularFile(candidate))) return new Result("MISSING_EVIDENCE",List.of("MISSING_EVIDENCE design bytes missing"));
        byte[] proposed=proposed(current,c);
        byte[] reviewedBytes=Files.readAllBytes(approved);
        List<String> changes="2".equals(c.getProperty("protocol"))?DesignDiff.compare(reviewedBytes,proposed,r,c):List.of();
        String status=ApprovalCheck.assess(reviewedBytes,proposed,values(r,"profiles"));
        if(!Set.of("APPROVED_VALID","CANDIDATE_VALID").contains(status)){var errors=new ArrayList<String>(integrationChanges);errors.add(status+" design identity/profile differs");errors.addAll(changes);return new Result(status,errors);}
        List<Path> resolved=new ArrayList<>(new LinkedHashSet<>(classpath));for(Path dependency:dependencyInputs)if(!resolved.contains(dependency))resolved.add(dependency);
        if("2".equals(c.getProperty("protocol"))){var resources=ResourceCheck.inspect(com.atom.lib.frontend.contract.ContractCodec.decode(proposed),resourceRoots,resolved,c);if(!resources.diagnostics().isEmpty())return new Result("INVALID",resources.diagnostics());}
        // Reviewed policy stays authoritative even when the candidate edits its policy.
        Map<String,String> legacy=new TreeMap<>();
        for(String k:r.stringPropertyNames()) if(k.startsWith("legacy.")) legacy.put(k.substring(7),new String(Base64.getDecoder().decode(r.getProperty(k)),java.nio.charset.StandardCharsets.UTF_8));
        Map<String,String> implementations=new TreeMap<>();for(String key:r.stringPropertyNames())if(key.startsWith("implementation."))implementations.put(key.substring(15),new String(Base64.getDecoder().decode(r.getProperty(key)),java.nio.charset.StandardCharsets.UTF_8));
        if("2".equals(r.getProperty("protocol"))&&r.getProperty("scene.main")!=null&&!implementations.containsKey(r.getProperty("scene.main")))return new Result("INVALID",List.of("MISSING_IMPLEMENTATION_BINDING scene runner "+r.getProperty("scene.main")));
        BoundaryPolicy policy=new BoundaryPolicy(values(r,"strict"),values(r,"trusted"),values(r,"drawingTypes"),values(r,"hostTypes"),values(r,"hostCallbacks"),legacy,implementations,"2".equals(r.getProperty("protocol")),values(r,"businessMethods"));
        BoundaryReport boundary=BoundaryCheck.inspect(sources,classpath,release,policy);
        List<String> inventory=MigrationInventory.describe(policy,boundary);
        if(!boundary.valid()) return new Result("INVALID",boundary.diagnostics(),inventory);
        if("2".equals(r.getProperty("protocol"))){var dependencyErrors=DependencyCheck.inspect(boundary,policy,resolved,c);if(!dependencyErrors.isEmpty())return new Result("INVALID",dependencyErrors);}
        if(!c.equals(r) || !integrationChanges.isEmpty()) status="CANDIDATE_VALID";
        if(status.equals("CANDIDATE_VALID")){integrationChanges.addAll(changes);integrationChanges.add("DESIGN_REVIEW_REQUIRED candidate/config changes cannot pass ordinary acceptance");}
        return new Result(status,integrationChanges,inventory);
    }
    public static byte[] proposed(Path config,Properties properties)throws Exception {
        String property="2".equals(properties.getProperty("protocol"))?"author":"design";
        String name=properties.getProperty(property);if(name==null)throw new IllegalArgumentException("MISSING_EVIDENCE "+property);
        Path root=config.toAbsolutePath().getParent(),file=root.resolve(name).normalize();if(!file.startsWith(root))throw new IllegalArgumentException("INVALID_CONFIGURATION author/design escapes input root");
        if(!property.equals("author"))return Files.readAllBytes(file);
        var d=com.atom.lib.frontend.contract.ContractCodec.decode(ContractAuthorCompiler.compile(file));
        if(!d.buildBindings().isEmpty())throw new IllegalArgumentException("INVALID_AUTHOR build bindings are compiler-owned inputs");
        Map<String,String> bindings=new TreeMap<>();
        bindings.put(com.atom.lib.frontend.contract.MechanismIdentity.BINDING,com.atom.lib.frontend.contract.MechanismIdentity.current());
        for(String key:properties.stringPropertyNames())if(!Set.of("author","design").contains(key))bindings.put("config/"+key,digest(properties.getProperty(key).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        for(String relative:values(properties,"protectedFiles")){Path target=root.resolve(relative).normalize();if(!target.startsWith(root)||!Files.isRegularFile(target))throw new IllegalArgumentException("MISSING_EVIDENCE protected build input "+relative);bindings.put("file/"+relative,digest(Files.readAllBytes(target)));}
        return com.atom.lib.frontend.contract.ContractCodec.encode(new com.atom.lib.frontend.contract.ContractDesign(d.format(),d.namespace(),d.version(),d.profile(),d.scopes(),d.states(),d.parameters(),d.families(),d.roles(),d.recipes(),d.layouts(),d.assets(),d.pages(),bindings));
    }
    static String digest(byte[] bytes)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static byte[] proposed(Path config)throws Exception{return proposed(config,load(config));}
    /** Called only after ordinary checks. Emits the same canonical package and runtime admission. */
    public static void emitRuntime(Path current,Path reviewed,Path output)throws Exception {
        Properties c=load(current),r=load(reviewed);if(!"2".equals(c.getProperty("protocol")))throw new IllegalArgumentException("UNSUPPORTED runtime admission requires protocol 2");
        byte[] candidate=proposed(current,c),approved=Files.readAllBytes(reviewed.toAbsolutePath().getParent().resolve(r.getProperty("design")));
        byte[] manifest=com.atom.lib.frontend.contract.ContractRegistry.manifest(candidate,approved);
        var design=com.atom.lib.frontend.contract.ContractCodec.decode(candidate);
        String prefix=design.namespace()+"/"+design.version()+"/";
        Path base=output.toAbsolutePath().normalize(),directory=base.resolve(prefix).normalize();if(!directory.startsWith(base))throw new IllegalArgumentException("INVALID_CONFIGURATION runtime resource path");
        Files.createDirectories(directory);Files.write(directory.resolve("design.fcl"),candidate);Files.write(directory.resolve("manifest.fcl"),manifest);
        Files.writeString(base.resolve(".generated-resources"),prefix+"design.fcl\n"+prefix+"manifest.fcl\n");
    }
    /** Removes only the files named by this tool's previous generated output index, never source trees. */
    public static void clearRuntime(Path output)throws Exception {
        Path base=output.toAbsolutePath().normalize(),index=base.resolve(".generated-resources");if(!Files.isRegularFile(index))return;
        for(String relative:Files.readAllLines(index)){
            if(!relative.matches("[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+/(design|manifest)\\.fcl"))throw new IllegalArgumentException("INVALID_CONFIGURATION generated runtime index");
            Path file=base.resolve(relative).normalize();if(!file.startsWith(base))throw new IllegalArgumentException("INVALID_CONFIGURATION generated index escapes runtime output");Files.deleteIfExists(file);
        }
        Files.deleteIfExists(index);
    }
    private static Set<String> values(Properties p,String key) {
        String raw=p.getProperty(key,"").trim();
        return raw.isEmpty()?Set.of():Set.copyOf(Arrays.asList(raw.split(";")));
    }
    private static Properties load(Path path) throws Exception {
        Properties p=new Properties();
        try(var in=Files.newBufferedReader(path,java.nio.charset.StandardCharsets.UTF_8)) {p.load(in);}
        return p;
    }
    public record Result(String status, List<String> diagnostics,List<String> inventory) {
        public Result(String status,List<String> diagnostics){this(status,diagnostics,List.of());}
        public Result { diagnostics=List.copyOf(diagnostics);inventory=List.copyOf(inventory); }
        public boolean ordinarySuccess() { return status.equals("APPROVED_VALID"); }
    }
}
