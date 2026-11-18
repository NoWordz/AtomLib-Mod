package com.atom.lib.frontend.checker;

/** Validates invocation provenance before any recorded sample is compared. */
public final class SceneProcessCheck {
    private SceneProcessCheck(){}
    public static void requireCurrent(SceneCaptureIO.Capture capture,String invocation,String designDigest){
        if(invocation==null||invocation.isBlank()||!invocation.equals(capture.invocation()))throw new IllegalArgumentException("STALE_SCENE_CAPTURE expected current invocation");
        if(designDigest==null||!designDigest.equals(capture.designDigest()))throw new IllegalArgumentException("SCENE_DESIGN_MISMATCH current package differs");
    }
    /** Appends output, invocation and design digest arguments. Caller supplies the source-bound runner command. */
    public static java.util.List<String> run(java.util.List<String> command,java.nio.file.Path output,byte[] design,java.util.List<ScenarioCheck.Baseline> reviewed,java.time.Duration timeout)throws Exception{
        if(command.isEmpty()||timeout.isNegative()||timeout.isZero()||timeout.compareTo(java.time.Duration.ofMinutes(5))>0)throw new IllegalArgumentException("INVALID_SCENE_RUNNER");
        var bytes=com.atom.lib.frontend.contract.ContractCodec.decode(design);String invocation=java.util.UUID.randomUUID().toString(),digest=ProjectCheck.digest(design);
        java.nio.file.Path target=output.toAbsolutePath().normalize();java.nio.file.Files.createDirectories(target.getParent());java.nio.file.Files.deleteIfExists(target);
        java.nio.file.Path log=target.resolveSibling(target.getFileName()+".process.log");
        java.util.List<String> args=new java.util.ArrayList<>(command);args.addAll(java.util.List.of(target.toString(),invocation,digest));
        Process process=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(timeout.toMillis(),java.util.concurrent.TimeUnit.MILLISECONDS)){process.destroyForcibly();process.waitFor();return java.util.List.of("SCENE_EXECUTION_TIMEOUT see "+log);}
        if(process.exitValue()!=0)return java.util.List.of("SCENE_EXECUTION_FAILED exit="+process.exitValue()+" see "+log);
        if(!java.nio.file.Files.isRegularFile(target))return java.util.List.of("MISSING_EVIDENCE current scene process produced no capture");
        var capture=SceneCaptureIO.read(target);try{requireCurrent(capture,invocation,digest);}catch(IllegalArgumentException e){return java.util.List.of(e.getMessage());}
        return ScenarioCheck.compare(bytes,reviewed,capture.samples());
    }
}
