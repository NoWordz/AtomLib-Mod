package com.atom.lib.frontend.checker;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/** Stage-one symbol analysis probe; not yet a project activation guarantee. */
public final class BoundaryCheck {
    private BoundaryCheck() {}
    public static BoundaryReport inspect(List<Path> sources, List<Path> classpath,
                                         int release, BoundaryPolicy policy) throws IOException {
        if (sources.isEmpty()) return new BoundaryReport(List.of("MISSING_EVIDENCE no production Java sources"), Map.of());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null || release > Runtime.version().feature())
            return new BoundaryReport(List.of("UNSUPPORTED_TOOLCHAIN JDK compiler must support release " + release), Map.of());
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(errors, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            // -proc:none does not disable javac's autoStart compiler plugins. Keep consumer jars off that loader.
            files.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH,List.of());
            List<String> options = new ArrayList<>(List.of("-proc:none", "--release", "" + release, "-encoding", "UTF-8"));
            options.addAll(List.of("-classpath", classpath.stream().map(Path::toString).collect(Collectors.joining(java.io.File.pathSeparator))));
            JavacTask task = (JavacTask) compiler.getTask(null, files, errors, options, null, files.getJavaFileObjectsFromPaths(sources));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            task.analyze();
            List<String> diagnostics = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : errors.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) diagnostics.add("SOURCE_ANALYSIS_FAILED " + d.getSource() + ":" + d.getLineNumber() + " " + d.getMessage(Locale.ROOT));
            }
            if (!diagnostics.isEmpty()) return new BoundaryReport(diagnostics, Map.of());
            Trees trees = Trees.instance(task);
            Map<String, Node> nodes = new TreeMap<>();
            Map<String,String> implementationStructures=new TreeMap<>();
            Map<String,Set<String>> typeDependencies=new TreeMap<>();
            Set<String> externalUi = new TreeSet<>();
            Set<String> generatedRecordAccessors = new HashSet<>();
            for (CompilationUnitTree unit : units) {
                new TreePathScanner<Void, Node>() {
                    @Override public Void visitClass(ClassTree t, Node unused) {
                        Element e=trees.getElement(getCurrentPath());if(e instanceof TypeElement type){
                            String name=task.getElements().getBinaryName(type).toString();implementationStructures.put(name,t.toString());
                            TreePath parent=getCurrentPath().getParentPath();while(parent!=null&&!(parent.getLeaf() instanceof ClassTree))parent=parent.getParentPath();
                            if(parent!=null&&trees.getElement(parent) instanceof TypeElement owner)typeDependencies.computeIfAbsent(task.getElements().getBinaryName(owner).toString(),k->new TreeSet<>()).add(name);
                        }
                        return super.visitClass(t, null);
                    }
                    private void typeReference(Node node){
                        Element target=trees.getElement(getCurrentPath());
                        if (node != null && target instanceof VariableElement field
                                && (field.getKind() == ElementKind.FIELD || field.getKind() == ElementKind.ENUM_CONSTANT)) {
                            String owner = task.getElements().getBinaryName((TypeElement) field.getEnclosingElement()).toString();
                            Object constant = field.getConstantValue();
                            if (constant != null) {
                                var value = Base64.getEncoder().encodeToString(constant.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                node.constants.add(owner + "#" + field.getSimpleName() + "=" + value);
                            } else {
                                node.fields.add(fieldSymbol(owner, field.getSimpleName().toString()));
                            }
                        }
                        while(target!=null&&!(target instanceof TypeElement))target=target.getEnclosingElement();
                        if(!(target instanceof TypeElement dependency))return;
                        TreePath owner=getCurrentPath();while(owner!=null&&!(owner.getLeaf() instanceof ClassTree))owner=owner.getParentPath();
                        if(owner!=null&&trees.getElement(owner) instanceof TypeElement type)
                            typeDependencies.computeIfAbsent(task.getElements().getBinaryName(type).toString(),k->new TreeSet<>()).add(task.getElements().getBinaryName(dependency).toString());
                    }
                    @Override public Void visitIdentifier(IdentifierTree t,Node node){typeReference(node);return super.visitIdentifier(t,node);}
                    @Override public Void visitMemberSelect(MemberSelectTree t,Node node){typeReference(node);return super.visitMemberSelect(t,node);}
                    @Override public Void visitVariable(VariableTree tree, Node current) {
                        Element element = trees.getElement(getCurrentPath());
                        if (current == null && element instanceof VariableElement field
                                && (field.getKind() == ElementKind.FIELD || field.getKind() == ElementKind.ENUM_CONSTANT)) {
                            String owner = task.getElements().getBinaryName((TypeElement) field.getEnclosingElement()).toString();
                            String key = fieldSymbol(owner, field.getSimpleName().toString());
                            current = new Node(key, tree.toString(), false, unit.getSourceFile().getName() + ":"
                                    + unit.getLineMap().getLineNumber(trees.getSourcePositions().getStartPosition(unit, tree)));
                            nodes.put(key, current);
                        }
                        return super.visitVariable(tree, current);
                    }
                    @Override public Void visitMethod(MethodTree t, Node unused) {
                        Element e = trees.getElement(getCurrentPath());
                        if (!(e instanceof ExecutableElement method)) return null;
                        String key = symbol(method,task.getElements());
                        boolean host = !policy.hostTypes().contains(method.getEnclosingElement().toString())
                            && policy.hostCallbacks().contains(method.getSimpleName().toString())
                            && matches(method.getEnclosingElement().asType(), policy.hostTypes());
                        Node node = new Node(key, t.toString(), host, unit.getSourceFile().getName() + ":" + unit.getLineMap().getLineNumber(trees.getSourcePositions().getStartPosition(unit,t)));
                        if(host) node.categories.add("HOST_CALLBACK");
                        if(!method.getEnclosingElement().toString().equals("com.atom.lib.frontend.ContractPage")
                            &&matches(method.getEnclosingElement().asType(),Set.of("com.atom.lib.frontend.ContractPage")))node.categories.add("CONTRACT_PAGE");
                        Set<String> events=Set.of("net.minecraftforge.client.event.RenderGuiEvent.Post","net.neoforged.neoforge.client.event.RenderGuiEvent.Post",
                            "net.minecraftforge.client.event.RegisterGuiOverlaysEvent","net.neoforged.neoforge.client.event.RegisterGuiLayersEvent");
                        if(method.getParameters().stream().anyMatch(p->events.contains(p.asType().toString()))) node.categories.add("HUD_EVENT");
                        if(method.getAnnotationMirrors().stream().anyMatch(a->a.getAnnotationType().toString().startsWith("org.spongepowered.asm.mixin.injection."))) node.categories.add("INJECTION_CALLBACK");
                        if(matches(method.getEnclosingElement().asType(),Set.of("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback","net.minecraftforge.client.gui.overlay.IGuiOverlay","net.minecraft.client.gui.LayeredDraw.Layer"))) node.categories.add("HUD_CALLBACK");
                        nodes.put(key, node);
                        return super.visitMethod(t, node);
                    }
                    private boolean matches(TypeMirror type, Set<String> names) {
                        if(type.getKind().isPrimitive() || type.getKind()==javax.lang.model.type.TypeKind.VOID || type.getKind()==javax.lang.model.type.TypeKind.NONE || type.getKind()==javax.lang.model.type.TypeKind.NULL) return false;
                        if (names.contains(task.getTypes().erasure(type).toString())) return true;
                        return task.getTypes().directSupertypes(type).stream().anyMatch(t -> matches(t, names));
                    }
                    private void call(Node current) {
                        Element e = trees.getElement(getCurrentPath());
                        if (!(e instanceof ExecutableElement method)) {
                            diagnostics.add("UNRESOLVED_SYMBOL " + getCurrentPath().getLeaf()); return;
                        }
                        String key = symbol(method,task.getElements());
                        String declaring=method.getEnclosingElement().toString();
                        if(method.getEnclosingElement() instanceof TypeElement record && record.getKind()==ElementKind.RECORD
                                && trees.getTree(method)==null
                                && record.getRecordComponents().stream().anyMatch(c->c.getAccessor().equals(method))){
                            TreePath declaration=trees.getPath(record);
                            if(declaration!=null&&units.contains(declaration.getCompilationUnit()))generatedRecordAccessors.add(key);
                        }
                        boolean dsl=closedPageApi(declaring,method.getSimpleName().toString());
                        boolean rawCapability=Set.of("com.atom.lib.geometry.Rect","com.atom.lib.layout.LayoutStyle","com.atom.lib.motion.MotionSpec","com.atom.lib.frontend.contract.ContractCodec","com.atom.lib.frontend.contract.DesignResolver","com.atom.lib.frontend.StrictPageSession","com.atom.lib.frontend.StrictPageHost","com.atom.lib.controls.HostedControl").contains(declaring)
                            || declaring.equals("com.atom.lib.frontend.contract.ContractDesign")||declaring.startsWith("com.atom.lib.frontend.contract.ContractDesign.")
                            || (declaring.equals("com.atom.lib.frontend.contract.ContractRegistry")&&Set.of("load","manifest").contains(method.getSimpleName().toString()));
                        boolean drawing = rawCapability || matches(method.getEnclosingElement().asType(), policy.drawingTypes())
                            || (method.getKind()==ElementKind.CONSTRUCTOR && matches(method.getEnclosingElement().asType(),policy.hostTypes()))
                            || matches(method.getReturnType(),policy.hostTypes());
                        boolean sound=matches(method.getEnclosingElement().asType(),Set.of("net.minecraft.client.sound.SoundManager","net.minecraft.client.sounds.SoundManager","net.minecraft.client.sounds.SoundEngine","net.minecraft.client.sound.PositionedSoundInstance","net.minecraft.client.resources.sounds.SimpleSoundInstance","com.atom.lib.frontend.contract.UiSoundSink"));
                        if (drawing || sound || dsl || policy.trustedMethods().contains(key)) externalUi.add(key);
                        if (current == null) {
                            TreePath p = getCurrentPath();
                            while (p != null && !(p.getLeaf() instanceof ClassTree)) p = p.getParentPath();
                            String owner = p == null ? unit.getSourceFile().getName() : task.getElements().getBinaryName((TypeElement)trees.getElement(p)).toString();
                            TreePath member=getCurrentPath();
                            while(member.getParentPath()!=null && !(member.getParentPath().getLeaf() instanceof ClassTree)) member=member.getParentPath();
                            String suffix=member.getLeaf() instanceof VariableTree field ? field.getName().toString()
                                : "block-"+((ClassTree)member.getParentPath().getLeaf()).getMembers().indexOf(member.getLeaf());
                            current = nodes.computeIfAbsent(owner + "#<initializer:"+suffix+">", n -> new Node(n, pText(getCurrentPath()), false, unit.getSourceFile().getName()));
                        }
                        current.calls.add(key);
                        if (drawing) {current.drawing.add(key);current.categories.add("NATIVE_UI_CALL");}
                        if(policy.enforceImplementations()&&policy.trustedMethods().contains(key)&&!declaring.equals("com.atom.lib.frontend.StrictPage.Number")&&method.getParameters().stream().anyMatch(p->Set.of(javax.lang.model.type.TypeKind.BYTE,javax.lang.model.type.TypeKind.SHORT,javax.lang.model.type.TypeKind.INT,javax.lang.model.type.TypeKind.LONG,javax.lang.model.type.TypeKind.FLOAT,javax.lang.model.type.TypeKind.DOUBLE).contains(p.asType().getKind())||matches(p.asType(),policy.drawingTypes())||p.asType().toString().matches(".*(Canvas|PresentationFrame|Viewport|LayoutStyle|[.$]Rect)(<.*>)?")))current.rawInputs.add(key);
                        if(sound) {current.sound.add(key);current.categories.add("SOUND_OUTPUT");}
                        if(getCurrentPath().getLeaf() instanceof MethodInvocationTree invocation && invocation.getMethodSelect() instanceof MemberSelectTree select) {
                            TypeMirror receiver=trees.getTypeMirror(new TreePath(new TreePath(getCurrentPath(),select),select.getExpression()));
                            if(method.getSimpleName().contentEquals("register") && carries(receiver,"net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback")) current.categories.add("HUD_REGISTRATION");
                        }
                        String owner = method.getEnclosingElement().toString();
                        if (owner.startsWith("java.lang.reflect.") || owner.equals("java.lang.Class") || owner.endsWith("ClassLoader") || owner.startsWith("java.lang.invoke.")) current.unsupported.add(key);
                        if(method.getModifiers().contains(Modifier.ABSTRACT) && !dsl&&!policy.trustedMethods().contains(key)&&!policy.businessMethods().contains(key)) current.unsupported.add(key);
                    }
                    @Override public Void visitMethodInvocation(MethodInvocationTree t, Node node) { call(node); return super.visitMethodInvocation(t,node); }
                    @Override public Void visitNewClass(NewClassTree t, Node node) { call(node); return super.visitNewClass(t,node); }
                    @Override public Void visitMemberReference(MemberReferenceTree t, Node node) { call(node); return super.visitMemberReference(t,node); }
                    private boolean carries(TypeMirror type,String name) {
                        if(type==null) return false;
                        if(matches(type,Set.of(name))) return true;
                        return type instanceof javax.lang.model.type.DeclaredType declared && declared.getTypeArguments().stream().anyMatch(t->carries(t,name));
                    }
                }.scan(unit,null);
            }
            for(String binding:policy.compositions().keySet()){
                Node node=nodes.get(binding);
                if(node!=null)node.categories.add("HOST_COMPOSITION");
            }
            for (String binding : policy.dispatch().keySet()) {
                Node node = nodes.get(binding);
                if (node != null) node.categories.add("HOST_DISPATCH");
            }
            Set<String> ui = new TreeSet<>(externalUi);
            nodes.values().stream().filter(n -> !n.categories.isEmpty()).forEach(n -> ui.add(n.key));
            boolean changed;
            do {
                changed = false;
                for (Node n : nodes.values()) if (n.calls.stream().anyMatch(ui::contains)) changed |= ui.add(n.key);
            } while (changed);
            Map<String,String> structures = new TreeMap<>();
            for (Node n : nodes.values()) {
                if (ui.contains(n.key) && !policy.trustedMethods().contains(n.key)) structures.put(n.key,n.structure);
            }
            Set<String> strictClosure = new HashSet<>();
            for(Node n:nodes.values()) for(String call:n.calls) {
                if(!nodes.containsKey(call) && !generatedRecordAccessors.contains(call)
                    && !policy.trustedMethods().contains(call)&&!policy.businessMethods().contains(call) && !n.drawing.contains(call)
                    && !call.startsWith("java.") && !call.startsWith("javax.")&&!closedPageSymbol(call)) n.unsupported.add(call);
            }
            Set<String> strictEntries=new TreeSet<>(policy.strictEntries());
            nodes.values().stream().filter(n->n.categories.contains("CONTRACT_PAGE")).forEach(n->strictEntries.add(n.key));
            for (String entry : strictEntries) {
                if (!nodes.containsKey(entry)) diagnostics.add("MISSING_ENTRY " + entry);
                inspectStrict(entry, nodes, policy, strictClosure, diagnostics, entry);
            }
            for (Map.Entry<String,String> entry : structures.entrySet()) {
                Node n = nodes.get(entry.getKey());
                if (strictClosure.contains(n.key)) continue;
                if (policy.compositions().containsKey(n.key) || policy.dispatch().containsKey(n.key)) continue;
                String owner=n.key.substring(0,n.key.indexOf('#'));
                if(policy.enforceImplementations()&&policy.implementations().containsKey(owner)&&sameStructure(policy.implementations().get(owner),implementationStructures.get(owner)))continue;
                String old = policy.legacy().get(n.key);
                if (old == null) diagnostics.add("UNCOVERED_ENTRY " + n.key + " at " + n.location + " register a strict entry or review legacy scope");
                else if (!sameStructure(old, entry.getValue())) diagnostics.add("LEGACY_UI_CHANGED " + n.key + " at " + n.location + " reviewed method structure differs");
            }
            for (var entry : policy.legacy().entrySet()) {
                String symbol = entry.getKey();
                if (policy.businessMethods().contains(symbol)) {
                    // A deployed data port can be part of the exact legacy inventory
                    // even if its body never reaches a drawing method.
                    Node node = nodes.get(symbol);
                    if (node == null || !sameStructure(entry.getValue(), node.structure)) {
                        diagnostics.add("LEGACY_DATA_CHANGED " + symbol + " reviewed method structure differs");
                    }
                } else if (!structures.containsKey(symbol) && !policy.strictEntries().contains(symbol)) {
                    diagnostics.add("LEGACY_UI_CHANGED " + symbol + " removed or moved entry needs classification");
                }
            }
            // The existing mixed-route switch stays in the host. Its attributed body
            // is exact, but its sibling branches stay in the ordinary legacy inventory.
            // Dispatch is deliberately unavailable as a dependency of a selected composition.
            for (var binding : policy.dispatch().entrySet()) {
                Node node = nodes.get(binding.getKey());
                if (node == null) {
                    diagnostics.add("MISSING_HOST_DISPATCH " + binding.getKey());
                } else if (!sameStructure(binding.getValue(), BoundaryReport.compositionSnapshot(
                        node.structure, node.calls, node.constants, node.fields))) {
                    diagnostics.add("HOST_DISPATCH_CHANGED " + node.key + " at " + node.location);
                }
            }
            for(var binding:policy.compositions().entrySet()){
                Node node=nodes.get(binding.getKey());
                if(node==null){diagnostics.add("MISSING_COMPOSITION "+binding.getKey());continue;}
                if(!sameStructure(binding.getValue(), BoundaryReport.compositionSnapshot(node.structure,node.calls,node.constants,node.fields))){
                    diagnostics.add("COMPOSITION_CHANGED "+node.key+" at "+node.location);
                }
                for(String call:node.compositionDependencies()){
                    Node local=nodes.get(call);
                    if (policy.dispatch().containsKey(call)) {
                        diagnostics.add("COMPOSITION_DISPATCH_BYPASS " + node.key + " -> " + call);
                        continue;
                    }
                    if(local!=null&&!policy.compositions().containsKey(call)
                            &&!policy.implementations().containsKey(call.substring(0,call.indexOf('#')))
                            &&!strictClosure.contains(call)&&!(policy.businessMethods().contains(call)&&policy.legacy().containsKey(call))){
                        diagnostics.add("COMPOSITION_DEPENDENCY_UNBOUND "+node.key+" -> "+call);
                    }
                }
            }
            Map<String,Set<String>> categories=new TreeMap<>(),dependencies=new TreeMap<>();
            if(policy.enforceImplementations()){
                Set<String> closure=new TreeSet<>(policy.implementations().keySet());
                for(var trusted:policy.trustedMethods()){int split=trusted.indexOf('#');if(split>0)closure.add(trusted.substring(0,split));}
                Deque<String> pending=new ArrayDeque<>(closure);
                while(!pending.isEmpty()){String owner=pending.remove();for(String dependency:typeDependencies.getOrDefault(owner,Set.of()))if(implementationStructures.containsKey(dependency)&&closure.add(dependency))pending.add(dependency);}
                for(String owner:closure)if(implementationStructures.containsKey(owner)&&!policy.implementations().containsKey(owner))diagnostics.add("MISSING_IMPLEMENTATION_BINDING "+owner+" trusted implementation dependency requires reviewed complete source structure");
                policy.implementations().forEach((id,ast)->{String actual=implementationStructures.get(id);if(actual==null)diagnostics.add("MISSING_IMPLEMENTATION "+id+" expected reviewed implementation source");else if(!sameStructure(ast, actual))diagnostics.add("IMPLEMENTATION_CHANGED "+id+" actual implementation/fields differ from reviewed semantic binding");});
            }
            Map<String,String> compositionSnapshots=new TreeMap<>();
            for (Node node : nodes.values()) {
                compositionSnapshots.put(node.key, BoundaryReport.compositionSnapshot(node.structure,
                        node.calls, node.constants, node.fields));
                if (structures.containsKey(node.key)) {
                    categories.put(node.key, node.categories.isEmpty() ? Set.of("UI_DEPENDENCY") : node.categories);
                    dependencies.put(node.key, policy.compositions().containsKey(node.key)
                            ? node.compositionDependencies() : node.calls);
                }
            }
            return new BoundaryReport(diagnostics,structures,categories,dependencies,implementationStructures,typeDependencies,compositionSnapshots);
        }
    }

    private static boolean sameStructure(String reviewed, String actual) {
        if (reviewed.equals(actual)) return true;
        // Javac's pretty printer uses the host line separator. Compare the AST
        // text portably while preserving the original reviewed package bytes.
        return actual != null && reviewed.replace("\r\n", "\n").equals(actual.replace("\r\n", "\n"));
    }

    private static boolean closedPageSymbol(String symbol){int split=symbol.indexOf('#'),end=symbol.indexOf('(',split);return split>0&&end>split&&closedPageApi(symbol.substring(0,split).replace('$','.'),symbol.substring(split+1,end));}
    /** Only the closed business DSL is intrinsic. Hosts, registries, style models and renderers remain privileged. */
    private static boolean closedPageApi(String owner,String method){
        owner=owner.replace('$','.');
        if(owner.equals("com.atom.lib.frontend.ContractPage"))return method.equals("<init>");
        if(owner.equals("com.atom.lib.frontend.StrictPage"))return method.equals("builder");
        if(owner.equals("com.atom.lib.frontend.StrictPage.Builder"))return Set.of("component","group","build").contains(method);
        if(owner.equals("com.atom.lib.frontend.contract.ContractRegistry.PageHandle"))return method.equals("recipe");
        if(method.equals("<init>"))for(String item:Set.of("Text","Number","Image","BooleanValue","TextInput","NumberInput","BooleanInput","Action","Content"))if(owner.equals("com.atom.lib.frontend.StrictPage."+item))return true;
        return false;
    }

    private static String pText(TreePath p) {
        // Retain the entire field/initializer block, not just the first call.
        while (p.getParentPath() != null && !(p.getParentPath().getLeaf() instanceof ClassTree)) p = p.getParentPath();
        return p.getLeaf().toString();
    }
    private static void inspectStrict(String key, Map<String,Node> nodes, BoundaryPolicy policy,
                                      Set<String> visited, List<String> diagnostics, String root) {
        if (policy.trustedMethods().contains(key) || !visited.add(key)) return;
        Node node = nodes.get(key);
        if (node == null) return;
        for (String call : node.drawing) diagnostics.add("PAGE_DRAWING " + root + " -> " + node.key + " -> " + call + " at " + node.location + " use an approved component");
        for (String call : node.sound) diagnostics.add("PAGE_SOUND_OUTPUT " + root + " -> " + node.key + " -> " + call + " use an approved feedback binding");
        for(String call:node.rawInputs)diagnostics.add("UNSAFE_COMPONENT_INPUT "+root+" -> "+call+" strict pages use typed content slots and handles, not raw numeric or drawing inputs");
        for (String call : node.unsupported) diagnostics.add("UNSUPPORTED_DYNAMIC_UI " + root + " -> " + call);
        for (String call : node.calls) inspectStrict(call,nodes,policy,visited,diagnostics,root);
    }
    static String symbol(ExecutableElement e,javax.lang.model.util.Elements elements) {
        return elements.getBinaryName((TypeElement)e.getEnclosingElement()) + "#" + e.getSimpleName() + "(" + e.getParameters().stream().map(p -> p.asType().toString()).collect(Collectors.joining(",")) + ")";
    }
    private static String fieldSymbol(String owner, String field) {
        return owner + "#<initializer:" + field + ">";
    }
    private static final class Node {
        final String key, structure, location;
        final boolean host;
        final Set<String> calls = new TreeSet<>(), drawing = new TreeSet<>(), unsupported = new TreeSet<>();
        final Set<String> categories=new TreeSet<>();
        final Set<String> sound=new TreeSet<>();
        final Set<String> rawInputs=new TreeSet<>();
        final Set<String> constants=new TreeSet<>(), fields=new TreeSet<>();
        Set<String> compositionDependencies() {
            var symbols = new TreeSet<>(calls);
            symbols.addAll(fields);
            return symbols;
        }
        Node(String key, String structure, boolean host, String location) {
            this.key=key; this.structure=structure; this.host=host; this.location=location;
        }
    }
}
