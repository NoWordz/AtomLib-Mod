package com.atom.lib.frontend.checker;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import javax.tools.*;
import javax.lang.model.element.Modifier;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;

/** Restricted authoring/serialization feasibility probe, not the full family model. */
public final class DesignPackage {
    private DesignPackage() {}
    public static byte[] compile(Path author) throws IOException {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null) throw new IllegalArgumentException("UNSUPPORTED_TOOLCHAIN JDK required");
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(var files=compiler.getStandardFileManager(diagnostics,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task=(JavacTask)compiler.getTask(null,files,diagnostics,List.of("-proc:none","--release","17"),null,files.getJavaFileObjects(author.toFile()));
            List<CompilationUnitTree> units=new ArrayList<>(); task.parse().forEach(units::add);
            if(diagnostics.getDiagnostics().stream().anyMatch(d->d.getKind()==Diagnostic.Kind.ERROR)) throw new IllegalArgumentException("INVALID_AUTHOR Java parse failed");
            var unit=units.get(0);
            if(!unit.getImports().isEmpty() || unit.getTypeDecls().size()!=1 || !(unit.getTypeDecls().get(0) instanceof ClassTree type)) throw new IllegalArgumentException("INVALID_AUTHOR one literal definition class required");
            if(type.getExtendsClause()!=null || !type.getImplementsClause().isEmpty() || !type.getTypeParameters().isEmpty() || !type.getModifiers().getAnnotations().isEmpty()) throw new IllegalArgumentException("INVALID_AUTHOR inheritance/annotations unsupported");
            Map<String,String> data=new TreeMap<>();
            for(Tree member:type.getMembers()) {
                if(!(member instanceof VariableTree field) || !field.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC,Modifier.FINAL)) || !field.getModifiers().getAnnotations().isEmpty()) throw new IllegalArgumentException("INVALID_AUTHOR only static final literal fields allowed");
                String kind=field.getType().toString();
                if(!Set.of("String","boolean","int","long","float","double").contains(kind)) throw new IllegalArgumentException("INVALID_AUTHOR unsupported field type "+kind);
                ExpressionTree expression=field.getInitializer();
                boolean negative=false;
                if(expression instanceof UnaryTree unary && unary.getKind()==Tree.Kind.UNARY_MINUS) {negative=true;expression=unary.getExpression();}
                if(!(expression instanceof LiteralTree literal) || literal.getValue()==null) throw new IllegalArgumentException("INVALID_AUTHOR no environment/functions/expressions permitted");
                Object value=literal.getValue();
                boolean valid=switch(kind) {
                    case "String" -> value instanceof String && !negative;
                    case "boolean" -> value instanceof Boolean && !negative;
                    case "int" -> value instanceof Integer;
                    case "long" -> value instanceof Long || value instanceof Integer;
                    default -> value instanceof Number && Double.isFinite(((Number)value).doubleValue());
                };
                if(!valid) throw new IllegalArgumentException("INVALID_AUTHOR incompatible literal for "+field.getName());
                String key=field.getName().toString();
                if(data.putIfAbsent(key,(negative?"-":"")+value)!=null) throw new IllegalArgumentException("INVALID_AUTHOR duplicate "+key);
                data.put(key+".type",kind);
            }
            for(String required:List.of("namespace","version","profile")) {
                if(!"String".equals(data.get(required+".type")) || data.get(required).isBlank()) throw new IllegalArgumentException("INVALID_AUTHOR missing "+required);
            }
            return encode(data);
        }
    }
    private static byte[] encode(Map<String,String> data) throws IOException {
        var bytes=new ByteArrayOutputStream();
        try(var out=new DataOutputStream(bytes)) {
            out.writeInt(0x46434C01);out.writeInt(data.size());
            for(var e:new TreeMap<>(data).entrySet()) {out.writeUTF(e.getKey());out.writeUTF(e.getValue());}
        }
        return bytes.toByteArray();
    }
    public static Map<String,String> read(byte[] bytes) {
        if(bytes.length>1_048_576) throw new IllegalArgumentException("INVALID_DESIGN package too large");
        try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if(in.readInt()!=0x46434C01) throw new IOException("format");
            int size=in.readInt(); if(size<6 || size>4096) throw new IOException("field count");
            Map<String,String> data=new TreeMap<>();
            for(int i=0;i<size;i++) if(data.put(in.readUTF(),in.readUTF())!=null) throw new IOException("duplicate field");
            if(in.read()!=-1 || !Arrays.equals(bytes,encode(data))) throw new IOException("noncanonical encoding");
            for(String key:List.of("namespace","version","profile")) if(!"String".equals(data.get(key+".type")) || data.get(key)==null || data.get(key).isBlank()) throw new IOException("identity missing");
            return Collections.unmodifiableMap(data);
        } catch(IOException e) {throw new IllegalArgumentException("INVALID_DESIGN "+e.getMessage(),e);}
    }
}
