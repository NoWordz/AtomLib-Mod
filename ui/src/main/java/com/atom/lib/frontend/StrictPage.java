package com.atom.lib.frontend;

import com.atom.lib.frontend.contract.*;
import java.util.*;
import java.util.function.Consumer;

/** Page authors supply business content and approved recipe handles, never geometry or paint. */
public final class StrictPage {
    public sealed interface Item permits Text,Number,Image,Action,BooleanValue,TextInput,NumberInput,BooleanInput {}
    public record Text(String value) implements Item {public Text{Objects.requireNonNull(value);}}
    public record Number(double value) implements Item {public Number{if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid business number");}}
    public record Image(String contentHandle) implements Item {public Image{Objects.requireNonNull(contentHandle);}}
    public record BooleanValue(boolean value) implements Item {}
    public record TextInput(String value,java.util.function.Predicate<String> commit) implements Item {public TextInput{Objects.requireNonNull(value);Objects.requireNonNull(commit);}}
    public record NumberInput(double value,java.util.function.DoublePredicate commit) implements Item {public NumberInput{if(!Double.isFinite(value))throw new IllegalArgumentException("Invalid business number");Objects.requireNonNull(commit);}}
    public record BooleanInput(boolean value,java.util.function.Predicate<Boolean> commit) implements Item {public BooleanInput{Objects.requireNonNull(commit);}}
    public record Action(java.util.function.BooleanSupplier commit) implements Item {
        public Action{Objects.requireNonNull(commit);}
        public Action(Runnable action){this(accepted(action));}
        private static java.util.function.BooleanSupplier accepted(Runnable action){Objects.requireNonNull(action);return ()->{action.run();return true;};}
    }
    public record Content(Map<String,Item> slots,boolean enabled) {public Content{slots=Map.copyOf(slots);}}
    public sealed interface Element permits Entry,Group {}
    public record Entry(String id,ContractRegistry.RecipeHandle recipe,Content content) implements Element {}
    public record Group(String id,ContractRegistry.RecipeHandle recipe,List<Element> children) implements Element {
        public Group { children=List.copyOf(children); }
    }
    private final ContractRegistry.PageHandle page;
    private final List<Element> elements;
    private final List<Entry> entries;
    private final String scrollGroup;
    private StrictPage(ContractRegistry.PageHandle page,List<Element> elements){
        this.page=page;
        this.elements=List.copyOf(elements);
        List<Entry> leaves=new ArrayList<>();
        scrollGroup=collect(this.elements,leaves,true);
        entries=List.copyOf(leaves);
    }
    private String collect(List<Element> elements,List<Entry> leaves,boolean directChild){
        String scroll=null;
        for(Element element:elements){
            if(element instanceof Entry component)leaves.add(component);
            else {
                Group group=(Group)element;
                if(page.groupLayout(group.recipe(),Map.of()).scroll()){
                    if(!directChild||scroll!=null||page.layout(Map.of()).axis()!=ContractDesign.Axis.COLUMN){
                        throw new IllegalArgumentException("INVALID_LAYOUT one scroll group can be placed at the top level of a column page: "+group.id());
                    }
                    scroll=group.id();
                }
                collect(group.children(),leaves,false);
            }
        }
        return scroll;
    }
    public ContractRegistry.PageHandle contract(){return page;}
    public List<Element> elements(){return elements;}
    public List<Entry> entries(){return entries;}
    String scrollGroup(){return scrollGroup;}
    void requireSameStructure(StrictPage next) {
        page.requirePage(next.contract());
        requireSameElements(elements, next.elements);
    }

    private static void requireSameElements(List<Element> current, List<Element> next) {
        if (current.size() != next.size()) throw new IllegalArgumentException("PAGE_STRUCTURE_CHANGED");
        for (int i = 0; i < current.size(); i++) {
            var before = current.get(i);
            var after = next.get(i);
            if (before instanceof Entry entry && after instanceof Entry replacement
                    && entry.id().equals(replacement.id()) && entry.recipe().id().equals(replacement.recipe().id())) {
                continue;
            }
            if (before instanceof Group group && after instanceof Group replacement
                    && group.id().equals(replacement.id()) && group.recipe().id().equals(replacement.recipe().id())) {
                requireSameElements(group.children(), replacement.children());
                continue;
            }
            throw new IllegalArgumentException("PAGE_STRUCTURE_CHANGED at index " + i);
        }
    }

    public static Builder builder(ContractRegistry.PageHandle page){return new Builder(Objects.requireNonNull(page));}
    static void validate(ContractRegistry.RecipeHandle recipe,Content content){
        Objects.requireNonNull(content);
        if(!recipe.slots().keySet().containsAll(content.slots().keySet()))throw new IllegalArgumentException("CONTENT_PURPOSE recipe="+recipe.id()+" expected="+recipe.slots().keySet()+" actual="+content.slots().keySet());
        recipe.slots().forEach((name,slot)->{Item item=content.slots().get(name);if(item==null){if(slot.required())throw new IllegalArgumentException("MISSING_CONTENT recipe="+recipe.id()+" slot="+name);return;}
            boolean valid=switch(slot.kind()){case TEXT->item instanceof Text||item instanceof TextInput;case BUSINESS_IMAGE->item instanceof Image;case ACTION->item instanceof Action;case NUMBER->item instanceof Number||item instanceof NumberInput;case BOOLEAN->item instanceof BooleanValue||item instanceof BooleanInput;};
            if(!valid)throw new IllegalArgumentException("CONTENT_PURPOSE recipe="+recipe.id()+" slot="+name+" expected="+slot.kind()+" actual="+item.getClass().getSimpleName());
        });
    }
    public static final class Builder {
        private final ContractRegistry.PageHandle page;
        private final List<Element> elements=new ArrayList<>();
        private final Set<String> ids=new HashSet<>();
        private Builder(ContractRegistry.PageHandle page){this.page=page;}
        private void requireId(String id){
            if(id==null||!id.matches("[a-zA-Z0-9_.-]+")||id.equals("root")||ids.contains(id)){
                throw new IllegalArgumentException("DUPLICATE_OR_RESERVED_COMPONENT page="+page.id()+" id="+id);
            }
        }
        public Builder component(String id,ContractRegistry.RecipeHandle recipe,Content content){
            requireId(id);
            page.require(recipe);
            if(recipe.structuralGroup())throw new IllegalArgumentException("ROLE_CAPABILITY group recipe needs children: "+recipe.id());
            validate(recipe,content);
            ids.add(id);
            elements.add(new Entry(id,recipe,content));
            return this;
        }
        /** The recipe owns the nested layout. The page only supplies business contents in order. */
        public Builder group(String id,ContractRegistry.RecipeHandle recipe,Consumer<Builder> contents){
            requireId(id);
            page.groupLayout(recipe,Map.of());
            Builder children=new Builder(page);
            contents.accept(children);
            if(children.elements.isEmpty())throw new IllegalArgumentException("MISSING_EVIDENCE empty group "+id);
            requireId(id);
            for(String child:children.ids){
                requireId(child);
                if(id.equals(child))throw new IllegalArgumentException("DUPLICATE_OR_RESERVED_COMPONENT group="+id);
            }
            ids.add(id);
            ids.addAll(children.ids);
            elements.add(new Group(id,recipe,children.elements));
            return this;
        }
        public StrictPage build(){
            if(elements.isEmpty())throw new IllegalArgumentException("MISSING_EVIDENCE page="+page.id()+" has no component instances");
            return new StrictPage(page,elements);
        }
    }
}
