package com.atom.lib.frontend.checker;

import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.jvm.toolchain.JavaToolchainService;
import java.io.File;
import java.util.*;

/** Maven buildscript adapter; intentionally absent from Minecraft runtime artifacts. */
public final class FrontendContractPlugin implements Plugin<Project> {
    @Override public void apply(Project project) {
        project.getPluginManager().apply("java");
        FrontendContractExtension config=project.getExtensions().create("frontendContract",FrontendContractExtension.class);
        var sourceSets=project.getExtensions().getByType(SourceSetContainer.class);var main=sourceSets.getByName("main");
        SourceSet draft=sourceSets.findByName("frontendDraft");if(draft==null)draft=sourceSets.create("frontendDraft");
        draft.setCompileClasspath(draft.getCompileClasspath().plus(main.getOutput()).plus(main.getCompileClasspath()));
        draft.setRuntimeClasspath(draft.getOutput().plus(draft.getCompileClasspath()).plus(main.getRuntimeClasspath()));
        var sourceCheck=register(project,config,"frontendContractSourceCheck","ordinary");
        var normal=registerScenes(project,config,sourceCheck);
        register(project,config,"frontendContractCandidate","candidate");
        var resourceRoot=project.getLayout().getBuildDirectory().dir("generated/frontendContractResources");
        project.getExtensions().getByType(SourceSetContainer.class).getByName("main").getResources().srcDir(project.files(resourceRoot).builtBy(sourceCheck));
        project.getTasks().named("processResources").configure(t->{t.dependsOn(sourceCheck);((org.gradle.language.jvm.tasks.ProcessResources)t).exclude("**/.generated-resources");});
        project.getTasks().named("check").configure(t->t.dependsOn(normal));
        project.getTasks().matching(t->t.getName().equals("jar") || t.getName().equals("distributionJar") || t.getName().startsWith("publish") || t.getName().equals("remapJar") || t.getName().equals("jarJar")).configureEach(t->{t.dependsOn(normal);if(t instanceof org.gradle.api.tasks.bundling.AbstractArchiveTask archive){t.notCompatibleWithConfigurationCache("Frontend draft archive inspection uses the resolved source-set output catalog");t.doLast(ignored->{
            Set<String> forbidden=new HashSet<>();for(File folder:sourceSets.getByName("frontendDraft").getOutput())if(folder.isDirectory())for(File file:project.fileTree(folder).getFiles())forbidden.add(folder.toPath().relativize(file.toPath()).toString().replace('\\','/'));
            try{var errors=DraftCheck.archive(archive.getArchiveFile().get().getAsFile().toPath(),forbidden);if(!errors.isEmpty())throw new GradleException(String.join("\n",errors));}catch(java.io.IOException e){throw new GradleException("MISSING_EVIDENCE production archive unreadable",e);}
        });}});
    }
    private org.gradle.api.tasks.TaskProvider<JavaExec> register(Project project,FrontendContractExtension config,String name,String mode) {
        return project.getTasks().register(name,JavaExec.class,t->{
            t.notCompatibleWithConfigurationCache("Frontend source attribution uses the complete resolved production source catalog");
            t.setGroup("verification");
            t.getMainClass().set(CheckerMain.class.getName());
            try {t.classpath(new File(CheckerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()),new File(com.atom.lib.frontend.contract.ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI()));}
            catch(Exception e){throw new GradleException("checker location unavailable",e);}
            JavaPluginExtension java=project.getExtensions().getByType(JavaPluginExtension.class);
            t.getJavaLauncher().set(project.getExtensions().getByType(JavaToolchainService.class).launcherFor(java.getToolchain()));
            t.dependsOn(project.getExtensions().getByType(SourceSetContainer.class).getByName("main").getAllJava().getBuildDependencies());
            var production=project.getExtensions().getByType(SourceSetContainer.class).getByName("main");
            t.dependsOn(production.getCompileClasspath().getBuildDependencies());
            var runtimeDependencies=project.getConfigurations().getByName(production.getRuntimeClasspathConfigurationName());
            t.dependsOn(runtimeDependencies.getBuildDependencies());
            t.doFirst(ignored->{
                if(config.getContract()==null) throw new GradleException("MISSING_EVIDENCE contract config required");
                var sets=project.getExtensions().getByType(SourceSetContainer.class);
                if(sets.getNames().stream().anyMatch(s->!Set.of("main","test","frontendDraft").contains(s))) throw new GradleException("UNSUPPORTED explicit adapter required for additional production source sets");
                SourceSet main=sets.getByName("main");
                var draft=sets.getByName("frontendDraft");var overlaps=DraftCheck.sources(main.getAllSource().getFiles().stream().map(File::toPath).toList(),draft.getAllSource().getFiles().stream().map(File::toPath).toList());if(!overlaps.isEmpty())throw new GradleException(String.join("\n",overlaps));
                if(main.getAllSource().getFiles().stream().anyMatch(f->f.getName().matches(".*\\.(kt|scala|groovy)$"))) throw new GradleException("UNSUPPORTED non-Java production source");
                List<String> args=new ArrayList<>(List.of("--config",config.getContract().getAbsolutePath(),"--mode",mode,"--release",""+t.getJavaLauncher().get().getMetadata().getLanguageVersion().asInt(),"--classpath",main.getCompileClasspath().getAsPath(),"--report",project.getLayout().getBuildDirectory().file("frontend-contract-lock/"+mode+".txt").get().getAsFile().getAbsolutePath()));
                if(mode.equals("ordinary"))args.addAll(List.of("--runtime-output",project.getLayout().getBuildDirectory().dir("generated/frontendContractResources/META-INF/atomlib/frontend").get().getAsFile().getAbsolutePath()));
                if(config.getReviewed()!=null) args.addAll(List.of("--reviewed",config.getReviewed().getAbsolutePath()));
                String reviewedRevision=System.getenv("FCL_REVIEWED_REVISION");
                if(reviewedRevision==null || reviewedRevision.isBlank()) reviewedRevision=config.getReviewedRevision();
                if(reviewedRevision!=null) {
                    if(config.getReviewed()!=null || config.getReviewedRepository()==null || config.getReviewedConfig()==null) throw new GradleException("MISSING_EVIDENCE Git reviewed source requires unambiguous repository/config inputs");
                    args.addAll(List.of("--reviewed-ref",reviewedRevision,"--reviewed-repo",config.getReviewedRepository().getAbsolutePath(),"--reviewed-config",config.getReviewedConfig()));
                }
                main.getAllJava().getFiles().stream().sorted().forEach(f->args.addAll(List.of("--source",f.getAbsolutePath())));
                runtimeDependencies.getFiles().stream().sorted().forEach(f->args.addAll(List.of("--dependency",f.getAbsolutePath())));
                main.getResources().getSrcDirs().stream().filter(f->!f.toPath().toAbsolutePath().normalize().startsWith(project.getLayout().getBuildDirectory().get().getAsFile().toPath().toAbsolutePath().normalize().resolve("generated/frontendContractResources"))).sorted().forEach(f->args.addAll(List.of("--resource-root",f.getAbsolutePath())));
                setArguments(project, t, "source-" + mode, args);
            });
        });
    }
    private org.gradle.api.tasks.TaskProvider<JavaExec> registerScenes(Project project,FrontendContractExtension config,org.gradle.api.tasks.TaskProvider<JavaExec> sourceCheck){
        var sets = project.getExtensions().getByType(SourceSetContainer.class);
        var test = sets.getByName("test");
        var agentJar = project.getTasks().register("frontendContractSceneAgent", Jar.class, t -> {
            t.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("frontend-contract-lock"));
            t.getArchiveFileName().set("scene-agent.jar");
            t.dependsOn(project.getTasks().named(test.getClassesTaskName()));
            String agent = sceneSettings(config).getProperty("scene.agent", "");
            String entry = agent.replace('.', '/');
            t.from(test.getOutput().getClassesDirs(), copy -> copy.include(entry + ".class", entry + "$*.class"));
            t.getManifest().attributes(Map.of("Premain-Class", agent));
        });
        return project.getTasks().register("frontendContractCheck",JavaExec.class,t->{
            t.setGroup("verification");t.notCompatibleWithConfigurationCache("Scene gate resolves the complete runtime and current reviewed source");t.dependsOn(sourceCheck,project.getTasks().named("classes"));
            var scene = sceneSettings(config);
            boolean useTest = "test".equals(scene.getProperty("scene.sourceSet"));
            boolean useAgent = useTest && scene.containsKey("scene.agent");
            if (useTest) {
                t.dependsOn(project.getTasks().named(test.getClassesTaskName()));
                t.dependsOn(test.getRuntimeClasspath().getBuildDependencies());
            }
            if (useAgent) t.dependsOn(agentJar);
            t.getMainClass().set(SceneCheckerMain.class.getName());
            try{t.classpath(new File(CheckerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()),new File(com.atom.lib.frontend.contract.ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI()));}catch(Exception e){throw new GradleException("checker location unavailable",e);}
            var java=project.getExtensions().getByType(JavaPluginExtension.class);t.getJavaLauncher().set(project.getExtensions().getByType(JavaToolchainService.class).launcherFor(java.getToolchain()));
            t.doFirst(ignored->{
                if(config.getContract()==null)throw new GradleException("MISSING_EVIDENCE contract config required");
                var main = sets.getByName("main");
                var runtime = useTest ? main.getOutput().plus(test.getRuntimeClasspath()) : main.getRuntimeClasspath();
                List<String> args=new ArrayList<>(List.of("--config",config.getContract().getAbsolutePath(),"--java",t.getJavaLauncher().get().getExecutablePath().getAsFile().getAbsolutePath(),"--runtime-classpath",runtime.getAsPath(),"--output",project.getLayout().getBuildDirectory().file("frontend-contract-lock/current.fcs").get().getAsFile().getAbsolutePath(),"--report",project.getLayout().getBuildDirectory().file("frontend-contract-lock/scenes.txt").get().getAsFile().getAbsolutePath()));
                if (useTest) {
                    Set<File> roots = new LinkedHashSet<>(test.getJava().getSrcDirs());
                    roots.addAll(test.getResources().getSrcDirs());
                    args.addAll(List.of("--scene-test-roots", project.files(roots).getAsPath()));
                }
                if (useAgent) args.addAll(List.of("--scene-agent", agentJar.get().getArchiveFile().get().getAsFile().getAbsolutePath()));
                if(config.getReviewed()!=null)args.addAll(List.of("--reviewed",config.getReviewed().getAbsolutePath()));
                String revision=System.getenv("FCL_REVIEWED_REVISION");if(revision==null||revision.isBlank())revision=config.getReviewedRevision();
                if(revision!=null){if(config.getReviewed()!=null||config.getReviewedRepository()==null||config.getReviewedConfig()==null)throw new GradleException("MISSING_EVIDENCE unambiguous reviewed scene source required");args.addAll(List.of("--reviewed-ref",revision,"--reviewed-repo",config.getReviewedRepository().getAbsolutePath(),"--reviewed-config",config.getReviewedConfig()));}
                setArguments(project, t, "scenes", args);
            });
        });
    }
    private static Properties sceneSettings(FrontendContractExtension config) {
        Properties properties = new Properties();
        if (config.getContract() == null || !config.getContract().isFile()) return properties;
        try (var reader = java.nio.file.Files.newBufferedReader(config.getContract().toPath())) {
            properties.load(reader);
            return properties;
        } catch (java.io.IOException e) {
            throw new GradleException("Cannot read frontend scene configuration", e);
        }
    }
    private static void setArguments(Project project, JavaExec task, String name, List<String> arguments) {
        var file = project.getLayout().getBuildDirectory().file("frontend-contract-lock/" + name + ".args")
                .get().getAsFile().toPath();
        try {
            task.setArgs(CheckerArguments.write(file, arguments));
        } catch (java.io.IOException e) {
            throw new GradleException("Cannot write frontend checker arguments", e);
        }
    }
}
