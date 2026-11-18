package com.atom.lib.frontend.checker;

import java.nio.file.*;
import java.util.*;

/** Independent process/capture gate, composed with source admission by the build adapter. */
public final class SceneCheckerMain {
    private SceneCheckerMain(){}
    public static ProjectCheck.Result check(Path config,Path reviewed,String javaExecutable,String runtimeClasspath,Path output)throws Exception{
        Properties current=load(config),old=load(reviewed);
        if("1".equals(current.getProperty("protocol")))return new ProjectCheck.Result("PROTOTYPE_SOURCE_ONLY",List.of("NOT_ACTIVATED protocol 1 has no production scenario contract"));
        if(!"2".equals(current.getProperty("protocol"))||!"2".equals(old.getProperty("protocol")))return new ProjectCheck.Result("UNSUPPORTED",List.of("UNSUPPORTED scene protocol"));
        String main=old.getProperty("scene.main"),reference=old.getProperty("scene.reference");
        if(main==null||reference==null)return new ProjectCheck.Result("MISSING_EVIDENCE",List.of("MISSING_SCENE_ENTRY source-bound production runner and independently reviewed captures required"));
        if(!main.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*")||!old.containsKey("implementation."+main))return new ProjectCheck.Result("MISSING_EVIDENCE",List.of("MISSING_IMPLEMENTATION_BINDING scene runner requires reviewed source closure: "+main));
        Set<String> protectedFiles=Set.copyOf(Arrays.asList(old.getProperty("protectedFiles","").split(";")));
        if(!protectedFiles.contains(reference))return new ProjectCheck.Result("MISSING_EVIDENCE",List.of("UNBOUND_SCENE_REFERENCE protectedFiles must include "+reference));
        byte[] proposed=ProjectCheck.proposed(config),approved=Files.readAllBytes(safe(reviewed,old.getProperty("design")));
        if(!Arrays.equals(proposed,approved)||!current.equals(old))return new ProjectCheck.Result("PENDING_DESIGN_REVIEW",List.of("PENDING_DESIGN_REVIEW source and scenario package differ"));
        var capture=SceneCaptureIO.read(safe(reviewed,reference));
        if(!capture.designDigest().equals(old.getProperty("scene.referenceDesignDigest")))return new ProjectCheck.Result("MISSING_EVIDENCE",List.of("SCENE_REFERENCE_IDENTITY reviewed original package digest required"));
        if(!old.containsKey("scene.channelTolerance")||!old.containsKey("scene.observationTolerance"))return new ProjectCheck.Result("MISSING_EVIDENCE",List.of("MISSING_SCENE_TOLERANCE explicit reviewed pixel and observation tolerances required"));
        int pixels=Integer.parseInt(old.getProperty("scene.channelTolerance"));double tolerance=Double.parseDouble(old.getProperty("scene.observationTolerance"));
        List<ScenarioCheck.Baseline> baselines=capture.samples().stream().map(s->{Map<String,Double> tolerances=new TreeMap<>();for(String key:s.observations().keySet())tolerances.put(key,Double.parseDouble(old.getProperty("scene.tolerance."+key,Double.toString(tolerance))));return new ScenarioCheck.Baseline(s,pixels,tolerances);}).toList();
        var errors=SceneProcessCheck.run(List.of(javaExecutable,"-cp",runtimeClasspath,main),output,proposed,baselines,java.time.Duration.ofSeconds(90));
        return new ProjectCheck.Result(errors.isEmpty()?"SCENARIOS_VALID":"INVALID",errors);
    }
    private static Path safe(Path config,String relative)throws Exception{if(relative==null)throw new IllegalArgumentException("MISSING_EVIDENCE path");Path root=config.toAbsolutePath().getParent(),file=root.resolve(relative).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file)||!file.toRealPath().startsWith(root.toRealPath()))throw new IllegalArgumentException("MISSING_EVIDENCE regular input within config root: "+relative);return file;}
    private static Properties load(Path file)throws Exception{Properties properties=new Properties();try(var reader=Files.newBufferedReader(file)){properties.load(reader);}return properties;}
    public static void main(String[] args)throws Exception{
        Map<String,String> options=new HashMap<>();for(int i=0;i<args.length;i+=2){if(i+1==args.length||options.put(args[i],args[i+1])!=null)throw new IllegalArgumentException("INVALID_SCENE_ARGUMENTS");}
        Path report=Path.of(options.get("--report"));ProjectCheck.Result result;
        try{
            Path reviewed=options.containsKey("--reviewed")?Path.of(options.get("--reviewed")):null;
            if(options.containsKey("--reviewed-ref")){if(reviewed!=null)throw new IllegalArgumentException("ambiguous reviewed sources");reviewed=GitReviewedSource.materialize(Path.of(options.get("--reviewed-repo")),options.get("--reviewed-ref"),options.get("--reviewed-config"),report.toAbsolutePath().getParent().resolve("git-scene-inputs")).config();}
            if(reviewed==null)throw new IllegalArgumentException("MISSING_EVIDENCE independent reviewed scenario source");
            result=check(Path.of(options.get("--config")),reviewed,options.get("--java"),options.get("--runtime-classpath"),Path.of(options.get("--output")));
        }catch(Exception e){result=new ProjectCheck.Result("INVALID",List.of("SCENE_CHECK_FAILED "+e.getMessage()));}
        String text="scope=PRODUCTION_SCENARIOS\nstatus="+result.status()+"\n"+String.join("\n",result.diagnostics())+"\n";Files.createDirectories(report.toAbsolutePath().getParent());Files.writeString(report,text);System.out.print(text);
        if(!Set.of("SCENARIOS_VALID","PROTOTYPE_SOURCE_ONLY").contains(result.status()))System.exit(2);
    }
}
