package com.atom.lib.frontend.checker;

import java.nio.file.*;
import java.util.*;

/** Standalone entry point also used by the Gradle adapter; all errors fail closed. */
public final class CheckerMain {
    private CheckerMain() {}
    public static void main(String[] args) throws Exception {
        args = CheckerArguments.expand(args);
        if(args.length==3 && args[0].equals("compile-design")) {
            Files.write(Path.of(args[2]),DesignPackage.compile(Path.of(args[1])));return;
        }
        if(args.length==3 && args[0].equals("compile-contract")) {Files.write(Path.of(args[2]),ContractAuthorCompiler.compile(Path.of(args[1])));return;}
        if(args.length==3 && args[0].equals("compile-project")){Files.write(Path.of(args[2]),ProjectCheck.proposed(Path.of(args[1])));return;}
        Map<String,String> options=new HashMap<>();List<Path> sources=new ArrayList<>(),resourceRoots=new ArrayList<>(),dependencies=new ArrayList<>();
        for(int i=0;i<args.length;i+=2) {
            if(i+1==args.length) throw new IllegalArgumentException("missing option value");
            if(args[i].equals("--source")) sources.add(Path.of(args[i+1]));
            else if(args[i].equals("--resource-root"))resourceRoots.add(Path.of(args[i+1]));
            else if(args[i].equals("--dependency"))dependencies.add(Path.of(args[i+1]));
            else if(options.put(args[i],args[i+1])!=null) throw new IllegalArgumentException("duplicate option "+args[i]);
        }
        Path report=Path.of(options.getOrDefault("--report","frontend-report.txt"));
        ProjectCheck.Result result;
        String reviewedRevision="";
        boolean candidate="candidate".equals(options.get("--mode"));
        try {
            Path runtimeOutput=options.containsKey("--runtime-output")?Path.of(options.get("--runtime-output")):null;
            if (candidate && runtimeOutput != null) {
                throw new IllegalArgumentException("CANDIDATE_RUNTIME_OUTPUT candidate mode emits reports only");
            }
            if(runtimeOutput!=null)ProjectCheck.clearRuntime(runtimeOutput);
            String cp=options.getOrDefault("--classpath","");
            List<Path> classpath=cp.isBlank()?List.of():Arrays.stream(cp.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))).map(Path::of).toList();
            Path reviewed=options.containsKey("--reviewed")?Path.of(options.get("--reviewed")):null;
            if(options.containsKey("--reviewed-ref")) {
                if(reviewed!=null) throw new IllegalArgumentException("ambiguous reviewed sources");
                var snapshot=GitReviewedSource.materialize(Path.of(options.get("--reviewed-repo")),options.get("--reviewed-ref"),options.get("--reviewed-config"),report.toAbsolutePath().getParent().resolve("git-inputs"));
                reviewed=snapshot.config();reviewedRevision=snapshot.revision();
            }
            Path configPath=Path.of(options.get("--config"));
            if(resourceRoots.isEmpty())resourceRoots.add(configPath.toAbsolutePath().getParent().resolve("src/main/resources"));
            int release = Integer.parseInt(options.getOrDefault("--release", "17"));
            result = candidate && reviewed == null
                    ? ProjectCheck.firstEnrollment(configPath, sources, classpath, release, resourceRoots, dependencies)
                    : ProjectCheck.run(configPath, reviewed, sources, classpath, release, resourceRoots, dependencies);
            if(result.ordinarySuccess()&&runtimeOutput!=null){Properties config=new Properties();try(var in=Files.newBufferedReader(Path.of(options.get("--config")))){config.load(in);}if("2".equals(config.getProperty("protocol")))ProjectCheck.emitRuntime(Path.of(options.get("--config")),reviewed,runtimeOutput);}
        } catch(Exception e) {result=new ProjectCheck.Result("INVALID",List.of("CHECK_FAILED "+e.getClass().getSimpleName()+": "+e.getMessage()));}
        String output="scope=SOURCE_AND_CONTRACT_CHECK\nstatus="+result.status()+"\nreviewedRevision="+reviewedRevision+"\n"+String.join("\n",result.diagnostics())+"\n"+String.join("\n",result.inventory())+"\n";
        if(report.toAbsolutePath().getParent()!=null) Files.createDirectories(report.toAbsolutePath().getParent());
        Files.writeString(report,output);System.out.print(output);
        if(!result.ordinarySuccess() && !(candidate && result.status().equals("CANDIDATE_VALID"))) System.exit(2);
    }
}
