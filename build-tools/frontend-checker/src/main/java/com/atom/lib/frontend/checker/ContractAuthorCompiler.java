package com.atom.lib.frontend.checker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import javax.tools.*;
import javax.lang.model.element.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.atom.lib.frontend.contract.*;
import com.atom.lib.frontend.contract.ContractDesign.*;

/** Parses type-checked literal record construction; never loads or executes an author's class. */
public final class ContractAuthorCompiler {
    private ContractAuthorCompiler(){}
    public static byte[] compile(Path author)throws IOException {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw invalid("JDK required");
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try(var manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8)){
            manager.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH,List.of());
            String model;
            try{model=Path.of(ContractDesign.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();}catch(Exception e){throw invalid("contract model unavailable");}
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none","--release","17","-classpath",model),null,manager.getJavaFileObjects(author.toFile()));
            List<CompilationUnitTree> units=new ArrayList<>();task.parse().forEach(units::add);
            if(units.size()!=1||units.get(0).getTypeDecls().size()!=1||!(units.get(0).getTypeDecls().get(0) instanceof ClassTree type))throw invalid("one literal design class required");
            if(type.getExtendsClause()!=null||!type.getImplementsClause().isEmpty()||!type.getModifiers().getAnnotations().isEmpty())throw invalid("inheritance and annotations unsupported");
            List<? extends Tree> members=new ArrayList<>(type.getMembers());
            if(members.size()!=1||!(members.get(0) instanceof VariableTree field)||!field.getName().contentEquals("DESIGN")||!field.getModifiers().getFlags().containsAll(Set.of(Modifier.STATIC,Modifier.FINAL))||!field.getModifiers().getAnnotations().isEmpty())throw invalid("only static final DESIGN field; no initializer code or helpers");
            task.analyze();if(diagnostics.getDiagnostics().stream().anyMatch(d->d.getKind()==Diagnostic.Kind.ERROR))throw invalid("type checking failed: "+diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).map(d->d.getMessage(Locale.ROOT)).findFirst().orElse("unknown"));
            Object result=new Evaluator(Trees.instance(task),units.get(0)).eval(field.getInitializer(),0);
            if(!(result instanceof ContractDesign design))throw invalid("DESIGN must be ContractDesign");return ContractCodec.encode(design);
        }
    }
    private static IllegalArgumentException invalid(String reason){return new IllegalArgumentException("INVALID_AUTHOR "+reason);}
    private static final class Evaluator {
        private final Trees trees;private final CompilationUnitTree unit;private final Map<String,Class<?>> types=new HashMap<>();int nodes;
        Evaluator(Trees trees,CompilationUnitTree unit){this.trees=trees;this.unit=unit;types.put(ContractDesign.class.getName(),ContractDesign.class);for(var root:List.of(ContractDesign.class,NamedCondition.class))for(var c:root.getDeclaredClasses())if(c.isRecord()||c.isEnum())types.put(c.getName().replace('$','.'),c);}
        private Element symbol(Tree tree){return trees.getElement(TreePath.getPath(unit,tree));}
        private String owner(Element element){return ((TypeElement)element.getEnclosingElement()).getQualifiedName().toString();}
        Object eval(ExpressionTree expression,int depth){
            if(expression==null||depth>64||++nodes>50000)throw invalid("missing or excessive expression");
            if(expression instanceof LiteralTree literal)return literal.getValue();
            if(expression instanceof UnaryTree unary&&unary.getKind()==Tree.Kind.UNARY_MINUS){Object n=eval(unary.getExpression(),depth+1);if(n instanceof Integer i)return -i;if(n instanceof Long l)return -l;if(n instanceof Number number)return -number.doubleValue();throw invalid("numeric literal required");}
            if(expression instanceof MemberSelectTree||expression instanceof IdentifierTree){Element e=symbol(expression);if(e!=null&&e.getKind()==ElementKind.ENUM_CONSTANT){Class<?> type=types.get(owner(e));if(type==null||!type.isEnum())throw invalid("enum outside contract model");return enumValue(type,e.getSimpleName().toString());}throw invalid("field access is not author data");}
            if(expression instanceof NewClassTree create){if(create.getClassBody()!=null||!create.getTypeArguments().isEmpty()||create.getEnclosingExpression()!=null)throw invalid("anonymous or enclosing construction unsupported");Element e=symbol(create);Class<?> type=types.get(owner(e));if(type==null||!type.isRecord())throw invalid("construction outside contract records");Object[] args=create.getArguments().stream().map(a->eval(a,depth+1)).toArray();var components=type.getRecordComponents();if(type==ContractDesign.class&&args.length==components.length-1){args=Arrays.copyOf(args,components.length);args[args.length-1]=Map.of();}if(args.length!=components.length)throw invalid("canonical record constructor required");Class<?>[] parameters=new Class<?>[args.length];for(int i=0;i<args.length;i++){parameters[i]=components[i].getType();if(parameters[i]==double.class&&args[i] instanceof Number n)args[i]=n.doubleValue();}
                try{return type.getDeclaredConstructor(parameters).newInstance(args);}catch(ReflectiveOperationException|IllegalArgumentException ex){throw invalid("invalid record "+type.getSimpleName()+": "+ex.getMessage());}}
            if(expression instanceof MethodInvocationTree call){Element e=symbol(call);if(!(e instanceof ExecutableElement))throw invalid("unresolved method");String owner=owner(e),name=e.getSimpleName().toString();Object[] args=call.getArguments().stream().map(a->eval(a,depth+1)).toArray();
                if(owner.equals("java.util.List")&&name.equals("of"))return List.of(args);
                if(owner.equals("java.util.Set")&&name.equals("of"))return Set.of(args);
                if(owner.equals("java.util.Map")&&(name.equals("of")||name.equals("ofEntries"))){Map<Object,Object> map=new HashMap<>();if(name.equals("of")){for(int i=0;i<args.length;i+=2)put(map,args[i],args[i+1]);}else for(Object arg:args){if(!(arg instanceof Map.Entry<?,?> entry))throw invalid("Map.entry required");put(map,entry.getKey(),entry.getValue());}return Map.copyOf(map);}
                if(owner.equals("java.util.Map")&&name.equals("entry")&&args.length==2)return Map.entry(args[0],args[1]);
                if(owner.equals(Requirement.class.getCanonicalName()))return switch(name){case "required"->Requirement.required();case "conditional"->Requirement.conditional((NamedCondition)args[0]);case "notApplicable"->Requirement.notApplicable((String)args[0]);case "unsupported"->Requirement.unsupported((String)args[0]);default->throw invalid("unknown requirement operation");};
                throw invalid("method not in literal construction vocabulary: "+owner+"#"+name);
            }
            throw invalid("unsupported expression "+expression.getKind());
        }
        private void put(Map<Object,Object> map,Object key,Object value){if(map.putIfAbsent(key,value)!=null)throw invalid("duplicate map key "+key);}
        @SuppressWarnings({"unchecked","rawtypes"})private Object enumValue(Class<?> type,String name){return Enum.valueOf((Class)type,name);}
    }
}
