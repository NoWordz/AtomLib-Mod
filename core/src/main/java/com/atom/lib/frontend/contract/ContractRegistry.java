package com.atom.lib.frontend.contract;

import java.util.*;
import java.io.*;
import static com.atom.lib.frontend.contract.ContractDesign.*;

/** Immutable project/design identity; handles cannot be constructed or transferred by ID. */
public final class ContractRegistry {
    private final ContractDesign design;
    private ContractRegistry(ContractDesign design){this.design=design;}
    /** Build adapter must obtain reviewed bytes from its external approved version source. */
    public static byte[] manifest(byte[] candidate,byte[] reviewed){
        ContractCodec.decode(candidate);ContractCodec.decode(reviewed);
        if(!Arrays.equals(candidate,reviewed))throw new IllegalArgumentException("PENDING_DESIGN_REVIEW candidate differs from approved design");
        try{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){out.writeInt(0x46434d01);out.writeInt(reviewed.length);out.write(reviewed);}return bytes.toByteArray();}catch(IOException e){throw new UncheckedIOException(e);}
    }
    public static ContractRegistry load(byte[] design,byte[] manifest,Set<String> profiles){
        if(manifest.length>4_194_312)throw new IllegalArgumentException("INVALID_MANIFEST size");
        ContractDesign decoded=ContractCodec.decode(design);
        try(var in=new DataInputStream(new ByteArrayInputStream(manifest))){if(in.readInt()!=0x46434d01||in.readInt()!=design.length||!Arrays.equals(in.readNBytes(design.length),design)||in.read()!=-1)throw new IOException("manifest and packaged design differ");}
        catch(IOException e){throw new IllegalArgumentException("MISSING_EVIDENCE approved build manifest: "+e.getMessage(),e);}
        if(!profiles.contains(decoded.profile()))throw new IllegalArgumentException("INCOMPATIBLE_PROFILE "+decoded.profile());MechanismIdentity.require(decoded);return new ContractRegistry(decoded);
    }
    public String namespace(){return design.namespace();}
    public String version(){return design.version();}
    public String profile(){return design.profile();}
    public PageHandle page(String id){Page p=design.pages().get(id);if(p==null)throw new IllegalArgumentException("UNKNOWN_PAGE "+id);return new PageHandle(id,p);}
    public Map<String,Double> parameters(Map<String,Double> input){
        if(!design.parameters().keySet().containsAll(input.keySet()))throw new IllegalArgumentException("UNKNOWN_PARAMETER "+input.keySet());
        Map<String,Double> result=new HashMap<>();design.parameters().forEach((id,p)->{double v=input.getOrDefault(id,p.initial());if(!Double.isFinite(v)||v<p.minimum()||v>p.maximum())throw new IllegalArgumentException("PARAMETER_RANGE "+id);result.put(id,v);});return Map.copyOf(result);
    }
    public record ResolvedLayout(Axis axis,float gap,float padding,float minimumWidth,float maximumWidth) {}
    public record BoundSound(String event,String resource,String channel,float volume,float pitch,int priority) {}
    private float unit(Ref ref,Map<String,Double> parameters){NumberValue n=DesignResolver.number(design,ref,parameters);if(n.unit()!=Unit.U||!Double.isFinite(n.value())||n.value()<0||n.value()>Float.MAX_VALUE)throw new IllegalArgumentException("UNIT_MISMATCH expected finite nonnegative logical u: "+ref);return (float)n.value();}
    public final class PageHandle {
        private final String id;private final Page page;
        private PageHandle(String id,Page page){this.id=id;this.page=page;}
        public String id(){return id;}
        public String namespace(){return design.namespace();}
        public String version(){return design.version();}
        public String profile(){return design.profile();}
        private ContractRegistry registry(){return ContractRegistry.this;}
        /** A host fixes the full page identity, not just a recipe namespace or a caller-supplied ID. */
        public void requirePage(PageHandle actual){
            if(actual==null||actual.registry()!=ContractRegistry.this)throw new IllegalArgumentException("FOREIGN_PAGE_HANDLE expected="+id);
            if(!id.equals(actual.id))throw new IllegalArgumentException("UNAPPROVED_PAGE expected="+id+" actual="+actual.id);
        }
        public Set<String> scenarios(){return page.scenarios();}
        public Set<String> namedStates(){return design.states();}
        public Map<String,Double> parameters(Map<String,Double> input){return ContractRegistry.this.parameters(input);}
        public RecipeHandle recipe(String id){if(!page.recipes().contains(id))throw new IllegalArgumentException("UNKNOWN_RECIPE page="+this.id+" actual="+id+" expected="+page.recipes());return new RecipeHandle(id,design.recipes().get(id));}
        public void require(RecipeHandle handle){if(handle==null||handle.registry()!=ContractRegistry.this)throw new IllegalArgumentException("FOREIGN_HANDLE page="+id);if(!page.recipes().contains(handle.id))throw new IllegalArgumentException("UNAPPROVED_RECIPE page="+id+" actual="+handle.id);}
        public ResolvedLayout layout(Map<String,Double> input){Map<String,Double> p=parameters(input);Layout l=design.layouts().get(page.layout());float min=unit(l.minimumWidth(),p),max=unit(l.maximumWidth(),p);if(min>max)throw new IllegalArgumentException("LAYOUT_RANGE min exceeds max");return new ResolvedLayout(l.axis(),unit(l.gap(),p),unit(l.padding(),p),min,max);}
    }
    public final class RecipeHandle {
        private final String id;private final Recipe recipe;
        private RecipeHandle(String id,Recipe recipe){this.id=id;this.recipe=recipe;}
        private ContractRegistry registry(){return ContractRegistry.this;}
        public String id(){return id;}
        public Binding implementation(){return recipe.implementation();}
        public Map<String,Slot> slots(){return recipe.slots();}
        public Role role(){return design.roles().get(recipe.role());}
        public Set<String> scenarios(){return recipe.scenarios();}
        public Optional<BoundSound> sound(String event,Map<String,Double> input,Map<String,Boolean> states){
            Ref ref=recipe.bindings().get(Aspect.SOUND);if(ref==null)throw new IllegalArgumentException("ROLE_CAPABILITY sound not declared for recipe="+id);
            Value value=DesignResolver.value(design,ref);if(!(value instanceof SoundValue sound))throw new IllegalArgumentException("INVALID_SOUND_BINDING recipe="+id);
            if(!sound.event().equals(event))throw new IllegalArgumentException("SOUND_EVENT_BINDING recipe="+id+" expected="+sound.event()+" actual="+event);
            if(!design.states().containsAll(states.keySet()))throw new IllegalArgumentException("UNKNOWN_STATE "+states.keySet());
            Map<String,Double> parameters=parameters(input);var requirement=role().requirements().get(Aspect.SOUND);
            if(requirement.rule()==Rule.CONDITIONAL&&!DesignResolver.evaluate(requirement.condition(),states))return Optional.empty();
            if(sound.asset().isEmpty()||(sound.when()!=null&&!DesignResolver.evaluate(sound.when(),states)))return Optional.empty();
            float volume=parameters.get(sound.volumeParameter()).floatValue();if(volume==0)return Optional.empty();
            return Optional.of(new BoundSound(event,design.assets().get(sound.asset()).resource(),sound.channel(),volume,(float)sound.pitch(),sound.priority()));
        }
        public Map<Aspect,Value> bindings(Map<String,Double> input){Map<String,Double> p=parameters(input);Map<Aspect,Value> values=new EnumMap<>(Aspect.class);recipe.bindings().forEach((aspect,ref)->{Value v=DesignResolver.value(design,ref);values.put(aspect,v instanceof ParameterValue?DesignResolver.number(design,ref,p):v);});return Map.copyOf(values);}
        /** Conditional capabilities are evaluated against the frame's actual named state, never a page predicate. */
        public Map<Aspect,Value> bindings(Map<String,Double> input,Map<String,Boolean> states){
            if(!design.states().containsAll(states.keySet()))throw new IllegalArgumentException("UNKNOWN_STATE "+states.keySet());
            Map<Aspect,Value> values=new EnumMap<>(Aspect.class);values.putAll(bindings(input));
            role().requirements().forEach((aspect,rule)->{if(rule.rule()==Rule.CONDITIONAL&&!DesignResolver.evaluate(rule.condition(),states))values.remove(aspect);});return Map.copyOf(values);
        }
    }
}
